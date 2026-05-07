"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  Plus,
  Search,
  ChevronLeft,
  ChevronRight,
  MoreHorizontal,
  Edit2,
  Trash2,
  Shield,
  ShieldCheck,
  X,
  CheckCircle,
  AlertCircle,
  Clock,
  UserX,
} from "lucide-react";
import { usersApi, type UserSummary } from "@/lib/api";
import { SkeletonTable } from "@/components/ui/Skeleton";

const PAGE_SIZE = 20;

const STATUS_COLORS: Record<
  string,
  { bg: string; text: string; label: string }
> = {
  ACTIVE: {
    bg: "oklch(0.72 0.18 145 / 0.12)",
    text: "oklch(0.72 0.18 145)",
    label: "Active",
  },
  BLOCKED: {
    bg: "oklch(0.65 0.22 25 / 0.12)",
    text: "oklch(0.65 0.22 25)",
    label: "Blocked",
  },
  DELETED: {
    bg: "oklch(0.55 0.05 250 / 0.12)",
    text: "oklch(0.55 0.05 250)",
    label: "Deleted",
  },
  EXPIRED: {
    bg: "oklch(0.55 0.02 250 / 0.12)",
    text: "oklch(0.55 0.02 250)",
    label: "Expired",
  },
};
const DEFAULT_STATUS_COLOR = {
  bg: "oklch(0.55 0.05 250 / 0.12)",
  text: "oklch(0.55 0.05 250)",
  label: "Unknown",
};

const ROLE_COLORS: Record<string, { bg: string; text: string }> = {
  ADMIN: { bg: "oklch(0.65 0.22 25 / 0.12)", text: "oklch(0.65 0.22 25)" },
  MANAGER: { bg: "oklch(0.75 0.15 85 / 0.12)", text: "oklch(0.75 0.15 85)" },
  DELEGATOR: {
    bg: "oklch(0.72 0.18 250 / 0.12)",
    text: "oklch(0.72 0.18 250)",
  },
  USER: { bg: "oklch(0.60 0.02 250 / 0.1)", text: "oklch(0.60 0.08 250)" },
  PGP_USER: { bg: "oklch(0.55 0.18 280 / 0.12)", text: "oklch(0.55 0.18 280)" },
};
const DEFAULT_COLOR = {
  bg: "oklch(0.55 0.05 250 / 0.12)",
  text: "oklch(0.55 0.05 250)",
};
const DEFAULT_ROLE = {
  bg: "oklch(0.55 0.05 250 / 0.12)",
  text: "oklch(0.55 0.05 250)",
};

function Badge({
  label,
  bg,
  text,
}: {
  label: string;
  bg: string;
  text: string;
}) {
  return (
    <span
      style={{
        padding: "2px 8px",
        borderRadius: "999px",
        fontSize: "0.7rem",
        fontWeight: 600,
        background: bg,
        color: text,
        whiteSpace: "nowrap",
      }}
    >
      {label}
    </span>
  );
}

function ConfirmModal({
  title,
  message,
  onConfirm,
  onCancel,
}: {
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div
      className="modal-backdrop"
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 100,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
      onClick={onCancel}
    >
      <div
        style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-xl)",
          padding: "1.5rem",
          maxWidth: "400px",
          width: "90%",
          boxShadow: "var(--shadow-lg)",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3
          style={{
            fontSize: "1.1rem",
            fontWeight: 700,
            marginBottom: "0.5rem",
          }}
        >
          {title}
        </h3>
        <p
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.9rem",
            marginBottom: "1.25rem",
          }}
        >
          {message}
        </p>
        <div
          style={{
            display: "flex",
            gap: "0.75rem",
            justifyContent: "flex-end",
          }}
        >
          <button onClick={onCancel} style={btnStyle("secondary")}>
            Cancel
          </button>
          <button onClick={onConfirm} style={btnStyle("danger")}>
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}

function btnStyle(
  variant: "primary" | "secondary" | "danger" | "ghost",
): React.CSSProperties {
  if (variant === "danger")
    return {
      padding: "0.5rem 1rem",
      background: "oklch(0.65 0.22 25)",
      color: "white",
      border: "none",
      borderRadius: "var(--radius-md)",
      fontSize: "0.85rem",
      fontWeight: 600,
      cursor: "pointer",
      transition: "opacity 0.15s",
    };
  if (variant === "ghost")
    return {
      padding: "0.5rem 0.75rem",
      background: "transparent",
      color: "var(--color-muted-foreground)",
      border: "1px solid var(--color-border)",
      borderRadius: "var(--radius-md)",
      fontSize: "0.85rem",
      cursor: "pointer",
    };
  if (variant === "primary")
    return {
      padding: "0.5rem 1rem",
      background: "var(--color-primary)",
      color: "var(--color-primary-foreground)",
      border: "none",
      borderRadius: "var(--radius-md)",
      fontSize: "0.85rem",
      fontWeight: 600,
      cursor: "pointer",
      transition: "opacity 0.15s",
    };
  return {
    padding: "0.5rem 1rem",
    background: "var(--color-secondary)",
    color: "var(--color-secondary-foreground)",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    fontSize: "0.85rem",
    cursor: "pointer",
  };
}

export default function AdminUsersPage() {
  const router = useRouter();
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<UserSummary | null>(null);
  const [error, setError] = useState("");
  const [menuOpen, setMenuOpen] = useState<number | null>(null);

  const token =
    typeof window !== "undefined"
      ? localStorage.getItem("printflow_token")
      : null;
  useEffect(() => {
    if (!token) router.push("/login");
  }, [router, token]);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await usersApi.list({
        page,
        limit: PAGE_SIZE,
        search: search || undefined,
        status: statusFilter || undefined,
      });
      setUsers(data.data?.data ?? []);
      setTotal(data.data?.total ?? 0);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  useEffect(() => {
    setPage(1);
  }, [search, statusFilter]);

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(deleteTarget.id);
    setMenuOpen(null);
    try {
      await usersApi.delete(deleteTarget.id);
      setDeleteTarget(null);
      fetchUsers();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setDeleting(null);
    }
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      {/* Header */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          marginBottom: "1.5rem",
          flexWrap: "wrap",
          gap: "1rem",
        }}
      >
        <div>
          <h1
            style={{
              fontSize: "1.5rem",
              fontWeight: 700,
              letterSpacing: "-0.02em",
            }}
          >
            Users
          </h1>
          <p
            style={{
              color: "var(--color-muted-foreground)",
              fontSize: "0.875rem",
              marginTop: "0.25rem",
            }}
          >
            Manage system users and their roles
          </p>
        </div>
        <Link
          href="/admin/users/new"
          style={{
            ...btnStyle("primary"),
            display: "inline-flex",
            alignItems: "center",
            gap: "0.5rem",
            textDecoration: "none",
          }}
        >
          <Plus size={16} /> Add User
        </Link>
      </div>

      {/* Filters */}
      <div
        style={{
          display: "flex",
          gap: "0.75rem",
          marginBottom: "1rem",
          flexWrap: "wrap",
          alignItems: "center",
        }}
      >
        {/* Search */}
        <div style={{ position: "relative", flex: "1", minWidth: "200px" }}>
          <Search
            size={16}
            style={{
              position: "absolute",
              left: "0.75rem",
              top: "50%",
              transform: "translateY(-50%)",
              color: "var(--color-muted-foreground)",
            }}
          />
          <input
            type="text"
            placeholder="Search by username or full name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: "100%",
              padding: "0.5rem 0.75rem 0.5rem 2.25rem",
              background: "var(--color-input)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-foreground)",
              fontSize: "0.875rem",
              outline: "none",
            }}
          />
          {search && (
            <button
              onClick={() => setSearch("")}
              style={{
                position: "absolute",
                right: "0.5rem",
                top: "50%",
                transform: "translateY(-50%)",
                background: "none",
                border: "none",
                cursor: "pointer",
                color: "var(--color-muted-foreground)",
                padding: "2px",
                display: "flex",
                alignItems: "center",
              }}
            >
              <X size={14} />
            </button>
          )}
        </div>

        {/* Status filter */}
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          style={{
            padding: "0.5rem 0.75rem",
            background: "var(--color-input)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-md)",
            color: "var(--color-foreground)",
            fontSize: "0.875rem",
            outline: "none",
            cursor: "pointer",
          }}
        >
          <option value="">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="BLOCKED">Blocked</option>
          <option value="DELETED">Deleted</option>
          <option value="EXPIRED">Expired</option>
        </select>
      </div>

      {/* Error */}
      {error && (
        <div
          style={{
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
          }}
        >
          <AlertCircle size={16} /> {error}
        </div>
      )}

      {/* Table */}
      <div
        style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-lg)",
          overflow: "hidden",
        }}
      >
        {loading ? (
          <SkeletonTable rows={6} cols={7} />
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                minWidth: "700px",
              }}
            >
              <thead>
                <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                  {[
                    "User",
                    "Roles",
                    "Status",
                    "Balance",
                    "Quota",
                    "Joined",
                    "Actions",
                  ].map((h) => (
                    <th
                      key={h}
                      style={{
                        padding: "0.75rem 1rem",
                        textAlign: "left",
                        fontSize: "0.75rem",
                        fontWeight: 600,
                        color: "var(--color-muted-foreground)",
                        textTransform: "uppercase",
                        letterSpacing: "0.05em",
                        background: "oklch(0.20 0.02 250)",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {users.length === 0 ? (
                  <tr>
                    <td
                      colSpan={7}
                      style={{
                        padding: "3rem",
                        textAlign: "center",
                        color: "var(--color-muted-foreground)",
                      }}
                    >
                      No users found
                    </td>
                  </tr>
                ) : (
                  users.map((user) => {
                    const sc =
                      STATUS_COLORS[user.status] ?? DEFAULT_STATUS_COLOR;
                    return (
                      <tr
                        key={user.id}
                        className="table-row-hover"
                        style={{
                          borderBottom: "1px solid var(--color-border)",
                        }}
                      >
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: "0.625rem",
                            }}
                          >
                            <div
                              style={{
                                width: "32px",
                                height: "32px",
                                borderRadius: "50%",
                                background: "oklch(0.72 0.18 250 / 0.2)",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                fontSize: "0.8rem",
                                fontWeight: 700,
                                color: "var(--color-primary)",
                                flexShrink: 0,
                              }}
                            >
                              {user.userName[0]?.toUpperCase()}
                            </div>
                            <div>
                              <div
                                style={{
                                  fontWeight: 600,
                                  fontSize: "0.875rem",
                                }}
                              >
                                {user.fullName}
                              </div>
                              <div
                                style={{
                                  fontSize: "0.75rem",
                                  color: "var(--color-muted-foreground)",
                                }}
                              >
                                @{user.userName}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div
                            style={{
                              display: "flex",
                              flexWrap: "wrap",
                              gap: "4px",
                            }}
                          >
                            {user.roles.map((role) => {
                              const rc = ROLE_COLORS[role] ?? DEFAULT_ROLE;
                              return (
                                <Badge
                                  key={role}
                                  label={role}
                                  bg={rc.bg}
                                  text={rc.text}
                                />
                              );
                            })}
                            {user.totpEnabled && (
                              <Badge
                                label="2FA"
                                bg="oklch(0.72 0.18 145 / 0.12)"
                                text="oklch(0.72 0.18 145)"
                              />
                            )}
                          </div>
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <Badge label={sc.label} bg={sc.bg} text={sc.text} />
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.85rem",
                          }}
                        >
                          {Number(user.printBalance ?? 0).toFixed(2)}
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.85rem",
                          }}
                        >
                          {Number(user.printQuota ?? 0).toFixed(2)}
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontSize: "0.8rem",
                            color: "var(--color-muted-foreground)",
                          }}
                        >
                          {new Date(user.dateCreated).toLocaleDateString()}
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            position: "relative",
                          }}
                        >
                          <div style={{ display: "flex", gap: "0.5rem" }}>
                            <Link
                              href={`/admin/users/${user.id}`}
                              className="table-action-btn"
                              style={{
                                padding: "0.375rem 0.625rem",
                                background: "transparent",
                                border: "1px solid var(--color-border)",
                                borderRadius: "var(--radius-md)",
                                color: "var(--color-muted-foreground)",
                                cursor: "pointer",
                                fontSize: "0.8rem",
                                textDecoration: "none",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: "4px",
                              }}
                            >
                              <Edit2 size={12} /> Edit
                            </Link>
                            <button
                              onClick={() => {
                                setDeleteTarget(user);
                                setMenuOpen(null);
                              }}
                              disabled={deleting === user.id}
                              className="table-action-btn danger"
                              style={{
                                padding: "0.375rem 0.625rem",
                                background: "transparent",
                                border: "1px solid var(--color-border)",
                                borderRadius: "var(--radius-md)",
                                color:
                                  deleting === user.id
                                    ? "oklch(0.65 0.22 25 / 0.5)"
                                    : "oklch(0.65 0.22 25)",
                                cursor:
                                  deleting === user.id
                                    ? "not-allowed"
                                    : "pointer",
                                fontSize: "0.8rem",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: "4px",
                              }}
                            >
                              <Trash2 size={12} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {!loading && users.length > 0 && (
          <div
            style={{
              padding: "0.75rem 1rem",
              borderTop: "1px solid var(--color-border)",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              flexWrap: "wrap",
              gap: "0.5rem",
            }}
          >
            <span
              style={{
                fontSize: "0.8rem",
                color: "var(--color-muted-foreground)",
              }}
            >
              Showing {(page - 1) * PAGE_SIZE + 1}–
              {Math.min(page * PAGE_SIZE, total)} of {total} users
            </span>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={!users.length || page <= 1}
                style={{
                  padding: "0.375rem 0.75rem",
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color:
                    page <= 1
                      ? "var(--color-muted-foreground)"
                      : "var(--color-foreground)",
                  cursor: page <= 1 ? "not-allowed" : "pointer",
                  fontSize: "0.8rem",
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "4px",
                }}
              >
                <ChevronLeft size={14} />
              </button>
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                const pageNum =
                  Math.max(1, Math.min(totalPages - 4, page - 2)) + i;
                return (
                  <button
                    key={pageNum}
                    onClick={() => setPage(pageNum)}
                    style={{
                      width: "32px",
                      height: "32px",
                      borderRadius: "var(--radius-md)",
                      border:
                        page === pageNum
                          ? "1px solid var(--color-primary)"
                          : "1px solid var(--color-border)",
                      background:
                        page === pageNum
                          ? "oklch(0.72 0.18 250 / 0.1)"
                          : "var(--color-card)",
                      color:
                        page === pageNum
                          ? "var(--color-primary)"
                          : "var(--color-foreground)",
                      cursor: "pointer",
                      fontSize: "0.8rem",
                      fontWeight: page === pageNum ? 600 : 400,
                    }}
                  >
                    {pageNum}
                  </button>
                );
              })}
              <button
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={!users.length || page >= totalPages}
                style={{
                  padding: "0.375rem 0.75rem",
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color:
                    page >= totalPages
                      ? "var(--color-muted-foreground)"
                      : "var(--color-foreground)",
                  cursor: page >= totalPages ? "not-allowed" : "pointer",
                  fontSize: "0.8rem",
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "4px",
                }}
              >
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {deleteTarget && (
        <ConfirmModal
          title="Delete User"
          message={`Are you sure you want to delete "${deleteTarget.fullName}" (@${deleteTarget.userName})? This will set the user status to DELETED.`}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}
