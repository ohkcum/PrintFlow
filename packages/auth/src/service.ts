// Auth Service - mirrors PrintFlowLite's UserService authentication logic
import type { DrizzleDB } from "@printflow/db";
import { users } from "@printflow/db/schema";
import { eq, and } from "drizzle-orm";
import { hashPassword, verifyPassword } from "./password.js";
import { createSessionManager, type Session, type SessionManager } from "./session.js";
import { createTotpService } from "./totp.js";

export interface AuthUser {
  id: number;
  uuid: string;
  userName: string;
  fullName: string;
  email: string | null;
  roles: string[];
  status: string;
  totpEnabled: boolean;
  printBalance: string;
}

export interface AuthResult {
  success: true;
  user: AuthUser;
  session: Session;
}

export interface AuthService {
  // User auth
  register(data: {
    userName: string;
    fullName: string;
    email?: string;
    password: string;
  }): Promise<AuthResult>;
  login(data: {
    userName: string;
    password: string;
    totpToken?: string;
    ipAddress?: string;
    userAgent?: string;
  }): Promise<AuthResult>;
  logout(token: string): Promise<void>;
  changePassword(userId: number, oldPassword: string, newPassword: string): Promise<void>;

  // Session
  validateSession(token: string): Promise<Session | null>;
  revokeAllSessions(userId: number): Promise<void>;

  // TOTP
  enableTotp(userId: number): Promise<{ secret: string; qrUrl: string; backupCodes: string[] }>;
  disableTotp(userId: number, totpToken: string): Promise<void>;
  verifyTotp(userId: number, token: string): boolean;

  // User management helpers
  getUserById(id: number): Promise<AuthUser | null>;
  getUserByUserName(userName: string): Promise<AuthUser | null>;
}

export function createAuthService(db: DrizzleDB): AuthService {
  const sessions = createSessionManager(db);
  const totp = createTotpService();

  function toAuthUser(row: typeof users.$inferSelect): AuthUser {
    return {
      id: row.id,
      uuid: row.uuid,
      userName: row.userName,
      fullName: row.fullName,
      email: row.email,
      roles: row.roles as string[],
      status: row.status,
      totpEnabled: row.totpEnabled,
      printBalance: row.printBalance ?? "0",
    };
  }

  return {
    async register(data) {
      const existing = await db
        .select()
        .from(users)
        .where(eq(users.userName, data.userName))
        .limit(1);
      if (existing.length > 0) {
        throw new AuthError("USERNAME_EXISTS", "Username already exists");
      }

      const passwordHash = await hashPassword(data.password);

      const [user] = await db
        .insert(users)
        .values({
          uuid: crypto.randomUUID(),
          userName: data.userName,
          fullName: data.fullName,
          email: data.email ?? null,
          passwordHash,
          userIdMethod: "INTERNAL",
          roles: ["USER"],
          status: "ACTIVE",
          printBalance: "0",
          printQuota: "100",
        })
        .returning();

      if (!user) throw new Error("Failed to create user");

      const session = await sessions.create({
        userId: user.id,
        userName: user.userName,
        roles: user.roles as string[],
      });

      return {
        success: true,
        user: toAuthUser(user),
        session,
      };
    },

    async login(data) {
      const [user] = await db
        .select()
        .from(users)
        .where(eq(users.userName, data.userName))
        .limit(1);

      if (!user) {
        throw new AuthError("INVALID_CREDENTIALS", "Invalid username or password");
      }

      if (user.status !== "ACTIVE") {
        throw new AuthError("ACCOUNT_INACTIVE", `Account is ${user.status.toLowerCase()}`);
      }

      const valid = await verifyPassword(data.password, user.passwordHash ?? "");
      if (!valid) {
        throw new AuthError("INVALID_CREDENTIALS", "Invalid username or password");
      }

      // Verify TOTP if enabled
      if (user.totpEnabled && user.totpSecret) {
        if (!data.totpToken) {
          throw new AuthError("TOTP_REQUIRED", "TOTP token required");
        }
        if (!totp.verify(data.totpToken, user.totpSecret)) {
          throw new AuthError("TOTP_INVALID", "Invalid TOTP token");
        }
      }

      const session = await sessions.create({
        userId: user.id,
        userName: user.userName,
        roles: user.roles as string[],
        ipAddress: data.ipAddress,
        userAgent: data.userAgent,
      });

      return {
        success: true,
        user: toAuthUser(user),
        session,
      };
    },

    async logout(token: string) {
      await sessions.revoke(token);
    },

    async changePassword(userId: number, oldPassword: string, newPassword: string) {
      const [user] = await db.select().from(users).where(eq(users.id, userId)).limit(1);
      if (!user) throw new AuthError("USER_NOT_FOUND", "User not found");

      const valid = await verifyPassword(oldPassword, user.passwordHash ?? "");
      if (!valid) throw new AuthError("INVALID_PASSWORD", "Current password is incorrect");

      const newHash = await hashPassword(newPassword);
      await db
        .update(users)
        .set({ passwordHash: newHash, dateModified: new Date() })
        .where(eq(users.id, userId));
    },

    async validateSession(token: string) {
      return sessions.validate(token);
    },

    async revokeAllSessions(userId: number) {
      await sessions.revokeAllForUser(userId);
    },

    async enableTotp(userId: number) {
      const secret = totp.generateSecret();
      const qrUrl = totp.generateQrCodeUrl(secret, "");
      const backupCodes = totp.generateBackupCodes(8);

      await db
        .update(users)
        .set({ totpSecret: secret, totpEnabled: true, dateModified: new Date() })
        .where(eq(users.id, userId));

      return { secret, qrUrl, backupCodes };
    },

    async disableTotp(userId: number, totpToken: string) {
      const [user] = await db.select().from(users).where(eq(users.id, userId)).limit(1);
      if (!user || !user.totpSecret) {
        throw new AuthError("TOTP_NOT_ENABLED", "TOTP is not enabled");
      }

      if (!totp.verify(totpToken, user.totpSecret)) {
        throw new AuthError("TOTP_INVALID", "Invalid TOTP token");
      }

      await db
        .update(users)
        .set({ totpSecret: null, totpEnabled: false, dateModified: new Date() })
        .where(eq(users.id, userId));
    },

    verifyTotp(userId: number, token: string): boolean {
      // This is handled in login flow via user.totpSecret
      return false;
    },

    async getUserById(id: number) {
      const [user] = await db.select().from(users).where(eq(users.id, id)).limit(1);
      return user ? toAuthUser(user) : null;
    },

    async getUserByUserName(userName: string) {
      const [user] = await db
        .select()
        .from(users)
        .where(eq(users.userName, userName))
        .limit(1);
      return user ? toAuthUser(user) : null;
    },
  };
}

export class AuthError extends Error {
  constructor(
    public readonly code: string,
    message: string
  ) {
    super(message);
    this.name = "AuthError";
  }
}
