"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  Printer,
  CreditCard,
  LogOut,
  Upload,
  Clock,
  ChevronRight,
  CheckCircle,
  AlertCircle,
  FileText,
  X,
  Loader2,
  RefreshCw,
  Trash2,
  ExternalLink,
} from "lucide-react";
import {
  documentsApi,
  printersApi,
  accountsApi,
  uploadApi,
  type DocIn,
  type Printer as PrinterType,
} from "@/lib/api";

const API_URL = process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001";

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  PENDING: { bg: "oklch(0.75 0.15 85 / 0.12)", text: "oklch(0.75 0.15 85)" },
  PROCESSING: {
    bg: "oklch(0.72 0.18 250 / 0.12)",
    text: "oklch(0.72 0.18 250)",
  },
  READY: { bg: "oklch(0.72 0.18 145 / 0.12)", text: "oklch(0.72 0.18 145)" },
  RELEASED: { bg: "oklch(0.55 0.18 280 / 0.12)", text: "oklch(0.55 0.18 280)" },
  CANCELLED: {
    bg: "oklch(0.55 0.05 250 / 0.12)",
    text: "oklch(0.55 0.05 250)",
  },
  ERROR: { bg: "oklch(0.65 0.22 25 / 0.12)", text: "oklch(0.65 0.22 25)" },
};
const DEFAULT_COLOR = {
  bg: "oklch(0.75 0.15 85 / 0.12)",
  text: "oklch(0.75 0.15 85)",
};

function formatSize(bytes: number): string {
  if (!bytes) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString();
}

// ─── Upload Modal ───────────────────────────────────────────────────────────────

function UploadModal({
  onClose,
  onUploaded,
}: {
  onClose: () => void;
  onUploaded: () => void;
}) {
  const [dragging, setDragging] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [progress, setProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setDragging(false);
    const dropped = e.dataTransfer.files[0];
    if (dropped) setFile(dropped);
  }

  async function handleUpload() {
    if (!file) return;
    setError("");
    setUploading(true);
    setProgress(0);
    try {
      await uploadApi.upload(file, (pct) => setProgress(pct));
      onUploaded();
      onClose();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setUploading(false);
    }
  }

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
      onClick={onClose}
    >
      <div
        style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-xl)",
          padding: "1.5rem",
          maxWidth: "480px",
          width: "90%",
          boxShadow: "var(--shadow-lg)",
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
          <h2 style={{ fontSize: "1.1rem", fontWeight: 700 }}>
            Upload Document
          </h2>
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
            <X size={20} />
          </button>
        </div>

        {!file ? (
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={handleDrop}
            onClick={() => fileRef.current?.click()}
            style={{
              border: `2px dashed ${dragging ? "var(--color-primary)" : "var(--color-border)"}`,
              borderRadius: "var(--radius-lg)",
              padding: "3rem 2rem",
              textAlign: "center",
              cursor: "pointer",
              transition: "border-color 0.15s, background 0.15s",
              background: dragging
                ? "oklch(0.72 0.18 250 / 0.05)"
                : "transparent",
            }}
          >
            <Upload
              size={32}
              style={{
                color: "var(--color-muted-foreground)",
                margin: "0 auto 0.75rem",
              }}
            />
            <p style={{ fontWeight: 600, marginBottom: "0.375rem" }}>
              Drag & drop your file here
            </p>
            <p
              style={{
                fontSize: "0.85rem",
                color: "var(--color-muted-foreground)",
                marginBottom: "0.75rem",
              }}
            >
              or click to browse
            </p>
            <p
              style={{
                fontSize: "0.75rem",
                color: "var(--color-muted-foreground)",
              }}
            >
              PDF, DOC, DOCX, PPT, PPTX, TXT
            </p>
            <input
              ref={fileRef}
              type="file"
              accept=".pdf,.doc,.docx,.ppt,.pptx,.txt"
              style={{ display: "none" }}
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) setFile(f);
              }}
            />
          </div>
        ) : (
          <div>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.75rem",
                padding: "0.875rem",
                background: "oklch(0.72 0.18 250 / 0.05)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                marginBottom: "1rem",
              }}
            >
              <FileText
                size={20}
                style={{ color: "var(--color-primary)", flexShrink: 0 }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    fontWeight: 600,
                    fontSize: "0.875rem",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                  }}
                >
                  {file.name}
                </div>
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "var(--color-muted-foreground)",
                  }}
                >
                  {formatSize(file.size)}
                </div>
              </div>
              <button
                onClick={() => setFile(null)}
                style={{
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  color: "var(--color-muted-foreground)",
                  flexShrink: 0,
                }}
              >
                <X size={16} />
              </button>
            </div>

            {uploading && (
              <div style={{ marginBottom: "1rem" }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    marginBottom: "0.375rem",
                    fontSize: "0.8rem",
                  }}
                >
                  <span>Uploading...</span>
                  <span style={{ color: "var(--color-muted-foreground)" }}>
                    {progress}%
                  </span>
                </div>
                <div
                  style={{
                    height: "4px",
                    background: "var(--color-border)",
                    borderRadius: "999px",
                    overflow: "hidden",
                  }}
                >
                  <div
                    style={{
                      height: "100%",
                      width: `${progress}%`,
                      background: "var(--color-primary)",
                      transition: "width 0.2s",
                      borderRadius: "999px",
                    }}
                  />
                </div>
              </div>
            )}

            {error && (
              <div
                style={{
                  padding: "0.625rem",
                  background: "oklch(0.65 0.22 25 / 0.1)",
                  border: "1px solid oklch(0.65 0.22 25 / 0.3)",
                  borderRadius: "var(--radius-md)",
                  color: "oklch(0.65 0.22 25)",
                  fontSize: "0.85rem",
                  marginBottom: "1rem",
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                }}
              >
                <AlertCircle size={14} /> {error}
              </div>
            )}

            <button
              onClick={handleUpload}
              disabled={uploading}
              style={{
                width: "100%",
                padding: "0.75rem",
                background: uploading
                  ? "oklch(0.72 0.18 250 / 0.5)"
                  : "var(--color-primary)",
                color: "var(--color-primary-foreground)",
                border: "none",
                borderRadius: "var(--radius-md)",
                fontWeight: 600,
                fontSize: "0.9rem",
                cursor: uploading ? "not-allowed" : "pointer",
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "0.5rem",
              }}
            >
              {uploading ? (
                <Loader2
                  size={16}
                  style={{ animation: "spin 1s linear infinite" }}
                />
              ) : (
                <Upload size={16} />
              )}
              {uploading ? "Uploading..." : "Upload & Process"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Print Release Modal ────────────────────────────────────────────────────────

function PrintReleaseModal({
  doc,
  onClose,
  onReleased,
}: {
  doc: DocIn;
  onClose: () => void;
  onReleased: () => void;
}) {
  const [printers, setPrinters] = useState<PrinterType[]>([]);
  const [selectedPrinter, setSelectedPrinter] = useState<PrinterType | null>(
    null,
  );
  const [options, setOptions] = useState({
    copies: 1,
    duplex: "NONE",
    colorMode: "AUTO",
    nUp: "1",
    paperSize: "A4",
    ecoPrint: false,
  });
  const [releasing, setReleasing] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    printersApi
      .list()
      .then((p) => setPrinters(p))
      .catch(() => {});
  }, []);

  const estimatedCost = selectedPrinter
    ? options.copies *
      (options.colorMode === "MONOCHROME"
        ? Number(selectedPrinter.costPerPageMono) * (doc.pageCount || 1)
        : Number(selectedPrinter.costPerPageColor) * (doc.pageCount || 1))
    : 0;

  async function handleRelease() {
    if (!selectedPrinter) {
      setError("Please select a printer");
      return;
    }
    setError("");
    setReleasing(true);
    try {
      const token = localStorage.getItem("printflow_token");
      const res = await fetch(`${API_URL}/api/v1/documents/${doc.id}/release`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ printerId: selectedPrinter.id, ...options }),
      });
      const data = await res.json();
      if (!data.success)
        throw new Error(data.error?.message ?? "Release failed");
      setSuccess(true);
      setTimeout(() => {
        onReleased();
        onClose();
      }, 1500);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setReleasing(false);
    }
  }

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
      onClick={onClose}
    >
      <div
        style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-xl)",
          padding: "1.5rem",
          maxWidth: "580px",
          width: "90%",
          boxShadow: "var(--shadow-lg)",
          maxHeight: "90vh",
          overflowY: "auto",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {success ? (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <CheckCircle
              size={48}
              style={{ color: "oklch(0.72 0.18 145)", margin: "0 auto 1rem" }}
            />
            <h2
              style={{
                fontSize: "1.2rem",
                fontWeight: 700,
                marginBottom: "0.5rem",
              }}
            >
              Print Job Sent!
            </h2>
            <p
              style={{
                color: "var(--color-muted-foreground)",
                fontSize: "0.9rem",
              }}
            >
              Your document is now printing at{" "}
              <strong>{selectedPrinter?.displayName}</strong>
            </p>
          </div>
        ) : (
          <>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                marginBottom: "1.25rem",
              }}
            >
              <h2 style={{ fontSize: "1.1rem", fontWeight: 700 }}>
                Release Print Job
              </h2>
              <button
                onClick={onClose}
                style={{
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  color: "var(--color-muted-foreground)",
                  padding: "4px",
                }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Document info */}
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.75rem",
                padding: "0.875rem",
                background: "oklch(0.22 0.02 250)",
                border: "1px solid oklch(0.72 0.18 250 / 0.2)",
                borderRadius: "var(--radius-md)",
                marginBottom: "1.25rem",
              }}
            >
              <FileText size={20} style={{ color: "var(--color-primary)" }} />
              <div>
                <div style={{ fontWeight: 600, fontSize: "0.875rem" }}>
                  {doc.docName}
                </div>
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "var(--color-muted-foreground)",
                  }}
                >
                  {doc.pageCount} pages · {formatSize(doc.fileSize)}
                </div>
              </div>
            </div>

            {/* Printer selection */}
            <div style={{ marginBottom: "1.25rem" }}>
              <label
                style={{
                  display: "block",
                  fontSize: "0.875rem",
                  fontWeight: 500,
                  marginBottom: "0.5rem",
                }}
              >
                Select Printer
              </label>
              {printers.length === 0 ? (
                <div
                  style={{
                    padding: "1rem",
                    textAlign: "center",
                    color: "var(--color-muted-foreground)",
                    fontSize: "0.875rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                  }}
                >
                  No printers available
                </div>
              ) : (
                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.5rem",
                    maxHeight: "200px",
                    overflowY: "auto",
                  }}
                >
                  {printers
                    .filter((p) => p.isEnabled)
                    .map((printer) => (
                      <button
                        key={printer.id}
                        onClick={() => setSelectedPrinter(printer)}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.75rem",
                          padding: "0.75rem 1rem",
                          background:
                            selectedPrinter?.id === printer.id
                              ? "oklch(0.72 0.18 250 / 0.1)"
                              : "var(--color-input)",
                          border: `1px solid ${selectedPrinter?.id === printer.id ? "var(--color-primary)" : "var(--color-border)"}`,
                          borderRadius: "var(--radius-md)",
                          cursor: "pointer",
                          textAlign: "left",
                          transition: "all 0.15s",
                          width: "100%",
                        }}
                      >
                        <Printer
                          size={18}
                          style={{
                            color:
                              selectedPrinter?.id === printer.id
                                ? "var(--color-primary)"
                                : "var(--color-muted-foreground)",
                            flexShrink: 0,
                          }}
                        />
                        <div style={{ flex: 1 }}>
                          <div
                            style={{ fontWeight: 600, fontSize: "0.875rem" }}
                          >
                            {printer.displayName || printer.name}
                          </div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "var(--color-muted-foreground)",
                            }}
                          >
                            {printer.printerStatus} · {printer.colorMode} · Mono
                            ${printer.costPerPageMono}/pg
                          </div>
                        </div>
                        {selectedPrinter?.id === printer.id && (
                          <CheckCircle
                            size={16}
                            style={{
                              color: "var(--color-primary)",
                              flexShrink: 0,
                            }}
                          />
                        )}
                      </button>
                    ))}
                </div>
              )}
            </div>

            {/* Print options */}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "0.875rem",
                marginBottom: "1.25rem",
              }}
            >
              {[
                {
                  label: "Copies",
                  key: "copies",
                  type: "number",
                  min: 1,
                  max: 10,
                },
                {
                  label: "Paper Size",
                  key: "paperSize",
                  type: "select",
                  options: ["A4", "A3", "Letter", "Legal"],
                },
                {
                  label: "Duplex",
                  key: "duplex",
                  type: "select",
                  options: ["NONE", "PORTRAIT", "LANDSCAPE"],
                },
                {
                  label: "Color Mode",
                  key: "colorMode",
                  type: "select",
                  options: ["AUTO", "MONOCHROME", "COLOR"],
                },
                {
                  label: "Pages per Sheet",
                  key: "nUp",
                  type: "select",
                  options: ["1", "2", "4"],
                },
              ].map(({ label, key, type, options: opts = [] }) => (
                <div key={key}>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.8rem",
                      color: "var(--color-muted-foreground)",
                      marginBottom: "0.25rem",
                    }}
                  >
                    {label}
                  </label>
                  {type === "number" ? (
                    <input
                      type="number"
                      min={1}
                      max={10}
                      value={(options as any)[key]}
                      onChange={(e) =>
                        setOptions((o) => ({
                          ...o,
                          [key]: parseInt(e.target.value),
                        }))
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
                  ) : (
                    <select
                      value={(options as any)[key]}
                      onChange={(e) =>
                        setOptions((o) => ({ ...o, [key]: e.target.value }))
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
                        cursor: "pointer",
                      }}
                    >
                      {opts.map((o: string) => (
                        <option key={o} value={o}>
                          {o}
                        </option>
                      ))}
                    </select>
                  )}
                </div>
              ))}
              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.8rem",
                    color: "var(--color-muted-foreground)",
                    marginBottom: "0.25rem",
                  }}
                >
                  Eco Print
                </label>
                <label
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    padding: "0.5rem 0.75rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                    cursor: "pointer",
                  }}
                >
                  <input
                    type="checkbox"
                    checked={options.ecoPrint}
                    onChange={(e) =>
                      setOptions((o) => ({ ...o, ecoPrint: e.target.checked }))
                    }
                    style={{ width: "16px", height: "16px" }}
                  />
                  <span style={{ fontSize: "0.875rem" }}>Eco Print</span>
                </label>
              </div>
            </div>

            {/* Cost estimate */}
            {selectedPrinter && (
              <div
                style={{
                  padding: "0.875rem",
                  background: "oklch(0.72 0.18 145 / 0.08)",
                  border: "1px solid oklch(0.72 0.18 145 / 0.2)",
                  borderRadius: "var(--radius-md)",
                  marginBottom: "1.25rem",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <span
                  style={{
                    fontSize: "0.875rem",
                    color: "var(--color-muted-foreground)",
                  }}
                >
                  Estimated cost
                </span>
                <span
                  style={{
                    fontFamily: "var(--font-mono)",
                    fontWeight: 700,
                    color: "oklch(0.72 0.18 145)",
                  }}
                >
                  {estimatedCost.toFixed(4)} credits
                </span>
              </div>
            )}

            {error && (
              <div
                style={{
                  padding: "0.625rem",
                  background: "oklch(0.65 0.22 25 / 0.1)",
                  border: "1px solid oklch(0.65 0.22 25 / 0.3)",
                  borderRadius: "var(--radius-md)",
                  color: "oklch(0.65 0.22 25)",
                  fontSize: "0.85rem",
                  marginBottom: "1rem",
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                }}
              >
                <AlertCircle size={14} /> {error}
              </div>
            )}

            <div style={{ display: "flex", gap: "0.75rem" }}>
              <button
                onClick={onClose}
                style={{
                  flex: 1,
                  padding: "0.75rem",
                  background: "var(--color-secondary)",
                  color: "var(--color-secondary-foreground)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  fontWeight: 600,
                  cursor: "pointer",
                }}
              >
                Cancel
              </button>
              <button
                onClick={handleRelease}
                disabled={!selectedPrinter || releasing}
                style={{
                  flex: 1,
                  padding: "0.75rem",
                  background:
                    !selectedPrinter || releasing
                      ? "oklch(0.72 0.18 145 / 0.4)"
                      : "oklch(0.72 0.18 145)",
                  color:
                    !selectedPrinter || releasing
                      ? "oklch(0.72 0.18 145)"
                      : "white",
                  border: "none",
                  borderRadius: "var(--radius-md)",
                  fontWeight: 600,
                  fontSize: "0.9rem",
                  cursor:
                    !selectedPrinter || releasing ? "not-allowed" : "pointer",
                  display: "inline-flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: "0.5rem",
                }}
              >
                {releasing && (
                  <Loader2
                    size={16}
                    style={{ animation: "spin 1s linear infinite" }}
                  />
                )}
                Release Print
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// ─── Main User Portal ──────────────────────────────────────────────────────────

export default function UserPortalPage() {
  const router = useRouter();
  const [user, setUser] = useState<any>(null);
  const [balance, setBalance] = useState<string>("0.0000");
  const [docs, setDocs] = useState<DocIn[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [showUpload, setShowUpload] = useState(false);
  const [releaseDoc, setReleaseDoc] = useState<DocIn | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("printflow_token");
    const userStr = localStorage.getItem("printflow_user");
    if (!token || !userStr) {
      router.push("/login");
      return;
    }
    try {
      setUser(JSON.parse(userStr));
    } catch {
      router.push("/login");
    }
  }, [router]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [docsData, balanceData] = await Promise.all([
        documentsApi.list({ limit: 20 }),
        user?.account?.id
          ? accountsApi.balance(user.account.id).catch(() => null)
          : Promise.resolve(null),
      ]);
      setDocs(docsData.data?.data ?? []);
      setTotal(docsData.data?.total ?? 0);
      if (balanceData) {
        setBalance((balanceData as any).balance ?? "0");
      }
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  async function handleLogout() {
    const token = localStorage.getItem("printflow_token");
    if (token) {
      await fetch(`${API_URL}/api/v1/auth/logout`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
    }
    localStorage.removeItem("printflow_token");
    localStorage.removeItem("printflow_user");
    router.push("/login");
  }

  async function handleDeleteDoc(id: number) {
    if (!confirm("Delete this document?")) return;
    try {
      await documentsApi.delete(id);
      fetchData();
    } catch (e: any) {
      setError(e.message);
    }
  }

  if (!user) return null;

  return (
    <div style={{ minHeight: "100vh", background: "var(--color-background)" }}>
      {/* Header */}
      <header
        style={{
          background: "oklch(0.14 0.02 250 / 0.8)",
          backdropFilter: "blur(12px)",
          borderBottom: "1px solid var(--color-border)",
          position: "sticky",
          top: 0,
          zIndex: 20,
        }}
      >
        <div
          style={{
            maxWidth: "1000px",
            margin: "0 auto",
            padding: "0 1.5rem",
            height: "64px",
            display: "flex",
            alignItems: "center",
            gap: "1.5rem",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.625rem",
              textDecoration: "none",
            }}
          >
            <div
              style={{
                width: "34px",
                height: "34px",
                borderRadius: "9px",
                background:
                  "linear-gradient(135deg, oklch(0.72 0.18 250), oklch(0.55 0.22 280))",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <Printer size={17} color="white" />
            </div>
            <span
              style={{
                fontWeight: 700,
                fontSize: "1.1rem",
                color: "var(--color-foreground)",
                letterSpacing: "-0.01em",
              }}
            >
              PrintFlow
            </span>
          </div>
          <div style={{ flex: 1 }} />
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              padding: "0.375rem 0.875rem",
              background: "oklch(0.72 0.18 145 / 0.1)",
              border: "1px solid oklch(0.72 0.18 145 / 0.2)",
              borderRadius: "999px",
            }}
          >
            <CreditCard size={14} style={{ color: "oklch(0.72 0.18 145)" }} />
            <span
              style={{
                fontSize: "0.875rem",
                fontWeight: 600,
                color: "oklch(0.72 0.18 145)",
              }}
            >
              {Number(balance).toFixed(4)} credits
            </span>
          </div>
          <div
            style={{ display: "flex", alignItems: "center", gap: "0.625rem" }}
          >
            <div
              style={{
                width: "32px",
                height: "32px",
                borderRadius: "50%",
                background: "oklch(0.72 0.18 250)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: "0.8rem",
                color: "white",
              }}
            >
              {user.userName?.[0]?.toUpperCase() ?? "U"}
            </div>
            <div>
              <div style={{ fontSize: "0.8rem", fontWeight: 600 }}>
                {user.fullName}
              </div>
              <div
                style={{
                  fontSize: "0.7rem",
                  color: "var(--color-muted-foreground)",
                }}
              >
                {user.userName}
              </div>
            </div>
          </div>
          <button
            onClick={handleLogout}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.375rem",
              padding: "0.375rem 0.75rem",
              background: "transparent",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-muted-foreground)",
              fontSize: "0.8rem",
              cursor: "pointer",
            }}
          >
            <LogOut size={14} /> Sign out
          </button>
          {user.roles?.includes("ADMIN") && (
            <Link
              href="/admin"
              style={{
                padding: "0.375rem 0.75rem",
                background: "var(--color-primary)",
                border: "none",
                borderRadius: "var(--radius-md)",
                color: "var(--color-primary-foreground)",
                fontSize: "0.8rem",
                fontWeight: 600,
                textDecoration: "none",
              }}
            >
              Admin
            </Link>
          )}
        </div>
      </header>

      <div
        style={{ maxWidth: "1000px", margin: "0 auto", padding: "2rem 1.5rem" }}
      >
        {/* Upload Hero */}
        <div
          className="animate-fade-in"
          style={{
            background:
              "linear-gradient(135deg, oklch(0.22 0.03 250), oklch(0.18 0.04 280))",
            border: "1px solid oklch(0.72 0.18 250 / 0.2)",
            borderRadius: "var(--radius-xl)",
            padding: "2.5rem",
            marginBottom: "2rem",
            textAlign: "center",
            position: "relative",
            overflow: "hidden",
          }}
        >
          <div
            style={{
              position: "absolute",
              top: "-60px",
              right: "-60px",
              width: "200px",
              height: "200px",
              borderRadius: "50%",
              border: "1px solid oklch(0.72 0.18 250 / 0.1)",
            }}
          />
          <div
            style={{
              position: "absolute",
              top: "-30px",
              right: "-30px",
              width: "140px",
              height: "140px",
              borderRadius: "50%",
              border: "1px solid oklch(0.72 0.18 250 / 0.15)",
            }}
          />
          <div style={{ position: "relative" }}>
            <div
              style={{
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                width: "56px",
                height: "56px",
                borderRadius: "16px",
                background: "oklch(0.72 0.18 250 / 0.15)",
                marginBottom: "1rem",
              }}
            >
              <Upload size={24} style={{ color: "var(--color-primary)" }} />
            </div>
            <h1
              style={{
                fontSize: "1.5rem",
                fontWeight: 700,
                marginBottom: "0.5rem",
              }}
            >
              Upload &amp; Print Securely
            </h1>
            <p
              style={{
                color: "var(--color-muted-foreground)",
                marginBottom: "1.5rem",
                maxWidth: "400px",
                margin: "0 auto 1.5rem",
              }}
            >
              Upload your documents, preview pages, choose your printer, and
              release when ready.
            </p>
            <button
              onClick={() => setShowUpload(true)}
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "0.5rem",
                padding: "0.75rem 1.5rem",
                background: "var(--color-primary)",
                color: "var(--color-primary-foreground)",
                border: "none",
                borderRadius: "var(--radius-md)",
                fontWeight: 600,
                fontSize: "0.9rem",
                cursor: "pointer",
              }}
            >
              <Upload size={16} /> Upload Document
            </button>
          </div>
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

        {/* SafePages */}
        <div>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              marginBottom: "1rem",
            }}
          >
            <h2 style={{ fontSize: "1.1rem", fontWeight: 600 }}>
              Your SafePages
            </h2>
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <span
                style={{
                  fontSize: "0.8rem",
                  color: "var(--color-muted-foreground)",
                }}
              >
                {total} documents
              </span>
              <button
                onClick={fetchData}
                disabled={loading}
                style={{
                  background: "none",
                  border: "none",
                  cursor: loading ? "not-allowed" : "pointer",
                  color: "var(--color-muted-foreground)",
                  padding: "4px",
                  display: "flex",
                  alignItems: "center",
                }}
              >
                <RefreshCw
                  size={14}
                  style={{
                    animation: loading ? "spin 1s linear infinite" : "none",
                  }}
                />
              </button>
            </div>
          </div>

          {loading ? (
            <div
              style={{
                textAlign: "center",
                padding: "3rem",
                color: "var(--color-muted-foreground)",
              }}
            >
              <Loader2
                size={24}
                style={{
                  animation: "spin 1s linear infinite",
                  margin: "0 auto",
                }}
              />
            </div>
          ) : docs.length === 0 ? (
            <div
              style={{
                textAlign: "center",
                padding: "3rem",
                background: "var(--color-card)",
                border: "1px dashed var(--color-border)",
                borderRadius: "var(--radius-lg)",
                color: "var(--color-muted-foreground)",
              }}
            >
              <FileText
                size={40}
                style={{ margin: "0 auto 1rem", opacity: 0.3 }}
              />
              <p>No documents yet. Upload your first file above.</p>
            </div>
          ) : (
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "0.75rem",
              }}
            >
              {docs.map((doc) => {
                const sc = STATUS_COLORS[doc.docStatus] ?? DEFAULT_COLOR;
                const canRelease = doc.docStatus === "READY";
                return (
                  <div
                    key={doc.id}
                    className="card-glow animate-fade-in"
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "1rem",
                      padding: "1rem 1.25rem",
                      background: "var(--color-card)",
                      border: "1px solid var(--color-border)",
                      borderRadius: "var(--radius-lg)",
                      transition: "border-color 0.15s",
                    }}
                  >
                    <div
                      style={{
                        width: "40px",
                        height: "40px",
                        borderRadius: "var(--radius-md)",
                        background: "oklch(0.22 0.02 250)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0,
                      }}
                    >
                      <FileText
                        size={20}
                        style={{ color: "var(--color-primary)" }}
                      />
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div
                        style={{
                          fontWeight: 600,
                          fontSize: "0.9rem",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {doc.docName}
                      </div>
                      <div
                        style={{
                          fontSize: "0.75rem",
                          color: "var(--color-muted-foreground)",
                          marginTop: "0.125rem",
                          display: "flex",
                          gap: "0.75rem",
                        }}
                      >
                        <span>{doc.pageCount || "—"} pages</span>
                        <span>{formatSize(doc.fileSize)}</span>
                        <span
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.25rem",
                          }}
                        >
                          <Clock size={11} /> {timeAgo(doc.dateCreated)}
                        </span>
                      </div>
                    </div>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.75rem",
                        flexShrink: 0,
                      }}
                    >
                      <span
                        style={{
                          padding: "0.25rem 0.625rem",
                          borderRadius: "999px",
                          fontSize: "0.7rem",
                          fontWeight: 600,
                          background: sc.bg,
                          color: sc.text,
                          whiteSpace: "nowrap",
                        }}
                      >
                        {doc.docStatus === "READY" && (
                          <CheckCircle
                            size={10}
                            style={{ display: "inline", marginRight: "3px" }}
                          />
                        )}
                        {doc.docStatus}
                      </span>
                      {canRelease && (
                        <button
                          onClick={() => setReleaseDoc(doc)}
                          style={{
                            padding: "0.375rem 0.875rem",
                            background: "oklch(0.72 0.18 145)",
                            color: "white",
                            border: "none",
                            borderRadius: "var(--radius-md)",
                            fontSize: "0.8rem",
                            fontWeight: 600,
                            cursor: "pointer",
                            whiteSpace: "nowrap",
                            display: "inline-flex",
                            alignItems: "center",
                            gap: "0.375rem",
                          }}
                        >
                          <Printer size={12} /> Release
                        </button>
                      )}
                      <button
                        onClick={() => handleDeleteDoc(doc.id)}
                        style={{
                          padding: "0.375rem",
                          background: "transparent",
                          border: "1px solid var(--color-border)",
                          borderRadius: "var(--radius-md)",
                          color: "var(--color-muted-foreground)",
                          cursor: "pointer",
                          display: "flex",
                          alignItems: "center",
                        }}
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {showUpload && (
        <UploadModal
          onClose={() => setShowUpload(false)}
          onUploaded={fetchData}
        />
      )}
      {releaseDoc && (
        <PrintReleaseModal
          doc={releaseDoc}
          onClose={() => setReleaseDoc(null)}
          onReleased={fetchData}
        />
      )}
    </div>
  );
}
