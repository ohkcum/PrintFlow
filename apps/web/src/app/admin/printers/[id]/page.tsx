"use client";

import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, AlertCircle, CheckCircle, Loader2, Trash2 } from "lucide-react";
import { printersApi, type Printer, type PrinterGroup } from "@/lib/api";

export default function AdminEditPrinterPage() {
  const router = useRouter();
  const params = useParams();
  const printerId = parseInt(params.id as string, 10);

  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => { if (!token) router.push("/login"); }, [router, token]);

  const [printer, setPrinter] = useState<Printer | null>(null);
  const [groups, setGroups] = useState<PrinterGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [form, setForm] = useState({
    name: "",
    displayName: "",
    description: "",
    printerType: "NETWORK",
    printerStatus: "OFFLINE",
    ippPrinterUri: "",
    cupsPrinterName: "",
    printerGroupId: "" as string | number,
    colorMode: "AUTO",
    supportsDuplex: true,
    supportsStaple: false,
    supportsPunch: false,
    supportsFold: false,
    supportsBanner: false,
    maxPaperSize: "A4",
    costPerPageMono: "0.01",
    costPerPageColor: "0.05",
    isEnabled: true,
    isPublic: true,
    requireRelease: true,
    ecoPrintDefault: false,
    snmpEnabled: false,
    snmpCommunity: "public",
  });

  useEffect(() => {
    if (!printerId || isNaN(printerId)) { router.push("/admin/printers"); return; }
    Promise.all([
      printersApi.get(printerId),
      printersApi.listGroups(),
    ]).then(([p, g]) => {
      setPrinter(p);
      setGroups(g);
      setForm({
        name: p.name,
        displayName: p.displayName ?? "",
        description: p.description ?? "",
        printerType: p.printerType,
        printerStatus: p.printerStatus,
        ippPrinterUri: p.ippPrinterUri ?? "",
        cupsPrinterName: p.cupsPrinterName ?? "",
        printerGroupId: p.printerGroupId ?? "",
        colorMode: p.colorMode ?? "AUTO",
        supportsDuplex: p.supportsDuplex ?? true,
        supportsStaple: p.supportsStaple ?? false,
        supportsPunch: p.supportsPunch ?? false,
        supportsFold: p.supportsFold ?? false,
        supportsBanner: p.supportsBanner ?? false,
        maxPaperSize: p.maxPaperSize ?? "A4",
        costPerPageMono: p.costPerPageMono ?? "0.01",
        costPerPageColor: p.costPerPageColor ?? "0.05",
        isEnabled: p.isEnabled ?? true,
        isPublic: p.isPublic ?? true,
        requireRelease: p.requireRelease ?? true,
        ecoPrintDefault: p.ecoPrintDefault ?? false,
        snmpEnabled: p.snmpEnabled ?? false,
        snmpCommunity: p.snmpCommunity ?? "public",
      });
    }).catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [printerId, router]);

  function update(field: string, value: any) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setSaving(true);
    try {
      await printersApi.update(printerId, {
        ...form,
        printerGroupId: form.printerGroupId ? Number(form.printerGroupId) : null,
      } as any);
      setSuccess("Printer updated successfully!");
      setTimeout(() => setSuccess(""), 3000);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDisable() {
    if (!confirm("Disable this printer? It will stop accepting new print jobs.")) return;
    setDeleting(true);
    try {
      await printersApi.delete(printerId);
      router.push("/admin/printers");
    } catch (e: any) {
      setError(e.message);
    } finally {
      setDeleting(false);
    }
  }

  if (loading) return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", padding: "4rem" }}>
      <Loader2 size={24} style={{ animation: "spin 1s linear infinite", color: "var(--color-muted-foreground)" }} />
    </div>
  );

  const STATUS_COLORS: Record<string, string> = {
    ONLINE: "oklch(0.72 0.18 145)", OFFLINE: "oklch(0.55 0.05 250)",
    IDLE: "oklch(0.75 0.15 85)", BUSY: "oklch(0.72 0.18 250)",
    ERROR: "oklch(0.65 0.22 25)", MAINTENANCE: "oklch(0.72 0.18 145)",
  };

  function input(props: any) {
    return <input {...props} style={{ width: "100%", padding: "0.5rem 0.75rem", background: "var(--color-input)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-foreground)", fontSize: "0.875rem", outline: "none", ...props.style }} onFocus={(e) => { e.target.style.borderColor = "var(--color-ring)"; }} onBlur={(e) => { e.target.style.borderColor = "var(--color-border)"; }} />;
  }
  function selectEl(props: any, children: React.ReactNode) {
    return <select {...props} style={{ width: "100%", padding: "0.5rem 0.75rem", background: "var(--color-input)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-foreground)", fontSize: "0.875rem", outline: "none", cursor: "pointer" }}>{children}</select>;
  }

  const cardStyle = { background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-xl)", padding: "1.5rem" };
  const sectionTitle = { fontSize: "0.9rem", fontWeight: 700, marginBottom: "1.25rem", paddingBottom: "0.75rem", borderBottom: "1px solid var(--color-border)" };
  const linkStyle = { display: "inline-flex", alignItems: "center", padding: "0.375rem 0.75rem", background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-muted-foreground)", textDecoration: "none" };
  const alertStyle = (t: string) => ({ padding: "0.75rem 1rem", background: t === "success" ? "oklch(0.72 0.18 145 / 0.1)" : "oklch(0.65 0.22 25 / 0.1)", border: `1px solid ${t === "success" ? "oklch(0.72 0.18 145 / 0.3)" : "oklch(0.65 0.22 25 / 0.3)"}`, borderRadius: "var(--radius-md)", color: t === "success" ? "oklch(0.72 0.18 145)" : "oklch(0.65 0.22 25)", fontSize: "0.875rem", marginBottom: "1rem", display: "flex", alignItems: "center", gap: "0.5rem" });

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1.5rem" }}>
        <Link href="/admin/printers" style={linkStyle}><ArrowLeft size={16} /></Link>
        <div style={{ flex: 1 }}>
          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
            <h1 style={{ fontSize: "1.5rem", fontWeight: 700 }}>{printer?.displayName || printer?.name}</h1>
            <span style={{ padding: "2px 8px", borderRadius: "999px", fontSize: "0.7rem", fontWeight: 600, background: "oklch(0.72 0.18 250 / 0.1)", color: STATUS_COLORS[printer?.printerStatus ?? "OFFLINE"] }}>
              {printer?.printerStatus}
            </span>
          </div>
          <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.8rem", fontFamily: "var(--font-mono)", marginTop: "0.25rem" }}>
            ID #{printerId} · {printer?.printerType}
          </p>
        </div>
        <button
          onClick={handleDisable}
          disabled={deleting}
          style={{
            padding: "0.5rem 1rem",
            background: "transparent",
            border: "1px solid oklch(0.65 0.22 25 / 0.4)",
            borderRadius: "var(--radius-md)",
            color: "oklch(0.65 0.22 25)",
            fontSize: "0.85rem",
            fontWeight: 600,
            cursor: deleting ? "not-allowed" : "pointer",
            display: "inline-flex",
            alignItems: "center",
            gap: "0.375rem",
          }}
        >
          <Trash2 size={14} />
          {deleting ? "..." : "Disable"}
        </button>
      </div>

      {success && <div style={alertStyle("success")}><CheckCircle size={16} /> {success}</div>}
      {error && <div style={alertStyle("error")}><AlertCircle size={16} /> {error}</div>}

      <form onSubmit={handleSave}>
        <div style={{ display: "grid", gap: "1.5rem", gridTemplateColumns: "repeat(auto-fill, minmax(440px, 1fr))" }}>
          <div style={cardStyle}>
            <h3 style={sectionTitle}>Basic Information</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>Printer Name</label>
                {input({ value: form.name, onChange: (e: any) => update("name", e.target.value) })}
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>Display Name</label>
                {input({ value: form.displayName, onChange: (e: any) => update("displayName", e.target.value) })}
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>Status</label>
                {selectEl({ value: form.printerStatus, onChange: (e: any) => update("printerStatus", e.target.value) },
                  ["ONLINE", "OFFLINE", "IDLE", "BUSY", "ERROR", "MAINTENANCE"].map((s) => <option key={s} value={s}>{s}</option>))}
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>Printer Group</label>
                {selectEl({ value: form.printerGroupId, onChange: (e: any) => update("printerGroupId", e.target.value) },
                  [<option key="" value="">No Group</option>, ...groups.map((g) => <option key={g.id} value={String(g.id)}>{g.name}</option>)])}
              </div>
            </div>
          </div>

          <div style={cardStyle}>
            <h3 style={sectionTitle}>Connection</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>IPP Printer URI</label>
                {input({ value: form.ippPrinterUri, onChange: (e: any) => update("ippPrinterUri", e.target.value), placeholder: "ipp://..." })}
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>CUPS Printer Name</label>
                {input({ value: form.cupsPrinterName, onChange: (e: any) => update("cupsPrinterName", e.target.value) })}
              </div>
            </div>
          </div>

          <div style={cardStyle}>
            <h3 style={sectionTitle}>Costing</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              {([["Mono", "costPerPageMono"], ["Color", "costPerPageColor"]] as [string, string][]).map(([label, key]) => (
                <div key={key}>
                  <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>Cost per Page ({label})</label>
                  {input({ value: (form as Record<string, unknown>)[key!], onChange: (e: any) => update(key!, e.target.value), type: "number", step: "0.0001", min: "0" })}
                </div>
              ))}
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", color: "var(--color-muted-foreground)", marginBottom: "0.25rem" }}>Color Mode</label>
                {selectEl({ value: form.colorMode, onChange: (e: any) => update("colorMode", e.target.value) },
                  ["AUTO", "MONOCHROME", "COLOR"].map((c) => <option key={c} value={c}>{c}</option>))}
              </div>
            </div>
          </div>

          <div style={cardStyle}>
            <h3 style={sectionTitle}>Settings</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
              {([
                ["isEnabled", "Printer Enabled"],
                ["isPublic", "Public"],
                ["requireRelease", "Require Release"],
                ["snmpEnabled", "SNMP Monitoring"],
              ] as [string, string][]).map(([key, label]) => (
                <label key={key} style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer", fontSize: "0.875rem" }}>
                  <input type="checkbox" checked={(form as Record<string, unknown>)[key!] as boolean} onChange={(e: any) => update(key!, e.target.checked)} style={{ width: "16px", height: "16px" }} />
                  {label}
                </label>
              ))}
            </div>
          </div>
        </div>

        <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end", marginTop: "1.5rem" }}>
          <Link href="/admin/printers" style={{ padding: "0.625rem 1.25rem", background: "var(--color-secondary)", color: "var(--color-secondary-foreground)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", textDecoration: "none", fontSize: "0.875rem", fontWeight: 600 }}>
            Cancel
          </Link>
          <button type="submit" disabled={saving} style={{
            padding: "0.625rem 1.5rem",
            background: saving ? "oklch(0.72 0.18 250 / 0.5)" : "var(--color-primary)",
            color: "var(--color-primary-foreground)",
            border: "none",
            borderRadius: "var(--radius-md)",
            fontSize: "0.875rem",
            fontWeight: 600,
            cursor: saving ? "not-allowed" : "pointer",
            display: "inline-flex",
            alignItems: "center",
            gap: "0.5rem",
          }}>
            {saving && <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} />}
            Save Changes
          </button>
        </div>
      </form>
    </div>
  );
}
