// Reports Router — mirrors PrintFlowLite's RestReportsService + ReportService
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { createDrizzle } from "@printflow/db";
import {
  accountTrx,
  accounts,
  users,
  printers,
  docLog,
  jobTickets,
  tblDocIn,
} from "@printflow/db/schema";
import { eq, desc, and, sql, gte, lte, count } from "drizzle-orm";
import { z } from "zod";

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
    email: { smtpPort: 587, smtpFrom: "noreply@printflow.local", imapPort: 993 },
    soffice: { path: "/usr/bin/soffice" },
    appUrl: "http://localhost:3000",
    apiUrl: "http://localhost:3001",
  });

function toCSV(rows: any[], headers: string[]): string {
  const escape = (v: any) => {
    const s = String(v ?? "");
    if (s.includes(",") || s.includes('"') || s.includes("\n")) {
      return `"${s.replace(/"/g, '""')}"`;
    }
    return s;
  };
  return [headers.join(","), ...rows.map((r) => headers.map((h) => escape(r[h])).join(",")).join("\r\n")].join("\r\n");
}

export async function createReportsRouter(app: FastifyInstance) {
  app.get("/summary", async (_request: FastifyRequest, reply: FastifyReply) => {
    const db = createDb();

    const [totalUsers] = await db.select({ count: count() }).from(users);
    const [totalTrx] = await db.select({ count: count() }).from(accountTrx);
    const [totalPrintJobs] = await db.select({ count: count() }).from(docLog);
    const [totalPrinters] = await db.select({ count: count() }).from(printers);
    const [totalTickets] = await db.select({ count: count() }).from(jobTickets);

    const monthlyStats = await db
      .select({
        month: sql<string>`to_char(${accountTrx.createdAt}, 'YYYY-MM')`,
        count: count(),
        totalAmount: sql<string>`sum(cast(${accountTrx.amount} as numeric))`,
      })
      .from(accountTrx)
      .where(sql`${accountTrx.createdAt} >= now() - interval '6 months'`)
      .groupBy(sql`to_char(${accountTrx.createdAt}, 'YYYY-MM')`)
      .orderBy(sql`to_char(${accountTrx.createdAt}, 'YYYY-MM')`);

    const topPrinters = await db
      .select({
        printerName: docLog.printerName,
        jobCount: count(),
        totalPages: sql<number>`sum(${docLog.pagesPrinted ?? 0})`,
      })
      .from(docLog)
      .where(sql`${docLog.printerName} IS NOT NULL`)
      .groupBy(docLog.printerName)
      .orderBy(sql`count(*) DESC`)
      .limit(10);

    const topUsers = await db
      .select({
        userId: docLog.userId,
        userName: sql<string>`coalesce(${users.userName}, 'Unknown')`,
        fullName: sql<string>`coalesce(${users.fullName}, 'Unknown')`,
        jobCount: count(),
        totalPages: sql<number>`sum(${docLog.pagesPrinted ?? 0})`,
        totalCost: sql<string>`sum(cast(${docLog.chargeAmount ?? '0'} as numeric))`,
      })
      .from(docLog)
      .leftJoin(users, eq(docLog.userId, users.id))
      .where(sql`${docLog.userId} IS NOT NULL`)
      .groupBy(docLog.userId, users.userName, users.fullName)
      .orderBy(sql`sum(${docLog.pagesPrinted ?? 0}) DESC`)
      .limit(10);

    const dailyPrints = await db
      .select({
        date: sql<string>`to_char(${docLog.createdAt}, 'YYYY-MM-DD')`,
        jobs: count(),
        pages: sql<number>`sum(${docLog.pagesPrinted ?? 0})`,
      })
      .from(docLog)
      .where(sql`${docLog.createdAt} >= now() - interval '30 days'`)
      .groupBy(sql`to_char(${docLog.createdAt}, 'YYYY-MM-DD')`)
      .orderBy(sql`to_char(${docLog.createdAt}, 'YYYY-MM-DD')`);

    const trxByType = await db
      .select({
        trxType: accountTrx.trxType,
        count: count(),
        total: sql<string>`sum(cast(${accountTrx.amount} as numeric))`,
      })
      .from(accountTrx)
      .groupBy(accountTrx.trxType);

    const ticketsByStatus = await db
      .select({
        status: jobTickets.status,
        count: count(),
      })
      .from(jobTickets)
      .groupBy(jobTickets.status);

    return reply.send({
      success: true,
      data: {
        overview: {
          totalUsers: totalUsers?.count ?? 0,
          totalTransactions: totalTrx?.count ?? 0,
          totalPrintJobs: totalPrintJobs?.count ?? 0,
          totalPrinters: totalPrinters?.count ?? 0,
          totalTickets: totalTickets?.count ?? 0,
        },
        monthlyStats,
        topPrinters,
        topUsers,
        dailyPrints,
        trxByType,
        ticketsByStatus,
      },
      timestamp: new Date().toISOString(),
    });
  });

  app.get(
    "/account-trx",
    {
      schema: {
        querystring: z.object({
          accountType: z.string().optional(),
          accountName: z.string().optional(),
          userId: z.string().optional(),
          trxType: z.string().optional(),
          dateFrom: z.string().optional(),
          dateTo: z.string().optional(),
          sortField: z.string().default("createdAt"),
          sortOrder: z.enum(["asc", "desc"]).default("desc"),
          page: z.coerce.number().default(1),
          limit: z.coerce.number().default(100),
          format: z.enum(["json", "csv"]).default("json"),
        }),
      },
    },
    async (request: FastifyRequest<{ Querystring: any }>, reply: FastifyReply) => {
      const db = createDb();
      const q = request.query;

      const conditions: any[] = [];
      if (q.dateFrom) conditions.push(gte(accountTrx.createdAt, new Date(q.dateFrom)));
      if (q.dateTo) conditions.push(lte(accountTrx.createdAt, new Date(q.dateTo)));
      if (q.trxType) conditions.push(eq(accountTrx.trxType, q.trxType));

      const rows = await db
        .select({
          id: accountTrx.id,
          uuid: accountTrx.uuid,
          accountId: accountTrx.accountId,
          trxType: accountTrx.trxType,
          amount: accountTrx.amount,
          balanceAfter: accountTrx.balanceAfter,
          description: accountTrx.description,
          notes: accountTrx.notes,
          isReversed: accountTrx.isReversed,
          createdAt: accountTrx.createdAt,
        })
        .from(accountTrx)
        .where(conditions.length > 0 ? and(...conditions) : undefined)
        .orderBy(q.sortOrder === "asc" ? accountTrx.createdAt : undefined)
        .limit(q.limit)
        .offset((q.page - 1) * q.limit);

      const [totalCount] = await db.select({ count: count() }).from(accountTrx);

      if (q.format === "csv") {
        const headers = ["ID", "UUID", "Account ID", "Type", "Amount", "Balance After", "Description", "Notes", "Reversed", "Date"];
        const csv = toCSV(
          rows.map((r) => ({
            ID: r.id,
            UUID: r.uuid,
            "Account ID": r.accountId,
            Type: r.trxType,
            Amount: r.amount,
            "Balance After": r.balanceAfter,
            Description: r.description ?? "",
            Notes: r.notes ?? "",
            Reversed: r.isReversed ? "Yes" : "No",
            Date: r.createdAt?.toISOString() ?? "",
          })),
          headers,
        );
        return reply
          .header("Content-Type", "text/csv; charset=utf-8")
          .header("Content-Disposition", `attachment; filename="account-trx-report.csv"`)
          .send(csv);
      }

      return reply.send({
        success: true,
        data: { data: rows, total: totalCount?.count ?? 0, page: q.page, limit: q.limit },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.get(
    "/user-printout",
    {
      schema: {
        querystring: z.object({
          groupBy: z.enum(["user", "printer_user"]).default("user"),
          aspect: z.enum(["pages", "jobs", "copies"]).default("pages"),
          dateFrom: z.string().optional(),
          dateTo: z.string().optional(),
          sortField: z.string().default("totalPages"),
          sortOrder: z.enum(["asc", "desc"]).default("desc"),
          page: z.coerce.number().default(1),
          limit: z.coerce.number().default(50),
          format: z.enum(["json", "csv"]).default("json"),
        }),
      },
    },
    async (request: FastifyRequest<{ Querystring: any }>, reply: FastifyReply) => {
      const db = createDb();
      const q = request.query;

      const dateConditions: any[] = [];
      if (q.dateFrom) dateConditions.push(gte(docLog.createdAt, new Date(q.dateFrom)));
      if (q.dateTo) dateConditions.push(lte(docLog.createdAt, new Date(q.dateTo)));

      const rows = await db
        .select({
          userId: docLog.userId,
          userName: sql<string>`coalesce(${users.userName}, 'Unknown')`,
          fullName: sql<string>`coalesce(${users.fullName}, 'Unknown')`,
          printerName: q.groupBy === "printer_user" ? docLog.printerName : sql<string>`null`,
          metric: sql<number>`sum(${docLog.pagesPrinted ?? 0})`,
          totalCost: sql<string>`sum(cast(${docLog.chargeAmount ?? '0'} as numeric))`,
        })
        .from(docLog)
        .leftJoin(users, eq(docLog.userId, users.id))
        .where(dateConditions.length > 0 ? and(...dateConditions) : undefined)
        .groupBy(docLog.userId, users.userName, users.fullName, q.groupBy === "printer_user" ? docLog.printerName : sql<string>`null`)
        .orderBy(sql`sum(${docLog.pagesPrinted ?? 0}) DESC`)
        .limit(q.limit)
        .offset((q.page - 1) * q.limit);

      if (q.format === "csv") {
        const headers = q.groupBy === "printer_user"
          ? ["User ID", "User Name", "Full Name", "Printer", "Metric", "Total Cost"]
          : ["User ID", "User Name", "Full Name", "Metric", "Total Cost"];
        const csv = toCSV(
          rows.map((r: any) => ({
            "User ID": r.userId ?? "",
            "User Name": r.userName ?? "",
            "Full Name": r.fullName ?? "",
            Printer: (r as any).printerName ?? "",
            Metric: r.metric ?? 0,
            "Total Cost": r.totalCost ?? "0",
          })),
          headers,
        );
        return reply
          .header("Content-Type", "text/csv; charset=utf-8")
          .header("Content-Disposition", `attachment; filename="user-printout-report.csv"`)
          .send(csv);
      }

      return reply.send({
        success: true,
        data: { data: rows, page: q.page, limit: q.limit, aspect: q.aspect, groupBy: q.groupBy },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.get("/vouchers", async (_request: FastifyRequest, reply: FastifyReply) => {
    const db = createDb();

    const statusBreakdown = await db
      .select({
        status: jobTickets.status,
        count: count(),
        totalValue: sql<string>`sum(cast(${jobTickets.totalCost} as numeric))`,
      })
      .from(jobTickets)
      .groupBy(jobTickets.status);

    const dailyVouchers = await db
      .select({
        date: sql<string>`to_char(${jobTickets.createdAt}, 'YYYY-MM-DD')`,
        created: count(),
      })
      .from(jobTickets)
      .where(sql`${jobTickets.createdAt} >= now() - interval '30 days'`)
      .groupBy(sql`to_char(${jobTickets.createdAt}, 'YYYY-MM-DD')`)
      .orderBy(sql`to_char(${jobTickets.createdAt}, 'YYYY-MM-DD')`);

    return reply.send({
      success: true,
      data: { statusBreakdown, dailyVouchers },
      timestamp: new Date().toISOString(),
    });
  });

  app.get(
    "/documents",
    {
      schema: {
        querystring: z.object({
          dateFrom: z.string().optional(),
          dateTo: z.string().optional(),
          page: z.coerce.number().default(1),
          limit: z.coerce.number().default(50),
          format: z.enum(["json", "csv"]).default("json"),
        }),
      },
    },
    async (request: FastifyRequest<{ Querystring: any }>, reply: FastifyReply) => {
      const db = createDb();
      const q = request.query;

      const conditions: any[] = [];
      if (q.dateFrom) conditions.push(gte(tblDocIn.createdAt, new Date(q.dateFrom)));
      if (q.dateTo) conditions.push(lte(tblDocIn.createdAt, new Date(q.dateTo)));

      const rows = await db
        .select({
          id: tblDocIn.id,
          docName: tblDocIn.docName,
          docType: tblDocIn.docType,
          pageCount: tblDocIn.pageCount,
          fileSize: tblDocIn.fileSize,
          mimeType: tblDocIn.mimeType,
          createdBy: tblDocIn.createdBy,
          createdAt: tblDocIn.createdAt,
        })
        .from(tblDocIn)
        .where(conditions.length > 0 ? and(...conditions) : undefined)
        .orderBy(desc(tblDocIn.createdAt))
        .limit(q.limit)
        .offset((q.page - 1) * q.limit);

      const [totalCount] = await db.select({ count: count() }).from(tblDocIn);

      const typeBreakdown = await db
        .select({ docType: tblDocIn.docType, count: count() })
        .from(tblDocIn)
        .groupBy(tblDocIn.docType);

      if (q.format === "csv") {
        const headers = ["ID", "Name", "Type", "Pages", "Size", "MIME", "Created By", "Date"];
        const csv = toCSV(
          rows.map((r) => ({
            ID: r.id,
            Name: r.docName,
            Type: r.docType,
            Pages: r.pageCount,
            Size: r.fileSize,
            MIME: r.mimeType,
            "Created By": r.createdBy ?? "",
            Date: r.createdAt?.toISOString() ?? "",
          })),
          headers,
        );
        return reply
          .header("Content-Type", "text/csv; charset=utf-8")
          .header("Content-Disposition", `attachment; filename="documents-report.csv"`)
          .send(csv);
      }

      return reply.send({
        success: true,
        data: { data: rows, total: totalCount?.count ?? 0, page: q.page, limit: q.limit, typeBreakdown },
        timestamp: new Date().toISOString(),
      });
    },
  );
}
