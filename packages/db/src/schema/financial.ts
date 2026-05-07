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
import { users, userGroups } from "./users";

// ─── Enums ────────────────────────────────────────────────────────────────────

export const accountTypeEnum = pgEnum("account_type", [
  "USER",
  "GROUP",
  "SHARED",
  "SYSTEM",
]);

export const accountTrxTypeEnum = pgEnum("account_trx_type", [
  "PRINT_JOB",
  "MANUAL_ADD",
  "MANUAL_DEDUCT",
  "VOUCHER_REDEEM",
  "REFUND",
  "TRANSFER_IN",
  "TRANSFER_OUT",
  "INITIAL",
]);

export const costChangeTypeEnum = pgEnum("cost_change_type", [
  "PRINT_JOB",
  "MANUAL_ADD",
  "MANUAL_DEDUCT",
  "VOUCHER_REDEEM",
  "REFUND",
  "TRANSFER_IN",
  "TRANSFER_OUT",
  "INITIAL",
]);

// ─── Accounts ─────────────────────────────────────────────────────────────────

export const accounts = pgTable("tbl_account", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),

  // Ownership
  accountType: accountTypeEnum("account_type").notNull().default("USER"),
  userId: integer("user_id").references(() => users.id, {
    onDelete: "cascade",
  }),
  groupId: integer("group_id").references(() => userGroups.id, {
    onDelete: "cascade",
  }),

  accountName: text("account_name").notNull(),
  description: text("description"),

  // Balance
  balance: decimal("balance", { precision: 10, scale: 4 })
    .notNull()
    .default("0"),
  overdraftLimit: decimal("overdraft_limit", {
    precision: 10,
    scale: 4,
  }).default("0"),
  creditLimit: decimal("credit_limit", { precision: 10, scale: 4 }).default(
    "0",
  ),
  isEnabled: boolean("is_enabled").notNull().default(true),

  // Costing
  defaultCostPerPageMono: decimal("default_cost_per_page_mono", {
    precision: 10,
    scale: 4,
  }).default("0.01"),
  defaultCostPerPageColor: decimal("default_cost_per_page_color", {
    precision: 10,
    scale: 4,
  }).default("0.05"),

  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── Account Transactions ────────────────────────────────────────────────────

export const accountTrx = pgTable("tbl_account_trx", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),

  accountId: integer("account_id")
    .notNull()
    .references(() => accounts.id, { onDelete: "cascade" }),
  userId: integer("user_id").references(() => users.id),

  trxType: accountTrxTypeEnum("trx_type").notNull(),
  amount: decimal("amount", { precision: 10, scale: 4 }).notNull(), // negative = debit, positive = credit
  balanceBefore: decimal("balance_before", {
    precision: 10,
    scale: 4,
  }).notNull(),
  balanceAfter: decimal("balance_after", { precision: 10, scale: 4 }).notNull(),

  // Reference
  referenceId: integer("reference_id"), // doc_log.id, voucher.id, etc.
  referenceType: text("reference_type"), // 'DOC_LOG', 'VOUCHER', 'MANUAL', 'TRANSFER'

  description: text("description"),
  notes: text("notes"),

  // Reversal
  reversedByTrxId: integer("reversed_by_trx_id"),
  isReversed: boolean("is_reversed").notNull().default(false),

  dateCreated: timestamp("date_created").notNull().defaultNow(),
});

// ─── Account Vouchers ─────────────────────────────────────────────────────────

export const accountVouchers = pgTable("tbl_account_voucher", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),

  voucherCode: text("voucher_code").notNull().unique(),
  accountId: integer("account_id")
    .notNull()
    .references(() => accounts.id),

  // Value
  nominalValue: decimal("nominal_value", { precision: 10, scale: 4 }).notNull(),
  remainingValue: decimal("remaining_value", {
    precision: 10,
    scale: 4,
  }).notNull(),

  // Validity
  validFrom: timestamp("valid_from").notNull(),
  validUntil: timestamp("valid_until"),
  isSingleUse: boolean("is_single_use").notNull().default(false),
  maxPrintPages: integer("max_print_pages").default(0),

  // Status
  isActive: boolean("is_active").notNull().default(true),
  usedByUserId: integer("used_by_user_id").references(() => users.id),
  usedAt: timestamp("used_at"),

  createdBy: integer("created_by").references(() => users.id),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
});

// ─── POS Items ────────────────────────────────────────────────────────────────

export const posItems = pgTable("tbl_pos_item", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),
  sku: text("sku").unique(),
  name: text("name").notNull(),
  description: text("description"),
  price: decimal("price", { precision: 10, scale: 4 }).notNull(),
  cost: decimal("cost", { precision: 10, scale: 4 }).default("0"),
  category: text("category"),
  stockQuantity: integer("stock_quantity").default(0),
  isActive: boolean("is_active").notNull().default(true),
  dateCreated: timestamp("date_created").notNull().defaultNow(),
  dateModified: timestamp("date_modified").notNull().defaultNow(),
});

// ─── POS Purchases ────────────────────────────────────────────────────────────

export const posPurchases = pgTable("tbl_pos_purchase", {
  id: serial("id").primaryKey(),
  uuid: text("uuid").notNull().unique(),

  userId: integer("user_id").references(() => users.id),
  accountId: integer("account_id").references(() => accounts.id),

  totalAmount: decimal("total_amount", { precision: 10, scale: 4 }).notNull(),
  paymentMethod: text("payment_method").notNull().default("CASH"), // CASH, CARD, ACCOUNT
  paymentReference: text("payment_reference"),

  status: text("status").notNull().default("COMPLETED"), // COMPLETED, REFUNDED, VOID
  cashierUserId: integer("cashier_user_id").references(() => users.id),

  dateCreated: timestamp("date_created").notNull().defaultNow(),
});

// ─── POS Purchase Items ────────────────────────────────────────────────────────

export const posPurchaseItems = pgTable("tbl_pos_purchase_item", {
  id: serial("id").primaryKey(),
  purchaseId: integer("purchase_id")
    .notNull()
    .references(() => posPurchases.id, { onDelete: "cascade" }),
  posItemId: integer("pos_item_id").references(() => posItems.id),
  itemName: text("item_name").notNull(),
  quantity: integer("quantity").notNull().default(1),
  unitPrice: decimal("unit_price", { precision: 10, scale: 4 }).notNull(),
  subtotal: decimal("subtotal", { precision: 10, scale: 4 }).notNull(),
});

// ─── Relations ───────────────────────────────────────────────────────────────

export const accountsRelations = relations(accounts, ({ one, many }) => ({
  user: one(users, { fields: [accounts.userId], references: [users.id] }),
  group: one(userGroups, {
    fields: [accounts.groupId],
    references: [userGroups.id],
  }),
  transactions: many(accountTrx),
  vouchers: many(accountVouchers),
}));

export const accountTrxRelations = relations(accountTrx, ({ one }) => ({
  account: one(accounts, {
    fields: [accountTrx.accountId],
    references: [accounts.id],
  }),
  user: one(users, { fields: [accountTrx.userId], references: [users.id] }),
}));

export const accountVouchersRelations = relations(
  accountVouchers,
  ({ one }) => ({
    account: one(accounts, {
      fields: [accountVouchers.accountId],
      references: [accounts.id],
    }),
    usedBy: one(users, {
      fields: [accountVouchers.usedByUserId],
      references: [users.id],
    }),
    createdByUser: one(users, {
      fields: [accountVouchers.createdBy],
      references: [users.id],
    }),
  }),
);

export const posPurchasesRelations = relations(
  posPurchases,
  ({ one, many }) => ({
    user: one(users, { fields: [posPurchases.userId], references: [users.id] }),
    account: one(accounts, {
      fields: [posPurchases.accountId],
      references: [accounts.id],
    }),
    cashier: one(users, {
      fields: [posPurchases.cashierUserId],
      references: [users.id],
    }),
    items: many(posPurchaseItems),
  }),
);

export const posPurchaseItemsRelations = relations(
  posPurchaseItems,
  ({ one }) => ({
    purchase: one(posPurchases, {
      fields: [posPurchaseItems.purchaseId],
      references: [posPurchases.id],
    }),
    item: one(posItems, {
      fields: [posPurchaseItems.posItemId],
      references: [posItems.id],
    }),
  }),
);
