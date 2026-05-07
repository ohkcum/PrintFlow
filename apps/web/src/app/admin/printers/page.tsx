"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  Plus,
  Search,
  ChevronLeft,
  ChevronRight,
  Edit2,
  PrinterIcon,
  Wifi,
  WifiOff,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Wrench,
} from "lucide-react";
import { printersApi, type Printer, type PrinterGroup } from "@/lib/api";
import { SkeletonCard } from "@/components/ui/Skeleton";

const PAGE_SIZE = 20;

const STATUS_META: Record<
  string,
  { bg: string; text: string; label: string; icon: any }
> = {
  ONLINE: {
    bg: "oklch(0.72 0.18 145 / 0.12)",
    text: "oklch(0.72 0.18 145)",
    label: "Online",
    icon: Wifi,
  },
  OFFLINE: {
    bg: "oklch(0.55 0.05 250 / 0.12)",
    text: "oklch(0.55 0.05 250)",
    label: "Offline",
    icon: WifiOff,
  },
  IDLE: {
    bg: "oklch(0.75 0.15 85 / 0.12)",
    text: "oklch(0.75 0.15 85)",
    label: "Idle",
    icon: CheckCircle,
  },
  BUSY: {
    bg: "oklch(0.72 0.18 250 / 0.12)",
    text: "oklch(0.72 0.18 250)",
    label: "Busy",
    icon: PrinterIcon,
  },
  ERROR: {
    bg: "oklch(0.65 0.22 25 / 0.12)",
    text: "oklch(0.65 0.22 25)",
    label: "Error",
    icon: AlertTriangle,
  },
  MAINTENANCE: {
    bg: "oklch(0.72 0.18 145 / 0.12)",
    text: "oklch(0.72 0.18 145)",
    label: "Maintenance",
    icon: Wrench,
  },
};
const DEFAULT_STATUS = {
  bg: "oklch(0.55 0.05 250 / 0.12)",
  text: "oklch(0.55 0.05 250)",
  label: "Offline",
  icon: WifiOff,
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

export default function AdminPrintersPage() {
  const router = useRouter();
  const [printers, setPrinters] = useState<Printer[]>([]);
  const [groups, setGroups] = useState<PrinterGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [groupFilter, setGroupFilter] = useState("");

  const token =
    typeof window !== "undefined"
      ? localStorage.getItem("printflow_token")
      : null;
  useEffect(() => {
    if (!token) router.push("/login");
  }, [router, token]);

  const fetchPrinters = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [pData, gData] = await Promise.all([
        printersApi.list(),
        printersApi.listGroups(),
      ]);
      setPrinters(pData.data ?? []);
      setGroups(gData.data ?? []);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPrinters();
  }, [fetchPrinters]);

  const filtered = printers.filter((p) => {
    if (groupFilter && String(p.printerGroupId) !== groupFilter) return false;
    return true;
  });

  return (
    <div>
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
            Printers
          </h1>
          <p
            style={{
              color: "var(--color-muted-foreground)",
              fontSize: "0.875rem",
              marginTop: "0.25rem",
            }}
          >
            Manage print queues and printer groups
          </p>
        </div>
        <Link
          href="/admin/printers/new"
          style={{
            padding: "0.5rem 1rem",
            background: "var(--color-primary)",
            color: "var(--color-primary-foreground)",
            border: "none",
            borderRadius: "var(--radius-md)",
            fontSize: "0.85rem",
            fontWeight: 600,
            textDecoration: "none",
            display: "inline-flex",
            alignItems: "center",
            gap: "0.5rem",
            cursor: "pointer",
          }}
        >
          <Plus size={16} /> Add Printer
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
        <select
          value={groupFilter}
          onChange={(e) => setGroupFilter(e.target.value)}
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
          <option value="">All Groups</option>
          {groups.map((g) => (
            <option key={g.id} value={String(g.id)}>
              {g.name}
            </option>
          ))}
        </select>
        <span
          style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}
        >
          {filtered.length} printer{filtered.length !== 1 ? "s" : ""}
        </span>
      </div>

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
          {error}
        </div>
      )}

      {/* Grid */}
      {loading ? (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))",
            gap: "1rem",
          }}
        >
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : filtered.length === 0 ? (
        <div
          style={{
            textAlign: "center",
            padding: "3rem",
            background: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-lg)",
            color: "var(--color-muted-foreground)",
          }}
        >
          <PrinterIcon
            size={40}
            style={{ margin: "0 auto 1rem", opacity: 0.3 }}
          />
          <p>No printers found</p>
        </div>
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))",
            gap: "1rem",
          }}
        >
          {filtered.map((printer) => {
            const meta = STATUS_META[printer.printerStatus] ?? DEFAULT_STATUS;
            const StatusIcon = meta.icon;
            return (
              <div
                key={printer.id}
                className="card-glow"
                style={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-lg)",
                  padding: "1.25rem",
                  transition: "border-color 0.15s",
                }}
              >
                {/* Header */}
                <div
                  style={{
                    display: "flex",
                    alignItems: "flex-start",
                    justifyContent: "space-between",
                    marginBottom: "0.875rem",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.625rem",
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
                      <PrinterIcon
                        size={20}
                        style={{ color: "var(--color-primary)" }}
                      />
                    </div>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: "0.95rem" }}>
                        {printer.displayName || printer.name}
                      </div>
                      <div
                        style={{
                          fontSize: "0.75rem",
                          color: "var(--color-muted-foreground)",
                          fontFamily: "var(--font-mono)",
                        }}
                      >
                        {printer.name}
                      </div>
                    </div>
                  </div>
                  <Link
                    href={`/admin/printers/${printer.id}`}
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
                    <Edit2 size={12} />
                  </Link>
                </div>

                {/* Status */}
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    marginBottom: "0.875rem",
                  }}
                >
                  <StatusIcon size={14} style={{ color: meta.text }} />
                  <Badge label={meta.label} bg={meta.bg} text={meta.text} />
                  <Badge
                    label={printer.printerType}
                    bg="oklch(0.60 0.02 250 / 0.1)"
                    text="oklch(0.60 0.08 250)"
                  />
                  {!printer.isEnabled && (
                    <Badge
                      label="Disabled"
                      bg="oklch(0.55 0.05 250 / 0.12)"
                      text="oklch(0.55 0.05 250)"
                    />
                  )}
                </div>

                {/* Info grid */}
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
                    gap: "0.5rem",
                  }}
                >
                  {[
                    {
                      label: "Mono",
                      value: `$${printer.costPerPageMono ?? "0.01"}/pg`,
                    },
                    {
                      label: "Color",
                      value: `$${printer.costPerPageColor ?? "0.05"}/pg`,
                    },
                    {
                      label: "Duplex",
                      value: printer.supportsDuplex ? "Yes" : "No",
                    },
                    { label: "Max Size", value: printer.maxPaperSize },
                    { label: "Jobs", value: String(printer.totalPrintJobs) },
                    { label: "Pages", value: String(printer.totalPrintPages) },
                  ].map(({ label, value }) => (
                    <div key={label}>
                      <div
                        style={{
                          fontSize: "0.7rem",
                          color: "var(--color-muted-foreground)",
                          textTransform: "uppercase",
                          letterSpacing: "0.05em",
                        }}
                      >
                        {label}
                      </div>
                      <div
                        style={{
                          fontSize: "0.8rem",
                          fontWeight: 600,
                          fontFamily: "var(--font-mono)",
                        }}
                      >
                        {value}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Capabilities */}
                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "4px",
                    marginTop: "0.75rem",
                  }}
                >
                  {printer.supportsStaple && (
                    <Badge
                      label="Staple"
                      bg="oklch(0.75 0.15 85 / 0.12)"
                      text="oklch(0.75 0.15 85)"
                    />
                  )}
                  {printer.supportsPunch && (
                    <Badge
                      label="Punch"
                      bg="oklch(0.75 0.15 85 / 0.12)"
                      text="oklch(0.75 0.15 85)"
                    />
                  )}
                  {printer.supportsBanner && (
                    <Badge
                      label="Banner"
                      bg="oklch(0.75 0.15 85 / 0.12)"
                      text="oklch(0.75 0.15 85)"
                    />
                  )}
                  {printer.ecoPrintDefault && (
                    <Badge
                      label="Eco Print"
                      bg="oklch(0.72 0.18 145 / 0.12)"
                      text="oklch(0.72 0.18 145)"
                    />
                  )}
                  {printer.requireRelease && (
                    <Badge
                      label="Release Req."
                      bg="oklch(0.60 0.02 250 / 0.1)"
                      text="oklch(0.60 0.08 250)"
                    />
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
