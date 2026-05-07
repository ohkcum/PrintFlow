"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  Plus, Search, ChevronLeft, ChevronRight,
  X, Printer, Ticket, RefreshCw,
  Eye, Trash2, Clock, CheckCircle2, XCircle, Ban,
  AlertCircle, FileText, Copy, Tag, Calendar,
} from "lucide-react";
import {
  jobTicketsApi,
  printersApi,
  type JobTicket,
  type JobTicketSummary,
  type Printer,
} from "@/lib/api";
import { SkeletonTable } from "@/components/ui/Skeleton";

const PAGE_SIZE = 20;

// ─── Shared Components ─────────────────────────────────────────────────────────

function btnStyle(variant: "primary" | "secondary" | "danger" | "ghost"): React.CSSProperties {
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
  if (variant === "primary") return { ...base, background: "var(--color-primary)", color: "var(--color-primary-foreground)" };
  if (variant === "danger") return { ...base, background: "oklch(0.65 0.22 25)", color: "white" };
  if (variant === "ghost") return { ...base, background: "transparent", color: "var(--color-muted-foreground)", border: "1px solid var(--color-border)" };
  return { ...base, background: "var(--color-secondary)", color: "var(--color-secondary-foreground)", border: "1px solid var(--color-border)" };
}

function Badge({ label, bg, text }: { label: string; bg: string; text: string }) {
  return (
    <span style={{
      padding: "2px 8px",
      borderRadius: "999px",
      fontSize: "0.7rem",
      fontWeight: 600,
      background: bg,
      color: text,
      whiteSpace: "nowrap",
    }}>
      {label}
    </span>
  );
}

function Modal({ title, children, onClose }: { title: string; children: React.ReactNode; onClose: () => void }) {
  return (
    <div
      style={{ position: "fixed", inset: 0, zIndex: 100, display: "flex", alignItems: "center", justifyContent: "center", background: "oklch(0 0 0 / 0.6)", backdropFilter: "blur(4px)" }}
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
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "1.25rem" }}>
          <h3 style={{ fontSize: "1.1rem", fontWeight: 700 }}>{title}</h3>
          <button onClick={onClose} style={{ background: "none", border: "none", cursor: "pointer", color: "var(--color-muted-foreground)", padding: "4px", display: "flex", alignItems: "center" }}>
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function ConfirmModal({ title, message, onConfirm, onCancel }: {
  title: string; message: string; onConfirm: () => void; onCancel: () => void;
}) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.9rem", marginBottom: "1.25rem" }}>{message}</p>
      <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end" }}>
        <button onClick={onCancel} style={btnStyle("secondary")}>Cancel</button>
        <button onClick={onConfirm} style={btnStyle("danger")}>Confirm</button>
      </div>
    </Modal>
  );
}

function Toast({ message, type }: { message: string; type: "success" | "error" }) {
  return (
    <div style={{
      position: "fixed",
      bottom: "1.5rem",
      right: "1.5rem",
      zIndex: 200,
      padding: "0.75rem 1.25rem",
      borderRadius: "var(--radius-md)",
      background: type === "success" ? "oklch(0.72 0.18 145)" : "oklch(0.65 0.22 25)",
      color: "white",
      fontSize: "0.875rem",
      fontWeight: 600,
      boxShadow: "var(--shadow-lg)",
      animation: "fadeIn 0.2s ease",
    }}>
      {message}
    </div>
  );
}

function Pagination({ page, totalPages, total, pageSize, onPage }: {
  page: number; totalPages: number; total: number; pageSize: number; onPage: (p: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div style={{
      padding: "0.75rem 1rem",
      borderTop: "1px solid var(--color-border)",
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      flexWrap: "wrap",
      gap: "0.5rem",
    }}>
      <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>
        Showing {((page - 1) * pageSize) + 1}–{Math.min(page * pageSize, total)} of {total}
      </span>
      <div style={{ display: "flex", gap: "0.375rem" }}>
        <button
          onClick={() => onPage(Math.max(1, page - 1))}
          disabled={page <= 1}
          style={{ ...btnStyle("ghost"), padding: "0.375rem 0.625rem", opacity: page <= 1 ? 0.5 : 1 }}
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
                width: "32px", height: "32px",
                borderRadius: "var(--radius-md)",
                border: page === p ? "1px solid var(--color-primary)" : "1px solid var(--color-border)",
                background: page === p ? "oklch(0.72 0.18 250 / 0.1)" : "var(--color-card)",
                color: page === p ? "var(--color-primary)" : "var(--color-foreground)",
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
          style={{ ...btnStyle("ghost"), padding: "0.375rem 0.625rem", opacity: page >= totalPages ? 0.5 : 1 }}
        >
          <ChevronRight size={14} />
        </button>
      </div>
    </div>
  );
}

// ─── Status Helpers ────────────────────────────────────────────────────────────

const STATUS_COLORS: Record<string, { bg: string; text: string; label: string }> = {
  PENDING: { bg: "oklch(0.75 0.15 85 / 0.12)", text: "oklch(0.75 0.15 85)", label: "Pending" },
  PRINTING: { bg: "oklch(0.72 0.18 250 / 0.12)", text: "oklch(0.72 0.18 250)", label: "Printing" },
  COMPLETED: { bg: "oklch(0.72 0.18 145 / 0.12)", text: "oklch(0.72 0.18 145)", label: "Completed" },
  CANCELLED: { bg: "oklch(0.65 0.22 25 / 0.12)", text: "oklch(0.65 0.22 25)", label: "Cancelled" },
};

function getStatusBadge(status: string) {
  const s = STATUS_COLORS[status] ?? { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)", label: status };
  return <Badge label={s.label} bg={s.bg} text={s.text} />;
}

function getStatusIcon(status: string) {
  switch (status) {
    case "PENDING": return <Clock size={14} />;
    case "PRINTING": return <Printer size={14} />;
    case "COMPLETED": return <CheckCircle2 size={14} />;
    case "CANCELLED": return <XCircle size={14} />;
    default: return <AlertCircle size={14} />;
  }
}

// ─── Stat Card ─────────────────────────────────────────────────────────────────

function StatCard({ icon: Icon, label, value, sublabel, color }: {
  icon: React.ElementType; label: string; value: string | number; sublabel?: string; color: string;
}) {
  return (
    <div style={{
      background: "var(--color-card)",
      border: "1px solid var(--color-border)",
      borderRadius: "var(--radius-xl)",
      padding: "1.25rem",
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginBottom: "0.75rem" }}>
        <div style={{
          width: "40px", height: "40px", borderRadius: "var(--radius-md)",
          background: `${color} / 0.12`,
          display: "flex", alignItems: "center", justifyContent: "center",
        }}>
          <Icon size={20} style={{ color }} />
        </div>
        <div style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)", fontWeight: 500 }}>
          {label}
        </div>
      </div>
      <div style={{ fontSize: "1.75rem", fontWeight: 700, fontFamily: "var(--font-mono)", letterSpacing: "-0.02em" }}>
        {value}
      </div>
      {sublabel && (
        <div style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)", marginTop: "0.25rem" }}>
          {sublabel}
        </div>
      )}
    </div>
  );
}

// ─── My Tickets Tab ────────────────────────────────────────────────────────────

function MyTicketsTab({
  userId,
  isAdmin,
  onToast,
}: {
  userId: number;
  isAdmin: boolean;
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [tickets, setTickets] = useState<JobTicket[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState<{ ticket: JobTicket; action: "print" | "cancel" | "reopen" | "settle" } | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchTickets = useCallback(async (showRefresh = false) => {
    if (showRefresh) setRefreshing(true);
    else setLoading(true);
    try {
      const data = await jobTicketsApi.list({
        page,
        limit: PAGE_SIZE,
        userId: isAdmin ? undefined : userId,
        search: search || undefined,
        status: statusFilter || undefined,
      });
      setTickets(data.data);
      setTotal(data.total);
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [page, search, statusFilter, userId, isAdmin, onToast]);

  useEffect(() => { fetchTickets(); }, [fetchTickets]);
  useEffect(() => { setPage(1); }, [search, statusFilter]);

  async function handleAction() {
    if (!confirmTarget) return;
    const { ticket, action } = confirmTarget;
    setActionLoading(true);
    try {
      if (action === "print") {
        await jobTicketsApi.print(ticket.uuid);
        onToast(`Print job sent for ticket #${ticket.ticketNumber}`, "success");
      } else if (action === "settle") {
        await jobTicketsApi.settle(ticket.uuid);
        onToast(`Ticket #${ticket.ticketNumber} settled`, "success");
      } else if (action === "cancel") {
        await jobTicketsApi.cancel(ticket.uuid);
        onToast(`Ticket #${ticket.ticketNumber} cancelled`, "success");
      } else if (action === "reopen") {
        await jobTicketsApi.reopen(ticket.uuid);
        onToast(`Ticket #${ticket.ticketNumber} reopened`, "success");
      }
      setConfirmTarget(null);
      fetchTickets();
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setActionLoading(false);
    }
  }

  function confirmLabel(action: string, ticket: JobTicket) {
    switch (action) {
      case "print": return `Send print job for ticket #${ticket.ticketNumber}?`;
      case "settle": return `Settle and complete ticket #${ticket.ticketNumber}?`;
      case "cancel": return `Cancel ticket #${ticket.ticketNumber}? This cannot be undone.`;
      case "reopen": return `Reopen ticket #${ticket.ticketNumber}?`;
      default: return `Perform this action?`;
    }
  }

  function confirmTitle(action: string) {
    switch (action) {
      case "print": return "Print Job";
      case "settle": return "Settle Ticket";
      case "cancel": return "Cancel Ticket";
      case "reopen": return "Reopen Ticket";
      default: return "Confirm Action";
    }
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      {/* Filters */}
      <div style={{ display: "flex", gap: "0.75rem", marginBottom: "1rem", flexWrap: "wrap", alignItems: "center" }}>
        <div style={{ position: "relative", flex: "1", minWidth: "200px" }}>
          <Search size={16} style={{ position: "absolute", left: "0.75rem", top: "50%", transform: "translateY(-50%)", color: "var(--color-muted-foreground)" }} />
          <input
            type="text"
            placeholder="Search by ticket number or document name..."
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
          <option value="PENDING">Pending</option>
          <option value="PRINTING">Printing</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        {(search || statusFilter) && (
          <button
            onClick={() => { setSearch(""); setStatusFilter(""); }}
            style={{ ...btnStyle("ghost"), fontSize: "0.8rem" }}
          >
            <X size={14} /> Clear
          </button>
        )}
        <button
          onClick={() => fetchTickets(true)}
          disabled={refreshing}
          style={{ ...btnStyle("ghost"), fontSize: "0.8rem", opacity: refreshing ? 0.5 : 1 }}
        >
          <RefreshCw size={14} style={refreshing ? { animation: "spin 1s linear infinite" } : {}} />
        </button>
      </div>

      {/* Table */}
      <div style={{ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)", overflow: "hidden" }}>
        {loading ? (
          <SkeletonTable rows={6} cols={7} />
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "800px" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                  {["#", "Document", "Copies", "Printer", "Status", "Cost", "Submitted", "Actions"].map((h) => (
                    <th key={h} style={{
                      padding: "0.75rem 1rem",
                      textAlign: "left",
                      fontSize: "0.75rem",
                      fontWeight: 600,
                      color: "var(--color-muted-foreground)",
                      textTransform: "uppercase",
                      letterSpacing: "0.05em",
                      background: "oklch(0.20 0.02 250)",
                      whiteSpace: "nowrap",
                    }}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {tickets.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ padding: "3rem", textAlign: "center", color: "var(--color-muted-foreground)" }}>
                      No job tickets found
                    </td>
                  </tr>
                ) : (
                  tickets.map((ticket) => {
                    const statusInfo = STATUS_COLORS[ticket.status] ?? { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)" };
                    const canPrint = ticket.status === "PENDING" || ticket.status === "PRINTING";
                    const canSettle = ticket.status === "PRINTING" || ticket.status === "COMPLETED";
                    const canCancel = ticket.status === "PENDING" || ticket.status === "PRINTING";
                    const canReopen = ticket.status === "COMPLETED" || ticket.status === "CANCELLED";
                    return (
                      <tr key={ticket.uuid} style={{ borderBottom: "1px solid var(--color-border)" }}>
                        {/* Ticket Number */}
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div style={{ fontWeight: 700, fontSize: "0.85rem", fontFamily: "var(--font-mono)", color: "var(--color-primary)" }}>
                            #{ticket.ticketNumber}
                          </div>
                          {ticket.label && (
                            <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", marginTop: "2px", display: "flex", alignItems: "center", gap: "3px" }}>
                              <Tag size={10} />
                              {ticket.label}
                            </div>
                          )}
                        </td>
                        {/* Document */}
                        <td style={{ padding: "0.75rem 1rem", maxWidth: "180px" }}>
                          <div style={{ fontWeight: 600, fontSize: "0.875rem", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={ticket.docName}>
                            <FileText size={12} style={{ display: "inline", marginRight: "4px", color: "var(--color-muted-foreground)" }} />
                            {ticket.docName}
                          </div>
                          {ticket.domain && (
                            <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", marginTop: "2px" }}>
                              {ticket.domain}
                            </div>
                          )}
                        </td>
                        {/* Copies */}
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div style={{ display: "flex", alignItems: "center", gap: "4px", fontFamily: "var(--font-mono)", fontSize: "0.875rem" }}>
                            <Copy size={12} style={{ color: "var(--color-muted-foreground)" }} />
                            {ticket.copies}
                            {ticket.copiesPrinted > 0 && (
                              <span style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)" }}>
                                ({ticket.copiesPrinted})
                              </span>
                            )}
                          </div>
                        </td>
                        {/* Printer */}
                        <td style={{ padding: "0.75rem 1rem", maxWidth: "130px" }}>
                          <div style={{ fontSize: "0.8rem", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={ticket.printerName}>
                            <Printer size={12} style={{ display: "inline", marginRight: "4px", color: "var(--color-muted-foreground)" }} />
                            {ticket.printerName}
                          </div>
                        </td>
                        {/* Status */}
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div style={{ display: "flex", alignItems: "center", gap: "5px" }}>
                            {getStatusIcon(ticket.status)}
                            {getStatusBadge(ticket.status)}
                          </div>
                          {ticket.isReopened && (
                            <div style={{ fontSize: "0.65rem", color: "oklch(0.75 0.15 85)", marginTop: "2px" }}>
                              Reopened
                            </div>
                          )}
                        </td>
                        {/* Cost */}
                        <td style={{ padding: "0.75rem 1rem", fontFamily: "var(--font-mono)", fontSize: "0.875rem" }}>
                          <span style={{ color: Number(ticket.totalCost) > 0 ? "var(--color-foreground)" : "var(--color-muted-foreground)" }}>
                            {Number(ticket.totalCost).toFixed(4)}
                          </span>
                        </td>
                        {/* Submitted */}
                        <td style={{ padding: "0.75rem 1rem", fontSize: "0.75rem", color: "var(--color-muted-foreground)", whiteSpace: "nowrap" }}>
                          {new Date(ticket.submitTime).toLocaleDateString()}
                          <br />
                          <span style={{ fontSize: "0.7rem" }}>
                            {new Date(ticket.submitTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                          </span>
                        </td>
                        {/* Actions */}
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div style={{ display: "flex", gap: "0.375rem", flexWrap: "wrap" }}>
                            {canPrint && (
                              <button
                                onClick={() => setConfirmTarget({ ticket, action: "print" })}
                                title="Print"
                                style={{
                                  padding: "0.3rem 0.5rem",
                                  background: "transparent",
                                  border: "1px solid var(--color-border)",
                                  borderRadius: "var(--radius-md)",
                                  color: "oklch(0.72 0.18 250)",
                                  cursor: "pointer",
                                  fontSize: "0.75rem",
                                  display: "inline-flex",
                                  alignItems: "center",
                                  gap: "3px",
                                }}
                              >
                                <Printer size={11} /> Print
                              </button>
                            )}
                            {canSettle && (
                              <button
                                onClick={() => setConfirmTarget({ ticket, action: "settle" })}
                                title="Settle"
                                style={{
                                  padding: "0.3rem 0.5rem",
                                  background: "transparent",
                                  border: "1px solid var(--color-border)",
                                  borderRadius: "var(--radius-md)",
                                  color: "oklch(0.72 0.18 145)",
                                  cursor: "pointer",
                                  fontSize: "0.75rem",
                                  display: "inline-flex",
                                  alignItems: "center",
                                  gap: "3px",
                                }}
                              >
                                <CheckCircle2 size={11} /> Settle
                              </button>
                            )}
                            {canCancel && (
                              <button
                                onClick={() => setConfirmTarget({ ticket, action: "cancel" })}
                                title="Cancel"
                                style={{
                                  padding: "0.3rem 0.5rem",
                                  background: "transparent",
                                  border: "1px solid var(--color-border)",
                                  borderRadius: "var(--radius-md)",
                                  color: "oklch(0.65 0.22 25)",
                                  cursor: "pointer",
                                  fontSize: "0.75rem",
                                  display: "inline-flex",
                                  alignItems: "center",
                                  gap: "3px",
                                }}
                              >
                                <Ban size={11} /> Cancel
                              </button>
                            )}
                            {canReopen && (
                              <button
                                onClick={() => setConfirmTarget({ ticket, action: "reopen" })}
                                title="Reopen"
                                style={{
                                  padding: "0.3rem 0.5rem",
                                  background: "transparent",
                                  border: "1px solid var(--color-border)",
                                  borderRadius: "var(--radius-md)",
                                  color: "oklch(0.75 0.15 85)",
                                  cursor: "pointer",
                                  fontSize: "0.75rem",
                                  display: "inline-flex",
                                  alignItems: "center",
                                  gap: "3px",
                                }}
                              >
                                <RefreshCw size={11} /> Reopen
                              </button>
                            )}
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
        <Pagination page={page} totalPages={totalPages} total={total} pageSize={PAGE_SIZE} onPage={setPage} />
      </div>

      {/* Confirm Modal */}
      {confirmTarget && (
        <ConfirmModal
          title={confirmTitle(confirmTarget.action)}
          message={confirmLabel(confirmTarget.action, confirmTarget.ticket)}
          onConfirm={handleAction}
          onCancel={() => setConfirmTarget(null)}
        />
      )}
    </div>
  );
}

// ─── Create Ticket Tab ─────────────────────────────────────────────────────────

function CreateTicketTab({
  userId,
  onToast,
  onCreated,
}: {
  userId: number;
  onToast: (msg: string, type: "success" | "error") => void;
  onCreated: () => void;
}) {
  const [printers, setPrinters] = useState<Printer[]>([]);
  const [loadingPrinters, setLoadingPrinters] = useState(true);
  const [formData, setFormData] = useState({
    docName: "",
    copies: "1",
    printerName: "",
    label: "",
    domain: "",
    use: "",
    tag: "",
    deliveryDate: "",
  });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    printersApi.list({ status: "enabled" })
      .then((data) => setPrinters(data.filter((p: Printer) => p.isEnabled)))
      .catch(() => setPrinters([]))
      .finally(() => setLoadingPrinters(false));
  }, []);

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  }

  async function handleSubmit() {
    if (!formData.docName.trim()) {
      onToast("Document name is required", "error");
      return;
    }
    if (!formData.printerName) {
      onToast("Please select a printer", "error");
      return;
    }
    const copies = parseInt(formData.copies);
    if (isNaN(copies) || copies < 1) {
      onToast("Copies must be at least 1", "error");
      return;
    }

    setSubmitting(true);
    try {
      await jobTicketsApi.create({
        docName: formData.docName.trim(),
        copies,
        printerName: formData.printerName,
        label: formData.label.trim() || undefined,
        domain: formData.domain.trim() || undefined,
        use: formData.use.trim() || undefined,
        tag: formData.tag.trim() || undefined,
        deliveryDate: formData.deliveryDate || undefined,
      });
      onToast("Job ticket created successfully", "success");
      setFormData({
        docName: "",
        copies: "1",
        printerName: "",
        label: "",
        domain: "",
        use: "",
        tag: "",
        deliveryDate: "",
      });
      onCreated();
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ maxWidth: "640px" }}>
      <div style={{
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-xl)",
        padding: "1.5rem",
      }}>
        <h3 style={{ fontSize: "1rem", fontWeight: 700, marginBottom: "1.25rem", display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <Ticket size={18} style={{ color: "var(--color-primary)" }} />
          New Job Ticket
        </h3>

        <div style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
          {/* Document Name */}
          <div>
            <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, marginBottom: "0.375rem" }}>
              Document Name <span style={{ color: "oklch(0.65 0.22 25)" }}>*</span>
            </label>
            <input
              type="text"
              name="docName"
              value={formData.docName}
              onChange={handleChange}
              placeholder="e.g. Report Q4 2025.pdf"
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

          {/* Copies + Printer */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: "1rem" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, marginBottom: "0.375rem" }}>
                Copies
              </label>
              <input
                type="number"
                name="copies"
                value={formData.copies}
                onChange={handleChange}
                min="1"
                max="999"
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
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, marginBottom: "0.375rem" }}>
                Printer <span style={{ color: "oklch(0.65 0.22 25)" }}>*</span>
              </label>
              {loadingPrinters ? (
                <div style={{ padding: "0.5rem 0.75rem", color: "var(--color-muted-foreground)", fontSize: "0.875rem" }}>
                  Loading printers...
                </div>
              ) : (
                <select
                  name="printerName"
                  value={formData.printerName}
                  onChange={handleChange}
                  style={{
                    width: "100%",
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
                  <option value="">Select a printer...</option>
                  {printers.map((p) => (
                    <option key={p.id} value={p.name}>
                      {p.displayName} ({p.name})
                    </option>
                  ))}
                </select>
              )}
            </div>
          </div>

          {/* Label (optional) */}
          <div>
            <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, marginBottom: "0.375rem" }}>
              Label <span style={{ color: "var(--color-muted-foreground)", fontWeight: 400 }}>(optional)</span>
            </label>
            <input
              type="text"
              name="label"
              value={formData.label}
              onChange={handleChange}
              placeholder="e.g. urgent, meeting-notes, invoice"
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
            <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", marginTop: "0.25rem" }}>
              Domain / use / tag — for categorizing and filtering your tickets
            </div>
          </div>

          {/* Domain, Use, Tag row */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "0.75rem" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, marginBottom: "0.375rem" }}>
                Domain
              </label>
              <input
                type="text"
                name="domain"
                value={formData.domain}
                onChange={handleChange}
                placeholder="e.g. finance"
                style={{
                  width: "100%",
                  padding: "0.5rem 0.75rem",
                  background: "var(--color-input)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                  fontSize: "0.8rem",
                  outline: "none",
                }}
              />
            </div>
            <div>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, marginBottom: "0.375rem" }}>
                Use
              </label>
              <input
                type="text"
                name="use"
                value={formData.use}
                onChange={handleChange}
                placeholder="e.g. report"
                style={{
                  width: "100%",
                  padding: "0.5rem 0.75rem",
                  background: "var(--color-input)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                  fontSize: "0.8rem",
                  outline: "none",
                }}
              />
            </div>
            <div>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, marginBottom: "0.375rem" }}>
                Tag
              </label>
              <input
                type="text"
                name="tag"
                value={formData.tag}
                onChange={handleChange}
                placeholder="e.g. monthly"
                style={{
                  width: "100%",
                  padding: "0.5rem 0.75rem",
                  background: "var(--color-input)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                  fontSize: "0.8rem",
                  outline: "none",
                }}
              />
            </div>
          </div>

          {/* Delivery Date */}
          <div>
            <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, marginBottom: "0.375rem" }}>
              Delivery Date <span style={{ color: "var(--color-muted-foreground)", fontWeight: 400 }}>(optional)</span>
            </label>
            <input
              type="date"
              name="deliveryDate"
              value={formData.deliveryDate}
              onChange={handleChange}
              min={new Date().toISOString().split("T")[0]}
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

          {/* Submit */}
          <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end", marginTop: "0.5rem" }}>
            <button
              onClick={() => setFormData({
                docName: "", copies: "1", printerName: "",
                label: "", domain: "", use: "", tag: "", deliveryDate: "",
              })}
              style={btnStyle("secondary")}
            >
              Clear
            </button>
            <button
              onClick={handleSubmit}
              disabled={submitting}
              style={{
                ...btnStyle("primary"),
                opacity: submitting ? 0.5 : 1,
              }}
            >
              <Ticket size={16} />
              {submitting ? "Creating..." : "Create Ticket"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function JobTicketsPage() {
  const router = useRouter();
  const [user, setUser] = useState<any>(null);
  const [summary, setSummary] = useState<JobTicketSummary | null>(null);
  const [loadingSummary, setLoadingSummary] = useState(true);
  const [activeTab, setActiveTab] = useState<"list" | "create">("list");
  const [toast, setToast] = useState<{ message: string; type: "success" | "error" } | null>(null);

  useEffect(() => {
    const token = localStorage.getItem("printflow_token");
    const userStr = localStorage.getItem("printflow_user");
    if (!token || !userStr) {
      router.push("/login");
      return;
    }
    try {
      const u = JSON.parse(userStr);
      setUser(u);
    } catch {
      router.push("/login");
    }
  }, [router]);

  useEffect(() => {
    jobTicketsApi.summary()
      .then(setSummary)
      .catch(() => setSummary(null))
      .finally(() => setLoadingSummary(false));
  }, []);

  useEffect(() => {
    if (toast) {
      const t = setTimeout(() => setToast(null), 3500);
      return () => clearTimeout(t);
    }
  }, [toast]);

  function showToast(msg: string, type: "success" | "error") {
    setToast({ message: msg, type });
  }

  if (!user) return null;

  const isAdmin = user.roles?.includes("ADMIN") || user.roles?.includes("MANAGER");

  const TABS = [
    { id: "list" as const, label: "My Tickets", icon: Ticket },
    { id: "create" as const, label: "Create Ticket", icon: Plus },
  ];

  return (
    <div>
      {/* Header */}
      <div style={{ marginBottom: "1.5rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em" }}>
          Job Tickets
        </h1>
        <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem", marginTop: "0.25rem" }}>
          Submit, track, and manage your print jobs
        </p>
      </div>

      {/* Summary Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))", gap: "1rem", marginBottom: "1.5rem" }}>
        {loadingSummary ? (
          <>
            {[1, 2, 3].map((i) => (
              <div key={i} style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-xl)",
                padding: "1.25rem",
                height: "100px",
                animation: "pulse 1.5s infinite",
              }} />
            ))}
          </>
        ) : summary ? (
          <>
            <StatCard
              icon={Clock}
              label="Pending"
              value={summary.totalPending}
              sublabel="awaiting print"
              color="oklch(0.75 0.15 85)"
            />
            <StatCard
              icon={Printer}
              label="Print Jobs"
              value={summary.printJobs}
              sublabel="in queue"
              color="oklch(0.72 0.18 250)"
            />
            <StatCard
              icon={Copy}
              label="Copy Jobs"
              value={summary.copyJobs}
              sublabel="in queue"
              color="oklch(0.72 0.18 145)"
            />
            <StatCard
              icon={FileText}
              label="Total Cost"
              value={Number(summary.totalCost).toFixed(4)}
              sublabel="all time"
              color="oklch(0.72 0.18 280)"
            />
          </>
        ) : (
          <div style={{ gridColumn: "1 / -1", padding: "2rem", textAlign: "center", color: "var(--color-muted-foreground)", background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)" }}>
            No ticket data available yet
          </div>
        )}
      </div>

      {/* Tabs */}
      <div style={{
        display: "flex",
        gap: "2px",
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-lg)",
        padding: "0.25rem",
        marginBottom: "1rem",
        width: "fit-content",
      }}>
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
                background: active ? "oklch(0.72 0.18 250 / 0.1)" : "transparent",
                color: active ? "var(--color-primary)" : "var(--color-muted-foreground)",
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
      {activeTab === "list" && (
        <MyTicketsTab
          userId={user.id}
          isAdmin={isAdmin}
          onToast={showToast}
        />
      )}
      {activeTab === "create" && (
        <CreateTicketTab
          userId={user.id}
          onToast={showToast}
          onCreated={() => {
            setActiveTab("list");
            jobTicketsApi.summary().then(setSummary).catch(() => {});
          }}
        />
      )}

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
