"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight, Printer, User, Clock, CheckCircle, XCircle } from "lucide-react";
import { documentsApi } from "@/lib/api";
import { SkeletonTable } from "@/components/ui/Skeleton";

const PAGE_SIZE = 30;

export default function AdminAuditLogPage() {
  const router = useRouter();
  const [logs, setLogs] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => { if (!token) router.push("/login"); }, [router, token]);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await documentsApi.listLogs({ page, limit: PAGE_SIZE });
      setLogs(data.data ?? []);
      setTotal(data.total ?? 0);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { fetchLogs(); }, [fetchLogs]);

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      <div style={{ marginBottom: "1.5rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em" }}>Audit Log</h1>
        <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem", marginTop: "0.25rem" }}>
          Complete history of print jobs and document activity
        </p>
      </div>

      {error && (
        <div style={{ padding: "0.75rem 1rem", background: "oklch(0.65 0.22 25 / 0.1)", border: "1px solid oklch(0.65 0.22 25 / 0.3)", borderRadius: "var(--radius-md)", color: "oklch(0.65 0.22 25)", fontSize: "0.875rem", marginBottom: "1rem" }}>
          {error}
        </div>
      )}

      <div style={{ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)", overflow: "hidden" }}>
        {loading ? (
          <SkeletonTable rows={6} cols={8} />
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "900px" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                  {["Time", "User", "Document", "Printer", "Pages", "Cost", "Status", "Options"].map((h) => (
                    <th key={h} style={{ padding: "0.75rem 1rem", textAlign: "left", fontSize: "0.75rem", fontWeight: 600, color: "var(--color-muted-foreground)", textTransform: "uppercase", letterSpacing: "0.05em", background: "oklch(0.20 0.02 250)", whiteSpace: "nowrap" }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {logs.length === 0 ? (
                  <tr><td colSpan={8} style={{ padding: "3rem", textAlign: "center", color: "var(--color-muted-foreground)" }}>No audit logs yet</td></tr>
                ) : (
                logs.map((log) => (
                  <tr key={log.id} className="table-row-hover" style={{ borderBottom: "1px solid var(--color-border)" }}>
                    <td style={{ padding: "0.75rem 1rem", whiteSpace: "nowrap" }}>
                      <div style={{ fontSize: "0.8rem" }}>{new Date(log.dateCreated).toLocaleDateString()}</div>
                      <div style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)" }}>{new Date(log.dateCreated).toLocaleTimeString()}</div>
                    </td>
                    <td style={{ padding: "0.75rem 1rem" }}>
                      <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>{log.userName ?? "—"}</div>
                    </td>
                    <td style={{ padding: "0.75rem 1rem" }}>
                      <div style={{ fontSize: "0.875rem", maxWidth: "200px", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={log.docName}>
                        {log.docName ?? "—"}
                      </div>
                      {log.docPageCount && (
                        <div style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)" }}>{log.docPageCount} pages</div>
                      )}
                    </td>
                    <td style={{ padding: "0.75rem 1rem" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: "0.375rem", fontSize: "0.8rem" }}>
                        <Printer size={13} style={{ color: "var(--color-muted-foreground)" }} />
                        {log.printerName ?? "—"}
                      </div>
                    </td>
                    <td style={{ padding: "0.75rem 1rem", fontFamily: "var(--font-mono)", fontSize: "0.85rem" }}>
                      {log.pagesPrinted ?? log.sheetsPrinted ?? "—"}
                    </td>
                    <td style={{ padding: "0.75rem 1rem", fontFamily: "var(--font-mono)", fontSize: "0.85rem" }}>
                      {log.totalCost ? `${Number(log.totalCost).toFixed(4)}` : "—"}
                    </td>
                    <td style={{ padding: "0.75rem 1rem" }}>
                      <span style={{
                        padding: "2px 8px",
                        borderRadius: "999px",
                        fontSize: "0.7rem",
                        fontWeight: 600,
                        background: log.jobStatus === "COMPLETED" ? "oklch(0.72 0.18 145 / 0.12)" : log.jobStatus === "FAILED" ? "oklch(0.65 0.22 25 / 0.12)" : "oklch(0.75 0.15 85 / 0.12)",
                        color: log.jobStatus === "COMPLETED" ? "oklch(0.72 0.18 145)" : log.jobStatus === "FAILED" ? "oklch(0.65 0.22 25)" : "oklch(0.75 0.15 85)",
                      }}>
                        {log.jobStatus ?? "—"}
                      </span>
                    </td>
                    <td style={{ padding: "0.75rem 1rem", fontSize: "0.75rem", color: "var(--color-muted-foreground)" }}>
                      {[
                        log.duplex && log.duplex !== "NONE" ? `Duplex` : null,
                        log.colorMode && log.colorMode !== "AUTO" ? log.colorMode : null,
                        log.nUp !== "1" ? `${log.nUp}-up` : null,
                        log.ecoPrint ? "Eco" : null,
                        log.copyCount > 1 ? `×${log.copyCount}` : null,
                      ].filter(Boolean).join(", ") || "—"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          </div>
        )}

        {!loading && logs.length > 0 && (
          <div style={{ padding: "0.75rem 1rem", borderTop: "1px solid var(--color-border)", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "0.5rem" }}>
            <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>
              Showing {((page - 1) * PAGE_SIZE) + 1}–{Math.min(page * PAGE_SIZE, total)} of {total}
            </span>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page <= 1}
                style={{ padding: "0.375rem 0.75rem", background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: page <= 1 ? "var(--color-muted-foreground)" : "var(--color-foreground)", cursor: page <= 1 ? "not-allowed" : "pointer", fontSize: "0.8rem", display: "inline-flex", alignItems: "center", gap: "4px" }}>
                <ChevronLeft size={14} />
              </button>
              <button onClick={() => setPage((p) => Math.min(totalPages, p + 1))} disabled={page >= totalPages}
                style={{ padding: "0.375rem 0.75rem", background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: page >= totalPages ? "var(--color-muted-foreground)" : "var(--color-foreground)", cursor: page >= totalPages ? "not-allowed" : "pointer", fontSize: "0.8rem", display: "inline-flex", alignItems: "center", gap: "4px" }}>
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
