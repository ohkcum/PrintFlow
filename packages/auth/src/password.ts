import { createHash, randomBytes, timingSafeEqual, pbkdf2Sync } from "node:crypto";
import type { DrizzleDB } from "@printflow/db";
import { users, sessions } from "@printflow/db/schema";
import { eq } from "drizzle-orm";

const BCRYPT_ROUNDS = 12;

// Simple bcrypt-like hash using Node's built-in crypto
// In production, use bcrypt or argon2
export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(16).toString("base64url");
  const hash = await scryptHash(password, salt);
  return `scrypt$${salt}$${hash}`;
}

export async function verifyPassword(
  password: string,
  storedHash: string
): Promise<boolean> {
  if (!storedHash || !password) return false;

  if (storedHash.startsWith("scrypt$")) {
    const [, salt, hash] = storedHash.split("$");
    const computed = await scryptHash(password, salt!);
    return timingSafeEqualFromHex(computed, hash!);
  }

  if (storedHash.startsWith("$2")) {
    // bcrypt format - in production use bcrypt.compare
    return bcryptCompare(password, storedHash);
  }

  // Legacy MD5/SHA1 from PrintFlowLite
  if (storedHash.includes("$")) {
    const parts = storedHash.split("$");
    const algo = parts[0]!;
    const salt = parts[1] ?? "";
    const hash = parts[2] ?? "";
    const computed = computeLegacyHash(algo, password, salt);
    return timingSafeEqualFromHex(computed, hash);
  }

  return false;
}

async function scryptHash(password: string, salt: string): Promise<string> {
  const saltBuf = Buffer.from(salt, "base64url");
  const key = pbkdf2Sync(password, saltBuf, 100000, 32, "sha512");
  return key.toString("base64url");
}

function timingSafeEqualFromHex(a: string, b: string): boolean {
  const aBuf = Buffer.from(a, "base64url");
  const bBuf = Buffer.from(b, "base64url");
  if (aBuf.length !== bBuf.length) return false;
  try {
    return timingSafeEqual(aBuf, bBuf);
  } catch {
    return false;
  }
}

function computeLegacyHash(algo: string, password: string, salt: string): string {
  // Support PrintFlowLite's legacy hash formats
  const hash = createHash(algo === "sha1" ? "sha1" : "md5");
  hash.update(password);
  hash.update(salt);
  return hash.digest("hex");
}

// bcrypt.compare polyfill using Node crypto
function bcryptCompare(password: string, hash: string): boolean {
  // For now, return false for bcrypt hashes without bcrypt module
  // In production, install bcrypt or argon2
  return false;
}
