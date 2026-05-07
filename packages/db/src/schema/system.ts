import { pgTable, text, timestamp, serial, integer } from "drizzle-orm/pg-core";
import { relations } from "drizzle-orm";
import { users } from "./users";

// ─── Config Properties ────────────────────────────────────────────────────────

export const configProperties = pgTable("tbl_config_property", {
  id: serial("id").primaryKey(),
  propKey: text("prop_key").notNull().unique(),
  propValue: text("prop_value"),
  description: text("description"),
  category: text("category").default("GENERAL"),
  isSecret: text("is_secret").default("false"), // stored as string "true"/"false"
  isSystem: text("is_system").default("false"),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── Sequences ────────────────────────────────────────────────────────────────

export const sequences = pgTable("tbl_sequence", {
  id: serial("id").primaryKey(),
  seqName: text("seq_name").notNull().unique(),
  seqValue: text("seq_value").notNull().default("0"),
  seqIncrement: text("seq_increment").notNull().default("1"),
});

// ─── App Logs ────────────────────────────────────────────────────────────────

export const appLogs = pgTable(
  "tbl_app_log",
  {
    id: serial("id").primaryKey(),
    logLevel: text("log_level").notNull().default("INFO"), // TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    logger: text("logger").notNull(),
    message: text("message").notNull(),
    context: text("context"), // JSON string for structured data
    userId: integer("user_id").references(() => users.id),
    userName: text("user_name"),
    ipAddress: text("ip_address"),
    exception: text("exception"), // stack trace if error
    dateCreated: timestamp("date_created").notNull().defaultNow(),
  },
  (table) => ({
    logLevelIdx: text("log_level"),
    dateIdx: timestamp("date_created"),
  }),
);

// ─── Relations ───────────────────────────────────────────────────────────────

export const appLogsRelations = relations(appLogs, ({ one }) => ({
  user: one(users, { fields: [appLogs.userId], references: [users.id] }),
}));
