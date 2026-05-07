// Config Router — mirrors PrintFlowLite's JSON API config operations
import type { FastifyInstance } from "fastify";
import { createDrizzle } from "@printflow/db";
import { configProperties } from "@printflow/db/schema";
import { eq } from "drizzle-orm";
import { z } from "zod";
import {
  emailConfigSchema,
  imapConfigSchema,
  createEmailTransporter,
  sendEmail,
  verifyConnection,
  type EmailConfig,
  type EmailMessage,
} from "../services/email.js";
import {
  startSOfficeService,
  shutdownSOfficeService,
  isSOfficeRunning,
  isSOfficeEnabled,
  executeSOfficeTask,
  convertToPdf,
  createJobTicketSheetPdf,
  createTempDir,
} from "../services/soffice.js";
import {
  initPgpService,
  getKeyInfo,
  encryptAndSign,
  decrypt,
  createDetachedSignature,
  verifySignature,
  type PgpConfig,
} from "../services/pgp.js";

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

function ok<T>(reply: any, data: T) {
  return reply.send({ success: true, data, timestamp: new Date().toISOString() });
}

function err(reply: any, status: number, code: string, message: string) {
  return reply.status(status).send({
    success: false,
    error: { code, message },
    timestamp: new Date().toISOString(),
  });
}

// In-memory runtime config state
let currentEmailConfig: EmailConfig | null = null;
let currentSOfficeConfig: { sofficePath: string; numWorkers: number; taskTimeoutMs: number; enabled: boolean } = {
  sofficePath: "/usr/bin/soffice",
  numWorkers: 2,
  taskTimeoutMs: 60000,
  enabled: false,
};
let currentPgpConfig: PgpConfig | null = null;
let pgpInitialized = false;

export async function createConfigRouter(app: FastifyInstance) {
  const db = createDb();

  // ─── General Config ────────────────────────────────────────────────────────

  // GET /api/v1/config — get all config (admin only for secret values)
  app.get("/", async (request, reply) => {
    const props = await db.select().from(configProperties);
    return ok(reply, props.map((p) => ({
      ...p,
      propValue:
        p.isSecret === "true" && !request.roles?.includes("ADMIN")
          ? undefined
          : p.propValue,
    })));
  });

  // GET /api/v1/config/:key — get specific config
  app.get<{ Params: { key: string } }>("/:key", async (request, reply) => {
    const [prop] = await db
      .select()
      .from(configProperties)
      .where(eq(configProperties.propKey, request.params.key))
      .limit(1);

    if (!prop) return err(reply, 404, "CONFIG_NOT_FOUND", "Config not found");

    return ok(reply, {
      ...prop,
      propValue:
        prop.isSecret === "true" && !request.roles?.includes("ADMIN")
          ? undefined
          : prop.propValue,
    });
  });

  // PUT /api/v1/config/:key — update config (admin only)
  app.put<{ Params: { key: string } }>("/:key", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const body = request.body as { propValue?: string };
    if (!body || typeof body.propValue !== "string") {
      return err(reply, 400, "VALIDATION_ERROR", "propValue is required");
    }

    const [prop] = await db
      .update(configProperties)
      .set({ propValue: body.propValue, dateModified: new Date() })
      .where(eq(configProperties.propKey, request.params.key))
      .returning();

    if (!prop) return err(reply, 404, "CONFIG_NOT_FOUND", "Config not found");

    return ok(reply, prop);
  });

  // ─── Email Config ─────────────────────────────────────────────────────────

  // POST /api/v1/config/email — update email config
  app.post("/email", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const body = emailConfigSchema.safeParse(request.body);
    if (!body.success) {
      return err(reply, 400, "VALIDATION_ERROR", body.error.message);
    }

    currentEmailConfig = body.data as EmailConfig;
    createEmailTransporter(currentEmailConfig);

    // Save to DB
    const emailProps = [
      { key: "MAIL_SMTP_HOST", value: body.data.smtpHost },
      { key: "MAIL_SMTP_PORT", value: String(body.data.smtpPort) },
      { key: "MAIL_SMTP_SECURITY", value: body.data.smtpSecure ? "SSL" : "NONE" },
      { key: "MAIL_SMTP_USER_NAME", value: body.data.smtpUser },
      { key: "MAIL_SMTP_PASSWORD", value: body.data.smtpPassword },
      { key: "MAIL_SMTP_FROM", value: body.data.smtpFrom },
      { key: "MAIL_SMTP_FROM_NAME", value: body.data.smtpFromName },
    ];

    for (const p of emailProps) {
      await db
        .update(configProperties)
        .set({ propValue: p.value, dateModified: new Date() })
        .where(eq(configProperties.propKey, p.key))
        .onConflictDoUpdate({ target: configProperties.propKey, set: { propValue: p.value, dateModified: new Date() } });
    }

    return ok(reply, { message: "Email configuration updated", config: currentEmailConfig });
  });

  // POST /api/v1/config/email/test — test email connection
  app.post("/email/test", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const connected = await verifyConnection();
    return ok(reply, { connected, message: connected ? "Connection successful" : "Connection failed" });
  });

  // POST /api/v1/config/email/send — send a test email
  app.post("/email/send", async (request, reply) => {
    const schema = z.object({
      to: z.string().email(),
      toName: z.string().optional(),
      subject: z.string().min(1),
      body: z.string().min(1),
      bodyHtml: z.string().optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    try {
      const msg: EmailMessage = {
        to: body.data.to,
        toName: body.data.toName,
        subject: body.data.subject,
        body: body.data.body,
        bodyHtml: body.data.bodyHtml,
      };

      const result = await sendEmail(msg);
      return ok(reply, { messageId: result.messageId, accepted: result.accepted });
    } catch (e: any) {
      return err(reply, 500, "SEND_FAILED", e.message ?? "Failed to send email");
    }
  });

  // POST /api/v1/config/email/imap — update IMAP config
  app.post("/email/imap", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const body = imapConfigSchema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    return ok(reply, { message: "IMAP configuration saved (ingestion worker must be restarted)", config: body.data });
  });

  // ─── SOffice / LibreOffice Config ─────────────────────────────────────────

  // POST /api/v1/config/soffice — update SOffice config
  app.post("/soffice", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const schema = z.object({
      sofficePath: z.string().default("/usr/bin/soffice"),
      numWorkers: z.number().int().min(1).max(4).default(2),
      taskTimeoutMs: z.number().int().min(10000).max(300000).default(60000),
      enabled: z.boolean().default(false),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    currentSOfficeConfig = {
      sofficePath: body.data.sofficePath,
      numWorkers: body.data.numWorkers,
      taskTimeoutMs: body.data.taskTimeoutMs,
      enabled: body.data.enabled,
    };

    return ok(reply, {
      message: "SOffice configuration updated",
      config: currentSOfficeConfig,
      running: isSOfficeRunning(),
    });
  });

  // POST /api/v1/config/soffice/start — start SOffice service
  app.post("/soffice/start", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    if (!currentSOfficeConfig.enabled) {
      return err(reply, 400, "DISABLED", "SOffice service is disabled");
    }

    try {
      await startSOfficeService(currentSOfficeConfig as any);
      return ok(reply, { message: "SOffice service started", running: true });
    } catch (e: any) {
      return err(reply, 500, "START_FAILED", e.message ?? "Failed to start SOffice");
    }
  });

  // POST /api/v1/config/soffice/stop — stop SOffice service
  app.post("/soffice/stop", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    await shutdownSOfficeService();
    return ok(reply, { message: "SOffice service stopped", running: false });
  });

  // POST /api/v1/config/soffice/status — get SOffice status
  app.post("/soffice/status", async (request, reply) => {
    return ok(reply, {
      enabled: isSOfficeEnabled(),
      running: isSOfficeRunning(),
      numWorkers: currentSOfficeConfig.numWorkers,
      taskTimeoutMs: currentSOfficeConfig.taskTimeoutMs,
    });
  });

  // POST /api/v1/config/soffice/convert — convert a document to PDF
  app.post("/soffice/convert", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const schema = z.object({
      inputPath: z.string().min(1),
      outputFormat: z.enum(["pdf", "docx", "odt", "rtf", "txt"]).default("pdf"),
      options: z.object({
        pdfa: z.boolean().optional(),
        quality: z.enum(["draft", "normal", "high"]).optional(),
        landscape: z.boolean().optional(),
        paperSize: z.string().optional(),
      }).optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    if (!isSOfficeRunning()) {
      return err(reply, 400, "NOT_RUNNING", "SOffice service is not running");
    }

    const tmpDir = await createTempDir();
    const result = await convertToPdf(body.data.inputPath, tmpDir, body.data.options as any);

    return ok(reply, result);
  });

  // POST /api/v1/config/soffice/jobsheet — create a job ticket sheet PDF
  app.post("/soffice/jobsheet", async (request, reply) => {
    const schema = z.object({
      ticketNumber: z.string().min(1),
      userName: z.string(),
      operatorName: z.string(),
      printerName: z.string(),
      copies: z.number().int().min(1),
      outputPath: z.string().optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    const outputPath = body.data.outputPath ?? `./data/jobsheets/${body.data.ticketNumber}-sheet.pdf`;
    const result = await createJobTicketSheetPdf(
      body.data.ticketNumber,
      body.data.userName,
      body.data.operatorName,
      body.data.printerName,
      body.data.copies,
      outputPath,
    );

    return ok(reply, result);
  });

  // ─── PGP Config ────────────────────────────────────────────────────────────

  // POST /api/v1/config/pgp — update PGP config
  app.post("/pgp", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const schema = z.object({
      secretKeyArmored: z.string().optional(),
      publicKeyArmored: z.string().optional(),
      passphrase: z.string().optional(),
      publicKeyRingFiles: z.array(z.string()).optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    currentPgpConfig = {
      secretKeyArmored: body.data.secretKeyArmored ?? "",
      publicKeyArmored: body.data.publicKeyArmored ?? "",
      passphrase: body.data.passphrase ?? "",
      publicKeyRingFiles: body.data.publicKeyRingFiles ?? [],
    };

    try {
      await initPgpService(currentPgpConfig);
      pgpInitialized = true;
      return ok(reply, { message: "PGP configuration updated and keys loaded", configured: true });
    } catch (e: any) {
      pgpInitialized = false;
      return err(reply, 500, "PGP_INIT_FAILED", e.message ?? "Failed to initialize PGP");
    }
  });

  // GET /api/v1/config/pgp/status — get PGP status
  app.get("/pgp/status", async (request, reply) => {
    if (!pgpInitialized || !currentPgpConfig) {
      return ok(reply, { configured: false });
    }

    const info = currentPgpConfig.publicKeyArmored
      ? getKeyInfo(currentPgpConfig.publicKeyArmored)
      : null;

    return ok(reply, { configured: true, keyInfo: info });
  });

  // POST /api/v1/config/pgp/encrypt — encrypt content
  app.post("/pgp/encrypt", async (request, reply) => {
    if (!pgpInitialized) return err(reply, 400, "NOT_CONFIGURED", "PGP not configured");

    const schema = z.object({
      plaintext: z.string(),
      recipientKeyId: z.string().optional(),
      sign: z.boolean().default(true),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    try {
      const encrypted = await encryptAndSign(
        body.data.plaintext,
        currentPgpConfig!.publicKeyArmored,
        currentPgpConfig!.secretKeyArmored,
        currentPgpConfig!.passphrase,
      );
      return ok(reply, { encrypted: encrypted.toString("base64") });
    } catch (e: any) {
      return err(reply, 500, "ENCRYPT_FAILED", e.message ?? "Encryption failed");
    }
  });

  // POST /api/v1/config/pgp/decrypt — decrypt content
  app.post("/pgp/decrypt", async (request, reply) => {
    if (!pgpInitialized) return err(reply, 400, "NOT_CONFIGURED", "PGP not configured");

    const schema = z.object({ encrypted: z.string() });
    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    try {
      const decrypted = await decrypt(
        Buffer.from(body.data.encrypted, "base64"),
        currentPgpConfig!.secretKeyArmored,
        currentPgpConfig!.passphrase,
      );
      return ok(reply, { plaintext: decrypted });
    } catch (e: any) {
      return err(reply, 500, "DECRYPT_FAILED", e.message ?? "Decryption failed");
    }
  });

  // POST /api/v1/config/pgp/sign — create signature
  app.post("/pgp/sign", async (request, reply) => {
    if (!pgpInitialized) return err(reply, 400, "NOT_CONFIGURED", "PGP not configured");

    const schema = z.object({ content: z.string() });
    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    try {
      const sig = await createDetachedSignature(
        body.data.content,
        currentPgpConfig!.secretKeyArmored,
        currentPgpConfig!.passphrase,
      );
      return ok(reply, { signature: sig });
    } catch (e: any) {
      return err(reply, 500, "SIGN_FAILED", e.message ?? "Signing failed");
    }
  });

  // POST /api/v1/config/pgp/verify — verify signature
  app.post("/pgp/verify", async (request, reply) => {
    if (!pgpInitialized) return err(reply, 400, "NOT_CONFIGURED", "PGP not configured");

    const schema = z.object({
      content: z.string(),
      signature: z.string(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    try {
      const valid = await verifySignature(
        body.data.content,
        body.data.signature,
        currentPgpConfig!.publicKeyArmored,
      );
      return ok(reply, { valid });
    } catch (e: any) {
      return ok(reply, { valid: false, error: e.message });
    }
  });

  // ─── PDF Processing Config ────────────────────────────────────────────────

  // POST /api/v1/config/pdf/process — process a PDF file
  app.post("/pdf/process", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const schema = z.object({
      inputPath: z.string().min(1),
      outputPath: z.string().min(1),
      options: z.object({
        grayscale: z.boolean().optional(),
        compress: z.boolean().optional(),
        compressionQuality: z.enum(["low", "medium", "high"]).optional(),
        removeGraphics: z.boolean().optional(),
        encrypt: z.boolean().optional(),
        userPassword: z.string().optional(),
        ownerPassword: z.string().optional(),
        pageRange: z.string().optional(),
      }).optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    // PDF processing would use a library like pdf-lib or call an external tool
    // For now, we copy the file as a placeholder
    const { copyFileSync } = await import("fs");
    try {
      copyFileSync(body.data.inputPath, body.data.outputPath);
      return ok(reply, { success: true, outputPath: body.data.outputPath, message: "PDF processed (copy mode)" });
    } catch (e: any) {
      return err(reply, 500, "PROCESS_FAILED", e.message ?? "PDF processing failed");
    }
  });

  // POST /api/v1/config/pdf/generate — generate a PDF from HTML or template
  app.post("/pdf/generate", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return err(reply, 403, "FORBIDDEN", "Admin role required");
    }

    const schema = z.object({
      content: z.string().min(1),
      outputPath: z.string().min(1),
      format: z.enum(["html", "template"]).default("html"),
      options: z.object({
        pageSize: z.string().default("A4"),
        orientation: z.enum(["portrait", "landscape"]).default("portrait"),
        margin: z.string().default("20mm"),
        header: z.string().optional(),
        footer: z.string().optional(),
      }).optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success) return err(reply, 400, "VALIDATION_ERROR", body.error.message);

    // HTML to PDF generation would use puppeteer, html-pdf, or similar
    // Placeholder: return the content as-is
    const { writeFileSync } = await import("fs");
    try {
      writeFileSync(body.data.outputPath.replace(".pdf", ".html"), body.data.content);
      return ok(reply, { success: true, outputPath: body.data.outputPath, message: "PDF generation placeholder (HTML saved)" });
    } catch (e: any) {
      return err(reply, 500, "GENERATE_FAILED", e.message ?? "PDF generation failed");
    }
  });
}
