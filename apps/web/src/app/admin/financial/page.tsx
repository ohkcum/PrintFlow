"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  Plus,
  Search,
  ChevronLeft,
  ChevronRight,
  X,
  CreditCard,
  TrendingUp,
  TrendingDown,
  ArrowUpRight,
  ArrowDownRight,
  Receipt,
  Ticket,
  RefreshCw,
  Download,
  Upload,
  ArrowLeftRight,
  Trash2,
  Eye,
} from "lucide-react";
import {
  accountsApi,
  usersApi,
  type Account,
  type AccountTransaction,
  type AccountVoucher,
  type FinancialSummary,
} from "@/lib/api";
import { SkeletonTable } from "@/components/ui/Skeleton";

const PAGE_SIZE = 20;

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
          maxWidth: "520px",
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
    <Modal title={title} onClose={onCancel}>
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
        style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end" }}
      >
        <button onClick={onCancel} style={btnStyle("secondary")}>
          Cancel
        </button>
        <button onClick={onConfirm} style={btnStyle("danger")}>
          Confirm
        </button>
      </div>
    </Modal>
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

const TRX_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
  MANUAL_ADD: {
    bg: "oklch(0.72 0.18 145 / 0.12)",
    text: "oklch(0.72 0.18 145)",
  },
  VOUCHER_REDEEM: {
    bg: "oklch(0.75 0.15 85 / 0.12)",
    text: "oklch(0.75 0.15 85)",
  },
  TRANSFER_IN: {
    bg: "oklch(0.72 0.18 145 / 0.12)",
    text: "oklch(0.72 0.18 145)",
  },
  TRANSFER_OUT: {
    bg: "oklch(0.65 0.22 25 / 0.12)",
    text: "oklch(0.65 0.22 25)",
  },
  MANUAL_DEDUCT: {
    bg: "oklch(0.65 0.22 25 / 0.12)",
    text: "oklch(0.65 0.22 25)",
  },
  PRINT_JOB: {
    bg: "oklch(0.72 0.18 250 / 0.12)",
    text: "oklch(0.72 0.18 250)",
  },
  REFUND: { bg: "oklch(0.75 0.15 85 / 0.12)", text: "oklch(0.75 0.15 85)" },
  INITIAL: { bg: "oklch(0.60 0.08 250 / 0.12)", text: "oklch(0.60 0.08 250)" },
};

const VOUCHER_STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  active: { bg: "oklch(0.72 0.18 145 / 0.12)", text: "oklch(0.72 0.18 145)" },
  used: { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)" },
  expired: { bg: "oklch(0.65 0.22 25 / 0.12)", text: "oklch(0.65 0.22 25)" },
};

const ACCOUNT_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
  USER: { bg: "oklch(0.72 0.18 250 / 0.12)", text: "oklch(0.72 0.18 250)" },
  GROUP: { bg: "oklch(0.75 0.15 85 / 0.12)", text: "oklch(0.75 0.15 85)" },
  SHARED: { bg: "oklch(0.72 0.18 145 / 0.12)", text: "oklch(0.72 0.18 145)" },
  SYSTEM: { bg: "oklch(0.55 0.18 280 / 0.12)", text: "oklch(0.55 0.18 280)" },
};

// ─── Stat Card ─────────────────────────────────────────────────────────────────

function StatCard({
  icon: Icon,
  label,
  value,
  sublabel,
  color,
}: {
  icon: React.ElementType;
  label: string;
  value: string | number;
  sublabel?: string;
  color: string;
}) {
  return (
    <div
      style={{
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-xl)",
        padding: "1.25rem",
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "0.75rem",
          marginBottom: "0.75rem",
        }}
      >
        <div
          style={{
            width: "40px",
            height: "40px",
            borderRadius: "var(--radius-md)",
            background: `${color} / 0.12`,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Icon size={20} style={{ color }} />
        </div>
        <div
          style={{
            fontSize: "0.75rem",
            color: "var(--color-muted-foreground)",
            fontWeight: 500,
          }}
        >
          {label}
        </div>
      </div>
      <div
        style={{
          fontSize: "1.75rem",
          fontWeight: 700,
          fontFamily: "var(--font-mono)",
          letterSpacing: "-0.02em",
        }}
      >
        {value}
      </div>
      {sublabel && (
        <div
          style={{
            fontSize: "0.75rem",
            color: "var(--color-muted-foreground)",
            marginTop: "0.25rem",
          }}
        >
          {sublabel}
        </div>
      )}
    </div>
  );
}

// ─── Pagination ────────────────────────────────────────────────────────────────

function Pagination({
  page,
  totalPages,
  total,
  pageSize,
  onPage,
}: {
  page: number;
  totalPages: number;
  total: number;
  pageSize: number;
  onPage: (p: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
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
        style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}
      >
        Showing {(page - 1) * pageSize + 1}–{Math.min(page * pageSize, total)}{" "}
        of {total}
      </span>
      <div style={{ display: "flex", gap: "0.375rem" }}>
        <button
          onClick={() => onPage(Math.max(1, page - 1))}
          disabled={page <= 1}
          style={{
            ...btnStyle("ghost"),
            padding: "0.375rem 0.625rem",
            opacity: page <= 1 ? 0.5 : 1,
          }}
        >
          <ChevronLeft size={14} />
        </button>
        {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
          const p = Math.max(1, Math.min(totalPages - 4, page - 2)) + i;
          return (
            <button
              key={p}
              onClick={() => onPage(p)}
              style={{
                width: "32px",
                height: "32px",
                borderRadius: "var(--radius-md)",
                border:
                  page === p
                    ? "1px solid var(--color-primary)"
                    : "1px solid var(--color-border)",
                background:
                  page === p
                    ? "oklch(0.72 0.18 250 / 0.1)"
                    : "var(--color-card)",
                color:
                  page === p
                    ? "var(--color-primary)"
                    : "var(--color-foreground)",
                cursor: "pointer",
                fontSize: "0.8rem",
                fontWeight: page === p ? 600 : 400,
              }}
            >
              {p}
            </button>
          );
        })}
        <button
          onClick={() => onPage(Math.min(totalPages, page + 1))}
          disabled={page >= totalPages}
          style={{
            ...btnStyle("ghost"),
            padding: "0.375rem 0.625rem",
            opacity: page >= totalPages ? 0.5 : 1,
          }}
        >
          <ChevronRight size={14} />
        </button>
      </div>
    </div>
  );
}

// ─── Accounts Tab ──────────────────────────────────────────────────────────────

function AccountsTab({
  onToast,
}: {
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [loading, setLoading] = useState(true);

  const [actionAccount, setActionAccount] = useState<Account | null>(null);
  const [actionType, setActionType] = useState<
    "refill" | "deduct" | "transfer" | null
  >(null);
  const [actionAmount, setActionAmount] = useState("");
  const [actionDesc, setActionDesc] = useState("");
  const [actionLoading, setActionLoading] = useState(false);
  const [transferToId, setTransferToId] = useState("");
  const [accountsForTransfer, setAccountsForTransfer] = useState<Account[]>([]);

  const fetchAccounts = useCallback(async () => {
    setLoading(true);
    try {
      const data = await accountsApi.list({
        page,
        limit: PAGE_SIZE,
        search: search || undefined,
        type: typeFilter || undefined,
      });
      setAccounts(data.data?.data ?? []);
      setTotal(data.data?.total ?? 0);
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setLoading(false);
    }
  }, [page, search, typeFilter, onToast]);

  const fetchAccountsForTransfer = useCallback(async () => {
    try {
      const data = await accountsApi.list({ limit: 100 });
      setAccountsForTransfer(data.data?.data ?? []);
    } catch {}
  }, []);

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  useEffect(() => {
    setPage(1);
  }, [search, typeFilter]);

  async function handleAction() {
    if (!actionAccount || !actionAmount) return;
    const amount = parseFloat(actionAmount);
    if (isNaN(amount) || amount <= 0) {
      onToast("Amount must be a positive number", "error");
      return;
    }

    setActionLoading(true);
    try {
      if (actionType === "refill") {
        await accountsApi.refill(actionAccount.id, {
          amount,
          description: actionDesc,
        });
        onToast(`Added ${amount} to ${actionAccount.accountName}`, "success");
      } else if (actionType === "deduct") {
        await accountsApi.deduct(actionAccount.id, {
          amount,
          description: actionDesc,
        });
        onToast(
          `Deducted ${amount} from ${actionAccount.accountName}`,
          "success",
        );
      } else if (actionType === "transfer") {
        const toId = parseInt(transferToId);
        if (isNaN(toId) || toId === actionAccount.id) {
          onToast("Invalid destination account", "error");
          setActionLoading(false);
          return;
        }
        await accountsApi.transfer(actionAccount.id, {
          toAccountId: toId,
          amount,
          description: actionDesc,
        });
        onToast(
          `Transferred ${amount} from ${actionAccount.accountName}`,
          "success",
        );
      }
      setActionAccount(null);
      setActionType(null);
      setActionAmount("");
      setActionDesc("");
      setTransferToId("");
      fetchAccounts();
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setActionLoading(false);
    }
  }

  function openAction(
    account: Account,
    type: "refill" | "deduct" | "transfer",
  ) {
    setActionAccount(account);
    setActionType(type);
    setActionAmount("");
    setActionDesc("");
    if (type === "transfer") {
      fetchAccountsForTransfer();
    }
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
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
            placeholder="Search accounts..."
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
        </div>
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
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
          <option value="">All Types</option>
          <option value="USER">User</option>
          <option value="GROUP">Group</option>
          <option value="SHARED">Shared</option>
          <option value="SYSTEM">System</option>
        </select>
      </div>

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
          <SkeletonTable rows={6} cols={5} />
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                minWidth: "600px",
              }}
            >
              <thead>
                <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                  {[
                    "Account",
                    "Type",
                    "Balance",
                    "Overdraft",
                    "Status",
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
                {accounts.length === 0 ? (
                  <tr>
                    <td
                      colSpan={6}
                      style={{
                        padding: "3rem",
                        textAlign: "center",
                        color: "var(--color-muted-foreground)",
                      }}
                    >
                      No accounts found
                    </td>
                  </tr>
                ) : (
                  accounts.map((acc) => {
                    const tc = ACCOUNT_TYPE_COLORS[acc.accountType] ?? {
                      bg: "oklch(0.55 0.05 250 / 0.12)",
                      text: "oklch(0.55 0.05 250)",
                    };
                    return (
                      <tr
                        key={acc.id}
                        style={{
                          borderBottom: "1px solid var(--color-border)",
                        }}
                      >
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div
                            style={{ fontWeight: 600, fontSize: "0.875rem" }}
                          >
                            {acc.accountName}
                          </div>
                          {acc.description && (
                            <div
                              style={{
                                fontSize: "0.75rem",
                                color: "var(--color-muted-foreground)",
                              }}
                            >
                              {acc.description}
                            </div>
                          )}
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <Badge
                            label={acc.accountType}
                            bg={tc.bg}
                            text={tc.text}
                          />
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.875rem",
                          }}
                        >
                          <span
                            style={{
                              color:
                                Number(acc.balance) >= 0
                                  ? "oklch(0.72 0.18 145)"
                                  : "oklch(0.65 0.22 25)",
                            }}
                          >
                            {Number(acc.balance).toFixed(4)}
                          </span>
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.875rem",
                            color: "var(--color-muted-foreground)",
                          }}
                        >
                          {Number(acc.overdraftLimit ?? "0").toFixed(4)}
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <Badge
                            label={acc.isEnabled ? "Active" : "Disabled"}
                            bg={
                              acc.isEnabled
                                ? "oklch(0.72 0.18 145 / 0.12)"
                                : "oklch(0.65 0.22 25 / 0.12)"
                            }
                            text={
                              acc.isEnabled
                                ? "oklch(0.72 0.18 145)"
                                : "oklch(0.65 0.22 25)"
                            }
                          />
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div style={{ display: "flex", gap: "0.375rem" }}>
                            <button
                              onClick={() => openAction(acc, "refill")}
                              className="table-action-btn"
                              title="Add credit"
                              style={{
                                padding: "0.375rem 0.625rem",
                                background: "transparent",
                                border: "1px solid var(--color-border)",
                                borderRadius: "var(--radius-md)",
                                color: "oklch(0.72 0.18 145)",
                                cursor: "pointer",
                                fontSize: "0.8rem",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: "4px",
                              }}
                            >
                              <Plus size={12} />
                            </button>
                            <button
                              onClick={() => openAction(acc, "deduct")}
                              className="table-action-btn"
                              title="Deduct credit"
                              style={{
                                padding: "0.375rem 0.625rem",
                                background: "transparent",
                                border: "1px solid var(--color-border)",
                                borderRadius: "var(--radius-md)",
                                color: "oklch(0.65 0.22 25)",
                                cursor: "pointer",
                                fontSize: "0.8rem",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: "4px",
                              }}
                            >
                              <ArrowDownRight size={12} />
                            </button>
                            <button
                              onClick={() => openAction(acc, "transfer")}
                              className="table-action-btn"
                              title="Transfer"
                              style={{
                                padding: "0.375rem 0.625rem",
                                background: "transparent",
                                border: "1px solid var(--color-border)",
                                borderRadius: "var(--radius-md)",
                                color: "var(--color-muted-foreground)",
                                cursor: "pointer",
                                fontSize: "0.8rem",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: "4px",
                              }}
                            >
                              <ArrowLeftRight size={12} />
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
        <Pagination
          page={page}
          totalPages={totalPages}
          total={total}
          pageSize={PAGE_SIZE}
          onPage={setPage}
        />
      </div>

      {/* Action Modal */}
      {actionAccount && actionType && (
        <Modal
          title={
            actionType === "refill"
              ? `Add Credit to ${actionAccount.accountName}`
              : actionType === "deduct"
                ? `Deduct Credit from ${actionAccount.accountName}`
                : `Transfer from ${actionAccount.accountName}`
          }
          onClose={() => {
            setActionAccount(null);
            setActionType(null);
          }}
        >
          <div
            style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
          >
            {actionType === "transfer" && (
              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.8rem",
                    fontWeight: 600,
                    marginBottom: "0.375rem",
                  }}
                >
                  Destination Account
                </label>
                <select
                  value={transferToId}
                  onChange={(e) => setTransferToId(e.target.value)}
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
                >
                  <option value="">Select destination account...</option>
                  {accountsForTransfer
                    .filter((a) => a.id !== actionAccount.id && a.isEnabled)
                    .map((a) => (
                      <option key={a.id} value={a.id}>
                        [{a.accountType}] {a.accountName}
                      </option>
                    ))}
                </select>
              </div>
            )}
            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  marginBottom: "0.375rem",
                }}
              >
                Amount
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                value={actionAmount}
                onChange={(e) => setActionAmount(e.target.value)}
                placeholder="0.00"
                style={{
                  width: "100%",
                  padding: "0.5rem 0.75rem",
                  background: "var(--color-input)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                  fontSize: "0.875rem",
                  fontFamily: "var(--font-mono)",
                  outline: "none",
                }}
              />
            </div>
            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  marginBottom: "0.375rem",
                }}
              >
                Description (optional)
              </label>
              <input
                type="text"
                value={actionDesc}
                onChange={(e) => setActionDesc(e.target.value)}
                placeholder="e.g. Monthly top-up"
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
            </div>
            <div
              style={{
                display: "flex",
                gap: "0.75rem",
                justifyContent: "flex-end",
                marginTop: "0.5rem",
              }}
            >
              <button
                onClick={() => {
                  setActionAccount(null);
                  setActionType(null);
                }}
                style={btnStyle("secondary")}
              >
                Cancel
              </button>
              <button
                onClick={handleAction}
                disabled={
                  actionLoading ||
                  !actionAmount ||
                  (actionType === "transfer" && !transferToId)
                }
                style={{
                  ...(actionType === "refill"
                    ? btnStyle("primary")
                    : actionType === "deduct"
                      ? btnStyle("danger")
                      : btnStyle("primary")),
                  opacity:
                    actionLoading ||
                    !actionAmount ||
                    (actionType === "transfer" && !transferToId)
                      ? 0.5
                      : 1,
                }}
              >
                {actionLoading
                  ? "Processing..."
                  : actionType === "refill"
                    ? `Add ${actionAmount || "0"}`
                    : actionType === "deduct"
                      ? `Deduct ${actionAmount || "0"}`
                      : `Transfer ${actionAmount || "0"}`}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ─── Transactions Tab ──────────────────────────────────────────────────────────

function TransactionsTab({
  onToast,
}: {
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [transactions, setTransactions] = useState<AccountTransaction[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [typeFilter, setTypeFilter] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [loading, setLoading] = useState(true);

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    try {
      // For now, get all transactions across all accounts (admin view)
      // In a real implementation, you'd have a dedicated /transactions endpoint
      // We fetch from the first account or show a placeholder
      const data = await accountsApi.list({ limit: 1 });
      if (data.data?.data && data.data.data.length > 0) {
        const accountId = data.data.data[0]?.id;
        if (accountId) {
          const txData = await accountsApi.transactions(accountId, {
            page,
            limit: PAGE_SIZE,
            type: typeFilter || undefined,
            dateFrom: dateFrom || undefined,
            dateTo: dateTo || undefined,
          });
          setTransactions(txData.data?.data ?? []);
          setTotal(txData.data?.total ?? 0);
        } else {
          setTransactions([]);
          setTotal(0);
        }
      } else {
        setTransactions([]);
        setTotal(0);
      }
    } catch (e: any) {
      // If no accounts exist yet, show empty state
      setTransactions([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [page, typeFilter, dateFrom, dateTo]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);
  useEffect(() => {
    setPage(1);
  }, [typeFilter, dateFrom, dateTo]);

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
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
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
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
          <option value="">All Types</option>
          <option value="MANUAL_ADD">Manual Add</option>
          <option value="MANUAL_DEDUCT">Manual Deduct</option>
          <option value="VOUCHER_REDEEM">Voucher Redeem</option>
          <option value="TRANSFER_IN">Transfer In</option>
          <option value="TRANSFER_OUT">Transfer Out</option>
          <option value="PRINT_JOB">Print Job</option>
          <option value="REFUND">Refund</option>
          <option value="INITIAL">Initial</option>
        </select>
        <input
          type="date"
          value={dateFrom}
          onChange={(e) => setDateFrom(e.target.value)}
          style={{
            padding: "0.5rem 0.75rem",
            background: "var(--color-input)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-md)",
            color: "var(--color-foreground)",
            fontSize: "0.875rem",
            outline: "none",
          }}
        />
        <span
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.875rem",
          }}
        >
          to
        </span>
        <input
          type="date"
          value={dateTo}
          onChange={(e) => setDateTo(e.target.value)}
          style={{
            padding: "0.5rem 0.75rem",
            background: "var(--color-input)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-md)",
            color: "var(--color-foreground)",
            fontSize: "0.875rem",
            outline: "none",
          }}
        />
        {(dateFrom || dateTo || typeFilter) && (
          <button
            onClick={() => {
              setTypeFilter("");
              setDateFrom("");
              setDateTo("");
            }}
            style={{ ...btnStyle("ghost"), fontSize: "0.8rem" }}
          >
            <X size={14} /> Clear
          </button>
        )}
      </div>

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
          <SkeletonTable rows={6} cols={6} />
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
                    "Date",
                    "Type",
                    "Description",
                    "Amount",
                    "Balance After",
                    "Reversed",
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
                {transactions.length === 0 ? (
                  <tr>
                    <td
                      colSpan={6}
                      style={{
                        padding: "3rem",
                        textAlign: "center",
                        color: "var(--color-muted-foreground)",
                      }}
                    >
                      No transactions found
                    </td>
                  </tr>
                ) : (
                  transactions.map((tx) => {
                    const tc = TRX_TYPE_COLORS[tx.trxType] ?? {
                      bg: "oklch(0.55 0.05 250 / 0.12)",
                      text: "oklch(0.55 0.05 250)",
                    };
                    const amt = Number(tx.amount);
                    return (
                      <tr
                        key={tx.id}
                        style={{
                          borderBottom: "1px solid var(--color-border)",
                        }}
                      >
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontSize: "0.8rem",
                            color: "var(--color-muted-foreground)",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {new Date(tx.dateCreated).toLocaleString()}
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <Badge
                            label={tx.trxType.replace(/_/g, " ")}
                            bg={tc.bg}
                            text={tc.text}
                          />
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontSize: "0.875rem",
                            maxWidth: "250px",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {tx.description ?? "—"}
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.875rem",
                          }}
                        >
                          <span
                            style={{
                              color:
                                amt >= 0
                                  ? "oklch(0.72 0.18 145)"
                                  : "oklch(0.65 0.22 25)",
                              display: "flex",
                              alignItems: "center",
                              gap: "4px",
                            }}
                          >
                            {amt >= 0 ? (
                              <TrendingUp size={14} />
                            ) : (
                              <TrendingDown size={14} />
                            )}
                            {amt >= 0 ? "+" : ""}
                            {amt.toFixed(4)}
                          </span>
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.875rem",
                          }}
                        >
                          {Number(tx.balanceAfter).toFixed(4)}
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          {tx.isReversed ? (
                            <Badge
                              label="Yes"
                              bg="oklch(0.65 0.22 25 / 0.12)"
                              text="oklch(0.65 0.22 25)"
                            />
                          ) : (
                            <span
                              style={{
                                color: "var(--color-muted-foreground)",
                                fontSize: "0.8rem",
                              }}
                            >
                              —
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}
        <Pagination
          page={page}
          totalPages={totalPages}
          total={total}
          pageSize={PAGE_SIZE}
          onPage={setPage}
        />
      </div>
    </div>
  );
}

// ─── Vouchers Tab ──────────────────────────────────────────────────────────────

function VouchersTab({
  onToast,
}: {
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [vouchers, setVouchers] = useState<AccountVoucher[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(true);

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createData, setCreateData] = useState({
    accountId: "",
    nominalValue: "",
    count: "1",
    validUntil: "",
    isSingleUse: true,
    description: "",
  });
  const [createLoading, setCreateLoading] = useState(false);

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [deleteTarget, setDeleteTarget] = useState<AccountVoucher | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const fetchVouchers = useCallback(async () => {
    setLoading(true);
    try {
      const data = await accountsApi.vouchers({
        page,
        limit: PAGE_SIZE,
        search: search || undefined,
        status: statusFilter || undefined,
      });
      setVouchers(data.data?.data ?? []);
      setTotal(data.data?.total ?? 0);
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter, onToast]);

  const fetchAccounts = useCallback(async () => {
    try {
      const data = await accountsApi.list({ limit: 100 });
      setAccounts(data.data?.data ?? []);
    } catch {}
  }, []);

  useEffect(() => {
    fetchVouchers();
  }, [fetchVouchers]);
  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);
  useEffect(() => {
    setPage(1);
  }, [search, statusFilter]);

  async function handleCreate() {
    const accountId = parseInt(createData.accountId);
    const nominalValue = parseFloat(createData.nominalValue);
    const count = parseInt(createData.count);

    if (
      !createData.accountId ||
      isNaN(accountId) ||
      isNaN(nominalValue) ||
      nominalValue <= 0
    ) {
      onToast("Please fill in all required fields", "error");
      return;
    }

    setCreateLoading(true);
    try {
      await accountsApi.createVouchers({
        accountId,
        nominalValue,
        count: isNaN(count) ? 1 : count,
        validUntil: createData.validUntil || undefined,
        isSingleUse: createData.isSingleUse,
        description: createData.description || undefined,
      });
      onToast(`Created ${count} voucher(s)`, "success");
      setShowCreateModal(false);
      setCreateData({
        accountId: "",
        nominalValue: "",
        count: "1",
        validUntil: "",
        isSingleUse: true,
        description: "",
      });
      fetchVouchers();
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setCreateLoading(false);
    }
  }

  async function handleDeactivate() {
    if (!deleteTarget) return;
    setDeleteLoading(true);
    try {
      await accountsApi.deactivateVoucher(deleteTarget.id);
      onToast("Voucher deactivated", "success");
      setDeleteTarget(null);
      fetchVouchers();
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setDeleteLoading(false);
    }
  }

  function voucherStatus(v: AccountVoucher): {
    label: string;
    bg: string;
    text: string;
  } {
    if (!v.isActive) return { label: "Used", ...VOUCHER_STATUS_COLORS["used"] };
    if (v.validUntil && new Date(v.validUntil) < new Date())
      return { label: "Expired", ...VOUCHER_STATUS_COLORS["expired"] };
    return { label: "Active", ...VOUCHER_STATUS_COLORS["active"] };
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      {/* Toolbar */}
      <div
        style={{
          display: "flex",
          gap: "0.75rem",
          marginBottom: "1rem",
          flexWrap: "wrap",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <div
          style={{
            display: "flex",
            gap: "0.75rem",
            flexWrap: "wrap",
            alignItems: "center",
            flex: 1,
          }}
        >
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
              placeholder="Search voucher codes..."
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
          </div>
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
            <option value="active">Active</option>
            <option value="used">Used</option>
          </select>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          style={btnStyle("primary")}
        >
          <Plus size={16} /> Create Vouchers
        </button>
      </div>

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
          <SkeletonTable rows={6} cols={6} />
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
                    "Code",
                    "Account",
                    "Nominal Value",
                    "Remaining",
                    "Valid Until",
                    "Status",
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
                {vouchers.length === 0 ? (
                  <tr>
                    <td
                      colSpan={7}
                      style={{
                        padding: "3rem",
                        textAlign: "center",
                        color: "var(--color-muted-foreground)",
                      }}
                    >
                      No vouchers found
                    </td>
                  </tr>
                ) : (
                  vouchers.map((v) => {
                    const status = voucherStatus(v);
                    return (
                      <tr
                        key={v.id}
                        style={{
                          borderBottom: "1px solid var(--color-border)",
                        }}
                      >
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.875rem",
                            fontWeight: 600,
                            letterSpacing: "0.05em",
                          }}
                        >
                          {v.voucherCode}
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontSize: "0.875rem",
                          }}
                        >
                          {v.accountId}
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.875rem",
                          }}
                        >
                          {Number(v.nominalValue).toFixed(4)}
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontFamily: "var(--font-mono)",
                            fontSize: "0.875rem",
                          }}
                        >
                          {Number(v.remainingValue).toFixed(4)}
                        </td>
                        <td
                          style={{
                            padding: "0.75rem 1rem",
                            fontSize: "0.8rem",
                            color: "var(--color-muted-foreground)",
                          }}
                        >
                          {v.validUntil
                            ? new Date(v.validUntil).toLocaleDateString()
                            : "No expiry"}
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <Badge
                            label={status.label}
                            bg={status.bg}
                            text={status.text}
                          />
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <button
                            onClick={() => setDeleteTarget(v)}
                            disabled={!v.isActive}
                            title="Deactivate voucher"
                            style={{
                              padding: "0.375rem 0.625rem",
                              background: "transparent",
                              border: "1px solid var(--color-border)",
                              borderRadius: "var(--radius-md)",
                              color: v.isActive
                                ? "oklch(0.65 0.22 25)"
                                : "var(--color-muted-foreground)",
                              cursor: v.isActive ? "pointer" : "not-allowed",
                              fontSize: "0.8rem",
                              display: "inline-flex",
                              alignItems: "center",
                              gap: "4px",
                              opacity: v.isActive ? 1 : 0.5,
                            }}
                          >
                            <Trash2 size={12} />
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}
        <Pagination
          page={page}
          totalPages={totalPages}
          total={total}
          pageSize={PAGE_SIZE}
          onPage={setPage}
        />
      </div>

      {/* Create Modal */}
      {showCreateModal && (
        <Modal
          title="Create Vouchers"
          onClose={() => setShowCreateModal(false)}
        >
          <div
            style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
          >
            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  marginBottom: "0.375rem",
                }}
              >
                Account *
              </label>
              <select
                value={createData.accountId}
                onChange={(e) =>
                  setCreateData({ ...createData, accountId: e.target.value })
                }
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
              >
                <option value="">Select account...</option>
                {accounts
                  .filter((a) => a.isEnabled)
                  .map((a) => (
                    <option key={a.id} value={a.id}>
                      [{a.accountType}] {a.accountName}
                    </option>
                  ))}
              </select>
            </div>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "1rem",
              }}
            >
              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.8rem",
                    fontWeight: 600,
                    marginBottom: "0.375rem",
                  }}
                >
                  Nominal Value *
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={createData.nominalValue}
                  onChange={(e) =>
                    setCreateData({
                      ...createData,
                      nominalValue: e.target.value,
                    })
                  }
                  placeholder="10.00"
                  style={{
                    width: "100%",
                    padding: "0.5rem 0.75rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                    color: "var(--color-foreground)",
                    fontSize: "0.875rem",
                    fontFamily: "var(--font-mono)",
                    outline: "none",
                  }}
                />
              </div>
              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.8rem",
                    fontWeight: 600,
                    marginBottom: "0.375rem",
                  }}
                >
                  Count
                </label>
                <input
                  type="number"
                  min="1"
                  max="1000"
                  value={createData.count}
                  onChange={(e) =>
                    setCreateData({ ...createData, count: e.target.value })
                  }
                  placeholder="1"
                  style={{
                    width: "100%",
                    padding: "0.5rem 0.75rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                    color: "var(--color-foreground)",
                    fontSize: "0.875rem",
                    fontFamily: "var(--font-mono)",
                    outline: "none",
                  }}
                />
              </div>
            </div>
            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  marginBottom: "0.375rem",
                }}
              >
                Valid Until (optional)
              </label>
              <input
                type="date"
                value={createData.validUntil}
                onChange={(e) =>
                  setCreateData({ ...createData, validUntil: e.target.value })
                }
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
            </div>
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <input
                type="checkbox"
                id="singleUse"
                checked={createData.isSingleUse}
                onChange={(e) =>
                  setCreateData({
                    ...createData,
                    isSingleUse: e.target.checked,
                  })
                }
                style={{ width: "16px", height: "16px", cursor: "pointer" }}
              />
              <label
                htmlFor="singleUse"
                style={{ fontSize: "0.875rem", cursor: "pointer" }}
              >
                Single-use voucher
              </label>
            </div>
            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  marginBottom: "0.375rem",
                }}
              >
                Description (optional)
              </label>
              <input
                type="text"
                value={createData.description}
                onChange={(e) =>
                  setCreateData({ ...createData, description: e.target.value })
                }
                placeholder="e.g. Monthly promotion"
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
            </div>
            <div
              style={{
                display: "flex",
                gap: "0.75rem",
                justifyContent: "flex-end",
                marginTop: "0.5rem",
              }}
            >
              <button
                onClick={() => setShowCreateModal(false)}
                style={btnStyle("secondary")}
              >
                Cancel
              </button>
              <button
                onClick={handleCreate}
                disabled={createLoading}
                style={{
                  ...btnStyle("primary"),
                  opacity: createLoading ? 0.5 : 1,
                }}
              >
                {createLoading
                  ? "Creating..."
                  : `Create ${createData.count || 1} Voucher(s)`}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Deactivate Confirm */}
      {deleteTarget && (
        <ConfirmModal
          title="Deactivate Voucher"
          message={`Are you sure you want to deactivate voucher "${deleteTarget.voucherCode}"? This cannot be undone.`}
          onConfirm={handleDeactivate}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function AdminFinancialPage() {
  const router = useRouter();
  const [summary, setSummary] = useState<FinancialSummary | null>(null);
  const [loadingSummary, setLoadingSummary] = useState(true);
  const [activeTab, setActiveTab] = useState<
    "accounts" | "transactions" | "vouchers"
  >("accounts");
  const [toast, setToast] = useState<{
    message: string;
    type: "success" | "error";
  } | null>(null);

  const token =
    typeof window !== "undefined"
      ? localStorage.getItem("printflow_token")
      : null;
  useEffect(() => {
    if (!token) router.push("/login");
  }, [router, token]);

  useEffect(() => {
    accountsApi
      .summary()
      .then(setSummary)
      .catch(() => setSummary(null))
      .finally(() => setLoadingSummary(false));
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

  const TABS = [
    { id: "accounts" as const, label: "Accounts", icon: CreditCard },
    { id: "transactions" as const, label: "Transactions", icon: Receipt },
    { id: "vouchers" as const, label: "Vouchers", icon: Ticket },
  ];

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
          Financial
        </h1>
        <p
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.875rem",
            marginTop: "0.25rem",
          }}
        >
          Account balances, transactions, and voucher management
        </p>
      </div>

      {/* Summary Cards */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
          gap: "1rem",
          marginBottom: "1.5rem",
        }}
      >
        {loadingSummary ? (
          <>
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                style={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-xl)",
                  padding: "1.25rem",
                  height: "100px",
                  animation: "pulse 1.5s infinite",
                }}
              />
            ))}
          </>
        ) : summary ? (
          <>
            <StatCard
              icon={CreditCard}
              label="Total Balance"
              value={summary?.data?.totalBalance?.toFixed(4) ?? "0.0000"}
              sublabel={`${summary?.data?.userAccountCount ?? 0} user accounts`}
              color="oklch(0.72 0.18 145)"
            />
            <StatCard
              icon={Receipt}
              label="Total Accounts"
              value={summary?.data?.totalAccounts ?? 0}
              sublabel={`${summary?.data?.userAccountCount ?? 0} user accounts`}
              color="oklch(0.72 0.18 250)"
            />
            <StatCard
              icon={TrendingUp}
              label="Total Transactions"
              value={summary?.data?.totalTransactions ?? 0}
              color="oklch(0.75 0.15 85)"
            />
            <StatCard
              icon={Ticket}
              label="Recent Activity"
              value={summary?.data?.recentTransactions?.length ?? 0}
              sublabel="last 10 transactions"
              color="oklch(0.72 0.18 280)"
            />
          </>
        ) : (
          <div
            style={{
              gridColumn: "1 / -1",
              padding: "2rem",
              textAlign: "center",
              color: "var(--color-muted-foreground)",
              background: "var(--color-card)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-lg)",
            }}
          >
            No financial data available yet
          </div>
        )}
      </div>

      {/* Tabs */}
      <div
        style={{
          display: "flex",
          gap: "2px",
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-lg)",
          padding: "0.25rem",
          marginBottom: "1rem",
          width: "fit-content",
        }}
      >
        {TABS.map((tab) => {
          const Icon = tab.icon;
          const active = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                padding: "0.5rem 1rem",
                borderRadius: "calc(var(--radius-lg) - 2px)",
                border: "none",
                background: active
                  ? "oklch(0.72 0.18 250 / 0.1)"
                  : "transparent",
                color: active
                  ? "var(--color-primary)"
                  : "var(--color-muted-foreground)",
                fontSize: "0.875rem",
                fontWeight: active ? 600 : 400,
                cursor: "pointer",
                transition: "all 0.15s",
              }}
            >
              <Icon size={16} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Tab Content */}
      {activeTab === "accounts" && <AccountsTab onToast={showToast} />}
      {activeTab === "transactions" && <TransactionsTab onToast={showToast} />}
      {activeTab === "vouchers" && <VouchersTab onToast={showToast} />}

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
