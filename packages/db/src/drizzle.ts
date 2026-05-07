import { drizzle } from "drizzle-orm/postgres-js";
import { createDb } from "./connection.js";
import * as usersSchema from "./schema/users.js";
import * as printersSchema from "./schema/printers.js";
import * as documentsSchema from "./schema/documents.js";
import * as financialSchema from "./schema/financial.js";
import * as systemSchema from "./schema/system.js";
import type { AppConfig } from "@printflow/common";

export const schema = {
  ...usersSchema,
  ...printersSchema,
  ...documentsSchema,
  ...financialSchema,
  ...systemSchema,
};

export function createDrizzle(config: AppConfig) {
  const db = createDb({
    url: config.database.url,
    max: config.database.poolMax,
  });
  return drizzle(db, { schema });
}

export type DrizzleDB = ReturnType<typeof createDrizzle>;
