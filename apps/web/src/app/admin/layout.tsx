"use client";

import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import {
  Printer,
  LayoutDashboard,
  FileText,
  Settings,
  LogOut,
  Users,
  CreditCard,
  BarChart3,
  Menu,
  X,
  ChevronDown,
  ShoppingCart,
  Activity,
  MessageSquare,
} from "lucide-react";
import { useState } from "react";

const NAV_ITEMS = [
  { href: "/admin", icon: LayoutDashboard, label: "Dashboard" },
  { href: "/admin/users", icon: Users, label: "Users" },
  { href: "/admin/printers", icon: Printer, label: "Printers" },
  { href: "/admin/documents", icon: FileText, label: "Documents" },
  { href: "/admin/audit-log", icon: BarChart3, label: "Audit Log" },
  { href: "/admin/financial", icon: CreditCard, label: "Financial" },
  { href: "/admin/reports", icon: BarChart3, label: "Reports" },
  { href: "/admin/pos", icon: ShoppingCart, label: "POS" },
  { href: "/admin/snmp", icon: Activity, label: "SNMP" },
  { href: "/admin/oauth", icon: Settings, label: "OAuth" },
  { href: "/admin/telegram", icon: MessageSquare, label: "Telegram" },
  { href: "/admin/system", icon: Settings, label: "System" },
  { href: "/admin/settings", icon: Settings, label: "Settings" },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [user, setUser] = useState<any>(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);

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

  async function handleLogout() {
    const token = localStorage.getItem("printflow_token");
    if (token) {
      await fetch(`${process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001"}/api/v1/auth/logout`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
    }
    localStorage.removeItem("printflow_token");
    localStorage.removeItem("printflow_user");
    router.push("/login");
  }

  if (!user) return null;

  return (
    <div style={{ display: "flex", minHeight: "100vh" }}>
      {/* Sidebar */}
      <aside
        className={`admin-sidebar${sidebarOpen ? " is-open" : ""}`}
        style={{
          width: "260px",
          background: "oklch(0.14 0.02 250)",
          borderRight: "1px solid var(--color-border)",
          display: "flex",
          flexDirection: "column",
          position: "fixed",
          top: 0,
          left: 0,
          height: "100vh",
          zIndex: 40,
        }}
      >
        {/* Logo */}
        <div
          style={{
            padding: "1.25rem 1.5rem",
            borderBottom: "1px solid var(--color-border)",
            display: "flex",
            alignItems: "center",
            gap: "0.75rem",
          }}
        >
          <div
            style={{
              width: "36px",
              height: "36px",
              borderRadius: "10px",
              background: "linear-gradient(135deg, oklch(0.72 0.18 250), oklch(0.55 0.22 280))",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
            }}
          >
            <Printer size={18} color="white" />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: "1rem", letterSpacing: "-0.01em" }}>PrintFlow</div>
            <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)" }}>Admin Panel</div>
          </div>
        </div>

        {/* Nav */}
        <nav style={{ flex: 1, padding: "1rem 0.75rem", overflowY: "auto" }}>
          {NAV_ITEMS.map((item) => {
            const active = pathname === item.href || pathname.startsWith(item.href + "/");
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setSidebarOpen(false)}
                className="admin-nav-link"
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  padding: "0.625rem 0.875rem",
                  borderRadius: "var(--radius-md)",
                  color: active ? "var(--color-primary)" : "var(--color-muted-foreground)",
                  background: active ? "oklch(0.72 0.18 250 / 0.1)" : "transparent",
                  fontWeight: active ? 600 : 400,
                  fontSize: "0.875rem",
                  textDecoration: "none",
                  marginBottom: "2px",
                }}
              >
                <Icon size={18} style={{ flexShrink: 0 }} />
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* User section */}
        <div
          style={{
            padding: "0.75rem",
            borderTop: "1px solid var(--color-border)",
          }}
        >
          {/* User menu */}
          <div style={{ position: "relative" }}>
            <button
              onClick={() => setUserMenuOpen((o) => !o)}
              className="admin-user-btn"
              style={{
                width: "100%",
                display: "flex",
                alignItems: "center",
                gap: "0.625rem",
                padding: "0.5rem 0.625rem",
                background: "transparent",
                border: "none",
                borderRadius: "var(--radius-md)",
                cursor: "pointer",
                color: "var(--color-foreground)",
                textAlign: "left",
              }}
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
                  fontSize: "0.8rem",
                  fontWeight: 700,
                  color: "white",
                  flexShrink: 0,
                }}
              >
                {user.userName?.[0]?.toUpperCase() ?? "U"}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: "0.8rem", fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {user.fullName}
                </div>
                <div style={{ fontSize: "0.7rem", color: "var(--color-muted-foreground)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {user.userName}
                </div>
              </div>
              <ChevronDown size={14} style={{ flexShrink: 0, transform: userMenuOpen ? "rotate(180deg)" : "none", transition: "transform 0.2s" }} />
            </button>

            {userMenuOpen && (
              <div
                className="admin-user-menu-dropdown animate-fade-in"
                style={{
                  position: "absolute",
                  bottom: "100%",
                  left: 0,
                  right: 0,
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  marginBottom: "0.25rem",
                  overflow: "hidden",
                  boxShadow: "var(--shadow-lg)",
                }}
              >
              <Link
                href="/user"
                className="admin-menu-item"
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                  padding: "0.625rem 0.875rem",
                  color: "var(--color-foreground)",
                  fontSize: "0.8rem",
                  textDecoration: "none",
                }}
              >
                User Portal
              </Link>
              <button
                onClick={handleLogout}
                className="admin-menu-item admin-menu-item-danger"
                style={{
                  width: "100%",
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                  padding: "0.625rem 0.875rem",
                  background: "transparent",
                  border: "none",
                  color: "oklch(0.65 0.22 25)",
                  fontSize: "0.8rem",
                  cursor: "pointer",
                  textAlign: "left",
                }}
              >
                <LogOut size={14} /> Sign out
              </button>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="admin-mobile-overlay"
          style={{
            position: "fixed",
            inset: 0,
            background: "oklch(0 0 0 / 0.6)",
            zIndex: 30,
          }}
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Main content */}
      <div className="admin-main" style={{ flex: 1, marginLeft: "260px", minHeight: "100vh" }}>
        {/* Top bar */}
        <header
          style={{
            height: "56px",
            borderBottom: "1px solid var(--color-border)",
            display: "flex",
            alignItems: "center",
            padding: "0 1.5rem",
            position: "sticky",
            top: 0,
            background: "oklch(0.12 0.02 250 / 0.8)",
            backdropFilter: "blur(12px)",
            WebkitBackdropFilter: "blur(12px)",
            zIndex: 20,
          }}
        >
          <button
            onClick={() => setSidebarOpen((o) => !o)}
            className="admin-hamburger-btn"
            style={{
              background: "none",
              border: "none",
              color: "var(--color-foreground)",
              cursor: "pointer",
              padding: "0.25rem",
            }}
          >
            <Menu size={20} />
          </button>
          <div style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>
            {pathname.replace("/admin", "Admin").replace(/\//g, " / ")}
          </div>
          <div style={{ flex: 1 }} />
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              fontSize: "0.8rem",
              color: "var(--color-muted-foreground)",
            }}
          >
            <div
              style={{
                width: "8px",
                height: "8px",
                borderRadius: "50%",
                background: "oklch(0.72 0.18 145)",
                boxShadow: "0 0 6px oklch(0.72 0.18 145 / 0.5)",
              }}
            />
            System Online
          </div>
        </header>

        {/* Page content */}
        <main style={{ padding: "1.5rem" }}>{children}</main>
      </div>
    </div>
  );
}
