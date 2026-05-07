"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Settings, Bell, Shield, Database, Globe } from "lucide-react";

export default function AdminSettingsPage() {
  const router = useRouter();

  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => { if (!token) router.push("/login"); }, [router, token]);

  return (
    <div>
      <div style={{ marginBottom: "1.5rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em" }}>Settings</h1>
        <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem", marginTop: "0.25rem" }}>
          System configuration and preferences
        </p>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "1rem" }}>
        {[
          { icon: Globe, label: "General", desc: "App name, logo, timezone, language", ready: false },
          { icon: Bell, label: "Notifications", desc: "Email, Telegram, webhook settings", ready: false },
          { icon: Shield, label: "Security", desc: "Password policy, 2FA, session timeout", ready: false },
          { icon: Database, label: "Storage", desc: "Document retention, file limits, cleanup", ready: false },
          { icon: Settings, label: "Print Defaults", desc: "Default costs, paper sizes, color mode", ready: false },
          { icon: Globe, label: "IPP Server", desc: "IPP port, CUPS connection, discovery", ready: false },
        ].map(({ icon: Icon, label, desc, ready }) => (
          <div key={label} className="card-glow" style={{ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-xl)", padding: "1.5rem", cursor: ready ? "pointer" : "not-allowed", opacity: ready ? 1 : 0.7, transition: "opacity 0.15s" }}>
            <div style={{ width: "40px", height: "40px", borderRadius: "var(--radius-md)", background: "oklch(0.72 0.18 250 / 0.12)", display: "flex", alignItems: "center", justifyContent: "center", marginBottom: "1rem" }}>
              <Icon size={20} style={{ color: "var(--color-primary)" }} />
            </div>
            <div style={{ fontWeight: 700, fontSize: "0.95rem", marginBottom: "0.375rem" }}>{label}</div>
            <div style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>{desc}</div>
            {!ready && <div style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)", marginTop: "0.75rem", fontStyle: "italic" }}>Coming soon</div>}
          </div>
        ))}
      </div>
    </div>
  );
}
