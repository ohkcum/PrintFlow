"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { LanguageSelector } from "@/components/LanguageSelector";

const API_URL = process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001";

export default function LoginPage() {
  const router = useRouter();
  const [form, setForm] = useState({ userName: "", password: "", totpToken: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [needsTotp, setNeedsTotp] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await fetch(`${API_URL}/api/v1/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });

      const data = await res.json();

      if (!data.success) {
        if (data.error?.code === "TOTP_REQUIRED") {
          setNeedsTotp(true);
          setLoading(false);
          return;
        }
        setError(data.error?.message ?? "Login failed");
        setLoading(false);
        return;
      }

      // Store token
      localStorage.setItem("printflow_token", data.data.token);
      localStorage.setItem("printflow_user", JSON.stringify(data.data.user));
      router.push("/user");
    } catch (err) {
      setError("Cannot connect to server");
      setLoading(false);
    }
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "1.5rem",
        background: "radial-gradient(ellipse at top, oklch(0.22 0.03 250) 0%, var(--color-background) 60%)",
      }}
    >
      {/* Logo */}
      <div style={{ textAlign: "center", marginBottom: "2rem" }}>
        <div
          style={{
            display: "inline-flex",
            alignItems: "center",
            justifyContent: "center",
            width: "56px",
            height: "56px",
            borderRadius: "14px",
            background: "linear-gradient(135deg, oklch(0.72 0.18 250), oklch(0.55 0.22 280))",
            boxShadow: "0 8px 32px oklch(0.72 0.18 250 / 0.3)",
            marginBottom: "1rem",
          }}
        >
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
            <rect x="6" y="4" width="20" height="14" rx="2" stroke="white" strokeWidth="2" />
            <rect x="6" y="20" width="20" height="8" rx="2" fill="white" fillOpacity="0.9" />
            <circle cx="22" cy="9" r="2" fill="white" />
          </svg>
        </div>
        <h1 style={{ fontSize: "1.75rem", fontWeight: 700, color: "var(--color-foreground)", letterSpacing: "-0.02em" }}>
          PrintFlow
        </h1>
        <p style={{ color: "var(--color-muted-foreground)", marginTop: "0.25rem", fontSize: "0.9rem" }}>
          Secure Pull Printing Portal
        </p>
      </div>

      {/* Login Card */}
      <div
        className="glass animate-fade-in"
        style={{
          width: "100%",
          maxWidth: "420px",
          borderRadius: "var(--radius-xl)",
          padding: "2rem",
        }}
      >
        <h2
          style={{
            fontSize: "1.25rem",
            fontWeight: 600,
            marginBottom: "1.5rem",
            color: "var(--color-foreground)",
          }}
        >
          Sign in to your account
        </h2>

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <div>
            <label
              htmlFor="userName"
              style={{
                display: "block",
                fontSize: "0.875rem",
                fontWeight: 500,
                color: "var(--color-foreground)",
                marginBottom: "0.375rem",
              }}
            >
              Username
            </label>
            <input
              id="userName"
              type="text"
              autoComplete="username"
              value={form.userName}
              onChange={(e) => setForm((f) => ({ ...f, userName: e.target.value }))}
              placeholder="Enter your username"
              required
              style={{
                width: "100%",
                padding: "0.625rem 0.875rem",
                background: "var(--color-input)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                color: "var(--color-foreground)",
                fontSize: "0.9rem",
              }}
            />
          </div>

          <div>
            <label
              htmlFor="password"
              style={{
                display: "block",
                fontSize: "0.875rem",
                fontWeight: 500,
                color: "var(--color-foreground)",
                marginBottom: "0.375rem",
              }}
            >
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
              placeholder="Enter your password"
              required
              style={{
                width: "100%",
                padding: "0.625rem 0.875rem",
                background: "var(--color-input)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                color: "var(--color-foreground)",
                fontSize: "0.9rem",
              }}
            />
          </div>

          {needsTotp && (
            <div className="animate-fade-in">
              <label
                htmlFor="totpToken"
                style={{
                  display: "block",
                  fontSize: "0.875rem",
                  fontWeight: 500,
                  color: "var(--color-foreground)",
                  marginBottom: "0.375rem",
                }}
              >
                Two-Factor Code
              </label>
              <input
                id="totpToken"
                type="text"
                inputMode="numeric"
                pattern="[0-9]{6}"
                maxLength={6}
                autoComplete="one-time-code"
                value={form.totpToken}
                onChange={(e) => setForm((f) => ({ ...f, totpToken: e.target.value }))}
                placeholder="000000"
                style={{
                  width: "100%",
                  padding: "0.625rem 0.875rem",
                  background: "var(--color-input)",
                  border: "1px solid oklch(0.72 0.18 250 / 0.5)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                fontSize: "1.25rem",
                fontFamily: "var(--font-mono)",
                letterSpacing: "0.25em",
                textAlign: "center",
              }}
            />
              <p style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)", marginTop: "0.375rem" }}>
                Enter the 6-digit code from your authenticator app
              </p>
            </div>
          )}

          {error && (
            <div
              style={{
                padding: "0.75rem",
                background: "oklch(0.65 0.22 25 / 0.1)",
                border: "1px solid oklch(0.65 0.22 25 / 0.3)",
                borderRadius: "var(--radius-md)",
                color: "oklch(0.65 0.22 25)",
                fontSize: "0.875rem",
              }}
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            style={{
              width: "100%",
              padding: "0.75rem",
              background: loading ? "oklch(0.72 0.18 250 / 0.5)" : "var(--color-primary)",
              color: "var(--color-primary-foreground)",
              border: "none",
              borderRadius: "var(--radius-md)",
              fontSize: "0.9rem",
              fontWeight: 600,
              cursor: loading ? "not-allowed" : "pointer",
              marginTop: "0.25rem",
            }}
          >
            {loading ? "Signing in..." : needsTotp ? "Verify & Sign In" : "Sign In"}
          </button>
        </form>

        <div style={{ marginTop: "1.5rem", textAlign: "center" }}>
          <p style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>
            Default: <code style={{ color: "var(--color-accent-foreground)", fontFamily: "var(--font-mono)" }}>admin</code> / <code style={{ color: "var(--color-accent-foreground)", fontFamily: "var(--font-mono)" }}>admin123</code>
          </p>
        </div>

        {/* Language Selector */}
        <div style={{
          marginTop: "1.5rem",
          paddingTop: "1rem",
          borderTop: "1px solid var(--color-border)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          gap: "0.5rem"
        }}>
          <span style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)" }}>Language:</span>
          <LanguageSelector />
        </div>
      </div>
    </div>
  );
}
