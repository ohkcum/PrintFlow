// Fastify server entry point
import Fastify from "fastify";
import cors from "@fastify/cors";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import sensible from "@fastify/sensible";
import multipart from "@fastify/multipart";
import { createAuthRouter } from "./routers/auth.js";
import { createUserRouter } from "./routers/users.js";
import { createPrinterRouter } from "./routers/printers.js";
import { createDocumentRouter } from "./routers/documents.js";
import { createAccountRouter } from "./routers/accounts.js";
import { createSseRouter } from "./routers/sse.js";
import { createConfigRouter } from "./routers/config.js";
import { createJobTicketRouter } from "./routers/job-tickets.js";
import { createIppServerRouter } from "./routers/ipp.js";
// TODO: Uncomment when database schemas are implemented
// import { createReportsRouter } from "./routers/reports.js";
// import { createPosRouter } from "./routers/pos.js";
// import { createQrRouter } from "./routers/qr.js";
// import { createTelegramRouter } from "./routers/telegram.js";
// import { createOAuthRouter } from "./routers/oauth.js";
// import { createSnmpRouter } from "./routers/snmp.js";
import { createAuthMiddleware } from "./middleware/auth.js";
import type { FastifyInstance } from "fastify";
import type { AppConfig } from "@printflow/common";

export interface AppContext {
  userId?: number;
  userName?: string;
  roles?: string[];
  sessionId?: number;
}

export type AppFastify = FastifyInstance<AppContext>;

export async function createApp(config: AppConfig): Promise<AppFastify> {
  const app = Fastify<AppContext>({
    logger: {
      level: config.logLevel,
    },
  });

  // ─── Plugins ────────────────────────────────────────────────────────────────
  await app.register(cors, {
    origin: [config.appUrl, "http://localhost:3000", "http://127.0.0.1:51733"],
    credentials: true,
  });
  await app.register(helmet, {
    contentSecurityPolicy: false,
  });
  await app.register(rateLimit, {
    max: 1000,
    timeWindow: "1 minute",
  });
  await app.register(sensible);
  await app.register(multipart, {
    limits: { fileSize: 50 * 1024 * 1024 }, // 50MB limit
  });

  // ─── Health check ───────────────────────────────────────────────────────────
  app.get("/health", async () => ({ status: "ok", version: "0.1.0" }));
  app.get("/api/v1/health", async () => ({
    status: "ok",
    timestamp: new Date().toISOString(),
  }));

  // ─── Auth middleware ───────────────────────────────────────────────────────
  await app.register(createAuthMiddleware);

  // ─── Routers ───────────────────────────────────────────────────────────────
  await app.register(createAuthRouter, { prefix: "/api/v1/auth" });
  await app.register(createUserRouter, { prefix: "/api/v1/users" });
  await app.register(createPrinterRouter, { prefix: "/api/v1/printers" });
  await app.register(createDocumentRouter, { prefix: "/api/v1/documents" });
  await app.register(createAccountRouter, { prefix: "/api/v1/accounts" });
  await app.register(createSseRouter, { prefix: "/api/v1/events" });
  await app.register(createConfigRouter, { prefix: "/api/v1/config" });
  await app.register(createJobTicketRouter, { prefix: "/api/v1/job-tickets" });
  await app.register(createIppServerRouter, { prefix: "/api/v1/ipp" });
  // TODO: Uncomment when database schemas are implemented
  // await app.register(createReportsRouter, { prefix: "/api/v1/reports" });
  // await app.register(createPosRouter, { prefix: "/api/v1/pos" });
  // await app.register(createQrRouter, { prefix: "/api/v1/qr" });
  // await app.register(createTelegramRouter, { prefix: "/api/v1/telegram" });
  // await app.register(createOAuthRouter, { prefix: "/api/v1/oauth" });
  // await app.register(createSnmpRouter, { prefix: "/api/v1/snmp" });

  // ─── Error handler ─────────────────────────────────────────────────────────
  app.setErrorHandler((error, request, reply) => {
    app.log.error({ err: error, url: request.url }, "Request error");
    if (error.statusCode) {
      return reply.status(error.statusCode).send({
        success: false,
        error: { code: error.code ?? "ERROR", message: error.message },
        timestamp: new Date().toISOString(),
      });
    }
    return reply.status(500).send({
      success: false,
      error: { code: "INTERNAL_ERROR", message: "Internal server error" },
      timestamp: new Date().toISOString(),
    });
  });

  return app;
}
