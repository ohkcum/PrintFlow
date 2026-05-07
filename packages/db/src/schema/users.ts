import { pgTable, text, timestamp, boolean, integer, decimal, serial, pgEnum } from "drizzle-orm/pg-core";
import { relations } from "drizzle-orm";
import type { UserRole, UserStatus } from "@printflow/common";

// ─── Enums ────────────────────────────────────────────────────────────────────

export const userRoleEnum = pgEnum("user_role", [
  "ADMIN", "MANAGER", "DELEGATOR", "USER",
  "JOB_TICKET_ISSUER", "MAIL_TICKET_ISSUER",
  "PRINT_SITE_USER", "PGP_USER",
]);

export const userStatusEnum = pgEnum("user_status", [
  "ACTIVE", "BLOCKED", "DELETED", "EXPIRED",
]);

export const userIdMethodEnum = pgEnum("user_id_method", [
  "INTERNAL", "LDAP", "OAUTH",
]);

// ─── Users ────────────────────────────────────────────────────────────────────

export const users = pgTable("tbl_user", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  userName: text("user_name").notNull().unique(),
  fullName: text("full_name").notNull(),
  email: text("email"),

  // Auth
  passwordHash: text("password_hash"),
  userIdMethod: userIdMethodEnum("user_id_method").notNull().default("INTERNAL"),
  oauthProvider: text("oauth_provider"),
  oauthId: text("oauth_id"),

  // Roles (array of roles)
  roles: userRoleEnum("roles").array().notNull().default(["USER"]),

  // Status & quota
  status: userStatusEnum("status").notNull().default("ACTIVE"),
  blockedReason: text("blocked_reason"),

  // Print quotas
  printQuota: decimal("print_quota", { precision: 10, scale: 4 }).default("0"),
  printBalance: decimal("print_balance", { precision: 10, scale: 4 }).default("0"),
  dailyPrintLimit: integer("daily_print_limit").default(0),

  // TOTP 2FA
  totpSecret: text("totp_secret"),
  totpEnabled: boolean("totp_enabled").notNull().default(false),

  // Stats
  totalPrintPages: integer("total_print_pages").default(0),
  totalPrintJobs: integer("total_print_jobs").default(0),
  totalPrintCost: decimal("total_print_cost", { precision: 10, scale: 4 }).default("0"),

  // Timestamps
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
  dateBlocked: timestamp("date_blocked"),
  dateDeleted: timestamp("date_deleted"),

  // Notes
  notes: text("notes"),
  externalId: text("external_id"),
}, (table) => ({
  userNameIdx: text("user_name").unique(),
  emailIdx: text("email").unique(),
  uuidIdx: text("uuid").unique(),
}));

// ─── User Accounts (financial) ───────────────────────────────────────────────

export const userAccounts = pgTable("tbl_user_account", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  userId: integer("user_id").notNull().references(() => users.id, { onDelete: "cascade" }),
  accountName: text("account_name").notNull(),
  balance: decimal("balance", { precision: 10, scale: 4 }).notNull().default("0"),
  overdraftLimit: decimal("overdraft_limit", { precision: 10, scale: 4 }).default("0"),
  creditLimit: decimal("credit_limit", { precision: 10, scale: 4 }).default("0"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── User Groups ──────────────────────────────────────────────────────────────

export const userGroups = pgTable("tbl_user_group", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  name: text("name").notNull().unique(),
  description: text("description"),
  isInternal: boolean("is_internal").notNull().default(false),
  defaultRoles: userRoleEnum("default_roles").array().default(["USER"]),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── User Group Members ───────────────────────────────────────────────────────

export const userGroupMembers = pgTable("tbl_user_group_member", {
  id: serial("id").primaryKey(),
  userId: integer("user_id").notNull().references(() => users.id, { onDelete: "cascade" }),
  groupId: integer("group_id").notNull().references(() => userGroups.id, { onDelete: "cascade" }),
  dateJoined: timestamp("date_joined").notNull().defaultNow(),
  dateRemoved: timestamp("date_removed"),
}, (table) => ({
  uniq: { columns: [table.userId, table.groupId] },
}));

// ─── User Cards (NFC/ID) ─────────────────────────────────────────────────────

export const userCards = pgTable("tbl_user_card", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  userId: integer("user_id").notNull().references(() => users.id, { onDelete: "cascade" }),
  cardId: text("card_id").notNull().unique(), // NFC UID / barcode
  cardType: text("card_type").notNull().default("NFC"), // NFC, BARCODE, RFID
  cardName: text("card_name"),
  isActive: boolean("is_active").notNull().default(true),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateDeactivated: timestamp("date_deactivated"),
});

// ─── User Emails ──────────────────────────────────────────────────────────────

export const userEmails = pgTable("tbl_user_email", {
  id: serial("id").primaryKey(),
  userId: integer("user_id").notNull().references(() => users.id, { onDelete: "cascade" }),
  email: text("email").notNull(),
  isPrimary: boolean("is_primary").notNull().default(false),
  isVerified: boolean("is_verified").notNull().default(false),
  verificationToken: text("verification_token"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
}, (table) => ({
  uniq: { columns: [table.userId, table.email] },
  emailIdx: text("email").unique(),
}));

// ─── User Custom Attributes ──────────────────────────────────────────────────

export const userAttrs = pgTable("tbl_user_attr", {
  id: serial("id").primaryKey(),
  userId: integer("user_id").notNull().references(() => users.id, { onDelete: "cascade" }),
  attrKey: text("attr_key").notNull(),
  attrValue: text("attr_value"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
}, (table) => ({
  uniq: { columns: [table.userId, table.attrKey] },
}));

// ─── Sessions ────────────────────────────────────────────────────────────────

export const sessions = pgTable("tbl_session", {
  id: serial("id").primaryKey(),
  tokenHash: text("token_hash").notNull().unique(),
  userId: integer("user_id").notNull().references(() => users.id, { onDelete: "cascade" }),
  expiresAt: timestamp("expires_at").notNull(),
  ipAddress: text("ip_address"),
  userAgent: text("user_agent"),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
});

// ─── Relations ───────────────────────────────────────────────────────────────

export const usersRelations = relations(users, ({ many, one }) => ({
  account: one(userAccounts, { fields: [users.id], references: [userAccounts.userId] }),
  cards: many(userCards),
  emails: many(userEmails),
  groups: many(userGroupMembers),
  sessions: many(sessions),
  attrs: many(userAttrs),
}));

export const userAccountsRelations = relations(userAccounts, ({ one }) => ({
  user: one(users, { fields: [userAccounts.userId], references: [users.id] }),
}));

export const userGroupsRelations = relations(userGroups, ({ many }) => ({
  members: many(userGroupMembers),
}));

export const userGroupMembersRelations = relations(userGroupMembers, ({ one }) => ({
  user: one(users, { fields: [userGroupMembers.userId], references: [users.id] }),
  group: one(userGroups, { fields: [userGroupMembers.groupId], references: [userGroups.id] }),
}));

export const userCardsRelations = relations(userCards, ({ one }) => ({
  user: one(users, { fields: [userCards.userId], references: [users.id] }),
}));

export const userEmailsRelations = relations(userEmails, ({ one }) => ({
  user: one(users, { fields: [userEmails.userId], references: [users.id] }),
}));

export const sessionsRelations = relations(sessions, ({ one }) => ({
  user: one(users, { fields: [sessions.userId], references: [users.id] }),
}));
