"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, AlertCircle, CheckCircle, Loader2 } from "lucide-react";
import { printersApi, type PrinterGroup } from "@/lib/api";

export default function AdminNewPrinterPage() {
  const router = useRouter();

  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => { if (!token) router.push("/login"); }, [router, token]);

  const [groups, setGroups] = useState<PrinterGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [form, setForm] = useState({
    name: "",
    displayName: "",
    description: "",
    printerType: "NETWORK",
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
    printersApi.listGroups()
      .then((g) => setGroups(g))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  function update(field: string, value: any) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");
    if (!form.name.trim()) { setError("Printer name is required"); return; }

    setSaving(true);
    try {
      await printersApi.create({
        ...form,
        printerGroupId: form.printerGroupId ? Number(form.printerGroupId) : undefined,
      });
      setSuccess("Printer created successfully!");
      setTimeout(() => router.push("/admin/printers"), 1500);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  function field(label: string, children: React.ReactNode, hint?: string) {
    return (
      <div>
        <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 500, marginBottom: "0.25rem", color: "var(--color-muted-foreground)" }}>
          {label}
        </label>
        {children}
        {hint && <p style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)", marginTop: "0.25rem" }}>{hint}</p>}
      </div>
    );
  }

  function input(props: any) {
    return (
      <input
        {...props}
        style={{
          width: "100%",
          padding: "0.5rem 0.75rem",
          background: "var(--color-input)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-md)",
          color: "var(--color-foreground)",
          fontSize: "0.875rem",
          outline: "none",
          ...props.style,
        }}
        onFocus={(e) => { e.target.style.borderColor = "var(--color-ring)"; }}
        onBlur={(e) => { e.target.style.borderColor = "var(--color-border)"; }}
      />
    );
  }

  function toggle(name: string, label: string) {
    return (
      <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer", fontSize: "0.875rem" }}>
        <input
          type="checkbox"
          checked={(form as any)[name]}
          onChange={(e) => update(name, e.target.checked)}
          style={{ width: "16px", height: "16px", cursor: "pointer" }}
        />
        {label}
      </label>
    );
  }

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1.5rem" }}>
        <Link href="/admin/printers" style={linkStyle()}>
          <ArrowLeft size={16} />
        </Link>
        <div>
          <h1 style={{ fontSize: "1.5rem", fontWeight: 700 }}>Add Printer</h1>
          <p style={{ color: "var(--color-muted-foreground)", fontSize: "0.875rem", marginTop: "0.25rem" }}>Register a new printer or print queue</p>
        </div>
      </div>

      {success && <div style={alertStyle("success", success)}><CheckCircle size={16} /> {success}</div>}
      {error && <div style={alertStyle("error", error)}><AlertCircle size={16} /> {error}</div>}

      <form onSubmit={handleSubmit}>
        <div style={{ display: "grid", gap: "1.5rem", gridTemplateColumns: "repeat(auto-fill, minmax(440px, 1fr))" }}>
          {/* Basic */}
          <div style={cardStyle()}>
            <h3 style={sectionTitle()}>Basic Information</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              {field("Printer Name *", input({ value: form.name, onChange: (e: any) => update("name", e.target.value), placeholder: "e.g. Canon-C5540i-Lobby" }))}
              {field("Display Name", input({ value: form.displayName, onChange: (e: any) => update("displayName", e.target.value), placeholder: "e.g. Canon C5540i — Lobby" }))}
              {field("Description", input({ value: form.description, onChange: (e: any) => update("description", e.target.value), placeholder: "Optional description" }))}
              {field("Printer Type",
                <select value={form.printerType} onChange={(e: any) => update("printerType", e.target.value)} style={selectStyle()}>
                  {["NETWORK", "USB", "DRIVER", "PDF", "EMAIL", "HOLD"].map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              )}
              {field("Printer Group",
                <select value={form.printerGroupId} onChange={(e: any) => update("printerGroupId", e.target.value)} style={selectStyle()}>
                  <option value="">No Group</option>
                  {groups.map((g) => <option key={g.id} value={String(g.id)}>{g.name}</option>)}
                </select>
              )}
            </div>
          </div>

          {/* Connection */}
          <div style={cardStyle()}>
            <h3 style={sectionTitle()}>Connection</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              {field("IPP Printer URI", input({ value: form.ippPrinterUri, onChange: (e: any) => update("ippPrinterUri", e.target.value), placeholder: "ipp://printer.local:631/ipp/print" }), "e.g. ipp://192.168.1.100:631/ipp/print")}
              {field("CUPS Printer Name", input({ value: form.cupsPrinterName, onChange: (e: any) => update("cupsPrinterName", e.target.value), placeholder: "e.g. Canon_C5540i" }))}
              <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>Capabilities</span>
                {toggle("supportsDuplex", "Duplex (two-sided)")}
                {toggle("supportsStaple", "Stapling")}
                {toggle("supportsPunch", "Hole Punching")}
                {toggle("supportsFold", "Folding")}
                {toggle("supportsBanner", "Banner Pages")}
              </div>
            </div>
          </div>

          {/* Costing */}
          <div style={cardStyle()}>
            <h3 style={sectionTitle()}>Costing</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              {field("Color Mode",
                <select value={form.colorMode} onChange={(e: any) => update("colorMode", e.target.value)} style={selectStyle()}>
                  {["AUTO", "MONOCHROME", "COLOR"].map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              )}
              {field("Cost per Page (Mono)", input({ value: form.costPerPageMono, onChange: (e: any) => update("costPerPageMono", e.target.value), type: "number", step: "0.0001", min: "0" }), "In credits per page")}
              {field("Cost per Page (Color)", input({ value: form.costPerPageColor, onChange: (e: any) => update("costPerPageColor", e.target.value), type: "number", step: "0.0001", min: "0" }), "In credits per page")}
              <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>Options</span>
                {toggle("ecoPrintDefault", "Eco Print by Default")}
              </div>
            </div>
          </div>

          {/* Flags */}
          <div style={cardStyle()}>
            <h3 style={sectionTitle()}>Settings</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
              {toggle("isEnabled", "Printer Enabled")}
              {toggle("isPublic", "Public (visible to all users)")}
              {toggle("requireRelease", "Require Release (pull printing)")}
              {toggle("snmpEnabled", "SNMP Monitoring")}
              {field("SNMP Community", input({ value: form.snmpCommunity, onChange: (e: any) => update("snmpCommunity", e.target.value) }), "Default: public")}
              {field("Max Paper Size", input({ value: form.maxPaperSize, onChange: (e: any) => update("maxPaperSize", e.target.value) }), "e.g. A4, A3, Letter")}
            </div>
          </div>
        </div>

        <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end", marginTop: "1.5rem" }}>
          <Link href="/admin/printers" style={cancelStyle()}>Cancel</Link>
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
            Create Printer
          </button>
        </div>
      </form>
    </div>
  );
}

const cardStyle = () => ({ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-xl)", padding: "1.5rem" });
const sectionTitle = () => ({ fontSize: "0.9rem", fontWeight: 700, marginBottom: "1.25rem", paddingBottom: "0.75rem", borderBottom: "1px solid var(--color-border)" });
const linkStyle = () => ({ display: "inline-flex", alignItems: "center", padding: "0.375rem 0.75rem", background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-muted-foreground)", textDecoration: "none" });
const cancelStyle = () => ({ padding: "0.625rem 1.25rem", background: "var(--color-secondary)", color: "var(--color-secondary-foreground)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", textDecoration: "none", fontSize: "0.875rem", fontWeight: 600 });
const selectStyle = () => ({ width: "100%", padding: "0.5rem 0.75rem", background: "var(--color-input)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-foreground)", fontSize: "0.875rem", outline: "none", cursor: "pointer" });
const alertStyle = (t: string, m: string) => ({ padding: "0.75rem 1rem", background: t === "success" ? "oklch(0.72 0.18 145 / 0.1)" : "oklch(0.65 0.22 25 / 0.1)", border: `1px solid ${t === "success" ? "oklch(0.72 0.18 145 / 0.3)" : "oklch(0.65 0.22 25 / 0.3)"}`, borderRadius: "var(--radius-md)", color: t === "success" ? "oklch(0.72 0.18 145)" : "oklch(0.65 0.22 25)", fontSize: "0.875rem", marginBottom: "1rem", display: "flex", alignItems: "center", gap: "0.5rem" });
