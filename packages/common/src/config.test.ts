import { describe, it, expect } from "vitest";
import { appConfigSchema } from "../src/config.js";

describe("Config validation", () => {
  it("should parse valid config", () => {
    const config = appConfigSchema.parse({
      nodeEnv: "development",
      port: 3001,
      database: { url: "postgresql://localhost/printflow", poolMin: 2, poolMax: 10 },
      redis: { url: "redis://localhost:6379" },
      auth: { secret: "a-very-long-secret-key-for-hashing" },
      storage: { documentPath: "./data", thumbnailPath: "./data" },
      print: { ippServerPort: 6310, cupsHost: "localhost", cupsPort: 631 },
      email: { smtpPort: 587, smtpFrom: "noreply@test.com", imapPort: 993 },
      soffice: { path: "/usr/bin/soffice" },
      appUrl: "http://localhost:3000",
      apiUrl: "http://localhost:3001",
    });
    expect(config.nodeEnv).toBe("development");
    expect(config.port).toBe(3001);
  });

  it("should apply defaults", () => {
    const config = appConfigSchema.parse({
      database: { url: "postgresql://localhost/printflow" },
      redis: { url: "redis://localhost:6379" },
      auth: { secret: "a-very-long-secret-key-for-hashing" },
      storage: { documentPath: "./data", thumbnailPath: "./data" },
      print: { cupsHost: "localhost", cupsPort: 631, ippServerPort: 6310 },
      email: { smtpFrom: "noreply@test.com", smtpPort: 587, imapPort: 993 },
      soffice: { path: "/usr/bin/soffice" },
      appUrl: "http://localhost:3000",
      apiUrl: "http://localhost:3001",
    });
    expect(config.logLevel).toBe("info");
    expect(config.database.poolMin).toBe(2);
    expect(config.database.poolMax).toBe(10);
  });

  it("should reject short auth secret", () => {
    expect(() =>
      appConfigSchema.parse({
        database: { url: "postgresql://localhost/printflow" },
        redis: { url: "redis://localhost:6379" },
        auth: { secret: "short" },
        storage: { documentPath: "./data", thumbnailPath: "./data" },
        print: { cupsHost: "localhost", cupsPort: 631, ippServerPort: 6310 },
        email: { smtpFrom: "noreply@test.com", smtpPort: 587, imapPort: 993 },
        soffice: { path: "/usr/bin/soffice" },
        appUrl: "http://localhost:3000",
        apiUrl: "http://localhost:3001",
      })
    ).toThrow();
  });

  it("should reject invalid port", () => {
    expect(() =>
      appConfigSchema.parse({
        port: 80,
        database: { url: "postgresql://localhost/printflow" },
        redis: { url: "redis://localhost:6379" },
        auth: { secret: "a-very-long-secret-key-for-hashing" },
        storage: { documentPath: "./data", thumbnailPath: "./data" },
        print: { cupsHost: "localhost", cupsPort: 631, ippServerPort: 6310 },
        email: { smtpFrom: "noreply@test.com", smtpPort: 587, imapPort: 993 },
        soffice: { path: "/usr/bin/soffice" },
        appUrl: "http://localhost:3000",
        apiUrl: "http://localhost:3001",
      })
    ).toThrow();
  });
});
