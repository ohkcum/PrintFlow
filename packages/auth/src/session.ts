import { randomBytes } from "node:crypto";
import { createHash } from "node:crypto";
import type { DrizzleDB } from "@printflow/db";
import { sessions, users } from "@printflow/db/schema";
import { eq, and, gt } from "drizzle-orm";

export interface Session {
  id: number;
  token: string;
  userId: number;
  userName: string;
  roles: string[];
  expiresAt: Date;
  ipAddress: string | null;
  userAgent: string | null;
}

export interface SessionManager {
  create(data: {
    userId: number;
    userName: string;
    roles: string[];
    ipAddress?: string;
    userAgent?: string;
    expiresInSeconds?: number;
  }): Promise<Session>;
  validate(token: string): Promise<Session | null>;
  revoke(token: string): Promise<void>;
  revokeAllForUser(userId: number): Promise<void>;
  cleanup(): Promise<number>;
}

export function createSessionManager(db: DrizzleDB): SessionManager {
  const SESSION_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 days

  async function createToken(): Promise<string> {
    return randomBytes(48).toString("base64url");
  }

  function hashToken(token: string): string {
    return createHash("sha256").update(token).digest("base64url");
  }

  return {
    async create(data) {
      const ttlSeconds = data.expiresInSeconds ?? SESSION_TTL_SECONDS;
      const token = await createToken();
      const tokenHash = hashToken(token);
      const expiresAt = new Date(Date.now() + ttlSeconds * 1000);

      const [session] = await db
        .insert(sessions)
        .values({
          tokenHash,
          userId: data.userId,
          expiresAt,
          ipAddress: data.ipAddress ?? null,
          userAgent: data.userAgent ?? null,
        })
        .returning();

      if (!session) throw new Error("Failed to create session");

      return {
        id: session.id,
        token,
        userId: session.userId,
        userName: data.userName,
        roles: data.roles,
        expiresAt: session.expiresAt,
        ipAddress: session.ipAddress,
        userAgent: session.userAgent,
      };
    },

    async validate(token: string): Promise<Session | null> {
      const tokenHash = hashToken(token);

      const [session] = await db
        .select()
        .from(sessions)
        .innerJoin(users, eq(sessions.userId, users.id))
        .where(
          and(
            eq(sessions.tokenHash, tokenHash),
            gt(sessions.expiresAt, new Date())
          )
        )
        .limit(1);

      if (!session) return null;

      const s = session.tbl_session;
      const u = session.tbl_user;
      return {
        id: s.id,
        token,
        userId: s.userId,
        userName: u.fullName,
        roles: (u.roles ?? []) as string[],
        expiresAt: s.expiresAt,
        ipAddress: s.ipAddress,
        userAgent: s.userAgent,
      };
    },

    async revoke(token: string): Promise<void> {
      const tokenHash = hashToken(token);
      await db.delete(sessions).where(eq(sessions.tokenHash, tokenHash));
    },

    async revokeAllForUser(userId: number): Promise<void> {
      await db.delete(sessions).where(eq(sessions.userId, userId));
    },

    async cleanup(): Promise<number> {
      await db
        .delete(sessions)
        .where(gt(sessions.expiresAt, new Date()));
      return 0; // drizzle doesn't return row count easily
    },
  };
}
