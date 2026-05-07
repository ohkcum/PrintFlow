// Printer Router — mirrors PrintFlowLite's JSON API printer operations
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { createDrizzle } from "@printflow/db";
import { printers, printerGroups } from "@printflow/db/schema";
import { eq, asc } from "drizzle-orm";
import { z } from "zod";

const dbConfig = {
  nodeEnv: "development",
  logLevel: "info",
  port: 3001,
  database: {
    url: process.env["DATABASE_URL"] ?? "postgresql://printflow:password@localhost:5432/printflow",
    poolMin: 2,
    poolMax: 10,
  },
  redis: { url: process.env["REDIS_URL"] ?? "redis://localhost:6379" },
  auth: { secret: "dev", expiresIn: "7d", totpIssuer: "PrintFlow" },
  storage: { documentPath: "./data", thumbnailPath: "./data" },
  print: { ippServerPort: 6310, cupsHost: "localhost", cupsPort: 631 },
  email: { smtpPort: 587, smtpFrom: "noreply@printflow.local", imapPort: 993 },
  soffice: { path: "/usr/bin/soffice" },
  appUrl: "http://localhost:3000",
  apiUrl: "http://localhost:3001",
};

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

export async function createPrinterRouter(app: FastifyInstance) {
  const db = createDrizzle(dbConfig);

  // GET /api/v1/printers — list printers
  app.get("/", async (request, reply) => {
    const { groupId, status, isPublic } = request.query as Record<string, string>;

    const rows = await db.select().from(printers).orderBy(asc(printers.displayName));
    return reply.send({
      success: true,
      data: rows,
      timestamp: new Date().toISOString(),
    });
  });

  // GET /api/v1/printers/groups — list printer groups
  app.get("/groups", async (request, reply) => {
    const groups = await db.select().from(printerGroups).orderBy(asc(printerGroups.displayOrder));
    return reply.send({
      success: true,
      data: groups,
      timestamp: new Date().toISOString(),
    });
  });

  // GET /api/v1/printers/:id — get printer details
  app.get<{ Params: { id: string } }>("/:id", async (request, reply) => {
    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid printer ID" },
        timestamp: new Date().toISOString(),
      });
    }

    const [printer] = await db.select().from(printers).where(eq(printers.id, id)).limit(1);
    if (!printer) {
      return reply.status(404).send({
        success: false,
        error: { code: "PRINTER_NOT_FOUND", message: "Printer not found" },
        timestamp: new Date().toISOString(),
      });
    }

    return reply.send({
      success: true,
      data: printer,
      timestamp: new Date().toISOString(),
    });
  });

  // POST /api/v1/printers — create printer
  app.post("/", async (request: FastifyRequest, reply: FastifyReply) => {
    if (!requireAdmin(request, reply)) return;

    const schema = z.object({
      name: z.string().min(1).max(200),
      description: z.string().optional(),
      displayName: z.string().optional(),
      printerType: z.enum(["NETWORK", "USB", "DRIVER", "PDF", "EMAIL", "HOLD"]).default("NETWORK"),
      ippPrinterUri: z.string().url().optional().or(z.string().optional()),
      cupsPrinterName: z.string().optional(),
      printerGroupId: z.number().optional(),
      colorMode: z.enum(["AUTO", "MONOCHROME", "COLOR"]).default("AUTO"),
      supportsDuplex: z.boolean().default(true),
      supportsStaple: z.boolean().default(false),
      supportsPunch: z.boolean().default(false),
      supportsFold: z.boolean().default(false),
      supportsBanner: z.boolean().default(false),
      maxPaperSize: z.string().default("A4"),
      minPaperSize: z.string().default("A5"),
      costPerPageMono: z.string().default("0.01"),
      costPerPageColor: z.string().default("0.05"),
      costPerSheet: z.string().default("0"),
      fixedCost: z.string().default("0"),
      ecoPrintCostPerPage: z.string().default("0.005"),
      isEnabled: z.boolean().default(true),
      isPublic: z.boolean().default(true),
      requireRelease: z.boolean().default(true),
      ecoPrintDefault: z.boolean().default(false),
      snmpEnabled: z.boolean().default(false),
      snmpCommunity: z.string().default("public"),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    const [printer] = await db
      .insert(printers)
      .values({
        uuid: crypto.randomUUID(),
        ...body.data,
        printerStatus: "OFFLINE",
      })
      .returning();

    return reply.status(201).send({
      success: true,
      data: printer,
      timestamp: new Date().toISOString(),
    });
  });

  // PUT /api/v1/printers/:id — update printer
  app.put<{ Params: { id: string } }>("/:id", async (request: FastifyRequest, reply: FastifyReply) => {
    if (!requireAdmin(request, reply)) return;

    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid printer ID" },
        timestamp: new Date().toISOString(),
      });
    }

    const schema = z.object({
      name: z.string().min(1).max(200).optional(),
      description: z.string().optional(),
      displayName: z.string().optional(),
      printerType: z.enum(["NETWORK", "USB", "DRIVER", "PDF", "EMAIL", "HOLD"]).optional(),
      printerStatus: z.enum(["ONLINE", "OFFLINE", "IDLE", "BUSY", "ERROR", "MAINTENANCE"]).optional(),
      ippPrinterUri: z.string().optional(),
      cupsPrinterName: z.string().optional(),
      printerGroupId: z.number().optional().nullable(),
      colorMode: z.enum(["AUTO", "MONOCHROME", "COLOR"]).optional(),
      supportsDuplex: z.boolean().optional(),
      supportsStaple: z.boolean().optional(),
      supportsPunch: z.boolean().optional(),
      supportsFold: z.boolean().optional(),
      supportsBanner: z.boolean().optional(),
      maxPaperSize: z.string().optional(),
      minPaperSize: z.string().optional(),
      costPerPageMono: z.string().optional(),
      costPerPageColor: z.string().optional(),
      costPerSheet: z.string().optional(),
      fixedCost: z.string().optional(),
      ecoPrintCostPerPage: z.string().optional(),
      isEnabled: z.boolean().optional(),
      isPublic: z.boolean().optional(),
      requireRelease: z.boolean().optional(),
      ecoPrintDefault: z.boolean().optional(),
      snmpEnabled: z.boolean().optional(),
      snmpCommunity: z.string().optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    const [printer] = await db
      .update(printers)
      .set({ ...body.data, dateModified: new Date() })
      .where(eq(printers.id, id))
      .returning();

    if (!printer) {
      return reply.status(404).send({
        success: false,
        error: { code: "PRINTER_NOT_FOUND", message: "Printer not found" },
        timestamp: new Date().toISOString(),
      });
    }

    return reply.send({
      success: true,
      data: printer,
      timestamp: new Date().toISOString(),
    });
  });

  // DELETE /api/v1/printers/:id
  app.delete<{ Params: { id: string } }>("/:id", async (request: FastifyRequest, reply: FastifyReply) => {
    if (!requireAdmin(request, reply)) return;

    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid printer ID" },
        timestamp: new Date().toISOString(),
      });
    }

    await db.update(printers).set({ isEnabled: false, dateModified: new Date() }).where(eq(printers.id, id));

    return reply.send({
      success: true,
      data: { message: "Printer disabled" },
      timestamp: new Date().toISOString(),
    });
  });
}
