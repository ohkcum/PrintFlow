// QR Code Router — mirrors PrintFlowLite's QR code release pages
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { z } from "zod";
import { createDrizzle } from "@printflow/db";
import { tblDocIn } from "@printflow/db/schema";
import { eq } from "drizzle-orm";

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

const ReleaseByQRSchema = z.object({
  uuid: z.string().min(1),
});

export async function createQrRouter(app: FastifyInstance) {
  app.get(
    "/release/:uuid",
    async (request: FastifyRequest<{ Params: { uuid: string } }>, reply: FastifyReply) => {
      const db = createDb();
      const { uuid } = request.params;

      const doc = await db
        .select({
          id: tblDocIn.id,
          uuid: tblDocIn.uuid,
          docName: tblDocIn.docName,
          docType: tblDocIn.docType,
          pageCount: tblDocIn.pageCount,
          mimeType: tblDocIn.mimeType,
          fileSize: tblDocIn.fileSize,
          defaultCopies: tblDocIn.defaultCopies,
          defaultDuplex: tblDocIn.defaultDuplex,
          defaultColorMode: tblDocIn.defaultColorMode,
          createdBy: tblDocIn.createdBy,
          dateCreated: tblDocIn.createdAt,
        })
        .from(tblDocIn)
        .where(eq(tblDocIn.uuid, uuid))
        .limit(1);

      if (doc.length === 0) {
        return reply.status(404).send({
          success: false,
          error: { code: "NOT_FOUND", message: "Document not found" },
        });
      }

      return reply.send({
        success: true,
        data: { document: doc[0] },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.post(
    "/release",
    { schema: { body: ReleaseByQRSchema } },
    async (request: FastifyRequest<{ Body: z.infer<typeof ReleaseByQRSchema> }>, reply: FastifyReply) => {
      const db = createDb();
      const { uuid } = request.body;

      const doc = await db
        .select()
        .from(tblDocIn)
        .where(eq(tblDocIn.uuid, uuid))
        .limit(1);

      if (doc.length === 0) {
        return reply.status(404).send({
          success: false,
          error: { code: "NOT_FOUND", message: "Document not found" },
        });
      }

      return reply.send({
        success: true,
        data: {
          message: "Document ready for release",
          documentId: doc[0].id,
          uuid: doc[0].uuid,
          docName: doc[0].docName,
          pageCount: doc[0].pageCount,
        },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.get(
    "/validate/:uuid",
    async (request: FastifyRequest<{ Params: { uuid: string } }>, reply: FastifyReply) => {
      const db = createDb();
      const { uuid } = request.params;

      const doc = await db
        .select({
          id: tblDocIn.id,
          uuid: tblDocIn.uuid,
          docName: tblDocIn.docName,
          docStatus: tblDocIn.docStatus,
          pageCount: tblDocIn.pageCount,
          expiresAt: tblDocIn.expiresAt,
        })
        .from(tblDocIn)
        .where(eq(tblDocIn.uuid, uuid))
        .limit(1);

      if (doc.length === 0) {
        return reply.status(404).send({
          success: false,
          error: { code: "NOT_FOUND", message: "QR code is invalid or expired" },
        });
      }

      const d = doc[0];
      const isExpired = d.expiresAt && new Date(d.expiresAt) < new Date();
      const isAvailable = d.docStatus === "DEPOSITED" && !isExpired;

      return reply.send({
        success: true,
        data: {
          valid: isAvailable,
          documentId: d.id,
          uuid: d.uuid,
          docName: d.docName,
          pageCount: d.pageCount,
          status: d.docStatus,
          expired: isExpired,
          message: isExpired
            ? "This QR code has expired"
            : d.docStatus === "DEPOSITED"
            ? "Document is available for printing"
            : `Document status: ${d.docStatus}`,
        },
        timestamp: new Date().toISOString(),
      });
    },
  );
}
