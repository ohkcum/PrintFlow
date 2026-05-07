"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import {
  Ticket,
  Search,
  ChevronLeft,
  ChevronRight,
  X,
  RefreshCw,
  Download,
  Trash2,
  Eye,
  Printer,
  CheckCircle,
  XCircle,
  Clock,
  FileText,
  DollarSign,
  List,
  Copy,
  AlertTriangle,
  RotateCcw,
  CheckSquare,
  Square,
  Zap,
} from "lucide-react";
import {
  jobTicketsApi,
  type JobTicket,
  type JobTicketSummary,
} from "@/lib/api";
import { SkeletonTable } from "@/components/ui/Skeleton";

const PAGE_SIZE = 20;

const STATUS_COLORS: Record<string, { bg: string; text: string; label: string }> = {
  PENDING: { bg: "oklch(0.75 0.15 85 / 0.12)", text: "oklch(0.75 0.15 85)", label: "Pending" },
  PRINTING: { bg: "oklch(0.72 0.18 250 / 0.12)", text: "oklch(0.72 0.18 250)", label: "Printing" },
  COMPLETED: { bg: "oklch(0.72 0.18 145 / 0.12)", text: "oklch(0.72 0.18 145)", label: "Completed" },
  CANCELLED: { bg: "oklch(0.65 0.22 25 / 0.12)", text: "oklch(0.65 0.22 25)", label: "Cancelled" },
};

// ─── Shared Components ─────────────────────────────────────────────────────────

function btnStyle(variant: "primary" | "secondary" | "danger" | "ghost" | "warning"): React.CSSProperties {
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
  if (variant === "warning") return { ...base, background: "oklch(0.75 0.15 85)", color: "white" };
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

function Modal({ title, children, onClose, wide }: { title: string; children: React.ReactNode; onClose: () => void; wide?: boolean }) {
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
          maxWidth: wide ? "720px" : "520px",
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

function Toast({ message, type }: { message: string; type: "success" | "error" | "info" }) {
  return (
    <div style={{
      position: "fixed",
      bottom: "1.5rem",
      right: "1.5rem",
      zIndex: 200,
      padding: "0.75rem 1.25rem",
      borderRadius: "var(--radius-md)",
      background: type === "success" ? "oklch(0.72 0.18 145)" : type === "error" ? "oklch(0.65 0.22 25)" : "oklch(0.72 0.18 250)",
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

// ─── Detail Modal ──────────────────────────────────────────────────────────────

function DetailModal({ ticket, onClose, onToast }: {
  ticket: JobTicket;
  onClose: () => void;
  onToast: (msg: string, type: "success" | "error" | "info") => void;
}) {
  const sc = STATUS_COLORS[ticket.status] ?? { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)", label: ticket.status };

  function fmtLabel(val: string | null, fallback = "—"): string {
    return val ?? fallback;
  }

  function fmtDate(val: string | null): string {
    if (!val) return "—";
    return new Date(val).toLocaleString();
  }

  function fmtCost(val: string): string {
    return Number(val).toFixed(4);
  }

  const infoRow: React.CSSProperties = {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "0.5rem 0",
    borderBottom: "1px solid var(--color-border)",
    fontSize: "0.875rem",
  };

  const labelStyle: React.CSSProperties = {
    color: "var(--color-muted-foreground)",
    fontSize: "0.8rem",
    fontWeight: 500,
  };

  const valueStyle: React.CSSProperties = {
    fontFamily: "var(--font-mono)",
    fontSize: "0.875rem",
    fontWeight: 600,
  };

  return (
    <Modal title="Job Ticket Details" onClose={onClose} wide>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1.5rem" }}>
        {/* Left column */}
        <div>
          <div style={{ marginBottom: "1rem" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginBottom: "1rem" }}>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: "1.1rem", fontWeight: 700, letterSpacing: "0.05em" }}>
                {ticket.ticketNumber}
              </span>
              <Badge label={sc.label} bg={sc.bg} text={sc.text} />
            </div>
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: "0" }}>
            <div style={infoRow}>
              <span style={labelStyle}>Document</span>
              <span style={valueStyle}>{fmtLabel(ticket.docName)}</span>
            </div>
            <div style={infoRow}>
              <span style={labelStyle}>Copies</span>
              <span style={valueStyle}>{ticket.copies}</span>
            </div>
            <div style={infoRow}>
              <span style={labelStyle}>Copies Printed</span>
              <span style={valueStyle}>{ticket.copiesPrinted}</span>
            </div>
            <div style={infoRow}>
              <span style={labelStyle}>Printer</span>
              <span style={valueStyle}>{fmtLabel(ticket.printerName)}</span>
            </div>
            {ticket.printerRedirect && (
              <div style={infoRow}>
                <span style={labelStyle}>Redirected To</span>
                <span style={valueStyle}>{ticket.printerRedirect}</span>
              </div>
            )}
            <div style={infoRow}>
              <span style={labelStyle}>Total Cost</span>
              <span style={{ ...valueStyle, color: "oklch(0.72 0.18 145)" }}>
                {fmtCost(ticket.totalCost)}
              </span>
            </div>
          </div>
        </div>

        {/* Right column */}
        <div>
          <div style={{ display: "flex", flexDirection: "column", gap: "0" }}>
            <div style={infoRow}>
              <span style={labelStyle}>User ID</span>
              <span style={valueStyle}>{ticket.userId}</span>
            </div>
            <div style={infoRow}>
              <span style={labelStyle}>Label</span>
              <span style={valueStyle}>{fmtLabel(ticket.label)}</span>
            </div>
            <div style={infoRow}>
              <span style={labelStyle}>Domain</span>
              <span style={valueStyle}>{fmtLabel(ticket.domain)}</span>
            </div>
            <div style={infoRow}>
              <span style={labelStyle}>Use</span>
              <span style={valueStyle}>{fmtLabel(ticket.use)}</span>
            </div>
            <div style={infoRow}>
              <span style={labelStyle}>Tag</span>
              <span style={valueStyle}>{fmtLabel(ticket.tag)}</span>
            </div>
            <div style={infoRow}>
              <span style={labelStyle}>Reopened</span>
              <span style={valueStyle}>{ticket.isReopened ? "Yes" : "No"}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Timestamps */}
      <div style={{
        marginTop: "1.5rem",
        padding: "1rem",
        background: "oklch(0.20 0.02 250)",
        borderRadius: "var(--radius-md)",
        display: "grid",
        gridTemplateColumns: "repeat(3, 1fr)",
        gap: "1rem",
      }}>
        <div>
          <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem", fontWeight: 500, textTransform: "uppercase", letterSpacing: "0.05em" }}>
            Submitted
          </div>
          <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.8rem" }}>
            {fmtDate(ticket.submitTime)}
          </div>
        </div>
        <div>
          <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem", fontWeight: 500, textTransform: "uppercase", letterSpacing: "0.05em" }}>
            Delivery
          </div>
          <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.8rem" }}>
            {fmtDate(ticket.deliveryTime)}
          </div>
        </div>
        <div>
          <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem", fontWeight: 500, textTransform: "uppercase", letterSpacing: "0.05em" }}>
            Created
          </div>
          <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.8rem" }}>
            {fmtDate(ticket.createdAt)}
          </div>
        </div>
      </div>
    </Modal>
  );
}

// ─── Export CSV ────────────────────────────────────────────────────────────────

function exportToCSV(tickets: JobTicket[]) {
  const headers = ["Ticket #", "User ID", "Doc Name", "Copies", "Copies Printed", "Printer", "Status", "Total Cost", "Submit Time", "Delivery Time", "Label", "Domain", "Use", "Tag"];
  const rows = tickets.map((t) => [
    t.ticketNumber,
    t.userId,
    t.docName,
    t.copies,
    t.copiesPrinted,
    t.printerName,
    t.status,
    t.totalCost,
    t.submitTime,
    t.deliveryTime,
    t.label ?? "",
    t.domain ?? "",
    t.use ?? "",
    t.tag ?? "",
  ]);
  const csv = [headers, ...rows]
    .map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(","))
    .join("\n");
  const blob = new Blob([csv], { type: "text/csv" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `job-tickets-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function AdminJobTicketsPage() {
  const router = useRouter();

  // Auth check
  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => {
    if (!token) router.push("/login");
  }, [router, token]);

  // State
  const [tickets, setTickets] = useState<JobTicket[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [summary, setSummary] = useState<JobTicketSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingSummary, setLoadingSummary] = useState(true);

  // Filters
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  // Tabs
  const [activeTab, setActiveTab] = useState<"all" | "print" | "copy">("all");

  // Selection
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // Actions
  const [detailTicket, setDetailTicket] = useState<JobTicket | null>(null);
  const [confirmTarget, setConfirmTarget] = useState<{ ticket: JobTicket; action: "cancel" | "delete" } | null>(null);
  const [bulkCancelConfirm, setBulkCancelConfirm] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  // Toast
  const [toast, setToast] = useState<{ message: string; type: "success" | "error" | "info" } | null>(null);

  // SSE for real-time updates
  const sseRef = useRef<EventSource | null>(null);
  const [sseStatus, setSseStatus] = useState<"connecting" | "connected" | "disconnected">("disconnected");

  function showToast(msg: string, type: "success" | "error" | "info" = "info") {
    setToast({ message: msg, type });
  }

  useEffect(() => {
    if (toast) {
      const t = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(t);
    }
  }, [toast]);

  // Fetch summary
  const fetchSummary = useCallback(async () => {
    try {
      const data = await jobTicketsApi.summary();
      setSummary(data);
    } catch {
      setSummary(null);
    } finally {
      setLoadingSummary(false);
    }
  }, []);

  // Fetch tickets
  const fetchTickets = useCallback(async () => {
    setLoading(true);
    try {
      let status = statusFilter || undefined;
      if (activeTab === "print") status = "PRINTING";
      else if (activeTab === "copy") status = "PENDING";

      const data = await jobTicketsApi.list({
        page,
        limit: PAGE_SIZE,
        search: search || undefined,
        status,
      });
      setTickets(data.data);
      setTotal(data.total);
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter, activeTab]);

  useEffect(() => { fetchSummary(); }, [fetchSummary]);
  useEffect(() => { fetchTickets(); }, [fetchTickets]);
  useEffect(() => { setPage(1); }, [search, statusFilter, activeTab]);

  // SSE real-time updates
  useEffect(() => {
    if (!token) return;

    function connectSSE() {
      const apiUrl = process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001";
      const es = new EventSource(`${apiUrl}/api/v1/events/stream?token=${encodeURIComponent(token)}`);
      setSseStatus("connecting");

      es.addEventListener("open", () => setSseStatus("connected"));

      es.addEventListener("printjob", (e: MessageEvent) => {
        try {
          const event = JSON.parse(e.data);
          if (event.type === "PRINT_STARTED" || event.type === "PRINT_COMPLETED" || event.type === "PRINT_CANCELLED" || event.type === "PRINT_FAILED") {
            fetchTickets();
            fetchSummary();
          }
        } catch {}
      });

      es.addEventListener("error", () => {
        setSseStatus("disconnected");
        es.close();
        // Reconnect after 5s
        setTimeout(connectSSE, 5000);
      });

      sseRef.current = es;
    }

    connectSSE();
    return () => {
      sseRef.current?.close();
      sseRef.current = null;
      setSseStatus("disconnected");
    };
  }, [token, fetchTickets, fetchSummary]);

  // Selection helpers
  function toggleSelect(uuid: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(uuid)) next.delete(uuid);
      else next.add(uuid);
      return next;
    });
  }

  function toggleSelectAll() {
    if (selectedIds.size === tickets.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(tickets.map((t) => t.uuid)));
    }
  }

  // Action handlers
  async function handleAction(ticket: JobTicket, action: "print" | "settle" | "cancel" | "complete" | "reopen" | "delete") {
    setActionLoading(true);
    try {
      if (action === "print") {
        const result = await jobTicketsApi.print(ticket.uuid);
        showToast(result.message, "success");
      } else if (action === "settle") {
        const result = await jobTicketsApi.settle(ticket.uuid);
        showToast(result.message, "success");
      } else if (action === "cancel") {
        const result = await jobTicketsApi.cancel(ticket.uuid);
        showToast(result.message, "success");
        setConfirmTarget(null);
      } else if (action === "complete") {
        const result = await jobTicketsApi.complete(ticket.uuid);
        showToast(result.message, "success");
      } else if (action === "reopen") {
        const result = await jobTicketsApi.reopen(ticket.uuid);
        showToast(result.message, "success");
      } else if (action === "delete") {
        const result = await jobTicketsApi.delete(ticket.uuid);
        showToast(result.message, "success");
        setConfirmTarget(null);
      }
      fetchTickets();
      fetchSummary();
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleBulkCancel() {
    if (selectedIds.size === 0) return;
    setActionLoading(true);
    try {
      const promises = Array.from(selectedIds).map((uuid) =>
        jobTicketsApi.cancel(uuid).catch((e) => ({ error: e.message, uuid }))
      );
      const results = await Promise.allSettled(promises);
      const succeeded = results.filter((r) => r.status === "fulfilled" && !(r.value as any).error).length;
      const failed = results.length - succeeded;
      showToast(
        failed > 0
          ? `Cancelled ${succeeded} ticket(s), ${failed} failed`
          : `Cancelled ${succeeded} ticket(s)`,
        failed > 0 ? "error" : "success"
      );
      setSelectedIds(new Set());
      setBulkCancelConfirm(false);
      fetchTickets();
      fetchSummary();
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setActionLoading(false);
    }
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);

  const TABS = [
    { id: "all" as const, label: "All Tickets", icon: List },
    { id: "print" as const, label: "Print Queue", icon: Printer },
    { id: "copy" as const, label: "Copy Queue", icon: Copy },
  ];

  const actionIconStyle: React.CSSProperties = {
    padding: "0.375rem 0.5rem",
    background: "transparent",
    border: "1px solid var(--color-border)",
    borderRadius: "var(--radius-md)",
    cursor: "pointer",
    fontSize: "0.8rem",
    display: "inline-flex",
    alignItems: "center",
    gap: "4px",
  };

  return (
    <div>
      {/* Header */}
      <div style={{ marginBottom: "1.5rem", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div>
          <h1 style={{ fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em" }}>Job Tickets</h1>
          <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem", marginTop: "0.25rem" }}>
            Manage print and copy job tickets across all users
          </p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          {/* SSE status indicator */}
          <div style={{ display: "flex", alignItems: "center", gap: "0.375rem", fontSize: "0.75rem", color: "var(--color-muted-foreground)" }}>
            <div style={{
              width: "8px", height: "8px", borderRadius: "50%",
              background: sseStatus === "connected" ? "oklch(0.72 0.18 145)" : sseStatus === "connecting" ? "oklch(0.75 0.15 85)" : "oklch(0.65 0.22 25)",
            }} />
            {sseStatus === "connected" ? "Live" : sseStatus === "connecting" ? "Connecting..." : "Offline"}
          </div>
          <button
            onClick={() => { fetchTickets(); fetchSummary(); }}
            style={btnStyle("ghost")}
            title="Refresh"
          >
            <RefreshCw size={14} />
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "1rem", marginBottom: "1.5rem" }}>
        {loadingSummary ? (
          <>
            {[1, 2, 3, 4].map((i) => (
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
              label="Pending Tickets"
              value={summary.totalPending}
              sublabel="Awaiting processing"
              color="oklch(0.75 0.15 85)"
            />
            <StatCard
              icon={Printer}
              label="Print Jobs"
              value={summary.printJobs}
              sublabel="In print queue"
              color="oklch(0.72 0.18 250)"
            />
            <StatCard
              icon={Copy}
              label="Copy Jobs"
              value={summary.copyJobs}
              sublabel="In copy queue"
              color="oklch(0.72 0.18 280)"
            />
            <StatCard
              icon={DollarSign}
              label="Total Cost"
              value={Number(summary.totalCost).toFixed(4)}
              sublabel="All completed jobs"
              color="oklch(0.72 0.18 145)"
            />
          </>
        ) : (
          <div style={{ gridColumn: "1 / -1", padding: "2rem", textAlign: "center", color: "var(--color-muted-foreground)", background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)" }}>
            No job ticket data available yet
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

      {/* Filter Bar */}
      <div style={{
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-lg)",
        padding: "1rem",
        marginBottom: "1rem",
      }}>
        <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap", alignItems: "center" }}>
          {/* Search */}
          <div style={{ position: "relative", flex: "1", minWidth: "200px" }}>
            <Search size={16} style={{ position: "absolute", left: "0.75rem", top: "50%", transform: "translateY(-50%)", color: "var(--color-muted-foreground)" }} />
            <input
              type="text"
              placeholder="Search by ticket number or doc name..."
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
            <option value="PENDING">Pending</option>
            <option value="PRINTING">Printing</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>

          {/* Date from */}
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
          <span style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem" }}>to</span>
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

          {/* Clear filters */}
          {(search || statusFilter || dateFrom || dateTo) && (
            <button
              onClick={() => { setSearch(""); setStatusFilter(""); setDateFrom(""); setDateTo(""); }}
              style={{ ...btnStyle("ghost"), fontSize: "0.8rem" }}
            >
              <X size={14} /> Clear
            </button>
          )}

          {/* Export */}
          <button
            onClick={() => exportToCSV(tickets)}
            disabled={tickets.length === 0}
            style={{ ...btnStyle("ghost"), opacity: tickets.length === 0 ? 0.5 : 1 }}
          >
            <Download size={14} /> Export CSV
          </button>
        </div>

        {/* Bulk actions */}
        {selectedIds.size > 0 && (
          <div style={{
            marginTop: "0.75rem",
            paddingTop: "0.75rem",
            borderTop: "1px solid var(--color-border)",
            display: "flex",
            alignItems: "center",
            gap: "0.75rem",
            flexWrap: "wrap",
          }}>
            <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>
              {selectedIds.size} selected
            </span>
            <button
              onClick={() => setBulkCancelConfirm(true)}
              style={btnStyle("danger")}
            >
              <XCircle size={14} /> Cancel Selected
            </button>
            <button
              onClick={() => setSelectedIds(new Set())}
              style={btnStyle("ghost")}
            >
              Clear selection
            </button>
          </div>
        )}
      </div>

      {/* Table */}
      <div style={{ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)", overflow: "hidden" }}>
        {loading ? (
          <SkeletonTable rows={6} cols={10} />
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "900px" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                  <th style={{
                    padding: "0.75rem 0.5rem",
                    width: "40px",
                    background: "oklch(0.20 0.02 250)",
                  }}>
                    <button
                      onClick={toggleSelectAll}
                      style={{
                        background: "none",
                        border: "none",
                        cursor: "pointer",
                        color: "var(--color-muted-foreground)",
                        display: "flex",
                        alignItems: "center",
                        padding: "2px",
                      }}
                    >
                      {selectedIds.size === tickets.length && tickets.length > 0
                        ? <CheckSquare size={14} style={{ color: "var(--color-primary)" }} />
                        : <Square size={14} />
                      }
                    </button>
                  </th>
                  {["Ticket #", "User", "Doc Name", "Copies", "Printer", "Status", "Cost", "Submit Time", "Actions"].map((h) => (
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
                    <td colSpan={10} style={{ padding: "3rem", textAlign: "center", color: "var(--color-muted-foreground)" }}>
                      No job tickets found
                    </td>
                  </tr>
                ) : (
                  tickets.map((ticket) => {
                    const sc = STATUS_COLORS[ticket.status] ?? { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)", label: ticket.status };
                    const isSelected = selectedIds.has(ticket.uuid);
                    return (
                      <tr
                        key={ticket.uuid}
                        style={{
                          borderBottom: "1px solid var(--color-border)",
                          background: isSelected ? "oklch(0.72 0.18 250 / 0.04)" : undefined,
                        }}
                      >
                        <td style={{ padding: "0.75rem 0.5rem", textAlign: "center" }}>
                          <button
                            onClick={() => toggleSelect(ticket.uuid)}
                            style={{
                              background: "none",
                              border: "none",
                              cursor: "pointer",
                              color: "var(--color-muted-foreground)",
                              display: "flex",
                              alignItems: "center",
                              padding: "2px",
                            }}
                          >
                            {isSelected
                              ? <CheckSquare size={14} style={{ color: "var(--color-primary)" }} />
                              : <Square size={14} />
                            }
                          </button>
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.8rem", fontWeight: 700, letterSpacing: "0.03em", color: "var(--color-primary)" }}>
                            {ticket.ticketNumber}
                          </span>
                        </td>
                        <td style={{ padding: "0.75rem 1rem", fontSize: "0.875rem" }}>
                          User #{ticket.userId}
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div style={{ display: "flex", alignItems: "center", gap: "0.375rem" }}>
                            <FileText size={12} style={{ color: "var(--color-muted-foreground)", flexShrink: 0 }} />
                            <span style={{ fontSize: "0.875rem", maxWidth: "160px", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={ticket.docName}>
                              {ticket.docName}
                            </span>
                          </div>
                        </td>
                        <td style={{ padding: "0.75rem 1rem", fontFamily: "var(--font-mono)", fontSize: "0.875rem", textAlign: "center" }}>
                          {ticket.copies}
                        </td>
                        <td style={{ padding: "0.75rem 1rem", fontSize: "0.8rem", color: "var(--color-muted-foreground)", maxWidth: "120px", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={ticket.printerName}>
                          {ticket.printerName}
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <Badge label={sc.label} bg={sc.bg} text={sc.text} />
                        </td>
                        <td style={{ padding: "0.75rem 1rem", fontFamily: "var(--font-mono)", fontSize: "0.875rem", color: "oklch(0.72 0.18 145)" }}>
                          {Number(ticket.totalCost).toFixed(4)}
                        </td>
                        <td style={{ padding: "0.75rem 1rem", fontSize: "0.75rem", color: "var(--color-muted-foreground)", whiteSpace: "nowrap" }}>
                          {ticket.submitTime ? new Date(ticket.submitTime).toLocaleDateString() + " " + new Date(ticket.submitTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"}
                        </td>
                        <td style={{ padding: "0.75rem 1rem" }}>
                          <div style={{ display: "flex", gap: "0.25rem" }}>
                            {/* View */}
                            <button
                              onClick={() => setDetailTicket(ticket)}
                              title="View details"
                              style={{ ...actionIconStyle, color: "var(--color-muted-foreground)" }}
                            >
                              <Eye size={12} />
                            </button>

                            {/* Print (only for PENDING) */}
                            {ticket.status === "PENDING" && (
                              <button
                                onClick={() => handleAction(ticket, "print")}
                                disabled={actionLoading}
                                title="Print"
                                style={{ ...actionIconStyle, color: "oklch(0.72 0.18 250)", opacity: actionLoading ? 0.5 : 1 }}
                              >
                                <Printer size={12} />
                              </button>
                            )}

                            {/* Settle (only for PRINTING) */}
                            {ticket.status === "PRINTING" && (
                              <button
                                onClick={() => handleAction(ticket, "settle")}
                                disabled={actionLoading}
                                title="Settle"
                                style={{ ...actionIconStyle, color: "oklch(0.72 0.18 145)", opacity: actionLoading ? 0.5 : 1 }}
                              >
                                <CheckCircle size={12} />
                              </button>
                            )}

                            {/* Complete (only for PRINTING) */}
                            {ticket.status === "PRINTING" && (
                              <button
                                onClick={() => handleAction(ticket, "complete")}
                                disabled={actionLoading}
                                title="Mark complete"
                                style={{ ...actionIconStyle, color: "oklch(0.72 0.18 145)", opacity: actionLoading ? 0.5 : 1 }}
                              >
                                <Zap size={12} />
                              </button>
                            )}

                            {/* Cancel (PENDING or PRINTING) */}
                            {(ticket.status === "PENDING" || ticket.status === "PRINTING") && (
                              <button
                                onClick={() => setConfirmTarget({ ticket, action: "cancel" })}
                                title="Cancel"
                                style={{ ...actionIconStyle, color: "oklch(0.75 0.15 85)" }}
                              >
                                <XCircle size={12} />
                              </button>
                            )}

                            {/* Reopen (COMPLETED or CANCELLED) */}
                            {(ticket.status === "COMPLETED" || ticket.status === "CANCELLED") && (
                              <button
                                onClick={() => handleAction(ticket, "reopen")}
                                disabled={actionLoading}
                                title="Reopen"
                                style={{ ...actionIconStyle, color: "oklch(0.72 0.18 280)", opacity: actionLoading ? 0.5 : 1 }}
                              >
                                <RotateCcw size={12} />
                              </button>
                            )}

                            {/* Delete (CANCELLED only) */}
                            {ticket.status === "CANCELLED" && (
                              <button
                                onClick={() => setConfirmTarget({ ticket, action: "delete" })}
                                title="Delete"
                                style={{ ...actionIconStyle, color: "oklch(0.65 0.22 25)" }}
                              >
                                <Trash2 size={12} />
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

      {/* Detail Modal */}
      {detailTicket && (
        <DetailModal
          ticket={detailTicket}
          onClose={() => setDetailTicket(null)}
          onToast={showToast}
        />
      )}

      {/* Confirm Cancel / Delete */}
      {confirmTarget && (
        <ConfirmModal
          title={confirmTarget.action === "cancel" ? "Cancel Ticket" : "Delete Ticket"}
          message={
            confirmTarget.action === "cancel"
              ? `Are you sure you want to cancel ticket "${confirmTarget.ticket.ticketNumber}"? This action cannot be undone.`
              : `Are you sure you want to permanently delete ticket "${confirmTarget.ticket.ticketNumber}"? This action cannot be undone.`
          }
          onConfirm={() => handleAction(confirmTarget.ticket, confirmTarget.action)}
          onCancel={() => setConfirmTarget(null)}
        />
      )}

      {/* Bulk Cancel Confirm */}
      {bulkCancelConfirm && (
        <ConfirmModal
          title="Cancel Selected Tickets"
          message={`Are you sure you want to cancel ${selectedIds.size} selected ticket(s)? This action cannot be undone.`}
          onConfirm={handleBulkCancel}
          onCancel={() => setBulkCancelConfirm(false)}
        />
      )}

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
