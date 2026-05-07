import { z } from "zod";

export const appConfigSchema = z.object({
  nodeEnv: z.enum(["development", "test", "production"]).default("development"),
  logLevel: z.enum(["fatal", "error", "warn", "info", "debug", "trace"]).default("info"),
  port: z.number().int().min(1024).max(65535).default(3001),

  database: z.object({
    url: z.string().url(),
    poolMin: z.number().int().min(0).default(2),
    poolMax: z.number().int().min(1).default(10),
  }),

  redis: z.object({
    url: z.string().url(),
  }),

  auth: z.object({
    secret: z.string().min(32),
    expiresIn: z.string().default("7d"),
    totpIssuer: z.string().default("PrintFlow"),
  }),

  storage: z.object({
    documentPath: z.string().default("./data/documents"),
    thumbnailPath: z.string().default("./data/thumbnails"),
  }),

  print: z.object({
    ippServerPort: z.number().int().default(6310),
    cupsHost: z.string().default("localhost"),
    cupsPort: z.number().int().default(631),
  }),

  email: z.object({
    smtpHost: z.string().optional(),
    smtpPort: z.number().int().default(587),
    smtpUser: z.string().optional(),
    smtpPass: z.string().optional(),
    smtpFrom: z.string().email().default("noreply@printflow.local"),
    imapHost: z.string().optional(),
    imapPort: z.number().int().default(993),
    imapUser: z.string().optional(),
    imapPass: z.string().optional(),
  }),

  soffice: z.object({
    path: z.string().default("/usr/bin/soffice"),
  }),

  appUrl: z.string().url().default("http://localhost:3000"),
  apiUrl: z.string().url().default("http://localhost:3001"),
});

export type AppConfig = z.infer<typeof appConfigSchema>;

function loadConfig(): AppConfig {
  const raw = {
    nodeEnv: process.env["NODE_ENV"] ?? "development",
    logLevel: process.env["LOG_LEVEL"] ?? "info",
    port: process.env["PORT"] ? Number(process.env["PORT"]) : 3001,
    database: {
      url: process.env["DATABASE_URL"] ?? "postgresql://printflow:password@localhost:5432/printflow",
      poolMin: process.env["DATABASE_POOL_MIN"] ? Number(process.env["DATABASE_POOL_MIN"]) : 2,
      poolMax: process.env["DATABASE_POOL_MAX"] ? Number(process.env["DATABASE_POOL_MAX"]) : 10,
    },
    redis: {
      url: process.env["REDIS_URL"] ?? "redis://localhost:6379",
    },
    auth: {
      secret: process.env["AUTH_SECRET"] ?? "dev-secret-change-in-production-min-32-chars",
      expiresIn: process.env["AUTH_EXPIRES_IN"] ?? "7d",
      totpIssuer: process.env["TOTP_ISSUER"] ?? "PrintFlow",
    },
    storage: {
      documentPath: process.env["DOCUMENT_STORAGE_PATH"] ?? "./data/documents",
      thumbnailPath: process.env["THUMBNAIL_STORAGE_PATH"] ?? "./data/thumbnails",
    },
    print: {
      ippServerPort: process.env["IPP_SERVER_PORT"] ? Number(process.env["IPP_SERVER_PORT"]) : 6310,
      cupsHost: process.env["CUPS_HOST"] ?? "localhost",
      cupsPort: process.env["CUPS_PORT"] ? Number(process.env["CUPS_PORT"]) : 631,
    },
    email: {
      smtpHost: process.env["SMTP_HOST"],
      smtpPort: process.env["SMTP_PORT"] ? Number(process.env["SMTP_PORT"]) : 587,
      smtpUser: process.env["SMTP_USER"],
      smtpPass: process.env["SMTP_PASS"],
      smtpFrom: process.env["SMTP_FROM"] ?? "noreply@printflow.local",
      imapHost: process.env["IMAP_HOST"],
      imapPort: process.env["IMAP_PORT"] ? Number(process.env["IMAP_PORT"]) : 993,
      imapUser: process.env["IMAP_USER"],
      imapPass: process.env["IMAP_PASS"],
    },
    soffice: {
      path: process.env["SOFFICE_PATH"] ?? "/usr/bin/soffice",
    },
    appUrl: process.env["NEXT_PUBLIC_APP_URL"] ?? "http://localhost:3000",
    apiUrl: process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001",
  };

  return appConfigSchema.parse(raw);
}

let cached: AppConfig | null = null;

export function getAppConfig(): AppConfig {
  if (!cached) {
    cached = loadConfig();
  }
  return cached;
}
