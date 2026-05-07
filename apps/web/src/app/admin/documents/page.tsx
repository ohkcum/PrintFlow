"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { FileText, Search, ChevronLeft, ChevronRight, Eye, Trash2, Clock, AlertCircle } from "lucide-react";
import { documentsApi, type DocIn } from "@/lib/api";
import { SkeletonTable } from "@/components/ui/Skeleton";

const PAGE_SIZE = 20;

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  PENDING:    { bg: "oklch(0.75 0.15 85 / 0.12)", text: "oklch(0.75 0.15 85)" },
  PROCESSING:  { bg: "oklch(0.72 0.18 250 / 0.12)", text: "oklch(0.72 0.18 250)" },
  READY:      { bg: "oklch(0.72 0.18 145 / 0.12)", text: "oklch(0.72 0.18 145)" },
  RELEASED:   { bg: "oklch(0.55 0.18 280 / 0.12)", text: "oklch(0.55 0.18 280)" },
  CANCELLED:  { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)" },
  EXPIRED:    { bg: "oklch(0.55 0.02 250 / 0.12)", text: "oklch(0.55 0.02 250)" },
  ERROR:      { bg: "oklch(0.65 0.22 25 / 0.12)", text: "oklch(0.65 0.22 25)" },
};
const DEFAULT_COLOR = { bg: "oklch(0.65 0.1 250 / 0.12)", text: "oklch(0.65 0.1 250)" };

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function AdminDocumentsPage() {
  const router = useRouter();
  const [docs, setDocs] = useState<DocIn[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => { if (!token) router.push("/login"); }, [router, token]);

  const fetchDocs = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await documentsApi.list({
        page,
        limit: PAGE_SIZE,
        search: search || undefined,
        status: statusFilter || undefined,
      });
      setDocs(data.data);
      setTotal(data.total);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter]);

  useEffect(() => { fetchDocs(); }, [fetchDocs]);
  useEffect(() => { setPage(1); }, [search, statusFilter]);

  async function handleDelete(id: number) {
    if (!confirm("Delete this document?")) return;
    try {
      await documentsApi.delete(id);
      fetchDocs();
    } catch (e: any) {
      setError(e.message);
    }
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      <div style={{ marginBottom: "1.5rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em" }}>Documents</h1>
        <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem", marginTop: "0.25rem" }}>
          All SafePages across the system
        </p>
      </div>

      <div style={{ display: "flex", gap: "0.75rem", marginBottom: "1rem", flexWrap: "wrap", alignItems: "center" }}>
        <div style={{ position: "relative", flex: "1", minWidth: "200px" }}>
          <Search size={16} style={{ position: "absolute", left: "0.75rem", top: "50%", transform: "translateY(-50%)", color: "var(--color-muted-foreground)" }} />
          <input
            type="text"
            placeholder="Search documents..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: "100%", padding: "0.5rem 0.75rem 0.5rem 2.25rem", background: "var(--color-input)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-foreground)", fontSize: "0.875rem" }}
          />
        </div>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}
          style={{ padding: "0.5rem 0.75rem", background: "var(--color-input)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-foreground)", fontSize: "0.875rem", outline: "none", cursor: "pointer" }}>
          <option value="">All Status</option>
          {["PENDING", "PROCESSING", "READY", "RELEASED", "CANCELLED", "EXPIRED", "ERROR"].map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        {loading === false && <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>{total} docs</span>}
      </div>

      {error && (
        <div style={{ padding: "0.75rem 1rem", background: "oklch(0.65 0.22 25 / 0.1)", border: "1px solid oklch(0.65 0.22 25 / 0.3)", borderRadius: "var(--radius-md)", color: "oklch(0.65 0.22 25)", fontSize: "0.875rem", marginBottom: "1rem", display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <AlertCircle size={16} /> {error}
        </div>
      )}

      <div style={{ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)", overflow: "hidden" }}>
        {loading ? (
          <SkeletonTable rows={6} cols={7} />
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "700px" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                  {["Document", "Owner", "Status", "Size", "Pages", "Created", "Actions"].map((h) => (
                    <th key={h} style={{ padding: "0.75rem 1rem", textAlign: "left", fontSize: "0.75rem", fontWeight: 600, color: "var(--color-muted-foreground)", textTransform: "uppercase", letterSpacing: "0.05em", background: "oklch(0.20 0.02 250)", whiteSpace: "nowrap" }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {docs.length === 0 ? (
                  <tr><td colSpan={7} style={{ padding: "3rem", textAlign: "center", color: "var(--color-muted-foreground)" }}>No documents found</td></tr>
                ) : (
                docs.map((doc) => {
                  const sc = STATUS_COLORS[doc.docStatus] ?? DEFAULT_COLOR;
                  return (
                    <tr key={doc.id} className="table-row-hover" style={{ borderBottom: "1px solid var(--color-border)" }}>
                      <td style={{ padding: "0.75rem 1rem" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: "0.625rem" }}>
                          <div style={{ width: "36px", height: "36px", borderRadius: "var(--radius-md)", background: "oklch(0.22 0.02 250)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                            <FileText size={18} style={{ color: "var(--color-primary)" }} />
                          </div>
                          <div>
                            <div style={{ fontWeight: 600, fontSize: "0.875rem", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", maxWidth: "240px" }}>{doc.docName}</div>
                            <div style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)", fontFamily: "var(--font-mono)" }}>{doc.docType}</div>
                          </div>
                        </div>
                      </td>
                      <td style={{ padding: "0.75rem 1rem" }}>
                        <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>{doc.userName}</div>
                      </td>
                      <td style={{ padding: "0.75rem 1rem" }}>
                        <span style={{ padding: "2px 8px", borderRadius: "999px", fontSize: "0.7rem", fontWeight: 600, background: sc.bg, color: sc.text }}>{doc.docStatus}</span>
                      </td>
                      <td style={{ padding: "0.75rem 1rem", fontFamily: "var(--font-mono)", fontSize: "0.85rem" }}>{formatSize(doc.fileSize)}</td>
                      <td style={{ padding: "0.75rem 1rem", fontFamily: "var(--font-mono)", fontSize: "0.85rem" }}>{doc.pageCount}</td>
                      <td style={{ padding: "0.75rem 1rem", fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>
                        {new Date(doc.dateCreated).toLocaleDateString()}
                      </td>
                      <td style={{ padding: "0.75rem 1rem" }}>
                        <button
                          onClick={() => handleDelete(doc.id)}
                          className="table-action-btn danger"
                          style={{ padding: "0.375rem 0.625rem", background: "transparent", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "oklch(0.65 0.22 25)", cursor: "pointer", fontSize: "0.8rem", display: "inline-flex", alignItems: "center", gap: "4px" }}>
                          <Trash2 size={12} /> Delete
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

        {!loading && docs.length > 0 && (
          <div style={{ padding: "0.75rem 1rem", borderTop: "1px solid var(--color-border)", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "0.5rem" }}>
            <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>
              {((page - 1) * PAGE_SIZE) + 1}–{Math.min(page * PAGE_SIZE, total)} of {total}
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
