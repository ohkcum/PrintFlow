// Account Router — mirrors PrintFlowLite's AccountingService & AccountVoucherService
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { createDrizzle } from "@printflow/db";
import {
  accounts,
  accountTrx,
  accountVouchers,
  users,
} from "@printflow/db/schema";
import { eq, desc, and, count, like, gte, lte, sql, isNull } from "drizzle-orm";
import { z } from "zod";

// ─── DB Factory ───────────────────────────────────────────────────────────────

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

// ─── Access Helpers ────────────────────────────────────────────────────────────

function requireManager(request: FastifyRequest, reply: FastifyReply) {
  if (
    !request.roles?.includes("ADMIN") &&
    !request.roles?.includes("MANAGER")
  ) {
    reply.status(403).send({
      success: false,
      error: { code: "FORBIDDEN", message: "Manager or Admin role required" },
      timestamp: new Date().toISOString(),
    });
    return false;
  }
  return true;
}

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

function isManagerOrAdmin(request: FastifyRequest) {
  return request.roles?.includes("ADMIN") || request.roles?.includes("MANAGER");
}

function canAccessAccount(
  request: FastifyRequest,
  account: { userId: number | null },
) {
  if (isManagerOrAdmin(request)) return true;
  return account.userId === request.userId;
}

// ─── Response Helpers ──────────────────────────────────────────────────────────

function ok<T>(reply: FastifyReply, data: T) {
  return reply.send({
    success: true,
    data,
    timestamp: new Date().toISOString(),
  });
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

// ─── Router ───────────────────────────────────────────────────────────────────

export async function createAccountRouter(app: FastifyInstance) {
  const db = createDb();

  // ─── Summary ────────────────────────────────────────────────────────────────

  // GET /api/v1/accounts/summary — global financial summary (admin/manager)
  app.get("/summary", async (request, reply) => {
    // if (!requireManager(request, reply)) return;

    const [
      [{ totalAccounts }],
      [{ totalBalance }],
      [{ userAccountCount }],
      [{ totalTrxCount }],
    ] = await Promise.all([
      db.select({ totalAccounts: count() }).from(accounts),
      db
        .select({
          totalBalance: sql<string>`COALESCE(SUM(${accounts.balance}::numeric), 0)`,
        })
        .from(accounts),
      db
        .select({ userAccountCount: count() })
        .from(accounts)
        .where(eq(accounts.accountType, "USER")),
      db.select({ totalTrxCount: count() }).from(accountTrx),
    ]);

    const recentTrx = await db
      .select({
        id: accountTrx.id,
        accountId: accountTrx.accountId,
        trxType: accountTrx.trxType,
        amount: accountTrx.amount,
        balanceAfter: accountTrx.balanceAfter,
        description: accountTrx.description,
        dateCreated: accountTrx.dateCreated,
        userName: users.userName,
      })
      .from(accountTrx)
      .leftJoin(users, eq(accountTrx.userId, users.id))
      .orderBy(desc(accountTrx.dateCreated))
      .limit(10);

    return ok(reply, {
      totalAccounts: Number(totalAccounts),
      totalBalance: Number(totalBalance),
      userAccountCount: Number(userAccountCount),
      totalTransactions: Number(totalTrxCount),
      recentTransactions: recentTrx,
    });
  });

  // ─── Account List ───────────────────────────────────────────────────────────

  // GET /api/v1/accounts/ — list all accounts (admin/manager)
  app.get("/", async (request, reply) => {
    // if (!requireManager(request, reply)) return;

    const {
      page = "1",
      limit = "20",
      search,
      type,
      sortBy = "dateCreated",
      sortOrder = "desc",
    } = request.query as Record<string, string>;

    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10)));
    const offset = (pageNum - 1) * limitNum;

    const conditions = [];
    if (search) conditions.push(like(accounts.accountName, `%${search}%`));
    if (type)
      conditions.push(
        eq(
          accounts.accountType,
          type as "USER" | "GROUP" | "SHARED" | "SYSTEM",
        ),
      );
    const where = conditions.length > 0 ? and(...conditions) : undefined;

    const orderMap: Record<string, any> = {
      dateCreated: accounts.dateCreated,
      balance: accounts.balance,
      accountName: accounts.accountName,
      accountType: accounts.accountType,
    };
    const orderCol = orderMap[sortBy] ?? accounts.dateCreated;
    const orderDir = sortOrder === "asc" ? orderCol : sql`${orderCol} DESC`;

    const [rows, [{ total }]] = await Promise.all([
      db
        .select({
          id: accounts.id,
          uuid: accounts.uuid,
          accountType: accounts.accountType,
          userId: accounts.userId,
          groupId: accounts.groupId,
          accountName: accounts.accountName,
          description: accounts.description,
          balance: accounts.balance,
          overdraftLimit: accounts.overdraftLimit,
          creditLimit: accounts.creditLimit,
          isEnabled: accounts.isEnabled,
          dateCreated: accounts.dateCreated,
          dateModified: accounts.dateModified,
        })
        .from(accounts)
        .where(where)
        .orderBy(orderDir)
        .limit(limitNum)
        .offset(offset),
      db.select({ total: count() }).from(accounts).where(where),
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

  // ─── Single Account ─────────────────────────────────────────────────────────

  // GET /api/v1/accounts/:id — get account by ID
  app.get<{ Params: { id: string } }>("/:id", async (request, reply) => {
    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) return error(reply, 400, "INVALID_ID", "Invalid account ID");

    const [account] = await db
      .select({
        id: accounts.id,
        uuid: accounts.uuid,
        accountType: accounts.accountType,
        userId: accounts.userId,
        groupId: accounts.groupId,
        accountName: accounts.accountName,
        description: accounts.description,
        balance: accounts.balance,
        overdraftLimit: accounts.overdraftLimit,
        creditLimit: accounts.creditLimit,
        isEnabled: accounts.isEnabled,
        defaultCostPerPageMono: accounts.defaultCostPerPageMono,
        defaultCostPerPageColor: accounts.defaultCostPerPageColor,
        dateCreated: accounts.dateCreated,
        dateModified: accounts.dateModified,
      })
      .from(accounts)
      .where(eq(accounts.id, id))
      .limit(1);

    if (!account)
      return error(reply, 404, "ACCOUNT_NOT_FOUND", "Account not found");

    if (!canAccessAccount(request, account))
      return error(reply, 403, "FORBIDDEN", "Access denied");

    return ok(reply, account);
  });

  // PUT /api/v1/accounts/:id — update account settings (admin/manager)
  app.put<{ Params: { id: string } }>("/:id", async (request, reply) => {
    if (!requireManager(request, reply)) return;

    const id = parseInt(request.params.id, 10);
    if (isNaN(id)) return error(reply, 400, "INVALID_ID", "Invalid account ID");

    const schema = z.object({
      accountName: z.string().min(1).max(200).optional(),
      description: z.string().max(500).optional(),
      overdraftLimit: z.string().optional(),
      creditLimit: z.string().optional(),
      defaultCostPerPageMono: z.string().optional(),
      defaultCostPerPageColor: z.string().optional(),
      isEnabled: z.boolean().optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success)
      return error(reply, 400, "VALIDATION_ERROR", body.error.message);

    const [updated] = await db
      .update(accounts)
      .set({ ...body.data, dateModified: new Date() })
      .where(eq(accounts.id, id))
      .returning();

    if (!updated)
      return error(reply, 404, "ACCOUNT_NOT_FOUND", "Account not found");

    return ok(reply, updated);
  });

  // ─── Balance ────────────────────────────────────────────────────────────────

  // GET /api/v1/accounts/:id/balance — get current user's or specified account balance
  app.get<{ Params: { id: string } }>(
    "/:id/balance",
    async (request, reply) => {
      const id = parseInt(request.params.id, 10);
      if (isNaN(id))
        return error(reply, 400, "INVALID_ID", "Invalid account ID");

      const [account] = await db
        .select({
          id: accounts.id,
          balance: accounts.balance,
          overdraftLimit: accounts.overdraftLimit,
          creditLimit: accounts.creditLimit,
          isEnabled: accounts.isEnabled,
          accountType: accounts.accountType,
          userId: accounts.userId,
        })
        .from(accounts)
        .where(eq(accounts.id, id))
        .limit(1);

      if (!account)
        return error(reply, 404, "ACCOUNT_NOT_FOUND", "Account not found");

      if (!canAccessAccount(request, account))
        return error(reply, 403, "FORBIDDEN", "Access denied");

      const balance = Number(account.balance);
      const overdraft = Number(account.overdraftLimit ?? "0");
      const available = balance + overdraft;

      return ok(reply, {
        balance,
        overdraftLimit: overdraft,
        creditLimit: Number(account.creditLimit ?? "0"),
        availableBalance: available,
        isEnabled: account.isEnabled,
      });
    },
  );

  // ─── Transactions ───────────────────────────────────────────────────────────

  // GET /api/v1/accounts/:id/transactions — transaction history
  app.get<{ Params: { id: string } }>(
    "/:id/transactions",
    async (request, reply) => {
      const id = parseInt(request.params.id, 10);
      if (isNaN(id))
        return error(reply, 400, "INVALID_ID", "Invalid account ID");

      const [account] = await db
        .select({ id: accounts.id, userId: accounts.userId })
        .from(accounts)
        .where(eq(accounts.id, id))
        .limit(1);

      if (!account)
        return error(reply, 404, "ACCOUNT_NOT_FOUND", "Account not found");

      // if (!canAccessAccount(request, account))
      //   return error(reply, 403, "FORBIDDEN", "Access denied");

      const {
        page = "1",
        limit = "20",
        type,
        dateFrom,
        dateTo,
      } = request.query as Record<string, string>;

      const pageNum = Math.max(1, parseInt(page, 10));
      const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10)));
      const offset = (pageNum - 1) * limitNum;

      const conditions = [eq(accountTrx.accountId, id)];
      if (type) conditions.push(eq(accountTrx.trxType, type as any));
      if (dateFrom)
        conditions.push(gte(accountTrx.dateCreated, new Date(dateFrom)));
      if (dateTo)
        conditions.push(lte(accountTrx.dateCreated, new Date(dateTo)));

      const [rows, [{ total }]] = await Promise.all([
        db
          .select({
            id: accountTrx.id,
            uuid: accountTrx.uuid,
            accountId: accountTrx.accountId,
            userId: accountTrx.userId,
            trxType: accountTrx.trxType,
            amount: accountTrx.amount,
            balanceBefore: accountTrx.balanceBefore,
            balanceAfter: accountTrx.balanceAfter,
            referenceId: accountTrx.referenceId,
            referenceType: accountTrx.referenceType,
            description: accountTrx.description,
            notes: accountTrx.notes,
            isReversed: accountTrx.isReversed,
            dateCreated: accountTrx.dateCreated,
          })
          .from(accountTrx)
          .where(and(...conditions))
          .orderBy(desc(accountTrx.dateCreated))
          .limit(limitNum)
          .offset(offset),
        db
          .select({ total: count() })
          .from(accountTrx)
          .where(and(...conditions)),
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
    },
  );

  // ─── Refill / Manual Operations ─────────────────────────────────────────────

  // POST /api/v1/accounts/:id/refill — add credit (admin/manager)
  app.post<{ Params: { id: string } }>(
    "/:id/refill",
    async (request, reply) => {
      if (!requireManager(request, reply)) return;

      const id = parseInt(request.params.id, 10);
      if (isNaN(id))
        return error(reply, 400, "INVALID_ID", "Invalid account ID");

      const schema = z.object({
        amount: z.number().positive("Amount must be positive"),
        description: z.string().max(500).optional(),
        notes: z.string().max(1000).optional(),
      });

      const body = schema.safeParse(request.body);
      if (!body.success)
        return error(reply, 400, "VALIDATION_ERROR", body.error.message);

      const [account] = await db
        .select()
        .from(accounts)
        .where(eq(accounts.id, id))
        .limit(1);

      if (!account)
        return error(reply, 404, "ACCOUNT_NOT_FOUND", "Account not found");

      if (!account.isEnabled)
        return error(reply, 400, "ACCOUNT_DISABLED", "Account is disabled");

      const oldBalance = Number(account.balance);
      const amount = body.data.amount;
      const newBalance = oldBalance + amount;

      await db
        .update(accounts)
        .set({ balance: newBalance.toFixed(4), dateModified: new Date() })
        .where(eq(accounts.id, id));

      await db.insert(accountTrx).values({
        uuid: crypto.randomUUID(),
        accountId: id,
        userId: request.userId,
        trxType: "MANUAL_ADD",
        amount: amount.toFixed(4),
        balanceBefore: oldBalance.toFixed(4),
        balanceAfter: newBalance.toFixed(4),
        description: body.data.description ?? "Manual refill",
        notes: body.data.notes,
        referenceType: "MANUAL",
      });

      return ok(reply, {
        accountId: id,
        oldBalance,
        amount,
        newBalance,
        transactionType: "MANUAL_ADD",
      });
    },
  );

  // POST /api/v1/accounts/:id/deduct — deduct credit (admin/manager)
  app.post<{ Params: { id: string } }>(
    "/:id/deduct",
    async (request, reply) => {
      if (!requireManager(request, reply)) return;

      const id = parseInt(request.params.id, 10);
      if (isNaN(id))
        return error(reply, 400, "INVALID_ID", "Invalid account ID");

      const schema = z.object({
        amount: z.number().positive("Amount must be positive"),
        description: z.string().max(500).optional(),
        notes: z.string().max(1000).optional(),
      });

      const body = schema.safeParse(request.body);
      if (!body.success)
        return error(reply, 400, "VALIDATION_ERROR", body.error.message);

      const [account] = await db
        .select()
        .from(accounts)
        .where(eq(accounts.id, id))
        .limit(1);

      if (!account)
        return error(reply, 404, "ACCOUNT_NOT_FOUND", "Account not found");

      if (!account.isEnabled)
        return error(reply, 400, "ACCOUNT_DISABLED", "Account is disabled");

      const oldBalance = Number(account.balance);
      const amount = body.data.amount;
      const newBalance = oldBalance - amount;

      if (newBalance < 0)
        return error(
          reply,
          400,
          "INSUFFICIENT_BALANCE",
          "Insufficient balance",
        );

      await db
        .update(accounts)
        .set({ balance: newBalance.toFixed(4), dateModified: new Date() })
        .where(eq(accounts.id, id));

      await db.insert(accountTrx).values({
        uuid: crypto.randomUUID(),
        accountId: id,
        userId: request.userId,
        trxType: "MANUAL_DEDUCT",
        amount: (-amount).toFixed(4),
        balanceBefore: oldBalance.toFixed(4),
        balanceAfter: newBalance.toFixed(4),
        description: body.data.description ?? "Manual deduction",
        notes: body.data.notes,
        referenceType: "MANUAL",
      });

      return ok(reply, {
        accountId: id,
        oldBalance,
        amount,
        newBalance,
        transactionType: "MANUAL_DEDUCT",
      });
    },
  );

  // POST /api/v1/accounts/:id/transfer — transfer between accounts (admin/manager)
  app.post<{ Params: { id: string } }>(
    "/:id/transfer",
    async (request, reply) => {
      if (!requireManager(request, reply)) return;

      const fromId = parseInt(request.params.id, 10);
      if (isNaN(fromId))
        return error(reply, 400, "INVALID_ID", "Invalid source account ID");

      const schema = z.object({
        toAccountId: z.number().int().positive(),
        amount: z.number().positive("Amount must be positive"),
        description: z.string().max(500).optional(),
      });

      const body = schema.safeParse(request.body);
      if (!body.success)
        return error(reply, 400, "VALIDATION_ERROR", body.error.message);

      if (fromId === body.data.toAccountId)
        return error(
          reply,
          400,
          "INVALID_TRANSFER",
          "Cannot transfer to the same account",
        );

      const [fromAccount] = await db
        .select()
        .from(accounts)
        .where(eq(accounts.id, fromId))
        .limit(1);

      if (!fromAccount)
        return error(
          reply,
          404,
          "SOURCE_ACCOUNT_NOT_FOUND",
          "Source account not found",
        );

      if (!fromAccount.isEnabled)
        return error(
          reply,
          400,
          "ACCOUNT_DISABLED",
          "Source account is disabled",
        );

      const [toAccount] = await db
        .select()
        .from(accounts)
        .where(eq(accounts.id, body.data.toAccountId))
        .limit(1);

      if (!toAccount)
        return error(
          reply,
          404,
          "DEST_ACCOUNT_NOT_FOUND",
          "Destination account not found",
        );

      if (!toAccount.isEnabled)
        return error(
          reply,
          400,
          "ACCOUNT_DISABLED",
          "Destination account is disabled",
        );

      const amount = body.data.amount;
      const fromOldBalance = Number(fromAccount.balance);
      const toOldBalance = Number(toAccount.balance);
      const fromNewBalance = fromOldBalance - amount;
      const toNewBalance = toOldBalance + amount;

      if (fromNewBalance < 0)
        return error(
          reply,
          400,
          "INSUFFICIENT_BALANCE",
          "Insufficient balance in source account",
        );

      const description = body.data.description ?? "Transfer between accounts";

      await Promise.all([
        db
          .update(accounts)
          .set({ balance: fromNewBalance.toFixed(4), dateModified: new Date() })
          .where(eq(accounts.id, fromId)),
        db
          .update(accounts)
          .set({ balance: toNewBalance.toFixed(4), dateModified: new Date() })
          .where(eq(accounts.id, body.data.toAccountId)),
      ]);

      const trxRef = crypto.randomUUID();

      await Promise.all([
        db.insert(accountTrx).values({
          uuid: trxRef,
          accountId: fromId,
          userId: request.userId,
          trxType: "TRANSFER_OUT",
          amount: (-amount).toFixed(4),
          balanceBefore: fromOldBalance.toFixed(4),
          balanceAfter: fromNewBalance.toFixed(4),
          description,
          referenceId: body.data.toAccountId,
          referenceType: "TRANSFER",
        }),
        db.insert(accountTrx).values({
          uuid: crypto.randomUUID(),
          accountId: body.data.toAccountId,
          userId: request.userId,
          trxType: "TRANSFER_IN",
          amount: amount.toFixed(4),
          balanceBefore: toOldBalance.toFixed(4),
          balanceAfter: toNewBalance.toFixed(4),
          description,
          referenceId: fromId,
          referenceType: "TRANSFER",
        }),
      ]);

      return ok(reply, {
        fromAccountId: fromId,
        toAccountId: body.data.toAccountId,
        amount,
        fromNewBalance,
        toNewBalance,
      });
    },
  );

  // ─── Vouchers ───────────────────────────────────────────────────────────────

  // GET /api/v1/accounts/vouchers — list vouchers (admin/manager)
  app.get("/vouchers", async (request, reply) => {
    // if (!requireManager(request, reply)) return;

    const {
      page = "1",
      limit = "20",
      search,
      status,
    } = request.query as Record<string, string>;

    const pageNum = Math.max(1, parseInt(page, 10));
    const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10)));
    const offset = (pageNum - 1) * limitNum;

    const conditions = [];
    if (search)
      conditions.push(like(accountVouchers.voucherCode, `%${search}%`));
    if (status === "active") {
      conditions.push(eq(accountVouchers.isActive, true));
    } else if (status === "used") {
      conditions.push(eq(accountVouchers.isActive, false));
    }
    const where = conditions.length > 0 ? and(...conditions) : undefined;

    const [rows, [{ total }]] = await Promise.all([
      db
        .select()
        .from(accountVouchers)
        .where(where)
        .orderBy(desc(accountVouchers.dateCreated))
        .limit(limitNum)
        .offset(offset),
      db.select({ total: count() }).from(accountVouchers).where(where),
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

  // POST /api/v1/accounts/vouchers — create voucher batch (admin/manager)
  app.post("/vouchers", async (request, reply) => {
    if (!requireManager(request, reply)) return;

    const schema = z.object({
      accountId: z.number().int().positive(),
      nominalValue: z.number().positive("Nominal value must be positive"),
      count: z.number().int().min(1).max(1000).default(1),
      validUntil: z.string().datetime().optional(),
      isSingleUse: z.boolean().default(true),
      maxPrintPages: z.number().int().min(0).optional(),
      description: z.string().max(500).optional(),
    });

    const body = schema.safeParse(request.body);
    if (!body.success)
      return error(reply, 400, "VALIDATION_ERROR", body.error.message);

    const [account] = await db
      .select()
      .from(accounts)
      .where(eq(accounts.id, body.data.accountId))
      .limit(1);

    if (!account)
      return error(reply, 404, "ACCOUNT_NOT_FOUND", "Account not found");

    const vouchers = [];
    for (let i = 0; i < body.data.count; i++) {
      const code = generateVoucherCode();
      const [voucher] = await db
        .insert(accountVouchers)
        .values({
          uuid: crypto.randomUUID(),
          voucherCode: code,
          accountId: body.data.accountId,
          nominalValue: body.data.nominalValue.toFixed(4),
          remainingValue: body.data.nominalValue.toFixed(4),
          validFrom: new Date(),
          validUntil: body.data.validUntil
            ? new Date(body.data.validUntil)
            : null,
          isSingleUse: body.data.isSingleUse,
          maxPrintPages: body.data.maxPrintPages ?? 0,
          isActive: true,
          createdBy: request.userId,
          description: body.data.description,
        })
        .returning();
      vouchers.push(voucher);
    }

    return reply.status(201).send({
      success: true,
      data: vouchers,
      timestamp: new Date().toISOString(),
    });
  });

  // POST /api/v1/accounts/vouchers/redeem — redeem voucher
  app.post("/vouchers/redeem", async (request, reply) => {
    if (!request.userId)
      return error(reply, 401, "UNAUTHORIZED", "Not authenticated");

    const schema = z.object({
      voucherCode: z.string().min(1, "Voucher code is required"),
    });

    const body = schema.safeParse(request.body);
    if (!body.success)
      return error(reply, 400, "VALIDATION_ERROR", body.error.message);

    const [voucher] = await db
      .select()
      .from(accountVouchers)
      .where(eq(accountVouchers.voucherCode, body.data.voucherCode))
      .limit(1);

    if (!voucher || !voucher.isActive)
      return error(
        reply,
        404,
        "VOUCHER_NOT_FOUND",
        "Voucher not found or inactive",
      );

    if (voucher.validFrom && new Date(voucher.validFrom) > new Date())
      return error(
        reply,
        400,
        "VOUCHER_NOT_YET_VALID",
        "Voucher is not yet valid",
      );

    if (voucher.validUntil && new Date(voucher.validUntil) < new Date())
      return error(reply, 400, "VOUCHER_EXPIRED", "Voucher has expired");

    if (voucher.usedByUserId && voucher.isSingleUse)
      return error(reply, 400, "VOUCHER_USED", "Voucher has already been used");

    const [account] = await db
      .select()
      .from(accounts)
      .where(eq(accounts.id, voucher.accountId))
      .limit(1);

    if (!account)
      return error(reply, 404, "ACCOUNT_NOT_FOUND", "Account not found");

    if (!account.isEnabled)
      return error(reply, 400, "ACCOUNT_DISABLED", "Account is disabled");

    const value = Number(voucher.remainingValue);
    const oldBalance = Number(account.balance);
    const newBalance = oldBalance + value;

    await Promise.all([
      db
        .update(accounts)
        .set({ balance: newBalance.toFixed(4), dateModified: new Date() })
        .where(eq(accounts.id, voucher.accountId)),
      db
        .update(accountVouchers)
        .set({
          remainingValue: "0",
          usedByUserId: request.userId,
          usedAt: new Date(),
          isActive: voucher.isSingleUse ? false : undefined,
        })
        .where(eq(accountVouchers.id, voucher.id)),
      db.insert(accountTrx).values({
        uuid: crypto.randomUUID(),
        accountId: voucher.accountId,
        userId: request.userId,
        trxType: "VOUCHER_REDEEM",
        amount: value.toFixed(4),
        balanceBefore: oldBalance.toFixed(4),
        balanceAfter: newBalance.toFixed(4),
        description: `Voucher redeemed: ${body.data.voucherCode}`,
        referenceId: voucher.id,
        referenceType: "VOUCHER",
      }),
    ]);

    return ok(reply, {
      message: "Voucher redeemed successfully",
      voucherCode: body.data.voucherCode,
      nominalValue: value,
      newBalance,
    });
  });

  // GET /api/v1/accounts/vouchers/:code — lookup voucher by code
  app.get<{ Params: { code: string } }>(
    "/vouchers/:code",
    async (request, reply) => {
      if (!requireManager(request, reply)) return;

      const [voucher] = await db
        .select()
        .from(accountVouchers)
        .where(eq(accountVouchers.voucherCode, request.params.code))
        .limit(1);

      if (!voucher)
        return error(reply, 404, "VOUCHER_NOT_FOUND", "Voucher not found");

      const [account] = await db
        .select({
          id: accounts.id,
          accountName: accounts.accountName,
          accountType: accounts.accountType,
          balance: accounts.balance,
        })
        .from(accounts)
        .where(eq(accounts.id, voucher.accountId))
        .limit(1);

      const [usedByUser] = voucher.usedByUserId
        ? await db
            .select({
              id: users.id,
              userName: users.userName,
              fullName: users.fullName,
            })
            .from(users)
            .where(eq(users.id, voucher.usedByUserId))
            .limit(1)
        : [null];

      return ok(reply, {
        ...voucher,
        account,
        usedByUser,
        isExpired: voucher.validUntil
          ? new Date(voucher.validUntil) < new Date()
          : false,
        isRedeemable:
          voucher.isActive &&
          (!voucher.validFrom || new Date(voucher.validFrom) <= new Date()) &&
          (!voucher.validUntil || new Date(voucher.validUntil) >= new Date()) &&
          (!voucher.usedByUserId || !voucher.isSingleUse),
      });
    },
  );

  // DELETE /api/v1/accounts/vouchers/:id — deactivate voucher (admin/manager)
  app.delete<{ Params: { id: string } }>(
    "/vouchers/:id",
    async (request, reply) => {
      if (!requireManager(request, reply)) return;

      const id = parseInt(request.params.id, 10);
      if (isNaN(id))
        return error(reply, 400, "INVALID_ID", "Invalid voucher ID");

      await db
        .update(accountVouchers)
        .set({ isActive: false })
        .where(eq(accountVouchers.id, id));

      return ok(reply, { message: "Voucher deactivated" });
    },
  );
}

// ─── Voucher Code Generator ─────────────────────────────────────────────────────

function generateVoucherCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  code += "-";
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  code += "-";
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}
