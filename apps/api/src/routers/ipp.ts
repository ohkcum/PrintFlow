// IPP Print Server — mirrors PrintFlowLite's IppPrintServer
// Handles IPP (Internet Printing Protocol) print requests

import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { createDrizzle } from "@printflow/db";
import {
  printers,
  docIn,
  docOut,
  docLog,
  printIn,
  userAccounts,
  accounts,
} from "@printflow/db/schema";
import { eq, desc, count } from "drizzle-orm";
import { z } from "zod";
import { broadcastSseEvent } from "./sse.js";

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

function ok<T>(reply: FastifyReply, data: T, status = 200) {
  return reply.status(status).send({ success: true, data, timestamp: new Date().toISOString() });
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

// ─── IPP Server Router ─────────────────────────────────────────────────────────

export async function createIppServerRouter(app: FastifyInstance) {
  const db = createDb();

  // GET /api/v1/ipp/queues — list available IPP queues
  app.get("/queues", async (request, reply) => {
    const { status = "all" } = request.query as Record<string, string>;

    const conditions = [];
    if (status === "enabled") {
      conditions.push(eq(printers.isEnabled, true));
    } else if (status === "disabled") {
      conditions.push(eq(printers.isEnabled, false));
    }

    const queueList = await db
      .select({
        id: printers.id,
        name: printers.name,
        displayName: printers.displayName,
        ippPrinterUri: printers.ippPrinterUri,
        isEnabled: printers.isEnabled,
        requireRelease: printers.requireRelease,
        printerType: printers.printerType,
        printerStatus: printers.printerStatus,
      })
      .from(printers)
      .where(conditions.length > 0 ? conditions[0] : undefined);

    return ok(reply, { data: queueList });
  });

  // GET /api/v1/ipp/queues/:name — get queue details
  app.get<{ Params: { name: string } }>(
    "/queues/:name",
    async (request, reply) => {
      const [printer] = await db
        .select()
        .from(printers)
        .where(eq(printers.name, request.params.name))
        .limit(1);

      if (!printer) {
        return error(reply, 404, "QUEUE_NOT_FOUND", "Printer queue not found");
      }

      const [{ jobCount }] = await db
        .select({ jobCount: count() })
        .from(printIn)
        .where(eq(printIn.printerId, printer.id));

      return ok(reply, {
        ...printer,
        activeJobCount: Number(jobCount),
      });
    },
  );

  // POST /api/v1/ipp/print — submit a print job via IPP
  app.post("/print", async (request, reply) => {
    const schema = z.object({
      printerName: z.string().min(1),
      userId: z.number().int().positive().optional(),
      userName: z.string().optional(),
      jobName: z.string().optional(),
      copies: z.number().int().min(1).max(100).default(1),
      duplex: z.enum(["NONE", "PORTRAIT", "LANDSCAPE"]).default("NONE"),
      colorMode: z.enum(["AUTO", "MONOCHROME", "COLOR"]).default("AUTO"),
      pageSize: z.string().default("A4"),
      documentBase64: z.string().optional(),
      documentUrl: z.string().url().optional(),
      options: z
        .record(z.union([z.string(), z.number(), z.boolean()]))
        .optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return error(reply, 400, "VALIDATION_ERROR", body.error.message);
    }

    const {
      printerName,
      userId,
      userName,
      jobName,
      copies,
      duplex,
      colorMode,
      pageSize,
      options,
    } = body.data;

    // Find printer
    const [printer] = await db
      .select()
      .from(printers)
      .where(eq(printers.name, printerName))
      .limit(1);

    if (!printer) {
      return error(reply, 404, "PRINTER_NOT_FOUND", "Printer not found");
    }

    if (!printer.isEnabled) {
      return error(reply, 400, "PRINTER_DISABLED", "Printer is disabled");
    }

    // Get user account
    const userIdToUse = userId ?? (request as any).userId;
    const effectiveUserName = userName ?? (request as any).userName ?? "anonymous";

    const accountConditions = userIdToUse ? eq(userAccounts.userId, userIdToUse) : undefined;
    const [account] = accountConditions
      ? await db.select().from(userAccounts).where(accountConditions).limit(1)
      : [null];

    // Calculate cost
    const costPerPage =
      colorMode === "MONOCHROME"
        ? Number(printer.costPerPageMono ?? "0.01")
        : Number(printer.costPerPageColor ?? "0.05");
    const fixedCost = Number(printer.fixedCost ?? "0");
    const totalCost = ((costPerPage + fixedCost) * copies).toFixed(4);

    // Check balance
    if (account) {
      const balance = Number(account.balance);
      const cost = Number(totalCost);
      if (balance < cost && !(request as any).roles?.includes("ADMIN")) {
        return error(
          reply,
          400,
          "INSUFFICIENT_BALANCE",
          `Insufficient balance. Need ${totalCost}, have ${balance.toFixed(4)}`,
        );
      }
    }

    // Create DocIn record
    const [doc] = await db
      .insert(docIn)
      .values({
        uuid: crypto.randomUUID(),
        userId: userIdToUse,
        userName: effectiveUserName,
        docName: jobName ?? "IPP Print Job",
        docType: "PRINT_IN",
        docStatus: printer.requireRelease ? "PENDING" : "PROCESSING",
        filePath: `./data/ipp/${crypto.randomUUID()}.pdf`,
        fileSize: 0,
        mimeType: "application/pdf",
        pageCount: copies,
        createdBy: "IPP",
        sourceInfo: `IPP Print: ${printerName}`,
        defaultCopies: copies,
        defaultDuplex: duplex,
        defaultColorMode: colorMode,
      })
      .returning();

    // Create PrintIn job
    const [printJob] = await db
      .insert(printIn)
      .values({
        uuid: crypto.randomUUID(),
        docInId: doc.id,
        userId: userIdToUse,
        userName: effectiveUserName,
        printerId: printer.id,
        printerName: printer.displayName || printer.name,
        copyCount: copies,
        duplex,
        colorMode,
        paperSize: pageSize,
        ecoPrint: options?.["ecoprint"] === true,
        estimatedPages: copies,
        estimatedCost: totalCost,
        accountId: account?.id,
        status: printer.requireRelease ? "QUEUED" : "PROCESSING",
      })
      .returning();

    // Create DocLog entry
    await db.insert(docLog).values({
      uuid: crypto.randomUUID(),
      userId: userIdToUse,
      userName: effectiveUserName,
      docName: jobName ?? "IPP Print Job",
      printerId: printer.id,
      printerName: printer.displayName || printer.name,
      pagesPrinted: copies,
      sheetsPrinted: duplex !== "NONE" ? Math.ceil(copies / 2) : copies,
      accountId: account?.id,
      accountName: account?.accountName ?? "N/A",
      costPerPage: costPerPage.toString(),
      totalCost,
      copyCount: copies,
      duplex,
      colorMode,
      paperSize: pageSize,
      jobId: String(printJob.id),
      jobStatus: printer.requireRelease ? "QUEUED" : "PROCESSING",
    });

    // Broadcast SSE events
    broadcastSseEvent("printjob:started", {
      jobId: printJob.id,
      docId: doc.id,
      docName: doc.docName,
      printerId: printer.id,
      printerName: printer.displayName || printer.name,
      pagesPrinted: copies,
      totalCost,
      userId: userIdToUse,
      userName: effectiveUserName,
    });

    return ok(
      reply,
      {
        jobId: printJob.id,
        docId: doc.id,
        printerName: printer.displayName || printer.name,
        totalCost,
        pagesPrinted: copies,
        status: printer.requireRelease ? "QUEUED" : "PROCESSING",
      },
      201,
    );
  });

  // GET /api/v1/ipp/jobs — list IPP print jobs
  app.get("/jobs", async (request, reply) => {
    const { printerId, status, page = "1", limit = "50" } = request.query as Record<string, string>;

    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10)));
    const offset = (pageNum - 1) * limitNum;

    const conditions = [];
    if (printerId) conditions.push(eq(printIn.printerId, parseInt(printerId)));
    if (status) conditions.push(eq(printIn.status, status as any));

    const [rows, [{ total }]] = await Promise.all([
      db
        .select()
        .from(printIn)
        .where(conditions.length > 0 ? conditions[0] : undefined)
        .orderBy(desc(printIn.dateCreated))
        .limit(limitNum)
        .offset(offset),
      db
        .select({ total: count() })
        .from(printIn)
        .where(conditions.length > 0 ? conditions[0] : undefined),
    ]);

    return ok(reply, {
      data: rows,
      total: Number(total),
      page: pageNum,
      limit: limitNum,
      totalPages: Math.ceil(Number(total) / limitNum),
      hasNext: pageNum * limitNum < Number(total),
      hasPrev: pageNum > 1,
    });
  });

  // GET /api/v1/ipp/jobs/:id — get job status
  app.get<{ Params: { id: string } }>("/jobs/:id", async (request, reply) => {
    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return error(reply, 400, "INVALID_ID", "Invalid job ID");
    }

    const [job] = await db.select().from(printIn).where(eq(printIn.id, id)).limit(1);

    if (!job) {
      return error(reply, 404, "JOB_NOT_FOUND", "Print job not found");
    }

    return ok(reply, job);
  });

  // POST /api/v1/ipp/jobs/:id/cancel — cancel a print job
  app.post<{ Params: { id: string } }>("/jobs/:id/cancel", async (request, reply) => {
    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return error(reply, 400, "INVALID_ID", "Invalid job ID");
    }

    const [job] = await db
      .select()
      .from(printIn)
      .where(eq(printIn.id, id))
      .limit(1);

    if (!job) {
      return error(reply, 404, "JOB_NOT_FOUND", "Print job not found");
    }

    if (job.status === "COMPLETED" || job.status === "CANCELLED") {
      return error(reply, 400, "INVALID_STATE", "Cannot cancel a completed or already cancelled job");
    }

    await db
      .update(printIn)
      .set({ status: "CANCELLED", dateModified: new Date() })
      .where(eq(printIn.id, id));

    broadcastSseEvent("printjob:cancelled", {
      jobId: id,
      printerId: job.printerId,
    });

    return ok(reply, { message: "Job cancelled", jobId: id });
  });

  // GET /api/v1/ipp/status — IPP server health status
  app.get("/status", async (request, reply) => {
    const [{ totalPrinters }] = await db
      .select({ totalPrinters: count() })
      .from(printers);

    const [{ activeJobs }] = await db
      .select({ activeJobs: count() })
      .from(printIn)
      .where(eq(printIn.status, "PROCESSING"));

    return ok(reply, {
      status: "running",
      version: "0.1.0",
      totalPrinters: Number(totalPrinters),
      activeJobs: Number(activeJobs),
      timestamp: new Date().toISOString(),
    });
  });
}
