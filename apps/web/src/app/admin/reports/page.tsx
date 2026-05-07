"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  BarChart3,
  PieChart,
  Table2,
  FileSpreadsheet,
  Download,
  RefreshCw,
  Users,
  CreditCard,
  Printer,
  Ticket,
  FileText,
  TrendingUp,
  Loader2,
  Search,
  X,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  Area,
  AreaChart,
} from "recharts";
import {
  reportsApi,
  type ReportsSummary,
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

// ─── Dashboard Tab ─────────────────────────────────────────────────────────────

function DashboardTab() {
  const [data, setData] = useState<ReportsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await reportsApi.summary();
      setData(result.data);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (loading) {
    return (
      <div>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
            gap: "1rem",
            marginBottom: "1.5rem",
          }}
        >
          {[1, 2, 3, 4, 5].map((i) => (
            <div
              key={i}
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-xl)",
                padding: "1.25rem",
                height: "120px",
                animation: "pulse 1.5s infinite",
              }}
            />
          ))}
        </div>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: "1rem",
            marginBottom: "1rem",
          }}
        >
          <div
            style={{
              background: "var(--color-card)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-lg)",
              padding: "1.5rem",
              height: "300px",
              animation: "pulse 1.5s infinite",
            }}
          />
          <div
            style={{
              background: "var(--color-card)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-lg)",
              padding: "1.5rem",
              height: "300px",
              animation: "pulse 1.5s infinite",
            }}
          />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{
          padding: "3rem",
          textAlign: "center",
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-lg)",
        }}
      >
        <div
          style={{
            color: "oklch(0.65 0.22 25)",
            marginBottom: "1rem",
            fontSize: "0.9rem",
          }}
        >
          {error}
        </div>
        <button onClick={fetchData} style={btnStyle("primary")}>
          <RefreshCw size={16} /> Retry
        </button>
      </div>
    );
  }

  if (!data) return null;

  const pieColors = [
    "oklch(0.72 0.18 250)",
    "oklch(0.72 0.18 145)",
    "oklch(0.75 0.15 85)",
    "oklch(0.65 0.22 25)",
    "oklch(0.72 0.18 280)",
  ];

  return (
    <div>
      {/* Summary Cards */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
          gap: "1rem",
          marginBottom: "1.5rem",
        }}
      >
        <StatCard
          icon={Users}
          label="Total Users"
          value={data.overview.totalUsers}
          color="oklch(0.72 0.18 250)"
        />
        <StatCard
          icon={CreditCard}
          label="Transactions"
          value={data.overview.totalTransactions}
          color="oklch(0.72 0.18 145)"
        />
        <StatCard
          icon={Printer}
          label="Print Jobs"
          value={data.overview.totalPrintJobs}
          color="oklch(0.75 0.15 85)"
        />
        <StatCard
          icon={BarChart3}
          label="Printers"
          value={data.overview.totalPrinters}
          color="oklch(0.72 0.18 280)"
        />
        <StatCard
          icon={Ticket}
          label="Tickets"
          value={data.overview.totalTickets}
          color="oklch(0.65 0.22 25)"
        />
      </div>

      {/* Charts Row 1 */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        {/* Daily Prints Chart */}
        <div
          style={{
            background: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-lg)",
            padding: "1.5rem",
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
            <BarChart3 size={16} style={{ color: "oklch(0.72 0.18 250)" }} />
            Daily Prints (Last 30 Days)
          </h3>
          <ResponsiveContainer width="100%" height={220}>
            <AreaChart
              data={data.dailyPrints.map((d) => ({
                date: new Date(d.date).toLocaleDateString("en-US", {
                  month: "short",
                  day: "numeric",
                }),
                pages: d.pages,
              }))}
            >
              <CartesianGrid
                strokeDasharray="3 3"
                stroke="var(--color-border)"
              />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 10, fill: "var(--color-muted-foreground)" }}
                interval="preserveStartEnd"
              />
              <YAxis
                tick={{ fontSize: 10, fill: "var(--color-muted-foreground)" }}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  fontSize: "0.8rem",
                }}
              />
              <Area
                type="monotone"
                dataKey="pages"
                stroke="oklch(0.72 0.18 250)"
                fill="oklch(0.72 0.18 250 / 0.2)"
                strokeWidth={2}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Monthly Stats Chart */}
        <div
          style={{
            background: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-lg)",
            padding: "1.5rem",
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
            <TrendingUp
              size={16}
              style={{ color: "oklch(0.72 0.18 145)" }}
            />
            Monthly Activity
          </h3>
          <ResponsiveContainer width="100%" height={220}>
            <LineChart
              data={data.monthlyStats.map((m) => ({
                month: m.month,
                count: m.count,
                amount: Number(m.totalAmount),
              }))}
            >
              <CartesianGrid
                strokeDasharray="3 3"
                stroke="var(--color-border)"
              />
              <XAxis
                dataKey="month"
                tick={{ fontSize: 10, fill: "var(--color-muted-foreground)" }}
              />
              <YAxis
                tick={{ fontSize: 10, fill: "var(--color-muted-foreground)" }}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  fontSize: "0.8rem",
                }}
              />
              <Line
                type="monotone"
                dataKey="count"
                stroke="oklch(0.72 0.18 145)"
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Charts Row 2 */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        {/* Transaction Types Pie */}
        <div
          style={{
            background: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-lg)",
            padding: "1.5rem",
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
            <PieChart
              size={16}
              style={{ color: "oklch(0.75 0.15 85)" }}
            />
            Transaction Types
          </h3>
          <ResponsiveContainer width="100%" height={200}>
            <RechartsPieChart>
              <Pie
                data={data.trxByType}
                dataKey="count"
                nameKey="trxType"
                cx="50%"
                cy="50%"
                outerRadius={80}
                innerRadius={40}
                paddingAngle={2}
              >
                {data.trxByType.map((_, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={pieColors[index % pieColors.length]}
                  />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  fontSize: "0.8rem",
                }}
              />
            </RechartsPieChart>
          </ResponsiveContainer>
          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              gap: "0.5rem",
              marginTop: "0.5rem",
            }}
          >
            {data.trxByType.map((t, i) => (
              <div
                key={t.trxType}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "4px",
                  fontSize: "0.7rem",
                  color: "var(--color-muted-foreground)",
                }}
              >
                <div
                  style={{
                    width: "8px",
                    height: "8px",
                    borderRadius: "50%",
                    background: pieColors[i % pieColors.length],
                  }}
                />
                {t.trxType}
              </div>
            ))}
          </div>
        </div>

        {/* Ticket Status Pie */}
        <div
          style={{
            background: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-lg)",
            padding: "1.5rem",
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
            <Ticket size={16} style={{ color: "oklch(0.72 0.18 280)" }} />
            Ticket Status
          </h3>
          <ResponsiveContainer width="100%" height={200}>
            <RechartsPieChart>
              <Pie
                data={data.ticketsByStatus}
                dataKey="count"
                nameKey="status"
                cx="50%"
                cy="50%"
                outerRadius={80}
                innerRadius={40}
                paddingAngle={2}
              >
                {data.ticketsByStatus.map((_, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={pieColors[index % pieColors.length]}
                  />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  fontSize: "0.8rem",
                }}
              />
            </RechartsPieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Tables Row */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1rem",
        }}
      >
        {/* Top Printers */}
        <div
          style={{
            background: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-lg)",
            overflow: "hidden",
          }}
        >
          <h3
            style={{
              fontSize: "0.95rem",
              fontWeight: 700,
              padding: "1rem 1.25rem",
              borderBottom: "1px solid var(--color-border)",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
            }}
          >
            <Printer size={16} style={{ color: "oklch(0.75 0.15 85)" }} />
            Top Printers
          </h3>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr
                style={{ borderBottom: "1px solid var(--color-border)" }}
              >
                {["Printer", "Jobs", "Pages"].map((h) => (
                  <th
                    key={h}
                    style={{
                      padding: "0.625rem 1rem",
                      textAlign: "left",
                      fontSize: "0.7rem",
                      fontWeight: 600,
                      color: "var(--color-muted-foreground)",
                      textTransform: "uppercase",
                      letterSpacing: "0.05em",
                    }}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.topPrinters.map((p, i) => (
                <tr
                  key={p.printerName}
                  style={{ borderBottom: "1px solid var(--color-border)" }}
                >
                  <td style={{ padding: "0.625rem 1rem", fontSize: "0.85rem" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                      <span
                        style={{
                          width: "20px",
                          height: "20px",
                          borderRadius: "50%",
                          background:
                            i === 0
                              ? "oklch(0.72 0.18 145)"
                              : "oklch(0.55 0.05 250 / 0.3)",
                          color: i === 0 ? "white" : "var(--color-foreground)",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          fontSize: "0.65rem",
                          fontWeight: 700,
                        }}
                      >
                        {i + 1}
                      </span>
                      {p.printerName}
                    </div>
                  </td>
                  <td
                    style={{
                      padding: "0.625rem 1rem",
                      fontFamily: "var(--font-mono)",
                      fontSize: "0.8rem",
                    }}
                  >
                    {p.jobCount}
                  </td>
                  <td
                    style={{
                      padding: "0.625rem 1rem",
                      fontFamily: "var(--font-mono)",
                      fontSize: "0.8rem",
                    }}
                  >
                    {p.totalPages.toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Top Users */}
        <div
          style={{
            background: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-lg)",
            overflow: "hidden",
          }}
        >
          <h3
            style={{
              fontSize: "0.95rem",
              fontWeight: 700,
              padding: "1rem 1.25rem",
              borderBottom: "1px solid var(--color-border)",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
            }}
          >
            <Users size={16} style={{ color: "oklch(0.72 0.18 250)" }} />
            Top Users
          </h3>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr
                style={{ borderBottom: "1px solid var(--color-border)" }}
              >
                {["User", "Jobs", "Pages", "Cost"].map((h) => (
                  <th
                    key={h}
                    style={{
                      padding: "0.625rem 1rem",
                      textAlign: "left",
                      fontSize: "0.7rem",
                      fontWeight: 600,
                      color: "var(--color-muted-foreground)",
                      textTransform: "uppercase",
                      letterSpacing: "0.05em",
                    }}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.topUsers.map((u, i) => (
                <tr
                  key={u.userId}
                  style={{ borderBottom: "1px solid var(--color-border)" }}
                >
                  <td style={{ padding: "0.625rem 1rem", fontSize: "0.85rem" }}>
                    <div style={{ fontWeight: 600 }}>{u.fullName}</div>
                    <div
                      style={{
                        fontSize: "0.7rem",
                        color: "var(--color-muted-foreground)",
                      }}
                    >
                      @{u.userName}
                    </div>
                  </td>
                  <td
                    style={{
                      padding: "0.625rem 1rem",
                      fontFamily: "var(--font-mono)",
                      fontSize: "0.8rem",
                    }}
                  >
                    {u.jobCount}
                  </td>
                  <td
                    style={{
                      padding: "0.625rem 1rem",
                      fontFamily: "var(--font-mono)",
                      fontSize: "0.8rem",
                    }}
                  >
                    {u.totalPages.toLocaleString()}
                  </td>
                  <td
                    style={{
                      padding: "0.625rem 1rem",
                      fontFamily: "var(--font-mono)",
                      fontSize: "0.8rem",
                      color: "oklch(0.75 0.15 85)",
                    }}
                  >
                    {Number(u.totalCost).toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// ─── Account Transactions Tab ──────────────────────────────────────────────────

function AccountTransactionsTab({
  onToast,
}: {
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [data, setData] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [accountType, setAccountType] = useState("");
  const [trxType, setTrxType] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await reportsApi.accountTrx({
        page,
        limit: PAGE_SIZE,
        accountType: accountType || undefined,
        trxType: trxType || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
      });
      setData(result.data?.data ?? []);
      setTotal(result.data?.total ?? 0);
    } catch (e: any) {
      onToast(e.message, "error");
      setData([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [page, accountType, trxType, dateFrom, dateTo, onToast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    setPage(1);
  }, [accountType, trxType, dateFrom, dateTo]);

  async function exportCSV() {
    try {
      const result = await reportsApi.accountTrx({
        accountType: accountType || undefined,
        trxType: trxType || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
        limit: 10000,
        format: "csv",
      });
      if (result.data?.downloadUrl) {
        window.open(result.data.downloadUrl, "_blank");
      }
    } catch (e: any) {
      onToast("Export failed: " + e.message, "error");
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
        <select
          value={accountType}
          onChange={(e) => setAccountType(e.target.value)}
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
          <option value="">All Account Types</option>
          <option value="USER">User</option>
          <option value="GROUP">Group</option>
          <option value="SHARED">Shared</option>
          <option value="SYSTEM">System</option>
        </select>
        <select
          value={trxType}
          onChange={(e) => setTrxType(e.target.value)}
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
          <option value="">All Transaction Types</option>
          <option value="MANUAL_ADD">Manual Add</option>
          <option value="MANUAL_DEDUCT">Manual Deduct</option>
          <option value="VOUCHER_REDEEM">Voucher Redeem</option>
          <option value="TRANSFER_IN">Transfer In</option>
          <option value="TRANSFER_OUT">Transfer Out</option>
          <option value="PRINT_JOB">Print Job</option>
          <option value="REFUND">Refund</option>
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
        <span style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem" }}>
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
        <button onClick={exportCSV} style={btnStyle("secondary")}>
          <Download size={16} /> Export CSV
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
          <SkeletonTable rows={6} cols={7} />
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                minWidth: "800px",
              }}
            >
              <thead>
                <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                  {["Date", "Account", "Type", "Description", "Amount", "Balance", "Ref"].map((h) => (
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
                {data.length === 0 ? (
                  <tr>
                    <td
                      colSpan={7}
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
                  data.map((row: any) => (
                    <tr
                      key={row.id}
                      style={{ borderBottom: "1px solid var(--color-border)" }}
                    >
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontSize: "0.8rem",
                          color: "var(--color-muted-foreground)",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {new Date(row.dateCreated).toLocaleString()}
                      </td>
                      <td style={{ padding: "0.75rem 1rem", fontSize: "0.85rem" }}>
                        {row.accountName ?? row.accountId}
                      </td>
                      <td style={{ padding: "0.75rem 1rem" }}>
                        <Badge
                          label={row.trxType?.replace(/_/g, " ") ?? "—"}
                          bg="oklch(0.55 0.05 250 / 0.12)"
                          text="oklch(0.55 0.05 250)"
                        />
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontSize: "0.85rem",
                          maxWidth: "200px",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {row.description ?? "—"}
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.85rem",
                          color:
                            Number(row.amount) >= 0
                              ? "oklch(0.72 0.18 145)"
                              : "oklch(0.65 0.22 25)",
                        }}
                      >
                        {Number(row.amount) >= 0 ? "+" : ""}
                        {Number(row.amount).toFixed(4)}
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.85rem",
                        }}
                      >
                        {Number(row.balanceAfter ?? 0).toFixed(4)}
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.75rem",
                          color: "var(--color-muted-foreground)",
                        }}
                      >
                        {row.referenceType
                          ? `${row.referenceType}#${row.referenceId}`
                          : "—"}
                      </td>
                    </tr>
                  ))
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

// ─── User Printout Tab ──────────────────────────────────────────────────────────

function UserPrintoutTab({
  onToast,
}: {
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [data, setData] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [groupBy, setGroupBy] = useState("user");
  const [aspect, setAspect] = useState("pages");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await reportsApi.userPrintout({
        page,
        limit: PAGE_SIZE,
        groupBy: groupBy || undefined,
        aspect: aspect || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
      });
      setData(result.data?.data ?? []);
      setTotal(result.data?.total ?? 0);
    } catch (e: any) {
      onToast(e.message, "error");
      setData([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [page, groupBy, aspect, dateFrom, dateTo, onToast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    setPage(1);
  }, [groupBy, aspect, dateFrom, dateTo]);

  async function exportCSV() {
    try {
      const result = await reportsApi.userPrintout({
        groupBy: groupBy || undefined,
        aspect: aspect || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
        limit: 10000,
        format: "csv",
      });
      if (result.data?.downloadUrl) {
        window.open(result.data.downloadUrl, "_blank");
      }
    } catch (e: any) {
      onToast("Export failed: " + e.message, "error");
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
        <select
          value={groupBy}
          onChange={(e) => setGroupBy(e.target.value)}
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
          <option value="user">Group by User</option>
          <option value="printer_user">Group by Printer User</option>
        </select>
        <select
          value={aspect}
          onChange={(e) => setAspect(e.target.value)}
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
          <option value="pages">Aspect: Pages</option>
          <option value="jobs">Aspect: Jobs</option>
          <option value="copies">Aspect: Copies</option>
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
        <span style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem" }}>
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
        <button onClick={exportCSV} style={btnStyle("secondary")}>
          <Download size={16} /> Export CSV
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
                  {groupBy === "user"
                    ? ["User", "Full Name", "Jobs", "Pages", "Cost"].map((h) => (
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
                      ))
                    : ["Printer", "User", "Jobs", "Pages", "Cost"].map((h) => (
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
                {data.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      style={{
                        padding: "3rem",
                        textAlign: "center",
                        color: "var(--color-muted-foreground)",
                      }}
                    >
                      No data found
                    </td>
                  </tr>
                ) : (
                  data.map((row: any, i: number) => (
                    <tr
                      key={i}
                      style={{ borderBottom: "1px solid var(--color-border)" }}
                    >
                      <td style={{ padding: "0.75rem 1rem", fontSize: "0.85rem" }}>
                        {groupBy === "user" ? row.userName : row.printerName}
                      </td>
                      <td style={{ padding: "0.75rem 1rem", fontSize: "0.85rem" }}>
                        {row.fullName ?? row.userName ?? "—"}
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.85rem",
                        }}
                      >
                        {row.jobCount ?? row.jobs ?? 0}
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.85rem",
                        }}
                      >
                        {(row.totalPages ?? row.pages ?? 0).toLocaleString()}
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.85rem",
                          color: "oklch(0.75 0.15 85)",
                        }}
                      >
                        {Number(row.totalCost ?? 0).toFixed(4)}
                      </td>
                    </tr>
                  ))
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

// ─── Documents Tab ─────────────────────────────────────────────────────────────

function DocumentsTab({
  onToast,
}: {
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [data, setData] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await reportsApi.documents({
        page,
        limit: PAGE_SIZE,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
      });
      setData(result.data?.data ?? []);
      setTotal(result.data?.total ?? 0);
    } catch (e: any) {
      onToast(e.message, "error");
      setData([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [page, dateFrom, dateTo, onToast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    setPage(1);
  }, [dateFrom, dateTo]);

  async function exportCSV() {
    try {
      const result = await reportsApi.documents({
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
        limit: 10000,
        format: "csv",
      });
      if (result.data?.downloadUrl) {
        window.open(result.data.downloadUrl, "_blank");
      }
    } catch (e: any) {
      onToast("Export failed: " + e.message, "error");
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
        <span style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem" }}>
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
        <button onClick={exportCSV} style={btnStyle("secondary")}>
          <Download size={16} /> Export CSV
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
                  {["Document", "User", "Type", "Pages", "Size", "Date"].map((h) => (
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
                {data.length === 0 ? (
                  <tr>
                    <td
                      colSpan={6}
                      style={{
                        padding: "3rem",
                        textAlign: "center",
                        color: "var(--color-muted-foreground)",
                      }}
                    >
                      No documents found
                    </td>
                  </tr>
                ) : (
                  data.map((doc: any) => (
                    <tr
                      key={doc.id}
                      style={{ borderBottom: "1px solid var(--color-border)" }}
                    >
                      <td style={{ padding: "0.75rem 1rem" }}>
                        <div
                          style={{
                            fontWeight: 600,
                            fontSize: "0.85rem",
                            maxWidth: "200px",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          <FileText
                            size={14}
                            style={{
                              display: "inline",
                              marginRight: "4px",
                              color: "var(--color-muted-foreground)",
                              verticalAlign: "middle",
                            }}
                          />
                          {doc.docName}
                        </div>
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontSize: "0.85rem",
                        }}
                      >
                        {doc.userName ?? "—"}
                      </td>
                      <td style={{ padding: "0.75rem 1rem" }}>
                        <Badge
                          label={doc.docType ?? "PDF"}
                          bg="oklch(0.55 0.05 250 / 0.12)"
                          text="oklch(0.55 0.05 250)"
                        />
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.85rem",
                        }}
                      >
                        {doc.pageCount ?? "—"}
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.85rem",
                          color: "var(--color-muted-foreground)",
                        }}
                      >
                        {doc.fileSize
                          ? `${(doc.fileSize / 1024 / 1024).toFixed(2)} MB`
                          : "—"}
                      </td>
                      <td
                        style={{
                          padding: "0.75rem 1rem",
                          fontSize: "0.8rem",
                          color: "var(--color-muted-foreground)",
                        }}
                      >
                        {doc.dateCreated
                          ? new Date(doc.dateCreated).toLocaleDateString()
                          : "—"}
                      </td>
                    </tr>
                  ))
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

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function AdminReportsPage() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<
    "dashboard" | "account-trx" | "user-printout" | "documents"
  >("dashboard");
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
    if (toast) {
      const t = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(t);
    }
  }, [toast]);

  function showToast(msg: string, type: "success" | "error") {
    setToast({ message: msg, type });
  }

  const TABS = [
    { id: "dashboard" as const, label: "Dashboard", icon: BarChart3 },
    { id: "account-trx" as const, label: "Account Transactions", icon: CreditCard },
    { id: "user-printout" as const, label: "User Printout", icon: Users },
    { id: "documents" as const, label: "Documents", icon: FileText },
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
          Reports
        </h1>
        <p
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.875rem",
            marginTop: "0.25rem",
          }}
        >
          System analytics, charts, and exportable reports
        </p>
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
          flexWrap: "wrap",
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
      {activeTab === "dashboard" && <DashboardTab />}
      {activeTab === "account-trx" && (
        <AccountTransactionsTab onToast={showToast} />
      )}
      {activeTab === "user-printout" && (
        <UserPrintoutTab onToast={showToast} />
      )}
      {activeTab === "documents" && <DocumentsTab onToast={showToast} />}

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
