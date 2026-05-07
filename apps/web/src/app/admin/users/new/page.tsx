"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, AlertCircle, CheckCircle, Loader2 } from "lucide-react";
import { usersApi, type UserSummary } from "@/lib/api";
import { userRoles } from "@printflow/common";

const ALL_ROLES = userRoles.filter(r => r !== "PRINT_SITE_USER" && r !== "MAIL_TICKET_ISSUER" && r !== "JOB_TICKET_ISSUER");

export default function AdminNewUserPage() {
  const router = useRouter();

  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => { if (!token) router.push("/login"); }, [router, token]);

  const [form, setForm] = useState({
    userName: "",
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    roles: ["USER"] as string[],
    printQuota: "100",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

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

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    if (form.password && form.password.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }
    if (!form.fullName.trim()) {
      setError("Full name is required");
      return;
    }

    setLoading(true);
    try {
      const data = await usersApi.create({
        userName: form.userName.trim(),
        fullName: form.fullName.trim(),
        email: form.email.trim() || undefined,
        password: form.password || undefined,
        roles: form.roles,
        printQuota: form.printQuota,
      });
      setSuccess(`User "${data.userName}" created successfully!`);
      setTimeout(() => router.push("/admin/users"), 1500);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1.5rem" }}>
        <Link href="/admin/users" style={{
          display: "inline-flex", alignItems: "center", gap: "0.375rem",
          padding: "0.375rem 0.75rem",
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-md)",
          color: "var(--color-muted-foreground)",
          textDecoration: "none",
          fontSize: "0.85rem",
          transition: "border-color 0.15s",
        }}>
          <ArrowLeft size={16} />
        </Link>
        <div>
          <h1 style={{ fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em" }}>Add New User</h1>
          <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem", marginTop: "0.25rem" }}>
            Create a new system user account
          </p>
        </div>
      </div>

      <div style={{ display: "grid", gap: "1.5rem", gridTemplateColumns: "repeat(auto-fill, minmax(480px, 1fr))" }}>
        {/* Form Card */}
        <div style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-xl)",
          padding: "1.5rem",
        }}>
          {success && (
            <div style={{
              padding: "0.75rem 1rem",
              background: "oklch(0.72 0.18 145 / 0.1)",
              border: "1px solid oklch(0.72 0.18 145 / 0.3)",
              borderRadius: "var(--radius-md)",
              color: "oklch(0.72 0.18 145)",
              fontSize: "0.875rem",
              marginBottom: "1rem",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
            }}>
              <CheckCircle size={16} /> {success}
            </div>
          )}

          {error && (
            <div style={{
              padding: "0.75rem 1rem",
              background: "oklch(0.65 0.22 25 / 0.1)",
              border: "1px solid oklch(0.65 0.22 25 / 0.3)",
              borderRadius: "var(--radius-md)",
              color: "oklch(0.65 0.22 25)",
              fontSize: "0.875rem",
              marginBottom: "1rem",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
            }}>
              <AlertCircle size={16} /> {error}
            </div>
          )}

          <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
            <div style={{ display: "grid", gap: "1rem", gridTemplateColumns: "1fr 1fr" }}>
              <Field label="Username" required>
                <input
                  type="text"
                  value={form.userName}
                  onChange={(e) => update("userName", e.target.value)}
                  placeholder="e.g. john.doe"
                  required
                  minLength={3}
                  maxLength={50}
                  style={inputStyle()}
                />
              </Field>
              <Field label="Full Name" required>
                <input
                  type="text"
                  value={form.fullName}
                  onChange={(e) => update("fullName", e.target.value)}
                  placeholder="e.g. John Doe"
                  required
                  style={inputStyle()}
                />
              </Field>
            </div>

            <Field label="Email">
              <input
                type="email"
                value={form.email}
                onChange={(e) => update("email", e.target.value)}
                placeholder="john.doe@company.com"
                style={inputStyle()}
              />
            </Field>

            <div style={{ display: "grid", gap: "1rem", gridTemplateColumns: "1fr 1fr" }}>
              <Field label="Password">
                <input
                  type="password"
                  value={form.password}
                  onChange={(e) => update("password", e.target.value)}
                  placeholder="Leave blank for no password"
                  minLength={8}
                  style={inputStyle()}
                />
              </Field>
              <Field label="Confirm Password">
                <input
                  type="password"
                  value={form.confirmPassword}
                  onChange={(e) => update("confirmPassword", e.target.value)}
                  placeholder="Confirm password"
                  style={inputStyle()}
                />
              </Field>
            </div>

            <Field label="Print Quota (credits)">
              <input
                type="number"
                value={form.printQuota}
                onChange={(e) => update("printQuota", e.target.value)}
                placeholder="100"
                min="0"
                step="0.01"
                style={inputStyle()}
              />
            </Field>

            <div>
              <div style={{ fontSize: "0.875rem", fontWeight: 500, marginBottom: "0.5rem", display: "flex", alignItems: "center", gap: "0.25rem" }}>
                Roles
                <span style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)", fontWeight: 400 }}>(select one or more)</span>
              </div>
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
                        transition: "all 0.15s",
                      }}
                    >
                      {role}
                    </button>
                  );
                })}
              </div>
            </div>

            <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end", paddingTop: "0.5rem", borderTop: "1px solid var(--color-border)" }}>
              <Link href="/admin/users" style={{
                padding: "0.625rem 1.25rem",
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
                disabled={loading}
                style={{
                  padding: "0.625rem 1.5rem",
                  background: loading ? "oklch(0.72 0.18 250 / 0.5)" : "var(--color-primary)",
                  color: "var(--color-primary-foreground)",
                  border: "none",
                  borderRadius: "var(--radius-md)",
                  fontSize: "0.875rem",
                  fontWeight: 600,
                  cursor: loading ? "not-allowed" : "pointer",
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "0.5rem",
                }}
              >
                {loading && <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} />}
                Create User
              </button>
            </div>
          </form>
        </div>

        {/* Help Card */}
        <div style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-xl)",
          padding: "1.5rem",
          alignSelf: "start",
        }}>
          <h3 style={{ fontSize: "0.9rem", fontWeight: 700, marginBottom: "1rem" }}>Role Permissions</h3>
          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
            {[
              { role: "ADMIN", desc: "Full system access, manage all users and settings" },
              { role: "MANAGER", desc: "Manage users and groups, view reports and logs" },
              { role: "DELEGATOR", desc: "Print on behalf of other users" },
              { role: "USER", desc: "Upload documents, release own print jobs" },
              { role: "PGP_USER", desc: "Sign and encrypt PDFs with PGP" },
            ].map(({ role, desc }) => (
              <div key={role} style={{ display: "flex", gap: "0.625rem", alignItems: "flex-start" }}>
                <span style={{
                  fontSize: "0.65rem", fontWeight: 700,
                  padding: "2px 6px",
                  background: "oklch(0.72 0.18 250 / 0.1)",
                  color: "var(--color-primary)",
                  borderRadius: "4px",
                  whiteSpace: "nowrap",
                  flexShrink: 0,
                }}>
                  {role}
                </span>
                <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>{desc}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function Field({ label, required, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <div>
      <label style={{ display: "block", fontSize: "0.875rem", fontWeight: 500, marginBottom: "0.375rem" }}>
        {label} {required && <span style={{ color: "oklch(0.65 0.22 25)" }}>*</span>}
      </label>
      {children}
    </div>
  );
}

function inputStyle(): React.CSSProperties {
  return {
    width: "100%",
    padding: "0.625rem 0.875rem",
    background: "var(--color-input)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    color: "var(--color-foreground)",
    fontSize: "0.875rem",
    outline: "none",
    transition: "border-color 0.15s, box-shadow 0.15s",
  };
}
