// Job Tickets Router — mirrors PrintFlowLite's JobTicketService
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { createDrizzle } from "@printflow/db";
import {
  accounts,
  accountTrx,
} from "@printflow/db/schema";
import { eq, desc, and, count, like, gte, sql } from "drizzle-orm";
import { z } from "zod";

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

function requireManager(request: FastifyRequest, reply: FastifyReply) {
  if (!request.roles?.includes("ADMIN") && !request.roles?.includes("MANAGER")) {
    reply.status(403).send({
      success: false,
      error: { code: "FORBIDDEN", message: "Manager or Admin role required" },
      timestamp: new Date().toISOString(),
    });
    return false;
  }
  return true;
}

function ok<T>(reply: FastifyReply, data: T) {
  return reply.send({ success: true, data, timestamp: new Date().toISOString() });
}

function error(
  reply: FastifyReply,
  status: number,
  code: string,
  message: string,
) {
  return reply.status(status).send({
    success: false,
    error: { code, message },
    timestamp: new Date().toISOString(),
  });
}

// ─── In-Memory Ticket Store (mirrors PrintFlowLite's ConcurrentHashMap cache) ─────

interface JobTicket {
  uuid: string;
  ticketNumber: string;
  userId: number;
  docName: string;
  copies: number;
  printerName: string;
  printerRedirect: string | null;
  status: "PENDING" | "PRINTING" | "COMPLETED" | "CANCELLED";
  copiesPrinted: number;
  totalCost: string;
  submitTime: string;
  deliveryTime: string;
  label: string | null;
  domain: string | null;
  use: string | null;
  tag: string | null;
  isReopened: boolean;
  createdAt: string;
}

const ticketStore = new Map<string, JobTicket>();
const ticketNumberIndex = new Map<string, string>();

let ticketCounter = Date.now();

function generateTicketNumber(label?: string): string {
  const chunk = (n: number, w: number) => {
    const hex = n.toString(16).toUpperCase().padStart(w, "0");
    return hex.replace(/([A-F0-9]{4})/g, "$1-").replace(/-$/, "");
  };
  const base = chunk(ticketCounter++, 4);
  return label ? `${label}/${base}` : base;
}

// ─── Router ───────────────────────────────────────────────────────────────────

export async function createJobTicketRouter(app: FastifyInstance) {
  const db = createDb();

  // GET /api/v1/job-tickets/summary — queue summary
  app.get("/summary", async (request, reply) => {
    if (!requireManager(request, reply)) return;

    const pending = Array.from(ticketStore.values()).filter(
      (t) => t.status === "PENDING" || t.status === "PRINTING",
    );
    const printJobs = pending.filter((t) => !isCopyJob(t));
    const copyJobs = pending.filter((t) => isCopyJob(t));

    const totalCost = pending.reduce(
      (sum, t) => sum + parseFloat(t.totalCost),
      0,
    );

    return ok(reply, {
      totalPending: pending.length,
      printJobs: printJobs.length,
      copyJobs: copyJobs.length,
      totalCost: totalCost.toFixed(4),
    });
  });

  // GET /api/v1/job-tickets — list tickets
  app.get("/", async (request, reply) => {
    const { userId, search, status, page = "1", limit = "20" } = request.query as Record<string, string>;
    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10)));

    let tickets = Array.from(ticketStore.values());

    if (userId) {
      tickets = tickets.filter((t) => t.userId === parseInt(userId));
    }
    if (search) {
      const s = search.toLowerCase();
      tickets = tickets.filter(
        (t) =>
          t.ticketNumber.toLowerCase().includes(s) ||
          t.docName.toLowerCase().includes(s),
      );
    }
    if (status) {
      tickets = tickets.filter((t) => t.status === status);
    }

    tickets.sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );

    const total = tickets.length;
    const paginated = tickets.slice(
      (pageNum - 1) * limitNum,
      pageNum * limitNum,
    );

    return ok(reply, {
      data: paginated,
      total,
      page: pageNum,
      limit: limitNum,
      totalPages: Math.ceil(total / limitNum),
      hasNext: pageNum * limitNum < total,
      hasPrev: pageNum > 1,
    });
  });

  // GET /api/v1/job-tickets/:uuid — get ticket
  app.get<{ Params: { uuid: string } }>("/:uuid", async (request, reply) => {
    const ticket = ticketStore.get(request.params.uuid);
    if (!ticket) {
      return error(reply, 404, "TICKET_NOT_FOUND", "Job ticket not found");
    }
    return ok(reply, ticket);
  });

  // POST /api/v1/job-tickets — create a job ticket
  app.post("/", async (request, reply) => {
    if (!request.userId) {
      return error(reply, 401, "UNAUTHORIZED", "Not authenticated");
    }

    const schema = z.object({
      docName: z.string().min(1),
      copies: z.number().int().min(1).max(100).default(1),
      printerName: z.string().min(1),
      label: z.string().optional(),
      domain: z.string().optional(),
      use: z.string().optional(),
      tag: z.string().optional(),
      deliveryDate: z.string().datetime().optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return error(reply, 400, "VALIDATION_ERROR", body.error.message);
    }

    const uuid = crypto.randomUUID();
    const ticketNumber = generateTicketNumber(body.data.label);
    const now = new Date();
    const deliveryDate = body.data.deliveryDate
      ? new Date(body.data.deliveryDate)
      : now;

    // Get user account and calculate cost
    const [account] = await db
      .select()
      .from(accounts)
      .where(eq(accounts.userId, request.userId))
      .limit(1);

    const totalCost = account
      ? (parseFloat(account.defaultCostPerPageMono ?? "0.01") * body.data.copies).toFixed(4)
      : "0.0000";

    const ticket: JobTicket = {
      uuid,
      ticketNumber,
      userId: request.userId,
      docName: body.data.docName,
      copies: body.data.copies,
      printerName: body.data.printerName,
      printerRedirect: null,
      status: "PENDING",
      copiesPrinted: 0,
      totalCost,
      submitTime: now.toISOString(),
      deliveryTime: deliveryDate.toISOString(),
      label: body.data.label ?? null,
      domain: body.data.domain ?? null,
      use: body.data.use ?? null,
      tag: body.data.tag ?? null,
      isReopened: false,
      createdAt: now.toISOString(),
    };

    ticketStore.set(uuid, ticket);
    ticketNumberIndex.set(ticketNumber, uuid);

    return reply.status(201).send({
      success: true,
      data: ticket,
      timestamp: new Date().toISOString(),
    });
  });

  // PUT /api/v1/job-tickets/:uuid — update ticket
  app.put<{ Params: { uuid: string } }>("/:uuid", async (request, reply) => {
    const ticket = ticketStore.get(request.params.uuid);
    if (!ticket) {
      return error(reply, 404, "TICKET_NOT_FOUND", "Job ticket not found");
    }

    if (ticket.userId !== request.userId && !requireManager(request, reply)) {
      return;
    }

    const schema = z.object({
      copies: z.number().int().min(1).max(100).optional(),
      printerName: z.string().optional(),
      printerRedirect: z.string().optional(),
      status: z.enum(["PENDING", "PRINTING", "COMPLETED", "CANCELLED"]).optional(),
      copiesPrinted: z.number().int().min(0).optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return error(reply, 400, "VALIDATION_ERROR", body.error.message);
    }

    const updated: JobTicket = {
      ...ticket,
      ...body.data,
    };

    ticketStore.set(request.params.uuid, updated);
    return ok(reply, updated);
  });

  // POST /api/v1/job-tickets/:uuid/print — print a ticket
  app.post<{ Params: { uuid: string } }>(
    "/:uuid/print",
    async (request, reply) => {
      if (!request.userId) {
        return error(reply, 401, "UNAUTHORIZED", "Not authenticated");
      }

      const ticket = ticketStore.get(request.params.uuid);
      if (!ticket) {
        return error(reply, 404, "TICKET_NOT_FOUND", "Job ticket not found");
      }

      if (ticket.status !== "PENDING") {
        return error(
          reply,
          400,
          "INVALID_STATE",
          "Ticket is not in PENDING state",
        );
      }

      const schema = z.object({
        printerId: z.number().optional(),
        printerName: z.string().optional(),
      });

      const body = schema.safeParse(request.body);
      if (!body.success) {
        return error(reply, 400, "VALIDATION_ERROR", body.error.message);
      }

      // Update ticket
      const updated: JobTicket = {
        ...ticket,
        status: "PRINTING",
        printerRedirect:
          body.data.printerName ?? ticket.printerName,
        copiesPrinted: ticket.copies,
      };

      ticketStore.set(request.params.uuid, updated);

      return ok(reply, {
        message: "Ticket printing started",
        ticket: updated,
      });
    },
  );

  // POST /api/v1/job-tickets/:uuid/settle — settle ticket (no print)
  app.post<{ Params: { uuid: string } }>(
    "/:uuid/settle",
    async (request, reply) => {
      if (!requireManager(request, reply)) return;

      const ticket = ticketStore.get(request.params.uuid);
      if (!ticket) {
        return error(reply, 404, "TICKET_NOT_FOUND", "Job ticket not found");
      }

      // Charge account
      if (accountTrx) {
        const [account] = await db
          .select()
          .from(accounts)
          .where(eq(accounts.userId, ticket.userId))
          .limit(1);

        if (account) {
          const oldBalance = parseFloat(account.balance as string);
          const cost = parseFloat(ticket.totalCost);
          const newBalance = oldBalance - cost;

          await db
            .update(accounts)
            .set({ balance: newBalance.toFixed(4), dateModified: new Date() })
            .where(eq(accounts.id, account.id));

          await db.insert(accountTrx).values({
            uuid: crypto.randomUUID(),
            accountId: account.id,
            userId: ticket.userId,
            trxType: "PRINT_JOB",
            amount: (-cost).toFixed(4),
            balanceBefore: oldBalance.toFixed(4),
            balanceAfter: newBalance.toFixed(4),
            description: `Job ticket settled: ${ticket.ticketNumber}`,
            referenceType: "JOB_TICKET",
          });
        }
      }

      const updated: JobTicket = {
        ...ticket,
        status: "COMPLETED",
        copiesPrinted: ticket.copies,
      };

      ticketStore.set(request.params.uuid, updated);

      return ok(reply, { message: "Ticket settled", ticket: updated });
    },
  );

  // POST /api/v1/job-tickets/:uuid/complete — mark as completed
  app.post<{ Params: { uuid: string } }>(
    "/:uuid/complete",
    async (request, reply) => {
      if (!requireManager(request, reply)) return;

      const ticket = ticketStore.get(request.params.uuid);
      if (!ticket) {
        return error(reply, 404, "TICKET_NOT_FOUND", "Job ticket not found");
      }

      const updated: JobTicket = {
        ...ticket,
        status: "COMPLETED",
      };

      ticketStore.set(request.params.uuid, updated);
      return ok(reply, { message: "Ticket completed", ticket: updated });
    },
  );

  // POST /api/v1/job-tickets/:uuid/cancel — cancel ticket
  app.post<{ Params: { uuid: string } }>(
    "/:uuid/cancel",
    async (request, reply) => {
      const ticket = ticketStore.get(request.params.uuid);
      if (!ticket) {
        return error(reply, 404, "TICKET_NOT_FOUND", "Job ticket not found");
      }

      if (ticket.userId !== request.userId && !requireManager(request, reply)) {
        return;
      }

      if (ticket.status === "COMPLETED") {
        return error(reply, 400, "CANNOT_CANCEL", "Cannot cancel completed ticket");
      }

      const updated: JobTicket = {
        ...ticket,
        status: "CANCELLED",
      };

      ticketStore.set(request.params.uuid, updated);
      return ok(reply, { message: "Ticket cancelled", ticket: updated });
    },
  );

  // POST /api/v1/job-tickets/:uuid/reopen — reopen for extra copies
  app.post<{ Params: { uuid: string } }>(
    "/:uuid/reopen",
    async (request, reply) => {
      if (!request.userId) {
        return error(reply, 401, "UNAUTHORIZED", "Not authenticated");
      }

      const ticket = ticketStore.get(request.params.uuid);
      if (!ticket) {
        return error(reply, 404, "TICKET_NOT_FOUND", "Job ticket not found");
      }

      if (ticket.userId !== request.userId && !requireManager(request, reply)) {
        return;
      }

      const schema = z.object({
        extraCopies: z.number().int().min(1).max(100).optional(),
      });

      const body = schema.safeParse(request.body);
      if (!body.success) {
        return error(reply, 400, "VALIDATION_ERROR", body.error.message);
      }

      // Create new ticket with reopened suffix
      const extraCopies = body.data.extraCopies ?? 1;
      const newUuid = crypto.randomUUID();
      const newTicketNumber = `${ticket.ticketNumber}+`;

      const newTicket: JobTicket = {
        ...ticket,
        uuid: newUuid,
        ticketNumber: newTicketNumber,
        copies: extraCopies,
        status: "PENDING",
        printerRedirect: null,
        copiesPrinted: 0,
        isReopened: true,
        createdAt: new Date().toISOString(),
      };

      ticketStore.set(newUuid, newTicket);
      ticketNumberIndex.set(newTicketNumber, newUuid);

      return reply.status(201).send({
        success: true,
        data: newTicket,
        timestamp: new Date().toISOString(),
      });
    },
  );

  // GET /api/v1/job-tickets/numbers — search ticket numbers (autocomplete)
  app.get("/numbers/search", async (request, reply) => {
    const { q, limit = "10" } = request.query as Record<string, string>;

    if (!q) {
      return ok(reply, { data: [] });
    }

    const s = q.toLowerCase();
    const results: string[] = [];

    for (const [number, uuid] of ticketNumberIndex) {
      if (number.toLowerCase().includes(s)) {
        results.push(number);
        if (results.length >= parseInt(limit)) break;
      }
    }

    return ok(reply, { data: results });
  });

  // DELETE /api/v1/job-tickets/:uuid — remove ticket
  app.delete<{ Params: { uuid: string } }>(
    "/:uuid",
    async (request, reply) => {
      if (!requireManager(request, reply)) return;

      const ticket = ticketStore.get(request.params.uuid);
      if (!ticket) {
        return error(reply, 404, "TICKET_NOT_FOUND", "Job ticket not found");
      }

      ticketNumberIndex.delete(ticket.ticketNumber);
      ticketStore.delete(request.params.uuid);

      return ok(reply, { message: "Ticket removed" });
    },
  );
}

function isCopyJob(ticket: JobTicket): boolean {
  return !ticket.docName || ticket.docName.toLowerCase().startsWith("copy");
}
