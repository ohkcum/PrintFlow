// PrintFlow Database Seed Script
// Creates initial system data from PrintFlowLite defaults

import postgres from "postgres";
import { drizzle } from "drizzle-orm/postgres-js";
import * as schema from "../schema/index.js";

const sql = postgres(
  process.env["DATABASE_URL"] ??
    "postgresql://printflow:password@localhost:5432/printflow",
);
const db = drizzle(sql, { schema });

async function seed() {
  console.log("Seeding database...");

  // Admin user
  const [admin] = await db
    .insert(schema.users)
    .values({
      uuid: crypto.randomUUID(),
      userName: "admin",
      fullName: "System Administrator",
      email: "admin@printflow.local",
      passwordHash:
        "scrypt$4sjepeFDp6PQdiikbbvLGw$giD8nh7Bl3aSRiH9XmIuLxNXj4b4OgfagtTLymOUSw0", // password: admin
      userIdMethod: "INTERNAL",
      roles: ["ADMIN"],
      status: "ACTIVE",
      printQuota: "1000",
      printBalance: "1000",
    })
    .returning();

  // Demo user
  const [demo] = await db
    .insert(schema.users)
    .values({
      uuid: crypto.randomUUID(),
      userName: "demo",
      fullName: "Demo User",
      email: "demo@printflow.local",
      passwordHash:
        "scrypt$YSdsIv1xuwQQZocnx2JhOg$eOwsCBHwkXlzB9mZItIbgFea9aaljMD4uMJQ9XrCF4Y", // password: demo
      userIdMethod: "INTERNAL",
      roles: ["USER"],
      status: "ACTIVE",
      printQuota: "100",
      printBalance: "100",
    })
    .returning();

  // User accounts
  if (!admin || !demo) throw new Error("Failed to create admin/demo users");
  await db.insert(schema.userAccounts).values([
    {
      uuid: crypto.randomUUID(),
      userId: admin.id,
      accountName: "Admin Account",
      balance: "1000",
    },
    {
      uuid: crypto.randomUUID(),
      userId: demo.id,
      accountName: "Demo Account",
      balance: "100",
    },
  ]);

  // Shared account
  await db.insert(schema.accounts).values({
    uuid: crypto.randomUUID(),
    accountType: "SHARED",
    accountName: "Shared Walk-Up Account",
    description: "Anonymous shared account for walk-up printing",
    balance: "0",
    isEnabled: true,
    defaultCostPerPageMono: "0.01",
    defaultCostPerPageColor: "0.05",
  });

  // User groups
  const [students] = await db
    .insert(schema.userGroups)
    .values({
      uuid: crypto.randomUUID(),
      name: "Students",
      description: "Default student group",
      defaultRoles: ["USER"],
    })
    .returning();

  const [staff] = await db
    .insert(schema.userGroups)
    .values({
      uuid: crypto.randomUUID(),
      name: "Staff",
      description: "Staff and faculty",
      defaultRoles: ["USER", "DELEGATOR"],
    })
    .returning();

  // Group members
  if (!demo || !students) throw new Error("Failed to create demo/students");
  await db
    .insert(schema.userGroupMembers)
    .values([{ userId: demo.id, groupId: students.id }]);

  // Printer groups
  const [bwGroup] = await db
    .insert(schema.printerGroups)
    .values({
      uuid: crypto.randomUUID(),
      name: "Black & White",
      description: "Black and white printers",
      displayOrder: 1,
    })
    .returning();

  const [colorGroup] = await db
    .insert(schema.printerGroups)
    .values({
      uuid: crypto.randomUUID(),
      name: "Color Printers",
      description: "Color printing devices",
      displayOrder: 2,
    })
    .returning();

  // IPP Queues
  if (!bwGroup || !colorGroup)
    throw new Error("Failed to create printer groups");
  await db.insert(schema.ippQueues).values([
    {
      uuid: crypto.randomUUID(),
      name: "bw-printer-queue",
      uri: "ipp://localhost:631/printers/bw-printer",
      isEnabled: true,
      isDefault: true,
      printerGroupId: bwGroup.id,
    },
    {
      uuid: crypto.randomUUID(),
      name: "color-printer-queue",
      uri: "ipp://localhost:631/printers/color-printer",
      isEnabled: true,
      isDefault: false,
      printerGroupId: colorGroup.id,
    },
  ]);

  // Printers
  await db.insert(schema.printers).values([
    {
      uuid: crypto.randomUUID(),
      name: "BW-HP-LaserJet",
      description: "HP LaserJet Pro B&W",
      ippPrinterUri: "ipp://localhost:631/printers/bw-printer",
      displayName: "Black & White Laser",
      printerType: "NETWORK",
      printerStatus: "OFFLINE",
      printerGroupId: bwGroup.id,
      colorMode: "MONOCHROME",
      supportsDuplex: true,
      supportsStaple: true,
      supportsPunch: false,
      supportsFold: false,
      maxPaperSize: "A4",
      minPaperSize: "A5",
      costPerPageMono: "0.01",
      costPerPageColor: "0.05",
      costPerSheet: "0",
      fixedCost: "0",
      ecoPrintCostPerPage: "0.005",
      isEnabled: true,
      isPublic: true,
      requireRelease: true,
      ecoPrintDefault: false,
      snmpEnabled: false,
    },
    {
      uuid: crypto.randomUUID(),
      name: "Color-Epson-WorkForce",
      description: "Epson WorkForce Color Printer",
      ippPrinterUri: "ipp://localhost:631/printers/color-printer",
      displayName: "Color Printer",
      printerType: "NETWORK",
      printerStatus: "OFFLINE",
      printerGroupId: colorGroup.id,
      colorMode: "AUTO",
      supportsDuplex: true,
      supportsStaple: false,
      supportsPunch: false,
      supportsFold: false,
      maxPaperSize: "A4",
      minPaperSize: "A5",
      costPerPageMono: "0.01",
      costPerPageColor: "0.10",
      costPerSheet: "0",
      fixedCost: "0",
      ecoPrintCostPerPage: "0.005",
      isEnabled: true,
      isPublic: true,
      requireRelease: true,
      ecoPrintDefault: false,
      snmpEnabled: false,
    },
  ]);

  // Config properties
  await db.insert(schema.configProperties).values([
    {
      propKey: "app.name",
      propValue: "PrintFlow",
      description: "Application name",
      category: "GENERAL",
    },
    {
      propKey: "app.version",
      propValue: "0.1.0",
      description: "Application version",
      category: "GENERAL",
    },
    {
      propKey: "document.retention.days",
      propValue: "30",
      description: "Days before documents expire",
      category: "DOCUMENT",
    },
    {
      propKey: "document.max.upload.size",
      propValue: "52428800",
      description: "Max upload size in bytes (50MB)",
      category: "DOCUMENT",
    },
    {
      propKey: "print.default.copies",
      propValue: "1",
      description: "Default number of copies",
      category: "PRINT",
    },
    {
      propKey: "print.eco.print.enabled",
      propValue: "true",
      description: "Enable eco print by default",
      category: "PRINT",
    },
    {
      propKey: "auth.session.timeout",
      propValue: "3600",
      description: "Session timeout in seconds",
      category: "AUTH",
    },
    {
      propKey: "auth.totp.enabled",
      propValue: "false",
      description: "Require TOTP for all users",
      category: "AUTH",
    },
  ]);

  console.log("Seed completed!");
  console.log(`Admin user ID: ${admin!.id}`);
  console.log(`Demo user ID: ${demo!.id}`);
  await sql.end();
}

seed().catch(console.error);
