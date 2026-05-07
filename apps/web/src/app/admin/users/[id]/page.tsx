"use client";

import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, AlertCircle, CheckCircle, Loader2, Shield, CreditCard, CreditCardIcon } from "lucide-react";
import { usersApi, type UserDetail } from "@/lib/api";
import { userRoles } from "@printflow/common";

const ALL_ROLES = userRoles.filter(r => r !== "PRINT_SITE_USER" && r !== "MAIL_TICKET_ISSUER" && r !== "JOB_TICKET_ISSUER");

const STATUS_OPTIONS = ["ACTIVE", "BLOCKED", "DELETED", "EXPIRED"] as const;

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  ACTIVE: { bg: "oklch(0.72 0.18 145 / 0.12)", text: "oklch(0.72 0.18 145)" },
  BLOCKED: { bg: "oklch(0.65 0.22 25 / 0.12)", text: "oklch(0.65 0.22 25)" },
  DELETED: { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)" },
  EXPIRED: { bg: "oklch(0.55 0.02 250 / 0.12)", text: "oklch(0.55 0.02 250)" },
};
const DEFAULT_COLOR = { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)" };

export default function AdminEditUserPage() {
  const router = useRouter();
  const params = useParams();
  const userId = parseInt(params.id as string, 10);

  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => { if (!token) router.push("/login"); }, [router, token]);

  const [user, setUser] = useState<UserDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    roles: [] as string[],
    status: "ACTIVE",
    printQuota: "0",
    blockedReason: "",
  });
  const [activeTab, setActiveTab] = useState<"details" | "cards" | "groups">("details");
  const [newCardId, setNewCardId] = useState("");
  const [addingCard, setAddingCard] = useState(false);

  useEffect(() => {
    if (!userId || isNaN(userId)) { router.push("/admin/users"); return; }
    usersApi.get(userId)
      .then((u) => {
        setUser(u);
        setForm({
          fullName: u.fullName,
          email: u.email ?? "",
          roles: u.roles as string[],
          status: u.status,
          printQuota: u.printQuota ?? "0",
          blockedReason: u.blockedReason ?? "",
        });
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [userId, router]);

  function update(field: string, value: string | string[]) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function toggleRole(role: string) {
    setForm((f) => ({
      ...f,
      roles: f.roles.includes(role)
        ? f.roles.filter((r) => r !== role)
        : [...f.roles, role],
    }));
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setSaving(true);
    try {
      await usersApi.update(userId, {
        fullName: form.fullName,
        email: form.email || undefined,
        roles: form.roles,
        status: form.status as any,
        printQuota: form.printQuota,
        blockedReason: form.blockedReason || undefined,
      });
      setSuccess("User updated successfully!");
      setTimeout(() => setSuccess(""), 3000);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAddCard() {
    if (!newCardId.trim()) return;
    setAddingCard(true);
    setError("");
    try {
      await usersApi.addCard(userId, { cardId: newCardId.trim() });
      const updated = await usersApi.get(userId);
      setUser(updated);
      setNewCardId("");
    } catch (e: any) {
      setError(e.message);
    } finally {
      setAddingCard(false);
    }
  }

  if (loading) {
    return (
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", padding: "4rem" }}>
        <Loader2 size={24} style={{ color: "var(--color-muted-foreground)", animation: "spin 1s linear infinite" }} />
      </div>
    );
  }

  if (!user) {
    return (
      <div style={{ textAlign: "center", padding: "3rem" }}>
        <AlertCircle size={40} style={{ color: "oklch(0.65 0.22 25)", margin: "0 auto 1rem" }} />
        <p style={{ color: "var(--color-muted-foreground)" }}>User not found</p>
      </div>
    );
  }

  const sc = STATUS_COLORS[user.status] ?? DEFAULT_COLOR;

  return (
    <div>
      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1.5rem" }}>
        <Link href="/admin/users" style={{
          display: "inline-flex", alignItems: "center",
          padding: "0.375rem 0.75rem",
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-md)",
          color: "var(--color-muted-foreground)",
          textDecoration: "none",
          fontSize: "0.85rem",
        }}>
          <ArrowLeft size={16} />
        </Link>
        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", flex: 1 }}>
          <div style={{
            width: "40px", height: "40px", borderRadius: "50%",
            background: "oklch(0.72 0.18 250 / 0.2)",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: "1rem", fontWeight: 700, color: "var(--color-primary)",
          }}>
            {user.userName[0]?.toUpperCase()}
          </div>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
              <h1 style={{ fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em" }}>{user.fullName}</h1>
              <span style={{
                padding: "2px 8px",
                borderRadius: "999px",
                fontSize: "0.7rem",
                fontWeight: 600,
                background: sc.bg,
                color: sc.text,
              }}>
                {user.status}
              </span>
            </div>
            <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.8rem" }}>@{user.userName} · ID #{user.id}</p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div style={{
        display: "flex",
        gap: "0",
        borderBottom: "1px solid var(--color-border)",
        marginBottom: "1.5rem",
      }}>
        {(["details", "cards", "groups"] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            style={{
              padding: "0.625rem 1.25rem",
              background: "none",
              border: "none",
              borderBottom: activeTab === tab ? "2px solid var(--color-primary)" : "2px solid transparent",
              color: activeTab === tab ? "var(--color-primary)" : "var(--color-muted-foreground)",
              fontWeight: activeTab === tab ? 600 : 400,
              fontSize: "0.875rem",
              cursor: "pointer",
              transition: "color 0.15s",
              textTransform: "capitalize",
            }}
          >
            {tab === "cards" ? `Cards (${user.cards.length})` : tab === "groups" ? `Groups (${user.groups.length})` : "Details"}
          </button>
        ))}
      </div>

      {error && (
        <div style={alertStyle("error", error)}>
          <AlertCircle size={16} /> {error}
        </div>
      )}
      {success && (
        <div style={alertStyle("success", success)}>
          <CheckCircle size={16} /> {success}
        </div>
      )}

      {activeTab === "details" && (
        <form onSubmit={handleSave}>
          <div style={{ display: "grid", gap: "1.5rem", gridTemplateColumns: "repeat(auto-fill, minmax(480px, 1fr))" }}>
            {/* Main Info */}
            <div style={cardStyle()}>
              <h3 style={{ fontSize: "0.9rem", fontWeight: 700, marginBottom: "1.25rem" }}>Account Information</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                <FieldRow label="Username">
                  <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.875rem" }}>@{user.userName}</span>
                </FieldRow>
                <FieldRow label="Full Name" required>
                  <input
                    type="text"
                    value={form.fullName}
                    onChange={(e) => update("fullName", e.target.value)}
                    required
                    style={inputStyle()}
                  />
                </FieldRow>
                <FieldRow label="Email">
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) => update("email", e.target.value)}
                    placeholder="No email set"
                    style={inputStyle()}
                  />
                </FieldRow>
                <FieldRow label="Status">
                  <select
                    value={form.status}
                    onChange={(e) => update("status", e.target.value)}
                    style={{ ...inputStyle(), cursor: "pointer" }}
                  >
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </FieldRow>
                {form.status === "BLOCKED" && (
                  <FieldRow label="Blocked Reason">
                    <input
                      type="text"
                      value={form.blockedReason}
                      onChange={(e) => update("blockedReason", e.target.value)}
                      placeholder="Reason for blocking..."
                      style={inputStyle()}
                    />
                  </FieldRow>
                )}
              </div>
            </div>

            {/* Roles & Quota */}
            <div style={cardStyle()}>
              <h3 style={{ fontSize: "0.9rem", fontWeight: 700, marginBottom: "1.25rem" }}>Roles & Quota</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                <FieldRow label="Roles">
                  <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
                    {ALL_ROLES.map((role) => {
                      const selected = form.roles.includes(role);
                      return (
                        <button
                          key={role}
                          type="button"
                          onClick={() => toggleRole(role)}
                          style={{
                            padding: "0.375rem 0.875rem",
                            background: selected ? "oklch(0.72 0.18 250 / 0.15)" : "var(--color-input)",
                            border: `1px solid ${selected ? "oklch(0.72 0.18 250)" : "var(--color-border)"}`,
                            borderRadius: "999px",
                            color: selected ? "var(--color-primary)" : "var(--color-muted-foreground)",
                            fontSize: "0.8rem",
                            fontWeight: selected ? 600 : 400,
                            cursor: "pointer",
                          }}
                        >
                          {role}
                        </button>
                      );
                    })}
                  </div>
                </FieldRow>
                <FieldRow label="Print Quota (credits)">
                  <input
                    type="number"
                    value={form.printQuota}
                    onChange={(e) => update("printQuota", e.target.value)}
                    min="0"
                    step="0.01"
                    style={inputStyle()}
                  />
                </FieldRow>
                <FieldRow label="Current Balance">
                  <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.875rem" }}>
                    {Number(user.printBalance ?? 0).toFixed(4)} credits
                  </span>
                </FieldRow>
                <FieldRow label="2FA Enabled">
                  <span style={{ fontSize: "0.875rem", color: user.totpEnabled ? "oklch(0.72 0.18 145)" : "var(--color-muted-foreground)" }}>
                    {user.totpEnabled ? "Yes" : "No"}
                  </span>
                </FieldRow>
                <FieldRow label="User Since">
                  <span style={{ fontSize: "0.875rem", color: "var(--color-muted-foreground)" }}>
                    {new Date(user.dateCreated).toLocaleDateString()} {new Date(user.dateCreated).toLocaleTimeString()}
                  </span>
                </FieldRow>
              </div>

              <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end", paddingTop: "1rem", marginTop: "0.5rem", borderTop: "1px solid var(--color-border)" }}>
                <Link href="/admin/users" style={{
                  padding: "0.5rem 1rem",
                  background: "var(--color-secondary)",
                  color: "var(--color-secondary-foreground)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  textDecoration: "none",
                  fontSize: "0.875rem",
                  fontWeight: 600,
                }}>
                  Cancel
                </Link>
                <button
                  type="submit"
                  disabled={saving}
                  style={{
                    padding: "0.5rem 1.5rem",
                    background: saving ? "oklch(0.72 0.18 250 / 0.5)" : "var(--color-primary)",
                    color: "var(--color-primary-foreground)",
                    border: "none",
                    borderRadius: "var(--radius-md)",
                    fontSize: "0.875rem",
                    fontWeight: 600,
                    cursor: saving ? "not-allowed" : "pointer",
                    display: "inline-flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  {saving && <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} />}
                  Save Changes
                </button>
              </div>
            </div>
          </div>
        </form>
      )}

      {activeTab === "cards" && (
        <div style={cardStyle()}>
          <h3 style={{ fontSize: "0.9rem", fontWeight: 700, marginBottom: "1.25rem" }}>Access Cards</h3>
          <div style={{ display: "flex", gap: "0.75rem", marginBottom: "1.25rem" }}>
            <input
              type="text"
              value={newCardId}
              onChange={(e) => setNewCardId(e.target.value)}
              placeholder="Scan or enter card ID..."
              style={{ ...inputStyle(), flex: 1 }}
              onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleAddCard(); } }}
            />
            <button
              onClick={handleAddCard}
              disabled={addingCard || !newCardId.trim()}
              style={{
                padding: "0.5rem 1.25rem",
                background: "var(--color-primary)",
                color: "var(--color-primary-foreground)",
                border: "none",
                borderRadius: "var(--radius-md)",
                fontSize: "0.875rem",
                fontWeight: 600,
                cursor: addingCard || !newCardId.trim() ? "not-allowed" : "pointer",
                display: "inline-flex",
                alignItems: "center",
                gap: "0.5rem",
              }}
            >
              {addingCard ? <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} /> : "+ Add Card"}
            </button>
          </div>
          {user.cards.length === 0 ? (
            <div style={{ textAlign: "center", padding: "2rem", color: "var(--color-muted-foreground)", fontSize: "0.875rem" }}>
              No access cards registered
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
              {user.cards.map((card) => (
                <div
                  key={card.id}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.75rem",
                    padding: "0.75rem 1rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                  }}
                >
                  <CreditCard size={16} style={{ color: "var(--color-muted-foreground)" }} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.875rem", fontWeight: 600 }}>{card.cardId}</div>
                    <div style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)" }}>
                      {card.cardType} {card.cardName ? `· ${card.cardName}` : ""}
                    </div>
                  </div>
                  <span style={{
                    padding: "2px 8px",
                    borderRadius: "999px",
                    fontSize: "0.7rem",
                    fontWeight: 600,
                    background: card.isActive ? "oklch(0.72 0.18 145 / 0.12)" : "oklch(0.55 0.05 250 / 0.12)",
                    color: card.isActive ? "oklch(0.72 0.18 145)" : "oklch(0.55 0.05 250)",
                  }}>
                    {card.isActive ? "Active" : "Inactive"}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === "groups" && (
        <div style={cardStyle()}>
          <h3 style={{ fontSize: "0.9rem", fontWeight: 700, marginBottom: "1.25rem" }}>User Groups</h3>
          {user.groups.length === 0 ? (
            <div style={{ textAlign: "center", padding: "2rem", color: "var(--color-muted-foreground)", fontSize: "0.875rem" }}>
              Not a member of any groups
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
              {user.groups.map((group) => (
                <div
                  key={group.id}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.75rem",
                    padding: "0.75rem 1rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                  }}
                >
                  <Shield size={16} style={{ color: "var(--color-muted-foreground)" }} />
                  <div style={{ fontWeight: 600, fontSize: "0.875rem" }}>{group.name}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function FieldRow({ label, required, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <div>
      <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>
        {label} {required && <span style={{ color: "oklch(0.65 0.22 25)" }}>*</span>}
      </label>
      {children}
    </div>
  );
}

function cardStyle(): React.CSSProperties {
  return {
    background: "var(--color-card)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-xl)",
    padding: "1.5rem",
  };
}

function inputStyle(): React.CSSProperties {
  return {
    width: "100%",
    padding: "0.5rem 0.75rem",
    background: "var(--color-input)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    color: "var(--color-foreground)",
    fontSize: "0.875rem",
    outline: "none",
  };
}

function alertStyle(type: "error" | "success", message: string): React.CSSProperties {
  const isError = type === "error";
  return {
    padding: "0.75rem 1rem",
    background: isError ? "oklch(0.65 0.22 25 / 0.1)" : "oklch(0.72 0.18 145 / 0.1)",
    border: `1px solid ${isError ? "oklch(0.65 0.22 25 / 0.3)" : "oklch(0.72 0.18 145 / 0.3)"}`,
    borderRadius: "var(--radius-md)",
    color: isError ? "oklch(0.65 0.22 25)" : "oklch(0.72 0.18 145)",
    fontSize: "0.875rem",
    marginBottom: "1rem",
    display: "flex",
    alignItems: "center",
    gap: "0.5rem",
  };
}
