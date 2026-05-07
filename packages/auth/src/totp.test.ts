import { describe, it, expect } from "vitest";
import { createTotpService } from "../src/totp.js";

describe("TOTP Service", () => {
  const totp = createTotpService("PrintFlow");

  it("should generate a secret", () => {
    const secret = totp.generateSecret();
    expect(secret).toBeTruthy();
    expect(typeof secret).toBe("string");
    expect(secret.length).toBeGreaterThan(20);
  });

  it("should generate a QR code URL", () => {
    const secret = "JBSWY3DPEHPK3PXP";
    const url = totp.generateQrCodeUrl(secret, "testuser", "PrintFlow");
    expect(url).toContain("otpauth://totp/");
    expect(url).toContain("secret=JBSWY3DPEHPK3PXP");
    expect(url).toContain("issuer=PrintFlow");
  });

  it("should generate backup codes", () => {
    const codes = totp.generateBackupCodes(8);
    expect(codes).toHaveLength(8);
    codes.forEach((code) => {
      expect(code).toHaveLength(8);
      expect(/^\d{8}$/.test(code)).toBe(true);
    });
  });

  it("should generate unique backup codes", () => {
    const codes1 = totp.generateBackupCodes(10);
    const codes2 = totp.generateBackupCodes(10);
    codes1.forEach((code, i) => {
      expect(code).not.toBe(codes2[i]);
    });
  });

  it("should reject invalid token format", () => {
    const secret = totp.generateSecret();
    expect(totp.verify("abc", secret)).toBe(false);
    expect(totp.verify("12345", secret)).toBe(false);
    expect(totp.verify("", secret)).toBe(false);
  });
});
