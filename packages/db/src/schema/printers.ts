import { pgTable, text, timestamp, boolean, integer, decimal, serial, pgEnum } from "drizzle-orm/pg-core";
import { relations } from "drizzle-orm";

// ─── Enums ────────────────────────────────────────────────────────────────────

export const printerStatusEnum = pgEnum("printer_status", [
  "ONLINE", "OFFLINE", "IDLE", "BUSY", "ERROR", "MAINTENANCE",
]);

export const printerTypeEnum = pgEnum("printer_type", [
  "NETWORK", "USB", "DRIVER", "PDF", "EMAIL", "HOLD",
]);

export const duplexEnum = pgEnum("duplex", ["NONE", "PORTRAIT", "LANDSCAPE"]);
export const tumbleEnum = pgEnum("tumble", ["NONE", "ODD", "EVEN"]);
export const colorModeEnum = pgEnum("color_mode", ["AUTO", "MONOCHROME", "COLOR"]);
export const nUpEnum = pgEnum("n_up", ["1", "2", "4", "6", "9", "16"]);

// ─── Printer Groups ─────────────────────────────────────────────────────────

export const printerGroups = pgTable("tbl_printer_group", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  name: text("name").notNull().unique(),
  description: text("description"),
  displayOrder: integer("display_order").default(0),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── IPP Queues ──────────────────────────────────────────────────────────────

export const ippQueues = pgTable("tbl_ipp_queue", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  name: text("name").notNull(),
  uri: text("uri").notNull(), // e.g. ipp://printer:631/ipp/print
  isEnabled: boolean("is_enabled").notNull().default(true),
  isDefault: boolean("is_default").notNull().default(false),
  printerGroupId: integer("printer_group_id").references(() => printerGroups.id),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── Printers (Proxy Printers) ───────────────────────────────────────────────

export const printers = pgTable("tbl_printer", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  name: text("name").notNull(),
  description: text("description"),

  // IPP / CUPS
  ippQueueId: integer("ipp_queue_id").references(() => ippQueues.id),
  ippPrinterUri: text("ipp_printer_uri"),
  cupsPrinterName: text("cups_printer_name"),

  // Display
  displayName: text("display_name"),
  printerType: printerTypeEnum("printer_type").notNull().default("NETWORK"),
  printerStatus: printerStatusEnum("printer_status").notNull().default("OFFLINE"),

  // Capabilities
  colorMode: colorModeEnum("color_mode").default("AUTO"),
  supportsDuplex: boolean("supports_duplex").notNull().default(true),
  supportsStaple: boolean("supports_staple").notNull().default(false),
  supportsPunch: boolean("supports_punch").notNull().default(false),
  supportsFold: boolean("supports_fold").notNull().default(false),
  supportsBanner: boolean("supports_banner").notNull().default(false),
  maxPaperSize: text("max_paper_size").default("A4"),
  minPaperSize: text("min_paper_size").default("A5"),

  // Group
  printerGroupId: integer("printer_group_id").references(() => printerGroups.id),

  // Costing
  costPerPageMono: decimal("cost_per_page_mono", { precision: 10, scale: 4 }).default("0.01"),
  costPerPageColor: decimal("cost_per_page_color", { precision: 10, scale: 4 }).default("0.05"),
  costPerSheet: decimal("cost_per_sheet", { precision: 10, scale: 4 }).default("0"),
  fixedCost: decimal("fixed_cost", { precision: 10, scale: 4 }).default("0"),
  ecoPrintCostPerPage: decimal("eco_print_cost_per_page", { precision: 10, scale: 4 }).default("0.005"),

  // Flags
  isEnabled: boolean("is_enabled").notNull().default(true),
  isPublic: boolean("is_public").notNull().default(true),
  requireRelease: boolean("require_release").notNull().default(true),
  ecoPrintDefault: boolean("eco_print_default").notNull().default(false),

  // SNMP
  snmpEnabled: boolean("snmp_enabled").notNull().default(false),
  snmpCommunity: text("snmp_community").default("public"),

  // Stats
  totalPrintJobs: integer("total_print_jobs").default(0),
  totalPrintPages: integer("total_print_pages").default(0),

  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── Printer Custom Attributes ──────────────────────────────────────────────

export const printerAttrs = pgTable("tbl_printer_attr", {
  id: serial("id").primaryKey(),
  printerId: integer("printer_id").notNull().references(() => printers.id, { onDelete: "cascade" }),
  attrKey: text("attr_key").notNull(),
  attrValue: text("attr_value"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
});

// ─── Devices (Print Devices) ─────────────────────────────────────────────────

export const devices = pgTable("tbl_device", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  name: text("name").notNull(),
  description: text("description"),
  deviceType: text("device_type").notNull().default("PRINTER"),
  macAddress: text("mac_address"),
  ipAddress: text("ip_address"),
  location: text("location"),
  deviceStatus: printerStatusEnum("device_status").notNull().default("OFFLINE"),
  snmpEnabled: boolean("snmp_enabled").notNull().default(false),
  snmpCommunity: text("snmp_community").default("public"),
  snmpVersion: text("snmp_version").default("2c"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── Device Custom Attributes ───────────────────────────────────────────────

export const deviceAttrs = pgTable("tbl_device_attr", {
  id: serial("id").primaryKey(),
  deviceId: integer("device_id").notNull().references(() => devices.id, { onDelete: "cascade" }),
  attrKey: text("attr_key").notNull(),
  attrValue: text("attr_value"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
});

// ─── Relations ───────────────────────────────────────────────────────────────

export const printerGroupsRelations = relations(printerGroups, ({ many }) => ({
  printers: many(printers),
  queues: many(ippQueues),
}));

export const ippQueuesRelations = relations(ippQueues, ({ one, many }) => ({
  group: one(printerGroups, { fields: [ippQueues.printerGroupId], references: [printerGroups.id] }),
  printers: many(printers),
}));

export const printersRelations = relations(printers, ({ one, many }) => ({
  queue: one(ippQueues, { fields: [printers.ippQueueId], references: [ippQueues.id] }),
  group: one(printerGroups, { fields: [printers.printerGroupId], references: [printerGroups.id] }),
  attrs: many(printerAttrs),
}));

export const devicesRelations = relations(devices, ({ many }) => ({
  attrs: many(deviceAttrs),
}));
