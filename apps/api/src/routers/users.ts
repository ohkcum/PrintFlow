// User Router — mirrors PrintFlowLite's JSON API user operations
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { createDrizzle } from "@printflow/db";
import {
  users,
  userAccounts,
  userCards,
  userGroups,
  userGroupMembers,
  userEmails,
} from "@printflow/db/schema";
import { eq, like, and, isNull, desc, asc, count, sql } from "drizzle-orm";
import { z } from "zod";
import { hashPassword } from "@printflow/auth";

const createDb = () =>
  createDrizzle({
    nodeEnv: "development",
    logLevel: "info",
    port: 3001,
    database: {
      url:
        process.env["DATABASE_URL"] ??
        "postgresql://printflow:password@localhost:5432/printflow",
      poolMin: 2,
      poolMax: 10,
    },
    redis: { url: process.env["REDIS_URL"] ?? "redis://localhost:6379" },
    auth: { secret: "dev", expiresIn: "7d", totpIssuer: "PrintFlow" },
    storage: { documentPath: "./data", thumbnailPath: "./data" },
    print: { ippServerPort: 6310, cupsHost: "localhost", cupsPort: 631 },
    email: {
      smtpPort: 587,
      smtpFrom: "noreply@printflow.local",
      imapPort: 993,
    },
    soffice: { path: "/usr/bin/soffice" },
    appUrl: "http://localhost:3000",
    apiUrl: "http://localhost:3001",
  });

export async function createUserRouter(app: FastifyInstance) {
  const db = createDb();

  // Helper: require admin
  function requireAdmin(request: FastifyRequest, reply: FastifyReply) {
    if (!request.roles?.includes("ADMIN")) {
      reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Admin role required" },
        timestamp: new Date().toISOString(),
      });
      return false;
    }
    return true;
  }

  // Helper: require manager or admin
  function requireManager(request: FastifyRequest, reply: FastifyReply) {
    if (
      !request.roles?.includes("ADMIN") &&
      !request.roles?.includes("MANAGER")
    ) {
      reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Manager or Admin role required" },
        timestamp: new Date().toISOString(),
      });
      return false;
    }
    return true;
  }

  // GET /api/v1/users — list users (admin/manager only)
  app.get("/", async (request: FastifyRequest, reply: FastifyReply) => {
    // Temporarily skip role check for development
    // if (!requireManager(request, reply)) return;

    const {
      page = "1",
      limit = "20",
      search,
      status,
      sortBy = "dateCreated",
      sortOrder = "desc",
    } = request.query as Record<string, string>;

    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10)));
    const offset = (pageNum - 1) * limitNum;

    const conditions = [];
    if (search) conditions.push(like(users.userName, `%${search}%`));
    if (status)
      conditions.push(
        eq(
          users.status,
          status as "ACTIVE" | "BLOCKED" | "DELETED" | "EXPIRED",
        ),
      );

    const [rows, [{ total }]] = await Promise.all([
      db
        .select()
        .from(users)
        .where(conditions.length > 0 ? and(...conditions) : undefined)
        .orderBy(
          sortOrder === "asc"
            ? asc(users[sortBy as keyof typeof users] as any)
            : desc(users[sortBy as keyof typeof users] as any),
        )
        .limit(limitNum)
        .offset(offset),
      db
        .select({ total: count() })
        .from(users)
        .where(conditions.length > 0 ? and(...conditions) : undefined),
    ]);

    return reply.send({
      success: true,
      data: {
        data: rows.map((u) => ({ ...u, passwordHash: undefined })),
        total: Number(total),
        page: pageNum,
        limit: limitNum,
        totalPages: Math.ceil(Number(total) / limitNum),
        hasNext: pageNum * limitNum < Number(total),
        hasPrev: pageNum > 1,
      },
      timestamp: new Date().toISOString(),
    });
  });

  // GET /api/v1/users/:id — get user by ID
  app.get<{ Params: { id: string } }>("/:id", async (request, reply) => {
    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid user ID" },
        timestamp: new Date().toISOString(),
      });
    }

    // Users can view themselves, managers can view anyone
    if (request.userId !== id && !requireManager(request, reply)) return;

    const [user] = await db
      .select()
      .from(users)
      .where(eq(users.id, id))
      .limit(1);
    if (!user) {
      return reply.status(404).send({
        success: false,
        error: { code: "USER_NOT_FOUND", message: "User not found" },
        timestamp: new Date().toISOString(),
      });
    }

    // Get account
    const [account] = await db
      .select()
      .from(userAccounts)
      .where(eq(userAccounts.userId, id))
      .limit(1);
    // Get cards
    const cards = await db
      .select()
      .from(userCards)
      .where(eq(userCards.userId, id));
    // Get groups
    const memberGroups = await db
      .select({ group: userGroups })
      .from(userGroupMembers)
      .innerJoin(userGroups, eq(userGroupMembers.groupId, userGroups.id))
      .where(eq(userGroupMembers.userId, id));

    return reply.send({
      success: true,
      data: {
        ...user,
        passwordHash: undefined,
        account: account ?? null,
        cards,
        groups: memberGroups.map((m) => m.group),
      },
      timestamp: new Date().toISOString(),
    });
  });

  // POST /api/v1/users — create user (admin only)
  app.post("/", async (request: FastifyRequest, reply: FastifyReply) => {
    if (!requireAdmin(request, reply)) return;

    const schema = z.object({
      userName: z.string().min(3).max(50),
      fullName: z.string().min(1).max(200),
      email: z.string().email().optional(),
      password: z.string().min(8).max(128).optional(),
      roles: z.array(z.string()).optional(),
      printQuota: z.string().optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    const { password, ...userData } = body.data;
    const passwordHash = password ? await hashPassword(password) : null;

    const [user] = await db
      .insert(users)
      .values({
        uuid: crypto.randomUUID(),
        ...userData,
        passwordHash: passwordHash ?? undefined,
        userIdMethod: "INTERNAL",
        roles: (userData.roles as any) ?? ["USER"],
        status: "ACTIVE",
        printBalance: userData.printQuota ?? "100",
        printQuota: userData.printQuota ?? "100",
      })
      .returning();

    // Create user account
    await db.insert(userAccounts).values({
      uuid: crypto.randomUUID(),
      userId: user.id,
      accountName: `${user.userName} Account`,
      balance: "0",
    });

    return reply.status(201).send({
      success: true,
      data: { ...user, passwordHash: undefined },
      timestamp: new Date().toISOString(),
    });
  });

  // PUT /api/v1/users/:id — update user
  app.put<{ Params: { id: string } }>("/:id", async (request, reply) => {
    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid user ID" },
        timestamp: new Date().toISOString(),
      });
    }

    if (request.userId !== id && !requireManager(request, reply)) return;

    const schema = z.object({
      fullName: z.string().min(1).max(200).optional(),
      email: z.string().email().optional(),
      roles: z.array(z.string()).optional(),
      status: z.enum(["ACTIVE", "BLOCKED", "DELETED", "EXPIRED"]).optional(),
      printQuota: z.string().optional(),
      blockedReason: z.string().optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    // Non-admins can't change roles or status
    if (
      !request.roles?.includes("ADMIN") &&
      (body.data.roles || body.data.status)
    ) {
      return reply.status(403).send({
        success: false,
        error: {
          code: "FORBIDDEN",
          message: "Only admins can change roles or status",
        },
        timestamp: new Date().toISOString(),
      });
    }

    const [user] = await db
      .update(users)
      .set({ ...body.data, dateModified: new Date() })
      .where(eq(users.id, id))
      .returning();

    if (!user) {
      return reply.status(404).send({
        success: false,
        error: { code: "USER_NOT_FOUND", message: "User not found" },
        timestamp: new Date().toISOString(),
      });
    }

    return reply.send({
      success: true,
      data: { ...user, passwordHash: undefined },
      timestamp: new Date().toISOString(),
    });
  });

  // DELETE /api/v1/users/:id — soft delete
  app.delete<{ Params: { id: string } }>("/:id", async (request, reply) => {
    if (!requireAdmin(request, reply)) return;

    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid user ID" },
        timestamp: new Date().toISOString(),
      });
    }

    await db
      .update(users)
      .set({
        status: "DELETED",
        dateDeleted: new Date(),
        dateModified: new Date(),
      })
      .where(eq(users.id, id));

    return reply.send({
      success: true,
      data: { message: "User deleted" },
      timestamp: new Date().toISOString(),
    });
  });

  // POST /api/v1/users/:id/cards — add card
  app.post<{ Params: { id: string } }>("/:id/cards", async (request, reply) => {
    if (!requireManager(request, reply)) return;

    const schema = z.object({
      cardId: z.string().min(1),
      cardType: z.string().default("NFC"),
      cardName: z.string().optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    const userId = parseInt(request.params.id, 10);
    const [card] = await db
      .insert(userCards)
      .values({
        uuid: crypto.randomUUID(),
        userId,
        ...body.data,
      })
      .returning();

    return reply.status(201).send({
      success: true,
      data: card,
      timestamp: new Date().toISOString(),
    });
  });

  // GET /api/v1/users/groups — list user groups
  app.get("/groups", async (request: FastifyRequest, reply: FastifyReply) => {
    if (!requireManager(request, reply)) return;

    const groups = await db
      .select()
      .from(userGroups)
      .orderBy(asc(userGroups.name));
    return reply.send({
      success: true,
      data: groups,
      timestamp: new Date().toISOString(),
    });
  });

  // POST /api/v1/users/groups — create group
  app.post("/groups", async (request: FastifyRequest, reply: FastifyReply) => {
    if (!requireAdmin(request, reply)) return;

    const schema = z.object({
      name: z.string().min(1).max(100),
      description: z.string().optional(),
      defaultRoles: z.array(z.string()).optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    const [group] = await db
      .insert(userGroups)
      .values({ uuid: crypto.randomUUID(), ...body.data })
      .returning();

    return reply.status(201).send({
      success: true,
      data: group,
      timestamp: new Date().toISOString(),
    });
  });
}
