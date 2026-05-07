// Document Router — mirrors PrintFlowLite's JSON API document operations
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { createDrizzle } from "@printflow/db";
import { docIn, docOut, docLog, printIn, printers, userAccounts } from "@printflow/db/schema";
import { eq, desc, and, count, like } from "drizzle-orm";
import { z } from "zod";
import { broadcastSseEvent } from "./sse.js";

export async function createDocumentRouter(app: FastifyInstance) {
  const db = createDrizzle({
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
  });

  // GET /api/v1/documents — list user's SafePages (inbox)
  app.get("/", async (request, reply) => {
    const { page = "1", limit = "20", status, search } = request.query as Record<string, string>;
    const userId = request.userId;

    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10)));
    const offset = (pageNum - 1) * limitNum;

    const conditions = [];
    if (userId) conditions.push(eq(docIn.userId, userId));
    if (status) conditions.push(eq(docIn.docStatus, status as "PENDING" | "PROCESSING" | "READY" | "RELEASED" | "CANCELLED" | "EXPIRED" | "ERROR"));
    if (search) conditions.push(like(docIn.docName, `%${search}%`));

    const where = conditions.length > 0 ? and(...conditions) : undefined;

    const [rows, [{ total }]] = await Promise.all([
      db.select().from(docIn).where(where).orderBy(desc(docIn.dateCreated)).limit(limitNum).offset(offset),
      db.select({ total: count() }).from(docIn).where(where),
    ]);

    return reply.send({
      success: true,
      data: {
        data: rows,
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

  // GET /api/v1/documents/:id — get document details
  app.get<{ Params: { id: string } }>("/:id", async (request, reply) => {
    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid document ID" },
        timestamp: new Date().toISOString(),
      });
    }

    const [doc] = await db.select().from(docIn).where(eq(docIn.id, id)).limit(1);
    if (!doc) {
      return reply.status(404).send({
        success: false,
        error: { code: "DOC_NOT_FOUND", message: "Document not found" },
        timestamp: new Date().toISOString(),
      });
    }

    // Check ownership (unless admin)
    if (doc.userId !== request.userId && !request.roles?.includes("ADMIN") && !request.roles?.includes("MANAGER")) {
      return reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Access denied" },
        timestamp: new Date().toISOString(),
      });
    }

    return reply.send({
      success: true,
      data: doc,
      timestamp: new Date().toISOString(),
    });
  });

  // DELETE /api/v1/documents/:id — delete document
  app.delete<{ Params: { id: string } }>("/:id", async (request, reply) => {
    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid document ID" },
        timestamp: new Date().toISOString(),
      });
    }

    const [doc] = await db.select().from(docIn).where(eq(docIn.id, id)).limit(1);
    if (!doc) {
      return reply.status(404).send({
        success: false,
        error: { code: "DOC_NOT_FOUND", message: "Document not found" },
        timestamp: new Date().toISOString(),
      });
    }

    if (doc.userId !== request.userId && !request.roles?.includes("ADMIN")) {
      return reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Access denied" },
        timestamp: new Date().toISOString(),
      });
    }

    // Soft delete
    await db.update(docIn).set({ docStatus: "CANCELLED", dateDeleted: new Date() }).where(eq(docIn.id, id));

    return reply.send({
      success: true,
      data: { message: "Document deleted" },
      timestamp: new Date().toISOString(),
    });
  });

  // GET /api/v1/documents/logs — audit log (admin/manager)
  app.get("/logs", async (request, reply) => {
    if (!request.roles?.includes("ADMIN") && !request.roles?.includes("MANAGER")) {
      return reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Manager or Admin role required" },
        timestamp: new Date().toISOString(),
      });
    }

    const { page = "1", limit = "50" } = request.query as Record<string, string>;
    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.min(200, Math.max(1, parseInt(limit, 10)));
    const offset = (pageNum - 1) * limitNum;

    const rows = await db.select().from(docLog).orderBy(desc(docLog.dateCreated)).limit(limitNum).offset(offset);
    const [{ total }] = await db.select({ total: count() }).from(docLog);

    return reply.send({
      success: true,
      data: {
        data: rows,
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

  // POST /api/v1/documents/upload — upload a document
  app.post("/upload", async (request, reply) => {
    const userId = request.userId;
    if (!userId) {
      return reply.status(401).send({
        success: false,
        error: { code: "UNAUTHORIZED", message: "Not authenticated" },
        timestamp: new Date().toISOString(),
      });
    }

    const data = await request.file().catch(() => null);
    if (!data) {
      return reply.status(400).send({
        success: false,
        error: { code: "NO_FILE", message: "No file uploaded" },
        timestamp: new Date().toISOString(),
      });
    }

    const buffer = await data.toBuffer();
    const fileName = data.filename ?? "untitled";
    const mimeType = data.mimetype ?? "application/octet-stream";
    const fileSize = buffer.length;

    // Generate a UUID-based filename for storage
    const storedFileName = `${crypto.randomUUID()}_${fileName.replace(/[^a-zA-Z0-9._-]/g, "_")}`;

    // In production, save to storage; in dev, we store metadata only
    const filePath = `./data/documents/${storedFileName}`;

    // Determine page count estimate (rough for non-PDF, real count would need pdf-parse)
    let pageCount = 0;
    let docType = "PRINT_IN";

    if (mimeType === "application/pdf") {
      docType = "PRINT_IN";
      pageCount = Math.max(1, Math.ceil(fileSize / 50000)); // rough estimate: 50KB/page
    } else if (mimeType.includes("word") || mimeType.includes("document")) {
      docType = "PRINT_IN";
      pageCount = Math.max(1, Math.ceil(fileSize / 15000));
    } else if (mimeType.includes("presentation") || mimeType.includes("powerpoint")) {
      docType = "PRINT_IN";
      pageCount = Math.max(1, Math.ceil(fileSize / 20000));
    } else if (mimeType === "text/plain") {
      docType = "PRINT_IN";
      pageCount = Math.max(1, Math.ceil(fileSize / 3000));
    }

    // Create DocIn record
    const [doc] = await db
      .insert(docIn)
      .values({
        uuid: crypto.randomUUID(),
        userId,
        userName: (request as any).userName ?? "unknown",
        docName: fileName,
        docType: docType as any,
        docStatus: mimeType === "application/pdf" ? "PROCESSING" : "PENDING",
        filePath,
        fileSize,
        mimeType,
        pageCount,
        createdBy: "WEB",
        sourceIp: request.ip,
        sourceInfo: `Upload: ${fileName}`,
        defaultCopies: 1,
        defaultDuplex: "NONE",
        defaultColorMode: "AUTO",
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days
      })
      .returning();

    // Simulate processing: set to READY after a short delay (in production this would be a BullMQ job)
    setTimeout(async () => {
      await db.update(docIn)
        .set({ docStatus: "READY", dateModified: new Date() })
        .where(eq(docIn.id, doc.id))
        .catch(() => {}); // swallow errors in background
      broadcastSseEvent("document:created", {
        id: doc.id,
        docName: doc.docName,
        docStatus: "READY",
        userId,
        userName: (request as any).userName ?? "unknown",
      });
    }, 2000);

    broadcastSseEvent("document:created", {
      id: doc.id,
      docName: doc.docName,
      docStatus: doc.docStatus,
      pageCount,
      userId,
      userName: (request as any).userName ?? "unknown",
    });

    return reply.status(201).send({
      success: true,
      data: { id: doc.id, docName: doc.docName, docStatus: doc.docStatus, pageCount: doc.pageCount },
      timestamp: new Date().toISOString(),
    });
  });

  // POST /api/v1/documents/:id/release — release print job
  app.post<{ Params: { id: string } }>("/:id/release", async (request, reply) => {
    const userId = request.userId;
    if (!userId) {
      return reply.status(401).send({
        success: false,
        error: { code: "UNAUTHORIZED", message: "Not authenticated" },
        timestamp: new Date().toISOString(),
      });
    }

    const docId = parseInt(request.params.id, 10);
    if (isNaN(docId)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid document ID" },
        timestamp: new Date().toISOString(),
      });
    }

    const [doc] = await db.select().from(docIn).where(eq(docIn.id, docId)).limit(1);
    if (!doc) {
      return reply.status(404).send({
        success: false,
        error: { code: "DOC_NOT_FOUND", message: "Document not found" },
        timestamp: new Date().toISOString(),
      });
    }

    if (doc.userId !== userId && !request.roles?.includes("ADMIN") && !request.roles?.includes("MANAGER")) {
      return reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Access denied" },
        timestamp: new Date().toISOString(),
      });
    }

    const schema = z.object({
      printerId: z.number(),
      copies: z.number().min(1).max(100).default(1),
      duplex: z.enum(["NONE", "PORTRAIT", "LANDSCAPE"]).default("NONE"),
      colorMode: z.enum(["AUTO", "MONOCHROME", "COLOR"]).default("AUTO"),
      nUp: z.enum(["1", "2", "4", "6", "9", "16"]).default("1"),
      paperSize: z.string().default("A4"),
      ecoPrint: z.boolean().default(false),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    const { printerId, copies, duplex, colorMode, nUp, paperSize, ecoPrint } = body.data;

    // Get printer info
    const [printer] = await db.select().from(printers).where(eq(printers.id, printerId)).limit(1);
    if (!printer) {
      return reply.status(404).send({
        success: false,
        error: { code: "PRINTER_NOT_FOUND", message: "Printer not found" },
        timestamp: new Date().toISOString(),
      });
    }

    if (!printer.isEnabled) {
      return reply.status(400).send({
        success: false,
        error: { code: "PRINTER_DISABLED", message: "Printer is disabled" },
        timestamp: new Date().toISOString(),
      });
    }

    // Get user's account
    const [account] = await db.select().from(userAccounts).where(eq(userAccounts.userId, userId)).limit(1);

    // Calculate cost
    const pageCount = doc.pageCount || 1;
    const costPerPage = ecoPrint
      ? Number(printer.ecoPrintCostPerPage ?? "0.005")
      : colorMode === "MONOCHROME"
        ? Number(printer.costPerPageMono ?? "0.01")
        : Number(printer.costPerPageColor ?? "0.05");
    const totalCost = (costPerPage * pageCount * copies).toFixed(4);

    // Check balance
    if (account) {
      const balance = Number(account.balance);
      const cost = Number(totalCost);
      if (balance < cost && !request.roles?.includes("ADMIN")) {
        return reply.status(400).send({
          success: false,
          error: { code: "INSUFFICIENT_BALANCE", message: `Insufficient balance. Need ${totalCost}, have ${balance.toFixed(4)}` },
          timestamp: new Date().toISOString(),
        });
      }
      // Deduct balance
      await db.update(userAccounts)
        .set({ balance: (balance - cost).toFixed(4), dateModified: new Date() })
        .where(eq(userAccounts.id, account.id));
    }

    // Update doc status
    await db.update(docIn)
      .set({ docStatus: "RELEASED", dateModified: new Date() })
      .where(eq(docIn.id, docId));

    // Create print job
    const [printJob] = await db
      .insert(printIn)
      .values({
        uuid: crypto.randomUUID(),
        docInId: docId,
        userId,
        userName: (request as any).userName ?? "unknown",
        printerId,
        printerName: printer.displayName || printer.name,
        copyCount: copies,
        duplex,
        colorMode,
        nUp,
        paperSize,
        ecoPrint,
        estimatedPages: pageCount * copies,
        estimatedCost: totalCost,
        accountId: account?.id,
        status: "QUEUED",
      })
      .returning();

    // Create DocLog entry
    await db.insert(docLog).values({
      uuid: crypto.randomUUID(),
      userId,
      userName: (request as any).userName ?? "unknown",
      docName: doc.docName,
      docSize: doc.fileSize,
      docPageCount: pageCount,
      printerId,
      printerName: printer.displayName || printer.name,
      pagesPrinted: pageCount * copies,
      sheetsPrinted: duplex !== "NONE" ? Math.ceil(pageCount * copies / 2) : pageCount * copies,
      accountId: account?.id,
      accountName: account?.accountName ?? "unknown",
      costPerPage: costPerPage.toString(),
      totalCost,
      copyCount: copies,
      duplex,
      colorMode,
      nUp,
      paperSize,
      ecoPrint,
      jobId: String(printJob.id),
      jobStatus: "QUEUED",
    });

    // Broadcast SSE events (PrintFlowLite: AdminPublisher.notifyUserEvent)
    broadcastSseEvent("document:updated", {
      id: docId,
      docName: doc.docName,
      docStatus: "RELEASED",
      userId,
      userName: (request as any).userName ?? "unknown",
    });
    broadcastSseEvent("printjob:started", {
      jobId: printJob.id,
      docId,
      docName: doc.docName,
      printerId,
      printerName: printer.displayName || printer.name,
      pagesPrinted: pageCount * copies,
      totalCost,
      userId,
      userName: (request as any).userName ?? "unknown",
    });

    return reply.status(201).send({
      success: true,
      data: { jobId: printJob.id, totalCost, pagesPrinted: pageCount * copies },
      timestamp: new Date().toISOString(),
    });
  });

  // GET /api/v1/documents/:id/overlay — get page overlay (SVG or JSON)
  app.get<{ Params: { id: string } }>("/:id/overlay", async (request, reply) => {
    const docId = parseInt(request.params.id, 10);
    if (isNaN(docId)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid document ID" },
        timestamp: new Date().toISOString(),
      });
    }

    const [doc] = await db.select().from(docIn).where(eq(docIn.id, docId)).limit(1);
    if (!doc) {
      return reply.status(404).send({
        success: false,
        error: { code: "DOC_NOT_FOUND", message: "Document not found" },
        timestamp: new Date().toISOString(),
      });
    }

    // Ownership check
    if (doc.userId !== request.userId && !request.roles?.includes("ADMIN") && !request.roles?.includes("MANAGER")) {
      return reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Access denied" },
        timestamp: new Date().toISOString(),
      });
    }

    // In production, load overlay from storage; for now return empty
    // PrintFlowLite stores overlay as JSON in tbl_doc_in overlay_data or file
    return reply.send({
      success: true,
      data: { svg64: null, json64: null },
      timestamp: new Date().toISOString(),
    });
  });

  // POST /api/v1/documents/:id/overlay — save page overlay (SVG + JSON)
  app.post<{ Params: { id: string } }>("/:id/overlay", async (request, reply) => {
    const docId = parseInt(request.params.id, 10);
    if (isNaN(docId)) {
      return reply.status(400).send({
        success: false,
        error: { code: "INVALID_ID", message: "Invalid document ID" },
        timestamp: new Date().toISOString(),
      });
    }

    const [doc] = await db.select().from(docIn).where(eq(docIn.id, docId)).limit(1);
    if (!doc) {
      return reply.status(404).send({
        success: false,
        error: { code: "DOC_NOT_FOUND", message: "Document not found" },
        timestamp: new Date().toISOString(),
      });
    }

    if (doc.userId !== request.userId && !request.roles?.includes("ADMIN")) {
      return reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Access denied" },
        timestamp: new Date().toISOString(),
      });
    }

    const schema = z.object({
      svgBase64: z.string().optional(),
      jsonBase64: z.string().optional(),
    });
    const body = schema.safeParse(request.body);
    if (!body.success) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: body.error.message },
        timestamp: new Date().toISOString(),
      });
    }

    // In production: save to tbl_doc_in.overlay_data or storage file
    // For now, log and return success
    app.log.info({
      docId,
      docName: doc.docName,
      hasSvg: !!body.data.svgBase64,
      hasJson: !!body.data.jsonBase64,
    }, "Page overlay saved");

    return reply.send({
      success: true,
      data: { message: "Overlay saved" },
      timestamp: new Date().toISOString(),
    });
  });
}

