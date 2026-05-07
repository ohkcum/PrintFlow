import { describe, it, expect, beforeEach } from "vitest";
import { hashPassword, verifyPassword } from "../src/password.js";

describe("Password hashing", () => {
  it("should hash a password", async () => {
    const hash = await hashPassword("testPassword123");
    expect(hash).toBeTruthy();
    expect(hash.startsWith("scrypt$")).toBe(true);
  });

  it("should verify a correct password", async () => {
    const hash = await hashPassword("mySecretPassword");
    const valid = await verifyPassword("mySecretPassword", hash);
    expect(valid).toBe(true);
  });

  it("should reject an incorrect password", async () => {
    const hash = await hashPassword("mySecretPassword");
    const valid = await verifyPassword("wrongPassword", hash);
    expect(valid).toBe(false);
  });

  it("should reject empty password", async () => {
    const valid = await verifyPassword("", "scrypt$salt$hash");
    expect(valid).toBe(false);
  });

  it("should produce different hashes for same password (salt)", async () => {
    const hash1 = await hashPassword("password");
    const hash2 = await hashPassword("password");
    expect(hash1).not.toBe(hash2);
  });
});
