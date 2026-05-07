// OAuth Router — mirrors PrintFlowLite's OAuthClientPlugin + ext/oauth/
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { z } from "zod";
import crypto from "crypto";

export const OAuthProviders = ["GOOGLE", "AZURE", "KEYCLOAK", "SMARTSCHOOL"] as const;
export type OAuthProvider = (typeof OAuthProviders)[number];

const pendingAuth: Map<string, { provider: string; redirectUri: string; createdAt: number }> = new Map();

interface OAuthClientConfig {
  clientId: string;
  clientSecret: string;
  redirectUri: string;
  authUrl: string;
  tokenUrl: string;
  userInfoUrl: string;
}

const providerConfigs: Record<string, OAuthClientConfig> = {
  GOOGLE: {
    clientId: process.env["OAUTH_GOOGLE_CLIENT_ID"] ?? "",
    clientSecret: process.env["OAUTH_GOOGLE_CLIENT_SECRET"] ?? "",
    redirectUri: `${process.env["APP_URL"] ?? "http://localhost:3000"}/auth/callback/google`,
    authUrl: "https://accounts.google.com/o/oauth2/v2/auth",
    tokenUrl: "https://oauth2.googleapis.com/token",
    userInfoUrl: "https://www.googleapis.com/oauth2/v2/userinfo",
  },
  AZURE: {
    clientId: process.env["OAUTH_AZURE_CLIENT_ID"] ?? "",
    clientSecret: process.env["OAUTH_AZURE_CLIENT_SECRET"] ?? "",
    redirectUri: `${process.env["APP_URL"] ?? "http://localhost:3000"}/auth/callback/azure`,
    authUrl: `https://login.microsoftonline.com/${process.env["OAUTH_AZURE_TENANT_ID"] ?? "common"}/oauth2/v2.0/authorize`,
    tokenUrl: `https://login.microsoftonline.com/${process.env["OAUTH_AZURE_TENANT_ID"] ?? "common"}/oauth2/v2.0/token`,
    userInfoUrl: "https://graph.microsoft.com/oidc/userinfo",
  },
  KEYCLOAK: {
    clientId: process.env["OAUTH_KEYCLOAK_CLIENT_ID"] ?? "",
    clientSecret: process.env["OAUTH_KEYCLOAK_CLIENT_SECRET"] ?? "",
    redirectUri: `${process.env["APP_URL"] ?? "http://localhost:3000"}/auth/callback/keycloak`,
    authUrl: `${process.env["OAUTH_KEYCLOAK_URL"] ?? "http://localhost:8080"}/realms/printflow/protocol/openid-connect/auth`,
    tokenUrl: `${process.env["OAUTH_KEYCLOAK_URL"] ?? "http://localhost:8080"}/realms/printflow/protocol/openid-connect/token`,
    userInfoUrl: `${process.env["OAUTH_KEYCLOAK_URL"] ?? "http://localhost:8080"}/realms/printflow/protocol/openid-connect/userinfo`,
  },
  SMARTSCHOOL: {
    clientId: process.env["OAUTH_SMARTSCHOOL_CLIENT_ID"] ?? "",
    clientSecret: process.env["OAUTH_SMARTSCHOOL_CLIENT_SECRET"] ?? "",
    redirectUri: `${process.env["APP_URL"] ?? "http://localhost:3000"}/auth/callback/smartschool`,
    authUrl: `${process.env["OAUTH_SMARTSCHOOL_URL"] ?? ""}/oauth/authorize`,
    tokenUrl: `${process.env["OAUTH_SMARTSCHOOL_URL"] ?? ""}/oauth/token`,
    userInfoUrl: `${process.env["OAUTH_SMARTSCHOOL_URL"] ?? ""}/api/v1/userinfo`,
  },
};

function buildAuthUrl(provider: string, state: string, scopes: string[]): string {
  const cfg = providerConfigs[provider];
  if (!cfg) return "";

  const params = new URLSearchParams({
    client_id: cfg.clientId,
    redirect_uri: cfg.redirectUri,
    response_type: "code",
    scope: scopes.join(" "),
    state,
  });

  return `${cfg.authUrl}?${params.toString()}`;
}

async function exchangeCode(code: string, provider: string): Promise<{ accessToken: string; idToken?: string } | null> {
  const cfg = providerConfigs[provider];
  if (!cfg) return null;

  const body = new URLSearchParams({
    client_id: cfg.clientId,
    client_secret: cfg.clientSecret,
    code,
    grant_type: "authorization_code",
    redirect_uri: cfg.redirectUri,
  });

  try {
    const res = await fetch(cfg.tokenUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString(),
    });

    if (!res.ok) return null;
    const data = await res.json();
    return { accessToken: data.access_token, idToken: data.id_token };
  } catch {
    return null;
  }
}

async function getUserInfo(provider: string, accessToken: string): Promise<{ userId: string; email?: string; name?: string } | null> {
  const cfg = providerConfigs[provider];
  if (!cfg) return null;

  try {
    const res = await fetch(cfg.userInfoUrl, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    if (!res.ok) return null;
    const data = await res.json();

    if (provider === "GOOGLE") {
      return { userId: data.sub ?? data.email, email: data.email, name: data.name };
    } else if (provider === "AZURE") {
      return { userId: data.sub ?? data.email, email: data.email, name: data.name };
    } else if (provider === "KEYCLOAK") {
      return { userId: data.sub ?? data.preferred_username, email: data.email, name: data.name };
    } else if (provider === "SMARTSCHOOL") {
      return { userId: data.id?.toString() ?? data.email, email: data.email, name: data.fullName ?? data.name };
    }

    return { userId: data.sub ?? data.email ?? "unknown", email: data.email, name: data.name };
  } catch {
    return null;
  }
}

export async function createOAuthRouter(app: FastifyInstance) {
  app.get("/providers", async (_request: FastifyRequest, reply: FastifyReply) => {
    const providers = OAuthProviders.map((p) => ({
      id: p,
      name: p.charAt(0) + p.slice(1).toLowerCase(),
      configured: !!providerConfigs[p].clientId,
      icon: `/icons/${p.toLowerCase()}.svg`,
    }));

    return reply.send({
      success: true,
      data: { providers },
      timestamp: new Date().toISOString(),
    });
  });

  app.get(
    "/authorize/:provider",
    async (request: FastifyRequest<{ Params: { provider: string } }>, reply: FastifyReply) => {
      const { provider } = request.params;

      if (!OAuthProviders.includes(provider as any)) {
        return reply.status(400).send({
          success: false,
          error: { code: "INVALID_PROVIDER", message: "Unsupported OAuth provider" },
        });
      }

      const state = crypto.randomBytes(32).toString("hex");
      const scopes =
        provider === "GOOGLE"
          ? ["openid", "email", "profile"]
          : provider === "AZURE"
          ? ["openid", "email", "profile"]
          : provider === "KEYCLOAK"
          ? ["openid", "email", "profile"]
          : ["read", "userinfo"];

      const redirectUri = request.query["redirect_uri"] as string ?? "/";
      pendingAuth.set(state, { provider, redirectUri, createdAt: Date.now() });

      const tenMinutesAgo = Date.now() - 10 * 60 * 1000;
      for (const [k, v] of pendingAuth.entries()) {
        if (v.createdAt < tenMinutesAgo) pendingAuth.delete(k);
      }

      const authUrl = buildAuthUrl(provider, state, scopes);
      return reply.redirect(authUrl || "/login?error=oauth_not_configured");
    },
  );

  app.get(
    "/callback/:provider",
    async (request: FastifyRequest<{ Params: { provider: string }; Querystring: any }>, reply: FastifyReply) => {
      const { provider } = request.params;
      const { code, state, error, error_description } = request.query;

      if (error) {
        return reply.redirect(`/login?error=oauth_denied&description=${encodeURIComponent(error_description ?? error)}`);
      }

      const pending = pendingAuth.get(state as string);
      if (!pending || pending.provider !== provider) {
        return reply.redirect("/login?error=oauth_invalid_state");
      }
      pendingAuth.delete(state as string);

      const tokens = await exchangeCode(code as string, provider);
      if (!tokens) {
        return reply.redirect("/login?error=oauth_token_exchange_failed");
      }

      const userInfo = await getUserInfo(provider, tokens.accessToken);
      if (!userInfo) {
        return reply.redirect("/login?error=oauth_userinfo_failed");
      }

      return reply.send({
        success: true,
        data: {
          provider,
          userId: userInfo.userId,
          email: userInfo.email,
          name: userInfo.name,
          accessToken: tokens.accessToken,
          idToken: tokens.idToken,
          redirectUri: pending.redirectUri,
        },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.get(
    "/userinfo",
    {
      schema: {
        querystring: z.object({
          provider: z.string(),
          accessToken: z.string(),
        }),
      },
    },
    async (
      request: FastifyRequest<{ Querystring: { provider: string; accessToken: string } }>,
      reply: FastifyReply,
    ) => {
      const { provider, accessToken } = request.query;
      const userInfo = await getUserInfo(provider, accessToken);

      if (!userInfo) {
        return reply.status(401).send({
          success: false,
          error: { code: "INVALID_TOKEN", message: "Could not retrieve user info" },
        });
      }

      return reply.send({
        success: true,
        data: { userInfo },
        timestamp: new Date().toISOString(),
      });
    },
  );
}
