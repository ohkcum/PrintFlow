import {
  pgTable,
  text,
  timestamp,
  boolean,
  integer,
  decimal,
  serial,
  pgEnum,
} from "drizzle-orm/pg-core";
import { relations } from "drizzle-orm";
import { users } from "./users";
import { printers } from "./printers";
import { accounts } from "./financial";

// ─── Enums ────────────────────────────────────────────────────────────────────

export const docStatusEnum = pgEnum("doc_status", [
  "PENDING",
  "PROCESSING",
  "READY",
  "RELEASED",
  "CANCELLED",
  "EXPIRED",
  "ERROR",
]);

export const docTypeEnum = pgEnum("doc_type", [
  "PRINT_IN",
  "PRINT_OUT",
  "PDF_IN",
  "PDF_OUT",
  "SAFE_PAGE",
  "JOB_TICKET",
]);

// ─── Doc Log (Audit) ──────────────────────────────────────────────────────────

export const docLog = pgTable("tbl_doc_log", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),

  // Who & what
  userId: integer("user_id").references(() => users.id),
  userName: text("user_name"),
  userFullName: text("user_full_name"),

  // Document info
  docName: text("doc_name").notNull(),
  docSize: integer("doc_size"), // bytes
  docPageCount: integer("doc_page_count"),

  // Job details
  printerId: integer("printer_id").references(() => printers.id),
  printerName: text("printer_name"),

  // Pages printed
  pagesPrinted: integer("pages_printed").default(0),
  sheetsPrinted: integer("sheets_printed").default(0),

  // Financial
  accountId: integer("account_id").references(() => accounts.id),
  accountName: text("account_name"),
  costPerPage: decimal("cost_per_page", { precision: 10, scale: 4 }),
  totalCost: decimal("total_cost", { precision: 10, scale: 4 }).default("0"),

  // Job options
  copyCount: integer("copy_count").default(1),
  duplex: text("duplex").default("NONE"),
  colorMode: text("color_mode").default("AUTO"),
  nUp: text("n_up").default("1"),
  paperSize: text("paper_size").default("A4"),
  orientation: text("orientation").default("PORTRAIT"),
  ecoPrint: boolean("eco_print").notNull().default(false),

  // Status & time
  jobId: text("job_id"),
  jobStatus: text("job_status"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateStarted: timestamp("date_started"),
  dateCompleted: timestamp("date_completed"),

  // Refund
  refunded: boolean("refunded").notNull().default(false),
  refundedBy: integer("refunded_by").references(() => users.id),
  refundReason: text("refund_reason"),
  dateRefunded: timestamp("date_refunded"),

  // Delegation
  delegatedBy: integer("delegated_by").references(() => users.id),
  delegatedByName: text("delegated_by_name"),
});

// ─── Doc In (SafePages / incoming documents) ───────────────────────────────────

export const docIn = pgTable("tbl_doc_in", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),

  // Ownership
  userId: integer("user_id")
    .notNull()
    .references(() => users.id, { onDelete: "cascade" }),
  userName: text("user_name"),

  // Document
  docName: text("doc_name").notNull(),
  docType: docTypeEnum("doc_type").notNull().default("PRINT_IN"),
  docStatus: docStatusEnum("doc_status").notNull().default("PENDING"),

  // Storage
  filePath: text("file_path").notNull(),
  fileSize: integer("file_size").notNull(), // bytes
  mimeType: text("mime_type").default("application/pdf"),
  pageCount: integer("page_count").default(0),
  md5Hash: text("md5_hash"),

  // Thumbnail
  thumbnailPath: text("thumbnail_path"),

  // Metadata
  createdBy: text("created_by").notNull(), // 'WEB', 'EMAIL', 'IPP', 'API'
  sourceIp: text("source_ip"),
  sourceInfo: text("source_info"),

  // Print options (stored defaults)
  defaultCopies: integer("default_copies").default(1),
  defaultDuplex: text("default_duplex").default("NONE"),
  defaultColorMode: text("default_color_mode").default("AUTO"),

  // Expiry
  expiresAt: timestamp("expires_at"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
  dateDeleted: timestamp("date_deleted"),

  // Notes
  notes: text("notes"),
});

// ─── Doc Out (outgoing / released documents) ───────────────────────────────────

export const docOut = pgTable("tbl_doc_out", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  docInId: integer("doc_in_id").references(() => docIn.id),

  userId: integer("user_id").references(() => users.id),
  userName: text("user_name"),

  docName: text("doc_name").notNull(),
  filePath: text("file_path"),
  fileSize: integer("file_size"),
  mimeType: text("mime_type").default("application/pdf"),

  // Destination
  destination: text("destination").notNull(), // 'IPP', 'EMAIL', 'PDF', 'WEB'
  destinationAddress: text("destination_address"), // email or printer URI

  status: docStatusEnum("doc_status").notNull().default("PENDING"),

  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateCompleted: timestamp("date_completed"),
  dateDeleted: timestamp("date_deleted"),
});

// ─── Print In (print input jobs) ─────────────────────────────────────────────

export const printIn = pgTable("tbl_print_in", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),

  docInId: integer("doc_in_id").references(() => docIn.id),
  userId: integer("user_id").references(() => users.id),
  userName: text("user_name"),

  printerId: integer("printer_id").references(() => printers.id),
  printerName: text("printer_name"),

  // Job options
  copyCount: integer("copy_count").default(1),
  pagesRange: text("pages_range"), // "1-5,8,10-12"
  duplex: text("duplex").default("NONE"), // NONE, PORTRAIT, LANDSCAPE
  tumble: text("tumble").default("NONE"),
  colorMode: text("color_mode").default("AUTO"),
  nUp: text("n_up").default("1"),
  paperSize: text("paper_size").default("A4"),
  orientation: text("orientation").default("PORTRAIT"),
  staple: boolean("staple").default(false),
  punch: boolean("punch").default(false),
  fold: boolean("fold").default(false),
  banner: boolean("banner").default(false),
  ecoPrint: boolean("eco_print").default(false),
  letterhead: text("letterhead"), // path to letterhead PDF

  // Cost estimate
  estimatedPages: integer("estimated_pages").default(0),
  estimatedCost: decimal("estimated_cost", { precision: 10, scale: 4 }).default(
    "0",
  ),
  accountId: integer("account_id").references(() => accounts.id),

  // Job ID from printer/CUPS
  cupsJobId: text("cups_job_id"),

  status: text("status").notNull().default("QUEUED"), // from printJobStatuses

  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── PDF Out ─────────────────────────────────────────────────────────────────

export const pdfOut = pgTable("tbl_pdf_out", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),

  docInId: integer("doc_in_id").references(() => docIn.id),
  userId: integer("user_id").references(() => users.id),

  docName: text("doc_name").notNull(),
  filePath: text("file_path").notNull(),
  fileSize: integer("file_size"),
  pageCount: integer("page_count"),

  // Transformation options
  options: text("options"), // JSON string of transformation options

  // PGP
  pgpEncrypted: boolean("pgp_encrypted").default(false),
  pgpKeyId: text("pgp_key_id"),

  status: docStatusEnum("doc_status").notNull().default("PENDING"),

  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateCompleted: timestamp("date_completed"),
});

// ─── Relations ───────────────────────────────────────────────────────────────

export const docLogRelations = relations(docLog, ({ one }) => ({
  user: one(users, { fields: [docLog.userId], references: [users.id] }),
  printer: one(printers, {
    fields: [docLog.printerId],
    references: [printers.id],
  }),
  account: one(accounts, {
    fields: [docLog.accountId],
    references: [accounts.id],
  }),
}));

export const docInRelations = relations(docIn, ({ one, many }) => ({
  user: one(users, { fields: [docIn.userId], references: [users.id] }),
  docOuts: many(docOut),
  printIns: many(printIn),
  pdfOuts: many(pdfOut),
}));

export const docOutRelations = relations(docOut, ({ one }) => ({
  docIn: one(docIn, { fields: [docOut.docInId], references: [docIn.id] }),
  user: one(users, { fields: [docOut.userId], references: [users.id] }),
}));

export const printInRelations = relations(printIn, ({ one }) => ({
  docIn: one(docIn, { fields: [printIn.docInId], references: [docIn.id] }),
  user: one(users, { fields: [printIn.userId], references: [users.id] }),
  printer: one(printers, {
    fields: [printIn.printerId],
    references: [printers.id],
  }),
  account: one(accounts, {
    fields: [printIn.accountId],
    references: [accounts.id],
  }),
}));

export const pdfOutRelations = relations(pdfOut, ({ one }) => ({
  docIn: one(docIn, { fields: [pdfOut.docInId], references: [docIn.id] }),
  user: one(users, { fields: [pdfOut.userId], references: [users.id] }),
}));
