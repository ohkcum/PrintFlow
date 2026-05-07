"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  Activity,
  Printer,
  RefreshCw,
  AlertCircle,
  CheckCircle,
  Loader2,
  Search,
  X,
  Plus,
  Monitor,
} from "lucide-react";
import {
  printersApi,
  snmpApi,
  type Printer,
  type SnmpPrinterStatus,
  type SnmpPrinterSupplies,
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

// ─── Supply Level Bar ─────────────────────────────────────────────────────────

function SupplyBar({
  label,
  level,
  max,
  unit,
}: {
  label: string;
  level: number | null;
  max: number | null;
  unit: string | null;
}) {
  const percentage = max && level !== null ? Math.min(100, Math.round((level / max) * 100)) : 0;
  const color =
    percentage > 50
      ? "oklch(0.72 0.18 145)"
      : percentage > 20
        ? "oklch(0.75 0.15 85)"
        : "oklch(0.65 0.22 25)";

  return (
    <div style={{ marginBottom: "1rem" }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          marginBottom: "0.375rem",
        }}
      >
        <span style={{ fontSize: "0.8rem", fontWeight: 600 }}>{label}</span>
        <span
          style={{
            fontSize: "0.8rem",
            fontFamily: "var(--font-mono)",
            color: "var(--color-muted-foreground)",
          }}
        >
          {level !== null ? level : "—"} {unit ?? ""} / {max ?? "—"}
        </span>
      </div>
      <div
        style={{
          height: "8px",
          background: "var(--color-border)",
          borderRadius: "999px",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            height: "100%",
            width: `${percentage}%`,
            background: color,
            borderRadius: "999px",
            transition: "width 0.3s ease",
          }}
        />
      </div>
    </div>
  );
}

// ─── Supplies Tab ─────────────────────────────────────────────────────────────

function SuppliesTab({
  printerId,
  printerName,
  onToast,
}: {
  printerId: number;
  printerName: string;
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [data, setData] = useState<SnmpPrinterSupplies | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSupplies = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await snmpApi.supplies(printerId);
      setData(result.data);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [printerId]);

  useEffect(() => {
    fetchSupplies();
  }, [fetchSupplies]);

  if (loading) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
          color: "var(--color-muted-foreground)",
        }}
      >
        <Loader2
          size={24}
          style={{
            margin: "0 auto 0.75rem",
            animation: "spin 1s linear infinite",
          }}
        />
        Loading supplies...
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
        }}
      >
        <div
          style={{
            color: "oklch(0.65 0.22 25)",
            marginBottom: "1rem",
            fontSize: "0.875rem",
          }}
        >
          {error}
        </div>
        <button onClick={fetchSupplies} style={btnStyle("secondary")}>
          <RefreshCw size={16} /> Retry
        </button>
      </div>
    );
  }

  if (!data || data.supplies.length === 0) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
          color: "var(--color-muted-foreground)",
          fontSize: "0.875rem",
        }}
      >
        No supply information available for this printer
      </div>
    );
  }

  const supplyLabels: Record<string, string> = {
    "1": "Other",
    "2": "Unknown",
    "3": "Toner",
    "4": "Magenta Toner",
    "5": "Cyan Toner",
    "6": "Yellow Toner",
    "7": "Black Toner",
    "8": "Waste Toner",
    "9": "Drum",
    "10": "Fuser",
    "11": "Fuser Oil",
    "12": "Transfer Belt",
    "13": "Transfer Roller",
    "14": "Banner Paper",
    "15": "Paper Feed",
    "16": "Output Tray",
    "17": "Maintenance Kit",
    "18": "Adhesive Film",
    "19": "Staples",
    "20": "Staple Cartridge",
    "21": "Separation Cushion",
    "22": "Transparency",
    "23": "Paper Path Roller",
  };

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "flex-end",
          marginBottom: "1rem",
        }}
      >
        <button onClick={fetchSupplies} style={btnStyle("ghost")}>
          <RefreshCw size={14} /> Refresh
        </button>
      </div>
      {data.supplies.map((supply) => (
        <SupplyBar
          key={supply.index}
          label={supplyLabels[String(supply.type)] ?? `Supply ${supply.type ?? supply.index}`}
          level={supply.level}
          max={supply.max}
          unit={supply.unit}
        />
      ))}
    </div>
  );
}

// ─── Counters Tab ─────────────────────────────────────────────────────────────

function CountersTab({
  printerId,
  onToast,
}: {
  printerId: number;
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchCounters = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await snmpApi.counters(printerId);
      setData(result.data);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [printerId]);

  useEffect(() => {
    fetchCounters();
  }, [fetchCounters]);

  if (loading) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
          color: "var(--color-muted-foreground)",
        }}
      >
        <Loader2
          size={24}
          style={{
            margin: "0 auto 0.75rem",
            animation: "spin 1s linear infinite",
          }}
        />
        Loading counters...
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
        }}
      >
        <div
          style={{
            color: "oklch(0.65 0.22 25)",
            marginBottom: "1rem",
            fontSize: "0.875rem",
          }}
        >
          {error}
        </div>
        <button onClick={fetchCounters} style={btnStyle("secondary")}>
          <RefreshCw size={16} /> Retry
        </button>
      </div>
    );
  }

  if (!data) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
          color: "var(--color-muted-foreground)",
          fontSize: "0.875rem",
        }}
      >
        No counter data available
      </div>
    );
  }

  return (
    <div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
          gap: "1rem",
        }}
      >
        {Object.entries(data)
          .filter(([key]) => !["printerId", "printerName", "polledAt"].includes(key))
          .map(([key, value]) => (
            <div
              key={key}
              style={{
                background: "var(--color-background)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                padding: "1rem",
              }}
            >
              <div
                style={{
                  fontSize: "0.7rem",
                  color: "var(--color-muted-foreground)",
                  textTransform: "uppercase",
                  letterSpacing: "0.05em",
                  marginBottom: "0.25rem",
                }}
              >
                {key.replace(/([A-Z])/g, " $1").trim()}
              </div>
              <div
                style={{
                  fontSize: "1.25rem",
                  fontWeight: 700,
                  fontFamily: "var(--font-mono)",
                }}
              >
                {typeof value === "number" ? value.toLocaleString() : String(value ?? "—")}
              </div>
            </div>
          ))}
      </div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function AdminSNMPPage() {
  const router = useRouter();
  const [printers, setPrinters] = useState<Printer[]>([]);
  const [loadingPrinters, setLoadingPrinters] = useState(true);
  const [selectedPrinter, setSelectedPrinter] = useState<Printer | null>(null);
  const [status, setStatus] = useState<SnmpPrinterStatus | null>(null);
  const [statusLoading, setStatusLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"status" | "supplies" | "counters">("status");
  const [discovering, setDiscovering] = useState(false);
  const [discoveredPrinters, setDiscoveredPrinters] = useState<any[]>([]);
  const [discoverNetwork, setDiscoverNetwork] = useState("");
  const [discoverError, setDiscoverError] = useState<string | null>(null);
  const [toast, setToast] = useState<{
    message: string;
    type: "success" | "error";
  } | null>(null);

  const token =
    typeof window !== "undefined"
      ? localStorage.getItem("printflow_token")
      : null;
  const user =
    typeof window !== "undefined"
      ? JSON.parse(localStorage.getItem("printflow_user") ?? "{}")
      : {};

  useEffect(() => {
    if (!token) {
      router.push("/login");
      return;
    }
    if (user.roles && !user.roles.includes("ADMIN")) {
      router.push("/");
      return;
    }
  }, [router, token, user]);

  useEffect(() => {
    printersApi
      .list()
      .then((res) => setPrinters(res.data ?? []))
      .catch(() => setPrinters([]))
      .finally(() => setLoadingPrinters(false));
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

  async function pollStatus() {
    if (!selectedPrinter) return;
    setStatusLoading(true);
    setStatus(null);
    try {
      const result = await snmpApi.status(selectedPrinter.id);
      setStatus(result.data);
      showToast("Status polled successfully", "success");
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setStatusLoading(false);
    }
  }

  async function discoverPrinters() {
    setDiscovering(true);
    setDiscoverError(null);
    setDiscoveredPrinters([]);
    try {
      const result = await snmpApi.discover({
        network: discoverNetwork || undefined,
      });
      const discovered = result.data?.printers ?? [];
      setDiscoveredPrinters(discovered);
      if (discovered.length === 0) {
        showToast("No printers discovered", "info");
      } else {
        showToast(`Found ${discovered.length} printer(s)`, "success");
      }
    } catch (e: any) {
      setDiscoverError(e.message);
    } finally {
      setDiscovering(false);
    }
  }

  const statusColor = (snmpStatus: string | null) => {
    if (!snmpStatus) return { bg: "oklch(0.55 0.05 250 / 0.12)", text: "oklch(0.55 0.05 250)" };
    const lower = snmpStatus.toLowerCase();
    if (lower.includes("idle") || lower.includes("ready") || lower.includes("normal"))
      return { bg: "oklch(0.72 0.18 145 / 0.12)", text: "oklch(0.72 0.18 145)" };
    if (lower.includes("error") || lower.includes("warning") || lower.includes("jam"))
      return { bg: "oklch(0.65 0.22 25 / 0.12)", text: "oklch(0.65 0.22 25)" };
    if (lower.includes("processing") || lower.includes("printing"))
      return { bg: "oklch(0.72 0.18 250 / 0.12)", text: "oklch(0.72 0.18 250)" };
    return { bg: "oklch(0.75 0.15 85 / 0.12)", text: "oklch(0.75 0.15 85)" };
  };

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
          SNMP Monitoring
        </h1>
        <p
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.875rem",
            marginTop: "0.25rem",
          }}
        >
          Monitor printer status, supplies, and counters via SNMP
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1.5rem",
          alignItems: "start",
        }}
      >
        {/* Left Column: Printer Selection + Status */}
        <div>
          {/* Printer Selector */}
          <div
            style={{
              background: "var(--color-card)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-lg)",
              padding: "1.25rem",
              marginBottom: "1rem",
            }}
          >
            <h3
              style={{
                fontSize: "0.95rem",
                fontWeight: 700,
                marginBottom: "1rem",
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
              }}
            >
              <Printer size={16} style={{ color: "oklch(0.72 0.18 250)" }} />
              Select Printer
            </h3>

            {loadingPrinters ? (
              <div
                style={{
                  padding: "2rem",
                  textAlign: "center",
                  color: "var(--color-muted-foreground)",
                }}
              >
                <Loader2
                  size={20}
                  style={{
                    margin: "0 auto 0.5rem",
                    animation: "spin 1s linear infinite",
                  }}
                />
                Loading printers...
              </div>
            ) : printers.length === 0 ? (
              <div
                style={{
                  padding: "1.5rem",
                  textAlign: "center",
                  color: "var(--color-muted-foreground)",
                  fontSize: "0.875rem",
                }}
              >
                No printers available
              </div>
            ) : (
              <>
                <select
                  value={selectedPrinter?.id ?? ""}
                  onChange={(e) => {
                    const p = printers.find((pr) => pr.id === Number(e.target.value));
                    setSelectedPrinter(p ?? null);
                    setStatus(null);
                  }}
                  style={{
                    width: "100%",
                    padding: "0.625rem 0.875rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                    color: "var(--color-foreground)",
                    fontSize: "0.875rem",
                    outline: "none",
                    cursor: "pointer",
                    marginBottom: "1rem",
                  }}
                >
                  <option value="">Select a printer...</option>
                  {printers.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} {p.displayName !== p.name ? `(${p.displayName})` : ""}
                    </option>
                  ))}
                </select>

                <button
                  onClick={pollStatus}
                  disabled={!selectedPrinter || statusLoading}
                  style={{
                    ...btnStyle("primary"),
                    width: "100%",
                    justifyContent: "center",
                    opacity: !selectedPrinter || statusLoading ? 0.6 : 1,
                  }}
                >
                  {statusLoading ? (
                    <>
                      <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} />
                      Polling...
                    </>
                  ) : (
                    <>
                      <Activity size={16} /> Poll Status
                    </>
                  )}
                </button>
              </>
            )}
          </div>

          {/* Status Display */}
          {selectedPrinter && (
            <div
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-lg)",
                padding: "1.25rem",
              }}
            >
              <h3
                style={{
                  fontSize: "0.95rem",
                  fontWeight: 700,
                  marginBottom: "1rem",
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                }}
              >
                <Activity size={16} style={{ color: "oklch(0.75 0.15 85)" }} />
                Printer Status
              </h3>

              {!status ? (
                <div
                  style={{
                    padding: "2rem",
                    textAlign: "center",
                    color: "var(--color-muted-foreground)",
                    fontSize: "0.875rem",
                  }}
                >
                  {statusLoading ? (
                    <>
                      <Loader2
                        size={24}
                        style={{
                          margin: "0 auto 0.75rem",
                          animation: "spin 1s linear infinite",
                        }}
                      />
                      Polling printer status...
                    </>
                  ) : (
                    "Select a printer and click 'Poll Status' to view SNMP data"
                  )}
                </div>
              ) : (
                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "1rem",
                  }}
                >
                  {/* Status info */}
                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "0.75rem",
                    }}
                  >
                    <div
                      style={{
                        background: "var(--color-background)",
                        border: "1px solid var(--color-border)",
                        borderRadius: "var(--radius-md)",
                        padding: "0.75rem",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "0.7rem",
                          color: "var(--color-muted-foreground)",
                          textTransform: "uppercase",
                          letterSpacing: "0.05em",
                        }}
                      >
                        Printer
                      </div>
                      <div
                        style={{
                          fontSize: "0.875rem",
                          fontWeight: 600,
                          marginTop: "0.25rem",
                        }}
                      >
                        {status.printerName}
                      </div>
                    </div>
                    <div
                      style={{
                        background: "var(--color-background)",
                        border: "1px solid var(--color-border)",
                        borderRadius: "var(--radius-md)",
                        padding: "0.75rem",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "0.7rem",
                          color: "var(--color-muted-foreground)",
                          textTransform: "uppercase",
                          letterSpacing: "0.05em",
                        }}
                      >
                        Host
                      </div>
                      <div
                        style={{
                          fontSize: "0.875rem",
                          fontFamily: "var(--font-mono)",
                          marginTop: "0.25rem",
                        }}
                      >
                        {status.host}
                      </div>
                    </div>
                  </div>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "0.75rem",
                    }}
                  >
                    <div
                      style={{
                        background: "var(--color-background)",
                        border: "1px solid var(--color-border)",
                        borderRadius: "var(--radius-md)",
                        padding: "0.75rem",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "0.7rem",
                          color: "var(--color-muted-foreground)",
                          textTransform: "uppercase",
                          letterSpacing: "0.05em",
                        }}
                      >
                        SNMP Status
                      </div>
                      <div style={{ marginTop: "0.5rem" }}>
                        <Badge
                          label={status.snmpStatus ?? "Unknown"}
                          {...statusColor(status.snmpStatus)}
                        />
                      </div>
                    </div>
                    <div
                      style={{
                        background: "var(--color-background)",
                        border: "1px solid var(--color-border)",
                        borderRadius: "var(--radius-md)",
                        padding: "0.75rem",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "0.7rem",
                          color: "var(--color-muted-foreground)",
                          textTransform: "uppercase",
                          letterSpacing: "0.05em",
                        }}
                      >
                        Error State
                      </div>
                      <div style={{ marginTop: "0.5rem" }}>
                        {status.errorState ? (
                          <Badge
                            label="Error"
                            bg="oklch(0.65 0.22 25 / 0.12)"
                            text="oklch(0.65 0.22 25)"
                          />
                        ) : (
                          <Badge
                            label="None"
                            bg="oklch(0.72 0.18 145 / 0.12)"
                            text="oklch(0.72 0.18 145)"
                          />
                        )}
                      </div>
                    </div>
                  </div>

                  {status.errorState && (
                    <div
                      style={{
                        padding: "0.75rem",
                        background: "oklch(0.65 0.22 25 / 0.08)",
                        border: "1px solid oklch(0.65 0.22 25 / 0.2)",
                        borderRadius: "var(--radius-md)",
                        fontSize: "0.8rem",
                        color: "oklch(0.65 0.22 25)",
                      }}
                    >
                      <strong>Error:</strong> {status.errorState}
                    </div>
                  )}

                  <div
                    style={{
                      fontSize: "0.75rem",
                      color: "var(--color-muted-foreground)",
                    }}
                  >
                    Last polled:{" "}
                    {status.polledAt
                      ? new Date(status.polledAt).toLocaleString()
                      : "—"}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right Column: Tabs + Discovery */}
        <div>
          {/* Tabs */}
          {selectedPrinter && (
            <div
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-lg)",
                overflow: "hidden",
                marginBottom: "1rem",
              }}
            >
              <div
                style={{
                  display: "flex",
                  borderBottom: "1px solid var(--color-border)",
                }}
              >
                {[
                  { id: "status" as const, label: "Status" },
                  { id: "supplies" as const, label: "Supplies" },
                  { id: "counters" as const, label: "Counters" },
                ].map((tab) => {
                  const active = activeTab === tab.id;
                  return (
                    <button
                      key={tab.id}
                      onClick={() => setActiveTab(tab.id)}
                      style={{
                        flex: 1,
                        padding: "0.75rem",
                        background: "transparent",
                        border: "none",
                        borderBottom: active
                          ? "2px solid var(--color-primary)"
                          : "2px solid transparent",
                        color: active
                          ? "var(--color-primary)"
                          : "var(--color-muted-foreground)",
                        fontSize: "0.8rem",
                        fontWeight: active ? 600 : 400,
                        cursor: "pointer",
                      }}
                    >
                      {tab.label}
                    </button>
                  );
                })}
              </div>
              <div style={{ padding: "1rem" }}>
                {activeTab === "supplies" && (
                  <SuppliesTab
                    printerId={selectedPrinter.id}
                    printerName={selectedPrinter.name}
                    onToast={showToast}
                  />
                )}
                {activeTab === "counters" && (
                  <CountersTab
                    printerId={selectedPrinter.id}
                    onToast={showToast}
                  />
                )}
                {activeTab === "status" && !status && (
                  <div
                    style={{
                      padding: "2rem",
                      textAlign: "center",
                      color: "var(--color-muted-foreground)",
                      fontSize: "0.875rem",
                    }}
                  >
                    Poll status to see detailed information
                  </div>
                )}
                {activeTab === "status" && status && (
                  <div
                    style={{
                      padding: "1rem",
                      textAlign: "center",
                      color: "var(--color-muted-foreground)",
                      fontSize: "0.875rem",
                    }}
                  >
                    Status information shown on the left
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Discover Printers */}
          <div
            style={{
              background: "var(--color-card)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-lg)",
              padding: "1.25rem",
            }}
          >
            <h3
              style={{
                fontSize: "0.95rem",
                fontWeight: 700,
                marginBottom: "1rem",
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
              }}
            >
              <Search size={16} style={{ color: "oklch(0.72 0.18 145)" }} />
              Discover Printers
            </h3>

            <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
              <input
                type="text"
                value={discoverNetwork}
                onChange={(e) => setDiscoverNetwork(e.target.value)}
                placeholder="e.g. 192.168.1.0/24 or 192.168.1.1-254"
                style={{
                  flex: 1,
                  padding: "0.5rem 0.75rem",
                  background: "var(--color-input)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                  fontSize: "0.875rem",
                  outline: "none",
                }}
              />
              <button
                onClick={discoverPrinters}
                disabled={discovering}
                style={{
                  ...btnStyle("secondary"),
                  opacity: discovering ? 0.7 : 1,
                }}
              >
                {discovering ? (
                  <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} />
                ) : (
                  <Search size={16} />
                )}
              </button>
            </div>

            {discoverError && (
              <div
                style={{
                  padding: "0.75rem",
                  background: "oklch(0.65 0.22 25 / 0.08)",
                  border: "1px solid oklch(0.65 0.22 25 / 0.2)",
                  borderRadius: "var(--radius-md)",
                  color: "oklch(0.65 0.22 25)",
                  fontSize: "0.8rem",
                  marginBottom: "1rem",
                }}
              >
                {discoverError}
              </div>
            )}

            {discoveredPrinters.length > 0 && (
              <div>
                <div
                  style={{
                    fontSize: "0.8rem",
                    color: "var(--color-muted-foreground)",
                    marginBottom: "0.75rem",
                  }}
                >
                  Found {discoveredPrinters.length} printer(s):
                </div>
                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.5rem",
                    maxHeight: "250px",
                    overflowY: "auto",
                  }}
                >
                  {discoveredPrinters.map((p, i) => (
                    <div
                      key={i}
                      style={{
                        padding: "0.75rem",
                        background: "var(--color-background)",
                        border: "1px solid var(--color-border)",
                        borderRadius: "var(--radius-md)",
                        display: "flex",
                        alignItems: "center",
                        gap: "0.75rem",
                      }}
                    >
                      <div
                        style={{
                          width: "32px",
                          height: "32px",
                          borderRadius: "var(--radius-sm)",
                          background: "oklch(0.72 0.18 250 / 0.1)",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                        }}
                      >
                        <Printer size={16} style={{ color: "oklch(0.72 0.18 250)" }} />
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div
                          style={{
                            fontSize: "0.875rem",
                            fontWeight: 600,
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {p.name ?? p.host ?? `Printer ${i + 1}`}
                        </div>
                        <div
                          style={{
                            fontSize: "0.75rem",
                            color: "var(--color-muted-foreground)",
                            fontFamily: "var(--font-mono)",
                          }}
                        >
                          {p.host}
                        </div>
                      </div>
                      <Badge
                        label={p.status ?? "Unknown"}
                        bg="oklch(0.55 0.05 250 / 0.12)"
                        text="oklch(0.55 0.05 250)"
                      />
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
