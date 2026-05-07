"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Printer, Users, FileText, CreditCard, Activity, TrendingUp, Clock, AlertCircle } from "lucide-react";
import { SkeletonStatCard } from "@/components/ui/Skeleton";

const API_URL = process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001";

function StatCard({ icon: Icon, label, value, sub, accent }: {
  icon: any; label: string; value: string; sub?: string; accent?: string;
}) {
  return (
    <div
      className="card-glow"
      style={{
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-lg)",
        padding: "1.25rem",
        flex: 1,
        minWidth: "180px",
      }}
    >
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: "0.75rem" }}>
        <div
          style={{
            width: "36px",
            height: "36px",
            borderRadius: "var(--radius-md)",
            background: accent ? `${accent}18` : "oklch(0.72 0.18 250 / 0.12)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Icon size={18} style={{ color: accent ?? "var(--color-primary)" }} />
        </div>
      </div>
      <div style={{ fontSize: "1.75rem", fontWeight: 700, letterSpacing: "-0.02em", color: "var(--color-foreground)" }}>
        {value}
      </div>
      <div style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginTop: "0.125rem" }}>{label}</div>
      {sub && <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", marginTop: "0.25rem" }}>{sub}</div>}
    </div>
  );
}

export default function AdminDashboardPage() {
  const router = useRouter();
  const [stats, setStats] = useState({ users: 0, printers: 0, documents: 0, printJobs: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("printflow_token");
    if (!token) { router.push("/login"); return; }

    async function fetchStats() {
      try {
        const [usersRes, printersRes] = await Promise.all([
          fetch(`${API_URL}/api/v1/users?limit=1`, { headers: { Authorization: `Bearer ${token}` } }),
          fetch(`${API_URL}/api/v1/printers`, { headers: { Authorization: `Bearer ${token}` } }),
        ]);
        const usersData = await usersRes.json();
        const printersData = await printersRes.json();
        setStats({
          users: usersData.data?.total ?? 0,
          printers: printersData.data?.length ?? 0,
          documents: 0,
          printJobs: 0,
        });
      } catch {
        // server might not be running yet
      } finally {
        setLoading(false);
      }
    }
    fetchStats();
  }, [router]);

  return (
    <div className="animate-fade-in">
      <div style={{ marginBottom: "1.5rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: 700, letterSpacing: "-0.02em" }}>Dashboard</h1>
        <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem", marginTop: "0.25rem" }}>
          PrintFlow system overview and activity
        </p>
      </div>

      {/* Stats Grid */}
      <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", marginBottom: "2rem" }}>
        {loading ? (
          <>
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
          </>
        ) : (
          <>
            <StatCard icon={Users} label="Total Users" value={String(stats.users)} accent="oklch(0.72 0.18 250)" />
            <StatCard icon={Printer} label="Printers" value={String(stats.printers)} accent="oklch(0.72 0.18 145)" />
            <StatCard icon={FileText} label="SafePages" value={String(stats.documents)} accent="oklch(0.75 0.15 85)" />
            <StatCard icon={Activity} label="Print Jobs" value={String(stats.printJobs)} accent="oklch(0.65 0.22 280)" />
          </>
        )}
      </div>

      {/* Quick Links */}
      <div style={{ marginBottom: "2rem" }}>
        <h2 style={{ fontSize: "1rem", fontWeight: 600, marginBottom: "1rem" }}>Quick Actions</h2>
        <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
          {[
            { href: "/admin/users", icon: Users, label: "Manage Users", color: "oklch(0.72 0.18 250)" },
            { href: "/admin/printers", icon: Printer, label: "Manage Printers", color: "oklch(0.72 0.18 145)" },
            { href: "/admin/financial", icon: CreditCard, label: "Financial", color: "oklch(0.75 0.15 85)" },
          ].map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="quick-action-link"
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                padding: "0.625rem 1rem",
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                color: "var(--color-foreground)",
                textDecoration: "none",
                fontSize: "0.875rem",
              }}
            >
              <item.icon size={16} style={{ color: item.color }} />
              {item.label}
            </Link>
          ))}
        </div>
      </div>

      {/* System Info */}
      <div
        style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-lg)",
          padding: "1.25rem",
        }}
      >
        <h2 style={{ fontSize: "1rem", fontWeight: 600, marginBottom: "1rem" }}>System Information</h2>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "0.75rem" }}>
          {[
            { label: "Version", value: "0.1.0" },
            { label: "Environment", value: process.env["NODE_ENV"] ?? "development" },
            { label: "API Server", value: API_URL },
            { label: "Database", value: "PostgreSQL 16" },
            { label: "Framework", value: "Fastify + tRPC" },
            { label: "Frontend", value: "Next.js 15" },
          ].map((item) => (
            <div key={item.label}>
              <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", textTransform: "uppercase", letterSpacing: "0.05em" }}>
                {item.label}
              </div>
              <div style={{ fontSize: "0.85rem", fontFamily: "var(--font-mono)", marginTop: "0.125rem" }}>
                {item.value}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
