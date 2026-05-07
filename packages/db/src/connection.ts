import postgres from "postgres";

export type DB = ReturnType<typeof postgres>;

let _db: DB | null = null;

export function createDb(config: { url: string; max?: number }): DB {
  _db = postgres(config.url, {
    max: config.max ?? 10,
    idle_timeout: 20,
    connect_timeout: 10,
    transform: {
      undefined: null,
    },
  });
  return _db;
}

export function getDb(): DB {
  if (!_db) {
    throw new Error("Database not initialized. Call createDb() first.");
  }
  return _db;
}
