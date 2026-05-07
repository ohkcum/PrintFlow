// TOTP (Time-based One-Time Password) Service
// Mirrors PrintFlowLite's TOTP functionality from Yubico lib

export interface TotpService {
  generateSecret(): string;
  generateQrCodeUrl(secret: string, userName: string, issuer?: string): string;
  verify(token: string, secret: string, window?: number): boolean;
  generateBackupCodes(count?: number): string[];
}

export function createTotpService(issuer = "PrintFlow"): TotpService {
  // Use a simple HMAC-SHA1 TOTP implementation
  // In production, use otplib or speakeasy

  function base32Encode(buffer: Uint8Array): string {
    const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    let bits = 0;
    let value = 0;
    let output = "";
    for (const byte of buffer) {
      value = (value << 8) | byte;
      bits += 8;
      while (bits >= 5) {
        output += alphabet[(value >>> (bits - 5)) & 31]!;
        bits -= 5;
      }
    }
    if (bits > 0) {
      output += alphabet[(value << (5 - bits)) & 31]!;
    }
    return output;
  }

  function base32Decode(str: string): Uint8Array {
    const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    const cleaned = str.toUpperCase().replace(/=+$/, "");
    const bytes: number[] = [];
    let bits = 0;
    let value = 0;
    for (const char of cleaned) {
      const v = alphabet.indexOf(char);
      if (v === -1) continue;
      value = (value << 5) | v;
      bits += 5;
      if (bits >= 8) {
        bytes.push((value >>> (bits - 8)) & 255);
        bits -= 8;
      }
    }
    return new Uint8Array(bytes);
  }

  function hmacSha1(key: Uint8Array, message: Uint8Array): Uint8Array {
    const crypto = require("node:crypto");
    const hmac = crypto.createHmac("sha1", Buffer.from(key));
    hmac.update(Buffer.from(message));
    return new Uint8Array(hmac.digest());
  }

  function generateSecret(): string {
    const crypto = require("node:crypto");
    const bytes = crypto.randomBytes(20);
    return base32Encode(new Uint8Array(bytes));
  }

  function getTimeStep(time: number, step = 30): number {
    return Math.floor(time / step);
  }

  function intToBytes(num: number): Uint8Array {
    return new Uint8Array([(num >>> 24) & 255, (num >>> 16) & 255, (num >>> 8) & 255, num & 255]);
  }

  function dynamicTruncate(digest: Uint8Array): number {
    const offset = digest[digest.length - 1]! & 15;
    return (
      ((digest[offset]! & 127) << 24) |
      ((digest[(offset + 1) % digest.length]! & 255) << 16) |
      ((digest[(offset + 2) % digest.length]! & 255) << 8) |
      (digest[(offset + 3) % digest.length]! & 255)
    );
  }

  function generateOtp(secret: string, time: number): string {
    const key = base32Decode(secret);
    const timeStep = getTimeStep(time);
    const timeBytes = intToBytes(timeStep);
    const hmac = hmacSha1(key, timeBytes);
    const otp = dynamicTruncate(hmac) % 1000000;
    return otp.toString().padStart(6, "0");
  }

  return {
    generateSecret,

    generateQrCodeUrl(secret: string, userName: string, iss?: string): string {
      const issuerName = iss ?? issuer;
      const label = encodeURIComponent(`${issuerName}:${userName}`);
      const issuerEncoded = encodeURIComponent(issuerName);
      const params = new URLSearchParams({
        secret,
        issuer: issuerName,
        algorithm: "SHA1",
        digits: "6",
        period: "30",
      });
      return `otpauth://totp/${label}?${params.toString()}`;
    },

    verify(token: string, secret: string, window = 1): boolean {
      const time = Math.floor(Date.now() / 1000);
      const tokenNum = parseInt(token, 10);
      if (isNaN(tokenNum) || token.length !== 6) return false;

      for (let i = -window; i <= window; i++) {
        const checkTime = time + i * 30;
        const expected = generateOtp(secret, checkTime);
        if (expected === token) return true;
      }
      return false;
    },

    generateBackupCodes(count = 8): string[] {
      const crypto = require("node:crypto");
      const codes: string[] = [];
      for (let i = 0; i < count; i++) {
        const bytes = crypto.randomBytes(8);
        const code = (bytes as unknown as number[])
          .map((b) => (b % 10).toString())
          .join("")
          .slice(0, 8);
        codes.push(code);
      }
      return codes;
    },
  };
}
