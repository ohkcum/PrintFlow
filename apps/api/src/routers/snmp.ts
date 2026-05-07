// SNMP Router — mirrors PrintFlowLite's SnmpRetrieveService + ext/snmp/
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { z } from "zod";
import dgram from "dgram";
import { createDrizzle } from "@printflow/db";
import { printers } from "@printflow/db/schema";
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

const OIDS = {
  PRINTER_STATUS: "1.3.6.1.2.1.25.3.5.1.1.1",
  PRINTER_DETAILED_STATUS: "1.3.6.1.2.1.25.3.5.1.1.2",
  SUPPLIES_TYPE: "1.3.6.1.2.1.43.11.1.1.5",
  SUPPLIES_LEVEL: "1.3.6.1.2.1.43.11.1.1.9",
  SUPPLIES_MAX_CAPACITY: "1.3.6.1.2.1.43.11.1.1.8",
  SUPPLIES_UNIT: "1.3.6.1.2.1.43.11.1.1.4",
  COUNTER_UNIT: "1.3.6.1.2.1.43.10.2.1.3",
};

const PRINTER_STATUS_MAP: Record<number, string> = {
  1: "other",
  2: "unknown",
  3: "idle",
  4: "printing",
  5: "warmup",
};

function snmpGet(host: string, oid: string, community = "public", timeout = 3000): Promise<string | null> {
  return new Promise((resolve) => {
    const client = dgram.createSocket("udp4");
    let resolved = false;

    const reqId = Math.floor(Math.random() * 0xffffffff);
    const pdu = buildSnmpGetRequest(reqId, oid, community);
    const msg = Buffer.from(pdu);

    const timer = setTimeout(() => {
      if (!resolved) {
        resolved = true;
        client.close();
        resolve(null);
      }
    }, timeout);

    client.on("message", (buf) => {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        client.close();
        resolve(parseSnmpResponse(buf));
      }
    });

    client.on("error", () => {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        client.close();
        resolve(null);
      }
    });

    try {
      client.send(msg, 0, msg.length, 161, host, (err) => {
        if (err && !resolved) {
          resolved = true;
          clearTimeout(timer);
          client.close();
          resolve(null);
        }
      });
    } catch {
      if (!resolved) {
        resolved = true;
        clearTimeout(timer);
        client.close();
        resolve(null);
      }
    }
  });
}

function buildSnmpGetRequest(reqId: number, oid: string, community: string): number[] {
  const oidBytes = oidToBytes(oid);
  const communityBytes = [...community].map((c) => c.charCodeAt(0));

  const varbind: number[] = [0x30, 0x00];
  const varbindContent = [...oidBytes, 0x05, 0x00];
  varbind[1] = varbindContent.length;
  varbind.push(...varbindContent);

  const reqIdBytes = integerToBytes(reqId);
  const pduContent: number[] = [
    0xa0, 0x00,
    0x02, 0x01, 0x01,
    0x04, communityBytes.length, ...communityBytes,
    0x30, 0x00,
    0x02, reqIdBytes.length, ...reqIdBytes,
    0x02, 0x01, 0x00,
    0x02, 0x01, 0x00,
    0x30, varbind.length, ...varbind,
  ];
  pduContent[2] = pduContent.length - 3;
  pduContent[6] = pduContent.length - 7;

  const msg: number[] = [0x30, 0x00, ...pduContent];
  msg[1] = msg.length - 2;
  return msg;
}

function oidToBytes(oid: string): number[] {
  const parts = oid.split(".").map(Number);
  const bytes: number[] = [];
  if (parts.length >= 2) {
    bytes.push(40 * parts[0] + parts[1]);
    for (let i = 2; i < parts.length; i++) {
      let val = parts[i];
      const octets: number[] = [];
      octets.unshift(val & 0x7f);
      val >>= 7;
      while (val > 0) {
        octets.unshift((val & 0x7f) | 0x80);
        val >>= 7;
      }
      bytes.push(...octets);
    }
  }
  return [0x06, bytes.length, ...bytes];
}

function integerToBytes(n: number): number[] {
  const bytes: number[] = [];
  bytes.unshift(n & 0xff);
  n >>= 8;
  while (n > 0) {
    bytes.unshift(n & 0xff);
    n >>= 8;
  }
  if (bytes.length === 0) bytes.push(0);
  return bytes;
}

function parseSnmpResponse(msg: Buffer): string | null {
  try {
    for (let i = 0; i < msg.length - 4; i++) {
      if (msg[i] === 0x04 || msg[i] === 0x44) {
        const len = msg[i + 1];
        if (len > 0 && len < 128 && i + 2 + len <= msg.length) {
          const value = msg.slice(i + 2, i + 2 + len);
          return value.toString("utf8").replace(/[^\x20-\x7e]/g, "");
        }
      }
    }
    return null;
  } catch {
    return null;
  }
}

function supplyTypeName(code: string | null): string {
  if (!code) return "unknown";
  const map: Record<number, string> = {
    1: "other", 2: "unknown", 3: "toner", 4: "wasteToner",
    5: "ink", 6: "inkCartridge", 7: "inkRibbon", 8: "wasteInk",
    9: "opc", 10: "developer", 11: "fuserOil", 12: "fuserWipeKit",
    13: "coronaWire", 14: "fuser", 17: "tonerCartridge",
    18: "tonerWaste", 19: "cleanerUnit", 20: "processingUnit",
  };
  return map[parseInt(code)] ?? "unknown";
}

function supplyUnitName(code: string | null): string {
  if (!code) return "unknown";
  const map: Record<number, string> = {
    1: "other", 2: "unknown", 3: "percent", 4: "inches",
    5: "imperialGallons", 6: "milliliters", 7: "grams",
    8: "ounces", 9: "pages", 10: "sheets", 11: "hours",
    12: "imperialFeet", 13: "meters", 14: "items", 15: "units",
  };
  return map[parseInt(code)] ?? "unknown";
}

export async function createSnmpRouter(app: FastifyInstance) {
  app.get(
    "/printer/:id/status",
    async (request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) => {
      const db = createDb();
      const printerId = parseInt(request.params.id);

      const [printer] = await db
        .select().from(printers).where(eq(printers.id, printerId)).limit(1);

      if (!printer) {
        return reply.status(404).send({ success: false, error: { code: "NOT_FOUND", message: "Printer not found" } });
      }

      const host = (request.query["host"] as string) ?? printer.ippPrinterUri?.replace(/.*@/, "").replace(/:.*/, "") ?? "localhost";
      const community = (request.query["community"] as string) ?? "public";

      const [statusOid, errorOid] = await Promise.all([
        snmpGet(host, OIDS.PRINTER_STATUS, community),
        snmpGet(host, OIDS.PRINTER_DETAILED_STATUS, community),
      ]);

      const statusCode = statusOid ? parseInt(statusOid) : null;
      const status = statusCode !== null ? (PRINTER_STATUS_MAP[statusCode] ?? "unknown") : "unavailable";

      const [suppliesType, suppliesLevel, suppliesMax, suppliesUnit] = await Promise.all([
        snmpGet(host, OIDS.SUPPLIES_TYPE, community),
        snmpGet(host, OIDS.SUPPLIES_LEVEL, community),
        snmpGet(host, OIDS.SUPPLIES_MAX_CAPACITY, community),
        snmpGet(host, OIDS.SUPPLIES_UNIT, community),
      ]);

      return reply.send({
        success: true,
        data: {
          printerId,
          printerName: printer.name,
          host,
          snmpStatus: status,
          snmpStatusCode: statusCode,
          errorState: errorOid ?? null,
          supplies: {
            type: suppliesType ? parseInt(suppliesType) : null,
            level: suppliesLevel ? parseInt(suppliesLevel) : null,
            max: suppliesMax ? parseInt(suppliesMax) : null,
            unit: suppliesUnit ? parseInt(suppliesUnit) : null,
          },
          polledAt: new Date().toISOString(),
        },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.get(
    "/printer/:id/supplies",
    async (request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) => {
      const db = createDb();
      const printerId = parseInt(request.params.id);

      const [printer] = await db
        .select().from(printers).where(eq(printers.id, printerId)).limit(1);

      if (!printer) {
        return reply.status(404).send({ success: false, error: { code: "NOT_FOUND", message: "Printer not found" } });
      }

      const host = (request.query["host"] as string) ?? printer.ippPrinterUri?.replace(/.*@/, "").replace(/:.*/, "") ?? "localhost";
      const community = (request.query["community"] as string) ?? "public";

      const supplies: Array<{
        index: number;
        type: string | null;
        level: number | null;
        max: number | null;
        unit: string | null;
        percentage: number | null;
      }> = [];

      for (let i = 1; i <= 8; i++) {
        const [type, level, max, unit] = await Promise.all([
          snmpGet(host, `${OIDS.SUPPLIES_TYPE}.${i}`, community),
          snmpGet(host, `${OIDS.SUPPLIES_LEVEL}.${i}`, community),
          snmpGet(host, `${OIDS.SUPPLIES_MAX_CAPACITY}.${i}`, community),
          snmpGet(host, `${OIDS.SUPPLIES_UNIT}.${i}`, community),
        ]);

        if (type) {
          const levelVal = level ? parseInt(level) : null;
          const maxVal = max ? parseInt(max) : null;
          supplies.push({
            index: i,
            type: supplyTypeName(type),
            level: levelVal,
            max: maxVal,
            unit: supplyUnitName(unit),
            percentage: levelVal !== null && maxVal !== null && maxVal > 0 ? Math.round((levelVal / maxVal) * 100) : null,
          });
        }
      }

      return reply.send({
        success: true,
        data: { printerId, printerName: printer.name, supplies },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.get(
    "/printer/:id/counters",
    async (request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) => {
      const db = createDb();
      const printerId = parseInt(request.params.id);

      const [printer] = await db
        .select().from(printers).where(eq(printers.id, printerId)).limit(1);

      if (!printer) {
        return reply.status(404).send({ success: false, error: { code: "NOT_FOUND", message: "Printer not found" } });
      }

      const host = (request.query["host"] as string) ?? printer.ippPrinterUri?.replace(/.*@/, "").replace(/:.*/, "") ?? "localhost";
      const community = (request.query["community"] as string) ?? "public";

      const [lifeCount] = await Promise.all([
        snmpGet(host, OIDS.COUNTER_UNIT, community),
      ]);

      return reply.send({
        success: true,
        data: {
          printerId,
          printerName: printer.name,
          host,
          markerLifeCount: lifeCount ? parseInt(lifeCount) : null,
          polledAt: new Date().toISOString(),
        },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.post(
    "/discover",
    {
      schema: {
        body: z.object({
          network: z.string().default("192.168.1.0/24"),
          community: z.string().default("public"),
          timeout: z.number().default(3000),
        }),
      },
    },
    async (request: FastifyRequest<{ Body: { network: string; community: string; timeout: number } }>, reply: FastifyReply) => {
      const { network, community, timeout } = request.body;

      const parts = network.split("/")[0].split(".");
      if (parts.length < 3) {
        return reply.status(400).send({
          success: false,
          error: { code: "INVALID_NETWORK", message: "Invalid network CIDR" },
        });
      }

      const base = parts.slice(0, 3).join(".");
      const promises = Array.from({ length: 20 }, (_, i) => {
        const host = `${base}.${i + 1}`;
        return snmpGet(host, OIDS.PRINTER_STATUS, community, timeout).then((status) => ({
          host,
          status: status ? (PRINTER_STATUS_MAP[parseInt(status)] ?? status) : null,
        }));
      });

      const results = await Promise.all(promises);
      const found = results.filter((r) => r.status !== null);

      return reply.send({
        success: true,
        data: { discovered: found, scanned: 20, network, community },
        timestamp: new Date().toISOString(),
      });
    },
  );
}
