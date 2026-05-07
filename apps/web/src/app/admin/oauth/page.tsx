"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Settings2,
  CheckCircle,
  AlertCircle,
  Loader2,
  RefreshCw,
  X,
  ExternalLink,
} from "lucide-react";
import { oauthApi, type OAuthProvider } from "@/lib/api";

// ─── Shared Components ─────────────────────────────────────────────────────────

function btnStyle(
  variant: "primary" | "secondary" | "danger" | "ghost",
): React.CSSProperties {
  const base: React.CSSProperties = {
    padding: "0.5rem 1rem",
    borderRadius: "var(--radius-md)",
    fontSize: "0.85rem",
    fontWeight: 600,
    cursor: "pointer",
    border: "none",
    transition: "opacity 0.15s",
    display: "inline-flex",
    alignItems: "center",
    gap: "0.375rem",
  };
  if (variant === "primary")
    return {
      ...base,
      background: "var(--color-primary)",
      color: "var(--color-primary-foreground)",
    };
  if (variant === "danger")
    return { ...base, background: "oklch(0.65 0.22 25)", color: "white" };
  if (variant === "ghost")
    return {
      ...base,
      background: "transparent",
      color: "var(--color-muted-foreground)",
      border: "1px solid var(--color-border)",
    };
  return {
    ...base,
    background: "var(--color-secondary)",
    color: "var(--color-secondary-foreground)",
    border: "1px solid var(--color-border)",
  };
}

function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: React.ReactNode;
  onClose: () => void;
}) {
  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 100,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "oklch(0 0 0 / 0.6)",
        backdropFilter: "blur(4px)",
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-xl)",
          padding: "1.5rem",
          maxWidth: "560px",
          width: "90%",
          boxShadow: "var(--shadow-lg)",
          maxHeight: "85vh",
          overflowY: "auto",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            marginBottom: "1.25rem",
          }}
        >
          <h3 style={{ fontSize: "1.1rem", fontWeight: 700 }}>{title}</h3>
          <button
            onClick={onClose}
            style={{
              background: "none",
              border: "none",
              cursor: "pointer",
              color: "var(--color-muted-foreground)",
              padding: "4px",
              display: "flex",
              alignItems: "center",
            }}
          >
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function Toast({
  message,
  type,
}: {
  message: string;
  type: "success" | "error";
}) {
  return (
    <div
      style={{
        position: "fixed",
        bottom: "1.5rem",
        right: "1.5rem",
        zIndex: 200,
        padding: "0.75rem 1.25rem",
        borderRadius: "var(--radius-md)",
        background:
          type === "success" ? "oklch(0.72 0.18 145)" : "oklch(0.65 0.22 25)",
        color: "white",
        fontSize: "0.875rem",
        fontWeight: 600,
        boxShadow: "var(--shadow-lg)",
        animation: "fadeIn 0.2s ease",
      }}
    >
      {message}
    </div>
  );
}

// ─── Provider Config ──────────────────────────────────────────────────────────

const PROVIDER_DEFAULTS: Record<
  string,
  { authUrl: string; tokenUrl: string; userInfoUrl: string; scopes: string }
> = {
  google: {
    authUrl: "https://accounts.google.com/o/oauth2/v2/auth",
    tokenUrl: "https://oauth2.googleapis.com/token",
    userInfoUrl: "https://www.googleapis.com/oauth2/v3/userinfo",
    scopes: "openid email profile",
  },
  azure: {
    authUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
    tokenUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/token",
    userInfoUrl: "https://graph.microsoft.com/v1.0/me",
    scopes: "openid email profile User.Read",
  },
  keycloak: {
    authUrl: "",
    tokenUrl: "",
    userInfoUrl: "",
    scopes: "openid email profile",
  },
  smartschool: {
    authUrl: "",
    tokenUrl: "",
    userInfoUrl: "",
    scopes: "openid email profile",
  },
};

const PROVIDER_ICONS: Record<string, string> = {
  google: "G",
  azure: "A",
  keycloak: "K",
  smartschool: "S",
};

const PROVIDER_COLORS: Record<string, string> = {
  google: "#4285F4",
  azure: "#0078D4",
  keycloak: "#FF3633",
  smartschool: "#6B46C1",
};

type ProviderConfig = {
  clientId: string;
  clientSecret: string;
  redirectUri: string;
  authUrl: string;
  tokenUrl: string;
  userInfoUrl: string;
  scopes: string;
  enabled: boolean;
};

function ProviderCard({
  provider,
  onConfigure,
  onTest,
  testing,
}: {
  provider: OAuthProvider;
  onConfigure: () => void;
  onTest: () => void;
  testing: boolean;
}) {
  const color = PROVIDER_COLORS[provider.id] ?? "var(--color-primary)";

  return (
    <div
      style={{
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-xl)",
        padding: "1.5rem",
        transition: "border-color 0.15s",
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
          marginBottom: "1rem",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
          <div
            style={{
              width: "48px",
              height: "48px",
              borderRadius: "var(--radius-lg)",
              background: `${color}20`,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "1.25rem",
              fontWeight: 700,
              color: color,
              border: `2px solid ${color}40`,
            }}
          >
            {PROVIDER_ICONS[provider.id] ?? provider.id[0].toUpperCase()}
          </div>
          <div>
            <div
              style={{
                fontSize: "1rem",
                fontWeight: 700,
                marginBottom: "0.25rem",
              }}
            >
              {provider.name}
            </div>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.375rem",
              }}
            >
              {provider.configured ? (
                <>
                  <CheckCircle
                    size={14}
                    style={{ color: "oklch(0.72 0.18 145)" }}
                  />
                  <span
                    style={{
                      fontSize: "0.75rem",
                      color: "oklch(0.72 0.18 145)",
                      fontWeight: 600,
                    }}
                  >
                    Configured
                  </span>
                </>
              ) : (
                <>
                  <AlertCircle
                    size={14}
                    style={{ color: "oklch(0.75 0.15 85)" }}
                  />
                  <span
                    style={{
                      fontSize: "0.75rem",
                      color: "oklch(0.75 0.15 85)",
                      fontWeight: 600,
                    }}
                  >
                    Not configured
                  </span>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <div style={{ display: "flex", gap: "0.5rem" }}>
        <button
          onClick={onConfigure}
          style={{
            ...btnStyle("secondary"),
            flex: 1,
            justifyContent: "center",
          }}
        >
          <Settings2 size={14} /> Configure
        </button>
        {provider.configured && (
          <button
            onClick={onTest}
            disabled={testing}
            style={{
              ...btnStyle("ghost"),
              opacity: testing ? 0.6 : 1,
            }}
          >
            {testing ? (
              <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} />
            ) : (
              <ExternalLink size={14} />
            )}
            Test
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Configure Modal ────────────────────────────────────────────────────────────

function ConfigureModal({
  provider,
  onClose,
  onSave,
}: {
  provider: OAuthProvider;
  onClose: () => void;
  onSave: (config: ProviderConfig) => void;
}) {
  const defaults = PROVIDER_DEFAULTS[provider.id] ?? {
    authUrl: "",
    tokenUrl: "",
    userInfoUrl: "",
    scopes: "openid email profile",
  };

  const [config, setConfig] = useState<ProviderConfig>(() => {
    const stored = localStorage.getItem(`oauth_${provider.id}`);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch {}
    }
    return {
      clientId: "",
      clientSecret: "",
      redirectUri: `${typeof window !== "undefined" ? window.location.origin : ""}/oauth/callback/${provider.id}`,
      authUrl: defaults.authUrl,
      tokenUrl: defaults.tokenUrl,
      userInfoUrl: defaults.userInfoUrl,
      scopes: defaults.scopes,
      enabled: false,
    };
  });
  const [showSecret, setShowSecret] = useState(false);

  function update(field: keyof ProviderConfig, value: string | boolean) {
    setConfig((prev) => ({ ...prev, [field]: value }));
  }

  function handleSave() {
    if (!config.clientId) {
      alert("Client ID is required");
      return;
    }
    localStorage.setItem(`oauth_${provider.id}`, JSON.stringify(config));
    onSave(config);
  }

  return (
    <Modal title={`Configure ${provider.name}`} onClose={onClose}>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "1rem",
          marginBottom: "1.5rem",
        }}
      >
        {/* Client ID */}
        <div>
          <label
            style={{
              display: "block",
              fontSize: "0.8rem",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            Client ID *
          </label>
          <input
            type="text"
            value={config.clientId}
            onChange={(e) => update("clientId", e.target.value)}
            placeholder={`Your ${provider.name} OAuth client ID`}
            style={{
              width: "100%",
              padding: "0.5rem 0.75rem",
              background: "var(--color-input)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-foreground)",
              fontSize: "0.875rem",
              outline: "none",
              fontFamily: "var(--font-mono)",
            }}
          />
        </div>

        {/* Client Secret */}
        <div>
          <label
            style={{
              display: "block",
              fontSize: "0.8rem",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            Client Secret
          </label>
          <div style={{ position: "relative" }}>
            <input
              type={showSecret ? "text" : "password"}
              value={config.clientSecret}
              onChange={(e) => update("clientSecret", e.target.value)}
              placeholder={`Your ${provider.name} OAuth client secret`}
              style={{
                width: "100%",
                padding: "0.5rem 0.75rem",
                background: "var(--color-input)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                color: "var(--color-foreground)",
                fontSize: "0.875rem",
                outline: "none",
                fontFamily: "var(--font-mono)",
                paddingRight: "2.5rem",
              }}
            />
            <button
              onClick={() => setShowSecret((s) => !s)}
              style={{
                position: "absolute",
                right: "0.5rem",
                top: "50%",
                transform: "translateY(-50%)",
                background: "none",
                border: "none",
                cursor: "pointer",
                color: "var(--color-muted-foreground)",
                padding: "4px",
              }}
            >
              {showSecret ? <X size={14} /> : <Settings2 size={14} />}
            </button>
          </div>
        </div>

        {/* Redirect URI */}
        <div>
          <label
            style={{
              display: "block",
              fontSize: "0.8rem",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            Redirect URI
          </label>
          <input
            type="text"
            value={config.redirectUri}
            onChange={(e) => update("redirectUri", e.target.value)}
            style={{
              width: "100%",
              padding: "0.5rem 0.75rem",
              background: "var(--color-input)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-foreground)",
              fontSize: "0.8rem",
              outline: "none",
              fontFamily: "var(--font-mono)",
            }}
          />
          <div
            style={{
              fontSize: "0.7rem",
              color: "var(--color-muted-foreground)",
              marginTop: "0.25rem",
            }}
          >
            Register this URI in your {provider.name} OAuth app settings
          </div>
        </div>

        {/* Auth URL */}
        <div>
          <label
            style={{
              display: "block",
              fontSize: "0.8rem",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            Authorization URL
          </label>
          <input
            type="text"
            value={config.authUrl}
            onChange={(e) => update("authUrl", e.target.value)}
            placeholder="https://..."
            style={{
              width: "100%",
              padding: "0.5rem 0.75rem",
              background: "var(--color-input)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-foreground)",
              fontSize: "0.8rem",
              outline: "none",
              fontFamily: "var(--font-mono)",
            }}
          />
        </div>

        {/* Token URL */}
        <div>
          <label
            style={{
              display: "block",
              fontSize: "0.8rem",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            Token URL
          </label>
          <input
            type="text"
            value={config.tokenUrl}
            onChange={(e) => update("tokenUrl", e.target.value)}
            placeholder="https://..."
            style={{
              width: "100%",
              padding: "0.5rem 0.75rem",
              background: "var(--color-input)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-foreground)",
              fontSize: "0.8rem",
              outline: "none",
              fontFamily: "var(--font-mono)",
            }}
          />
        </div>

        {/* User Info URL */}
        <div>
          <label
            style={{
              display: "block",
              fontSize: "0.8rem",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            User Info URL
          </label>
          <input
            type="text"
            value={config.userInfoUrl}
            onChange={(e) => update("userInfoUrl", e.target.value)}
            placeholder="https://..."
            style={{
              width: "100%",
              padding: "0.5rem 0.75rem",
              background: "var(--color-input)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-foreground)",
              fontSize: "0.8rem",
              outline: "none",
              fontFamily: "var(--font-mono)",
            }}
          />
        </div>

        {/* Scopes */}
        <div>
          <label
            style={{
              display: "block",
              fontSize: "0.8rem",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            Scopes
          </label>
          <input
            type="text"
            value={config.scopes}
            onChange={(e) => update("scopes", e.target.value)}
            placeholder="openid email profile"
            style={{
              width: "100%",
              padding: "0.5rem 0.75rem",
              background: "var(--color-input)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-foreground)",
              fontSize: "0.875rem",
              outline: "none",
            }}
          />
          <div
            style={{
              fontSize: "0.7rem",
              color: "var(--color-muted-foreground)",
              marginTop: "0.25rem",
            }}
          >
            Space-separated list of OAuth scopes
          </div>
        </div>

        {/* Enable Toggle */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "0.75rem",
            padding: "0.75rem",
            background: "var(--color-background)",
            borderRadius: "var(--radius-md)",
          }}
        >
          <label
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.75rem",
              cursor: "pointer",
              flex: 1,
            }}
          >
            <input
              type="checkbox"
              checked={config.enabled}
              onChange={(e) => update("enabled", e.target.checked)}
              style={{ width: "18px", height: "18px", cursor: "pointer" }}
            />
            <div>
              <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>
                Enable Provider
              </div>
              <div
                style={{
                  fontSize: "0.75rem",
                  color: "var(--color-muted-foreground)",
                }}
              >
                Allow users to sign in with {provider.name}
              </div>
            </div>
          </label>
        </div>
      </div>

      {/* Actions */}
      <div
        style={{
          display: "flex",
          gap: "0.75rem",
          justifyContent: "flex-end",
          paddingTop: "1rem",
          borderTop: "1px solid var(--color-border)",
        }}
      >
        <button onClick={onClose} style={btnStyle("secondary")}>
          Cancel
        </button>
        <button onClick={handleSave} style={btnStyle("primary")}>
          Save Configuration
        </button>
      </div>
    </Modal>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function AdminOAuthPage() {
  const router = useRouter();
  const [providers, setProviders] = useState<OAuthProvider[]>([]);
  const [loading, setLoading] = useState(true);
  const [configureProvider, setConfigureProvider] = useState<OAuthProvider | null>(null);
  const [testingProvider, setTestingProvider] = useState<string | null>(null);
  const [toast, setToast] = useState<{
    message: string;
    type: "success" | "error";
  } | null>(null);

  const token =
    typeof window !== "undefined"
      ? localStorage.getItem("printflow_token")
      : null;
  const user =
    typeof window !== "undefined"
      ? JSON.parse(localStorage.getItem("printflow_user") ?? "{}")
      : {};

  useEffect(() => {
    if (!token) {
      router.push("/login");
      return;
    }
    if (user.roles && !user.roles.includes("ADMIN")) {
      router.push("/");
      return;
    }
  }, [router, token, user]);

  useEffect(() => {
    oauthApi
      .providers()
      .then((res) => {
        setProviders(res.data?.providers ?? []);
      })
      .catch(() => {
        setProviders([]);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (toast) {
      const t = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(t);
    }
  }, [toast]);

  function showToast(msg: string, type: "success" | "error") {
    setToast({ message: msg, type });
  }

  function handleSaveConfig(config: ProviderConfig) {
    setConfigureProvider(null);
    setProviders((prev) =>
      prev.map((p) =>
        p.id === configureProvider?.id
          ? { ...p, configured: !!(config.clientId) }
          : p,
      ),
    );
    showToast(
      `${configureProvider?.name} configuration saved`,
      "success",
    );
  }

  function handleTest(provider: OAuthProvider) {
    const stored = localStorage.getItem(`oauth_${provider.id}`);
    if (!stored) {
      showToast(`${provider.name} is not configured`, "error");
      return;
    }

    setTestingProvider(provider.id);
    const config = JSON.parse(stored) as ProviderConfig;

    if (!config.authUrl || !config.clientId) {
      showToast(`${provider.name} is not properly configured`, "error");
      setTestingProvider(null);
      return;
    }

    // Open OAuth flow in popup
    const params = new URLSearchParams({
      client_id: config.clientId,
      redirect_uri: config.redirectUri,
      response_type: "code",
      scope: config.scopes,
      state: Math.random().toString(36).substring(7),
    });

    const authUrl = `${config.authUrl}${config.authUrl.includes("?") ? "&" : "?"}${params}`;
    const popup = window.open(
      authUrl,
      `oauth_${provider.id}`,
      "width=600,height=700,scrollbars=yes",
    );

    if (popup) {
      const checkClosed = setInterval(() => {
        if (popup.closed) {
          clearInterval(checkClosed);
          setTestingProvider(null);
          showToast(
            "OAuth flow completed. Check the result in the popup.",
            "info",
          );
        }
      }, 500);
    } else {
      showToast("Popup blocked. Allow popups for this site.", "error");
      setTestingProvider(null);
    }
  }

  return (
    <div>
      {/* Header */}
      <div style={{ marginBottom: "1.5rem" }}>
        <h1
          style={{
            fontSize: "1.5rem",
            fontWeight: 700,
            letterSpacing: "-0.02em",
          }}
        >
          OAuth Providers
        </h1>
        <p
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.875rem",
            marginTop: "0.25rem",
          }}
        >
          Configure third-party authentication providers for SSO
        </p>
      </div>

      {/* OAuth Flow Info */}
      <div
        style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-lg)",
          padding: "1.25rem",
          marginBottom: "1.5rem",
        }}
      >
        <h3
          style={{
            fontSize: "0.875rem",
            fontWeight: 700,
            marginBottom: "0.75rem",
          }}
        >
          How OAuth Works
        </h3>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
            gap: "1rem",
          }}
        >
          {[
            {
              step: "1",
              title: "Register App",
              desc: `Create an OAuth app in your provider's developer console`,
            },
            {
              step: "2",
              title: "Configure",
              desc: "Enter Client ID, Secret, and URLs in the form above",
            },
            {
              step: "3",
              title: "Set Redirect URI",
              desc: "Register the redirect URI in your provider's app settings",
            },
            {
              step: "4",
              title: "Test & Enable",
              desc: "Test the connection and enable the provider",
            },
          ].map((item) => (
            <div
              key={item.step}
              style={{
                display: "flex",
                gap: "0.75rem",
                alignItems: "flex-start",
              }}
            >
              <div
                style={{
                  width: "24px",
                  height: "24px",
                  borderRadius: "50%",
                  background: "var(--color-primary)",
                  color: "var(--color-primary-foreground)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "0.75rem",
                  fontWeight: 700,
                  flexShrink: 0,
                }}
              >
                {item.step}
              </div>
              <div>
                <div
                  style={{
                    fontSize: "0.8rem",
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                  }}
                >
                  {item.title}
                </div>
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "var(--color-muted-foreground)",
                  }}
                >
                  {item.desc}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Provider Cards */}
      {loading ? (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
            gap: "1rem",
          }}
        >
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-xl)",
                padding: "1.5rem",
                height: "150px",
                animation: "pulse 1.5s infinite",
              }}
            />
          ))}
        </div>
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
            gap: "1rem",
          }}
        >
          {providers.map((provider) => (
            <ProviderCard
              key={provider.id}
              provider={provider}
              onConfigure={() => setConfigureProvider(provider)}
              onTest={() => handleTest(provider)}
              testing={testingProvider === provider.id}
            />
          ))}
        </div>
      )}

      {/* Callback URL Info */}
      <div
        style={{
          marginTop: "1.5rem",
          padding: "1rem 1.25rem",
          background: "oklch(0.72 0.18 250 / 0.06)",
          border: "1px solid oklch(0.72 0.18 250 / 0.2)",
          borderRadius: "var(--radius-lg)",
        }}
      >
        <div
          style={{
            fontSize: "0.8rem",
            fontWeight: 600,
            marginBottom: "0.5rem",
            color: "oklch(0.72 0.18 250)",
          }}
        >
          OAuth Callback URL
        </div>
        <div
          style={{
            fontFamily: "var(--font-mono)",
            fontSize: "0.8rem",
            color: "var(--color-muted-foreground)",
          }}
        >
          {typeof window !== "undefined"
            ? `${window.location.origin}/oauth/callback/{provider}`
            : "https://your-domain.com/oauth/callback/{provider}"}
        </div>
        <div
          style={{
            fontSize: "0.75rem",
            color: "var(--color-muted-foreground)",
            marginTop: "0.5rem",
          }}
        >
          Register this callback URL in your OAuth provider's app settings for
          each provider you configure.
        </div>
      </div>

      {/* Configure Modal */}
      {configureProvider && (
        <ConfigureModal
          provider={configureProvider}
          onClose={() => setConfigureProvider(null)}
          onSave={handleSaveConfig}
        />
      )}

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
