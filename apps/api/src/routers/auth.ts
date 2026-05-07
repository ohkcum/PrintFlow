// Auth Router — mirrors PrintFlowLite's JSON API auth operations
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { createAuthService, AuthError } from "@printflow/auth";
import { createDrizzle } from "@printflow/db";
import { z } from "zod";

const loginSchema = z.object({
  userName: z.string().min(1),
  password: z.string().min(1),
  totpToken: z.string().optional(),
});

const registerSchema = z.object({
  userName: z.string().min(3).max(50),
  fullName: z.string().min(1).max(200),
  email: z.string().email().optional(),
  password: z.string().min(8).max(128),
});

const changePasswordSchema = z.object({
  oldPassword: z.string().min(1),
  newPassword: z.string().min(8).max(128),
});

export async function createAuthRouter(app: FastifyInstance) {
  const db = createDrizzle({
    nodeEnv:
      (process.env["NODE_ENV"] as "development" | "production" | undefined) ??
      "development",
    logLevel: process.env["LOG_LEVEL"] ?? "info",
    port: 3001,
    database: {
      url:
        process.env["DATABASE_URL"] ??
        "postgresql://printflow:password@localhost:5432/printflow",
      poolMin: 2,
      poolMax: 10,
    },
    redis: { url: process.env["REDIS_URL"] ?? "redis://localhost:6379" },
    auth: {
      secret:
        process.env["AUTH_SECRET"] ??
        "dev-secret-change-in-production-min-32-chars",
      expiresIn: "7d",
      totpIssuer: "PrintFlow",
    },
    storage: { documentPath: "./data", thumbnailPath: "./data" },
    print: { ippServerPort: 6310, cupsHost: "localhost", cupsPort: 631 },
    email: {
      smtpPort: 587,
      smtpFrom: "noreply@printflow.local",
      imapPort: 993,
    },
    soffice: { path: process.env["SOFFICE_PATH"] ?? "/usr/bin/soffice" },
    appUrl: process.env["NEXT_PUBLIC_APP_URL"] ?? "http://localhost:3000",
    apiUrl: process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001",
  });
  const auth = createAuthService(db);

  // POST /api/v1/auth/register
  app.post(
    "/register",
    async (request: FastifyRequest, reply: FastifyReply) => {
      const body = registerSchema.safeParse(request.body);
      if (!body.success) {
        return reply.status(400).send({
          success: false,
          error: { code: "VALIDATION_ERROR", message: body.error.message },
          timestamp: new Date().toISOString(),
        });
      }

      try {
        const result = await auth.register(body.data);
        return reply.status(201).send({
          success: true,
          data: {
            user: result.user,
            token: result.session.token,
            expiresAt: result.session.expiresAt.toISOString(),
          },
          timestamp: new Date().toISOString(),
        });
      } catch (err) {
        if (err instanceof AuthError) {
          return reply.status(400).send({
            success: false,
            error: { code: err.code, message: err.message },
            timestamp: new Date().toISOString(),
          });
        }
        throw err;
      }
    },
  );

  // POST /api/v1/auth/login
  app.post("/login", async (request: FastifyRequest, reply: FastifyReply) => {
    const body = loginSchema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    try {
      const result = await auth.login({
        ...body.data,
        ipAddress: request.ip,
        userAgent: request.headers["user-agent"],
      });
      return reply.send({
        success: true,
        data: {
          user: result.user,
          token: result.session.token,
          expiresAt: result.session.expiresAt.toISOString(),
        },
        timestamp: new Date().toISOString(),
      });
    } catch (err) {
      if (err instanceof AuthError) {
        const status =
          err.code === "TOTP_REQUIRED"
            ? 403
            : err.code === "INVALID_CREDENTIALS" ||
                err.code === "ACCOUNT_INACTIVE"
              ? 401
              : 400;
        return reply.status(status).send({
          success: false,
          error: { code: err.code, message: err.message },
          timestamp: new Date().toISOString(),
        });
      }
      throw err;
    }
  });

  // POST /api/v1/auth/logout
  app.post("/logout", async (request: FastifyRequest, reply: FastifyReply) => {
    const authHeader = request.headers.authorization;
    if (authHeader?.startsWith("Bearer ")) {
      const token = authHeader.slice(7);
      await auth.logout(token);
    }
    return reply.send({
      success: true,
      data: { message: "Logged out" },
      timestamp: new Date().toISOString(),
    });
  });

  // GET /api/v1/auth/me
  app.get("/me", async (request: FastifyRequest, reply: FastifyReply) => {
    if (!request.userId) {
      return reply.status(401).send({
        success: false,
        error: { code: "UNAUTHORIZED", message: "Not authenticated" },
        timestamp: new Date().toISOString(),
      });
    }

    const user = await auth.getUserById(request.userId);
    if (!user) {
      return reply.status(404).send({
        success: false,
        error: { code: "USER_NOT_FOUND", message: "User not found" },
        timestamp: new Date().toISOString(),
      });
    }

    return reply.send({
      success: true,
      data: { user },
      timestamp: new Date().toISOString(),
    });
  });

  // POST /api/v1/auth/change-password
  app.post(
    "/change-password",
    async (request: FastifyRequest, reply: FastifyReply) => {
      if (!request.userId) {
        return reply.status(401).send({
          success: false,
          error: { code: "UNAUTHORIZED", message: "Not authenticated" },
          timestamp: new Date().toISOString(),
        });
      }

      const body = changePasswordSchema.safeParse(request.body);
      if (!body.success) {
        return reply.status(400).send({
          success: false,
          error: { code: "VALIDATION_ERROR", message: body.error.message },
          timestamp: new Date().toISOString(),
        });
      }

      try {
        await auth.changePassword(
          request.userId,
          body.data.oldPassword,
          body.data.newPassword,
        );
        return reply.send({
          success: true,
          data: { message: "Password changed" },
          timestamp: new Date().toISOString(),
        });
      } catch (err) {
        if (err instanceof AuthError) {
          return reply.status(400).send({
            success: false,
            error: { code: err.code, message: err.message },
            timestamp: new Date().toISOString(),
          });
        }
        throw err;
      }
    },
  );

  // POST /api/v1/auth/totp/setup
  app.post(
    "/totp/setup",
    async (request: FastifyRequest, reply: FastifyReply) => {
      if (!request.userId) {
        return reply.status(401).send({
          success: false,
          error: { code: "UNAUTHORIZED", message: "Not authenticated" },
          timestamp: new Date().toISOString(),
        });
      }

      const result = await auth.enableTotp(request.userId);
      return reply.send({
        success: true,
        data: result,
        timestamp: new Date().toISOString(),
      });
    },
  );

  // POST /api/v1/auth/totp/disable
  app.post(
    "/totp/disable",
    async (request: FastifyRequest, reply: FastifyReply) => {
      if (!request.userId) {
        return reply.status(401).send({
          success: false,
          error: { code: "UNAUTHORIZED", message: "Not authenticated" },
          timestamp: new Date().toISOString(),
        });
      }

      const body = z
        .object({ totpToken: z.string().min(6).max(6) })
        .safeParse(request.body);
      if (!body.success) {
        return reply.status(400).send({
          success: false,
          error: { code: "VALIDATION_ERROR", message: body.error.message },
          timestamp: new Date().toISOString(),
        });
      }

      try {
        await auth.disableTotp(request.userId, body.data.totpToken);
        return reply.send({
          success: true,
          data: { message: "TOTP disabled" },
          timestamp: new Date().toISOString(),
        });
      } catch (err) {
        if (err instanceof AuthError) {
          return reply.status(400).send({
            success: false,
            error: { code: err.code, message: err.message },
            timestamp: new Date().toISOString(),
          });
        }
        throw err;
      }
    },
  );
}
