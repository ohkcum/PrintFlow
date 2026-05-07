// Auth middleware for Fastify
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import type { AppContext } from "../index.js";
import { createAuthService } from "@printflow/auth";
import { createDrizzle } from "@printflow/db";
import type { AppConfig } from "@printflow/common";

export async function createAuthMiddleware(app: FastifyInstance<AppContext>) {
  const config = app.printflowConfig ?? {
    database: { url: process.env["DATABASE_URL"] ?? "", poolMax: 10 },
  };
  const db = createDrizzle({
    nodeEnv: "development",
    logLevel: "info",
    port: 3001,
    database: config.database,
    redis: { url: process.env["REDIS_URL"] ?? "redis://localhost:6379" },
    auth: {
      secret: process.env["AUTH_SECRET"] ?? "dev-secret",
      expiresIn: "7d",
      totpIssuer: "PrintFlow",
    },
    storage: { documentPath: "./data", thumbnailPath: "./data" },
    print: { ippServerPort: 6310, cupsHost: "localhost", cupsPort: 631 },
    email: { smtpPort: 587, smtpFrom: "noreply@printflow.local", imapPort: 993 },
    soffice: { path: "/usr/bin/soffice" },
    appUrl: "http://localhost:3000",
    apiUrl: "http://localhost:3001",
  });
  const auth = createAuthService(db);

  // Decorate request with user context
  app.decorateRequest("userId", null);
  app.decorateRequest("userName", null);
  app.decorateRequest("roles", null);
  app.decorateRequest("sessionId", null);

  // Store config on app for use in middleware
  (app as FastifyInstance & { printflowConfig?: object }).printflowConfig = config;

  // Add preHandler hook to validate session
  app.addHook("preHandler", async (request: FastifyRequest, reply: FastifyReply) => {
    // Skip auth for public routes
    const publicPaths = [
      "/api/v1/auth/login",
      "/api/v1/auth/register",
      "/api/v1/auth/totp/setup",
      "/api/v1/health",
      "/health",
    ];

    if (publicPaths.includes(request.url)) {
      return;
    }

    const authHeader = request.headers.authorization;
    if (!authHeader?.startsWith("Bearer ")) {
      // Allow unauthenticated access for now, but mark as anonymous
      request.userId = undefined;
      return;
    }

    const token = authHeader.slice(7);
    try {
      const session = await auth.validateSession(token);
      if (session) {
        request.userId = session.userId;
        request.userName = session.userName;
        request.roles = session.roles;
        request.sessionId = session.id;
      }
    } catch (err) {
      request.log.warn({ err }, "Session validation failed");
    }
  });
}

declare module "fastify" {
  interface FastifyRequest {
    userId?: number;
    userName?: string;
    roles?: string[];
    sessionId?: number;
  }
}
