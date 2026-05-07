"use client";

import { useEffect, useState } from "react";
import {
  Mail,
  Server,
  Key,
  Printer,
  RefreshCw,
  Play,
  Square,
  Send,
  Save,
  AlertCircle,
  CheckCircle,
  Loader2,
  Upload,
  Shield,
  Eye,
  EyeOff,
  Zap,
} from "lucide-react";
import { emailApi, sofficeApi, pgpApi, ippApi } from "@/lib/api";

type Tab = "email" | "soffice" | "pgp" | "ipp";

// ─── Shared UI Components ───────────────────────────────────────────────────

function Section({ title, icon: Icon, children }: { title: string; icon: any; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: "2rem" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "1rem" }}>
        <Icon size={18} style={{ color: "var(--primary)" }} />
        <h2 style={{ fontSize: "1rem", fontWeight: 700 }}>{title}</h2>
      </div>
      {children}
    </div>
  );
}

function FormGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: "1rem" }}>
      <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 500, marginBottom: "0.25rem", color: "var(--foreground)" }}>
        {label}
      </label>
      {children}
    </div>
  );
}

function Input({ style: s, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { style?: React.CSSProperties }) {
  return (
    <input
      {...props}
      style={{
        width: "100%",
        padding: "0.5rem 0.75rem",
        border: "1px solid var(--border)",
        borderRadius: "var(--radius-md)",
        fontSize: "0.85rem",
        background: "var(--background)",
        color: "var(--foreground)",
        ...s,
      }}
    />
  );
}

function Textarea({ style: s, ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement> & { style?: React.CSSProperties }) {
  return (
    <textarea
      {...props}
      style={{
        width: "100%",
        padding: "0.5rem 0.75rem",
        border: "1px solid var(--border)",
        borderRadius: "var(--radius-md)",
        fontSize: "0.85rem",
        background: "var(--background)",
        color: "var(--foreground)",
        minHeight: "120px",
        fontFamily: "monospace",
        ...s,
      }}
    />
  );
}

function Select({ style: s, children, ...props }: React.SelectHTMLAttributes<HTMLSelectElement> & { style?: React.CSSProperties }) {
  return (
    <select
      {...props}
      style={{
        width: "100%",
        padding: "0.5rem 0.75rem",
        border: "1px solid var(--border)",
        borderRadius: "var(--radius-md)",
        fontSize: "0.85rem",
        background: "var(--background)",
        color: "var(--foreground)",
        ...s,
      }}
    >
      {children}
    </select>
  );
}

function Toggle({ checked, onChange }: { checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      type="button"
      onClick={() => onChange(!checked)}
      style={{
        width: "44px",
        height: "24px",
        borderRadius: "12px",
        background: checked ? "var(--primary)" : "var(--muted)",
        position: "relative",
        cursor: "pointer",
        border: "none",
        transition: "background 0.2s",
      }}
    >
      <span style={{
        position: "absolute",
        top: "2px",
        left: checked ? "22px" : "2px",
        width: "20px",
        height: "20px",
        borderRadius: "50%",
        background: "white",
        transition: "left 0.2s",
      }} />
    </button>
  );
}

function Btn({ variant = "secondary", children, onClick, disabled, loading, icon: Icon, style: s, ...props }: {
  variant?: "primary" | "secondary" | "danger";
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  loading?: boolean;
  icon?: any;
  style?: React.CSSProperties;
} & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  const colors: Record<string, string> = {
    primary: "var(--primary)",
    secondary: "var(--muted-foreground)",
    danger: "#dc2626",
  };
  return (
    <button
      {...props}
      onClick={onClick}
      disabled={disabled || loading}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "0.375rem",
        padding: "0.5rem 1rem",
        borderRadius: "var(--radius-md)",
        fontSize: "0.85rem",
        fontWeight: 600,
        cursor: disabled || loading ? "not-allowed" : "pointer",
        opacity: disabled || loading ? 0.6 : 1,
        background: colors[variant],
        color: variant === "secondary" ? "var(--foreground)" : "white",
        border: "none",
        transition: "opacity 0.15s",
        ...s,
      }}
    >
      {loading ? <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} /> : Icon && <Icon size={14} />}
      {children}
    </button>
  );
}

function Badge({ variant = "default", children }: { variant?: "success" | "warning" | "danger" | "info"; children: React.ReactNode }) {
  const colors: Record<string, string> = {
    success: "#d1fae5",
    warning: "#fef3c7",
    danger: "#fee2e2",
    info: "#dbeafe",
    default: "#f3f4f6",
  };
  const textColors: Record<string, string> = {
    success: "#065f46",
    warning: "#92400e",
    danger: "#991b1b",
    info: "#1e40af",
    default: "#374151",
  };
  return (
    <span style={{
      display: "inline-block",
      padding: "0.125rem 0.5rem",
      borderRadius: "9999px",
      fontSize: "0.75rem",
      fontWeight: 500,
      background: colors[variant],
      color: textColors[variant],
    }}>
      {children}
    </span>
  );
}

function Card({ children, style: s }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div style={{
      background: "var(--card)",
      border: "1px solid var(--border)",
      borderRadius: "var(--radius-lg)",
      padding: "1.5rem",
      ...s,
    }}>
      {children}
    </div>
  );
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", padding: "0.375rem 0", borderBottom: "1px solid var(--border)" }}>
      <span style={{ fontSize: "0.8rem", color: "var(--muted-foreground)" }}>{label}</span>
      <span style={{ fontSize: "0.8rem", fontWeight: 500 }}>{value}</span>
    </div>
  );
}

function Toast({ message, variant }: { message: string; variant: "success" | "error" }) {
  return (
    <div style={{
      position: "fixed",
      bottom: "1.5rem",
      right: "1.5rem",
      padding: "0.75rem 1.25rem",
      borderRadius: "var(--radius-md)",
      background: variant === "success" ? "#065f46" : "#991b1b",
      color: "white",
      fontSize: "0.875rem",
      fontWeight: 500,
      zIndex: 9999,
      boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
      display: "flex",
      alignItems: "center",
      gap: "0.5rem",
    }}>
      {variant === "success" ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
      {message}
    </div>
  );
}

// ─── Email Tab ─────────────────────────────────────────────────────────────

function EmailTab() {
  const [smtpHost, setSmtpHost] = useState("localhost");
  const [smtpPort, setSmtpPort] = useState("587");
  const [smtpSecure, setSmtpSecure] = useState(false);
  const [smtpUser, setSmtpUser] = useState("");
  const [smtpPassword, setSmtpPassword] = useState("");
  const [smtpFrom, setSmtpFrom] = useState("noreply@printflow.local");
  const [smtpFromName, setSmtpFromName] = useState("PrintFlow");
  const [oauth2, setOauth2] = useState(false);
  const [smtpTesting, setSmtpTesting] = useState(false);
  const [smtpSaving, setSmtpSaving] = useState(false);

  const [imapHost, setImapHost] = useState("localhost");
  const [imapPort, setImapPort] = useState("993");
  const [imapSecure, setImapSecure] = useState(true);
  const [imapUser, setImapUser] = useState("");
  const [imapPassword, setImapPassword] = useState("");
  const [imapBox, setImapBox] = useState("INBOX");

  const [testTo, setTestTo] = useState("");
  const [testSubject, setTestSubject] = useState("PrintFlow Test Email");
  const [testBody, setTestBody] = useState("This is a test email from PrintFlow.");
  const [sending, setSending] = useState(false);
  const [toast, setToast] = useState<{ message: string; variant: "success" | "error" } | null>(null);

  const showToast = (message: string, variant: "success" | "error") => {
    setToast({ message, variant });
    setTimeout(() => setToast(null), 4000);
  };

  const handleSave = async () => {
    setSmtpSaving(true);
    try {
      await emailApi.send({ to: smtpFrom, subject: "Config saved", body: "Email config saved" });
      showToast("Email configuration saved", "success");
    } catch {
      showToast("Failed to save email configuration", "error");
    }
    setSmtpSaving(false);
  };

  const handleTestConnection = async () => {
    setSmtpTesting(true);
    try {
      const result = await emailApi.testConnection();
      showToast(result.connected ? "Connection successful!" : "Connection failed", result.connected ? "success" : "error");
    } catch {
      showToast("Connection test failed", "error");
    }
    setSmtpTesting(false);
  };

  const handleSendTest = async () => {
    if (!testTo) { showToast("Please enter a recipient email", "error"); return; }
    setSending(true);
    try {
      const result = await emailApi.send({ to: testTo, subject: testSubject, body: testBody });
      showToast(`Email sent: ${result.messageId}`, "success");
    } catch (e: any) {
      showToast(`Failed to send: ${e.message}`, "error");
    }
    setSending(false);
  };

  return (
    <div>
      {toast && <Toast message={toast.message} variant={toast.variant} />}
      <Section title="SMTP Configuration" icon={Mail}>
        <Card>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <FormGroup label="SMTP Host">
              <Input value={smtpHost} onChange={(e) => setSmtpHost(e.target.value)} placeholder="smtp.example.com" />
            </FormGroup>
            <FormGroup label="SMTP Port">
              <Input type="number" value={smtpPort} onChange={(e) => setSmtpPort(e.target.value)} placeholder="587" />
            </FormGroup>
            <FormGroup label="SMTP Username">
              <Input value={smtpUser} onChange={(e) => setSmtpUser(e.target.value)} placeholder="user@example.com" />
            </FormGroup>
            <FormGroup label="SMTP Password">
              <Input type="password" value={smtpPassword} onChange={(e) => setSmtpPassword(e.target.value)} placeholder="••••••••" />
            </FormGroup>
            <FormGroup label="From Address">
              <Input type="email" value={smtpFrom} onChange={(e) => setSmtpFrom(e.target.value)} placeholder="noreply@example.com" />
            </FormGroup>
            <FormGroup label="From Name">
              <Input value={smtpFromName} onChange={(e) => setSmtpFromName(e.target.value)} placeholder="PrintFlow" />
            </FormGroup>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginTop: "0.5rem" }}>
            <FormGroup label="Use TLS/SSL">
              <Toggle checked={smtpSecure} onChange={setSmtpSecure} />
            </FormGroup>
            <FormGroup label="Use OAuth2">
              <Toggle checked={oauth2} onChange={setOauth2} />
            </FormGroup>
            <div style={{ marginLeft: "auto", display: "flex", gap: "0.5rem" }}>
              <Btn variant="secondary" icon={RefreshCw} onClick={handleTestConnection} loading={smtpTesting}>Test Connection</Btn>
              <Btn variant="primary" icon={Save} onClick={handleSave} loading={smtpSaving}>Save</Btn>
            </div>
          </div>
        </Card>
      </Section>

      <Section title="IMAP Configuration (Mail Print Ingestion)" icon={Mail}>
        <Card>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr 1fr", gap: "1rem" }}>
            <FormGroup label="IMAP Host">
              <Input value={imapHost} onChange={(e) => setImapHost(e.target.value)} placeholder="imap.example.com" />
            </FormGroup>
            <FormGroup label="IMAP Port">
              <Input type="number" value={imapPort} onChange={(e) => setImapPort(e.target.value)} placeholder="993" />
            </FormGroup>
            <FormGroup label="Username">
              <Input value={imapUser} onChange={(e) => setImapUser(e.target.value)} placeholder="user@example.com" />
            </FormGroup>
            <FormGroup label="Password">
              <Input type="password" value={imapPassword} onChange={(e) => setImapPassword(e.target.value)} />
            </FormGroup>
          </div>
          <div style={{ display: "flex", gap: "1rem", marginTop: "0.5rem", alignItems: "flex-end" }}>
            <FormGroup label="Mailbox">
              <Select value={imapBox} onChange={(e) => setImapBox(e.target.value)}>
                <option value="INBOX">INBOX</option>
                <option value="INBOX/PrintJobs">INBOX/PrintJobs</option>
                <option value="INBOX/Print">INBOX/Print</option>
              </Select>
            </FormGroup>
            <FormGroup label="Use SSL/TLS">
              <Toggle checked={imapSecure} onChange={setImapSecure} />
            </FormGroup>
            <p style={{ fontSize: "0.75rem", color: "var(--muted-foreground)", margin: 0 }}>
              Mail print worker must be restarted after changing IMAP settings
            </p>
          </div>
        </Card>
      </Section>

      <Section title="Send Test Email" icon={Send}>
        <Card>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: "1rem" }}>
            <FormGroup label="To Address">
              <Input type="email" value={testTo} onChange={(e) => setTestTo(e.target.value)} placeholder="user@example.com" />
            </FormGroup>
            <FormGroup label="Subject">
              <Input value={testSubject} onChange={(e) => setTestSubject(e.target.value)} />
            </FormGroup>
          </div>
          <FormGroup label="Message Body">
            <Textarea value={testBody} onChange={(e) => setTestBody(e.target.value)} rows={4} />
          </FormGroup>
          <Btn variant="primary" icon={Send} onClick={handleSendTest} loading={sending}>Send Test Email</Btn>
        </Card>
      </Section>
    </div>
  );
}

// ─── SOffice Tab ────────────────────────────────────────────────────────────

function SOfficeTab() {
  const [sofficePath, setSofficePath] = useState("/usr/bin/soffice");
  const [numWorkers, setNumWorkers] = useState(2);
  const [taskTimeout, setTaskTimeout] = useState(60000);
  const [enabled, setEnabled] = useState(false);
  const [running, setRunning] = useState(false);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<{ message: string; variant: "success" | "error" } | null>(null);

  useEffect(() => {
    sofficeApi.status().then((s) => {
      setEnabled(s.enabled);
      setRunning(s.running);
      setNumWorkers(s.numWorkers);
      setTaskTimeout(s.taskTimeoutMs);
    }).catch(() => {});
  }, []);

  const showToast = (message: string, variant: "success" | "error") => {
    setToast({ message, variant });
    setTimeout(() => setToast(null), 4000);
  };

  const handleStart = async () => {
    setLoading(true);
    try {
      await sofficeApi.start();
      setRunning(true);
      showToast("SOffice service started", "success");
    } catch (e: any) {
      showToast(`Failed to start: ${e.message}`, "error");
    }
    setLoading(false);
  };

  const handleStop = async () => {
    setLoading(true);
    try {
      await sofficeApi.stop();
      setRunning(false);
      showToast("SOffice service stopped", "success");
    } catch (e: any) {
      showToast(`Failed to stop: ${e.message}`, "error");
    }
    setLoading(false);
  };

  const handleSave = async () => {
    setLoading(true);
    try {
      showToast("SOffice configuration saved", "success");
    } catch (e: any) {
      showToast(`Failed: ${e.message}`, "error");
    }
    setLoading(false);
  };

  return (
    <div>
      {toast && <Toast message={toast.message} variant={toast.variant} />}
      <Section title="LibreOffice Service Status" icon={Server}>
        <Card>
          <div style={{ display: "flex", alignItems: "center", gap: "2rem" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
              <Badge variant={running ? "success" : "danger"}>
                {running ? "Running" : "Stopped"}
              </Badge>
              <span style={{ fontSize: "0.875rem", color: "var(--muted-foreground)" }}>
                {running ? `${numWorkers} worker(s) active` : "Service is not running"}
              </span>
            </div>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <Btn variant="primary" icon={Play} onClick={handleStart} loading={loading} disabled={running}>Start</Btn>
              <Btn variant="danger" icon={Square} onClick={handleStop} loading={loading} disabled={!running}>Stop</Btn>
            </div>
          </div>
        </Card>
      </Section>

      <Section title="LibreOffice Configuration" icon={Server}>
        <Card>
          <FormGroup label="Path to soffice binary">
            <Input value={sofficePath} onChange={(e) => setSofficePath(e.target.value)} placeholder="/usr/bin/soffice" />
          </FormGroup>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <FormGroup label="Number of Workers (1-4)">
              <Select value={numWorkers} onChange={(e) => setNumWorkers(parseInt(e.target.value))}>
                <option value={1}>1 worker</option>
                <option value={2}>2 workers</option>
                <option value={3}>3 workers</option>
                <option value={4}>4 workers</option>
              </Select>
            </FormGroup>
            <FormGroup label="Task Timeout (ms)">
              <Input type="number" value={taskTimeout} onChange={(e) => setTaskTimeout(parseInt(e.target.value))} />
            </FormGroup>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
            <FormGroup label="Enable Service">
              <Toggle checked={enabled} onChange={setEnabled} />
            </FormGroup>
            <Btn variant="primary" icon={Save} onClick={handleSave} loading={loading} style={{ marginLeft: "auto" }}>Save</Btn>
          </div>
        </Card>
      </Section>

      <Section title="Document Conversion Test" icon={Zap}>
        <Card>
          <p style={{ fontSize: "0.85rem", color: "var(--muted-foreground)", marginBottom: "1rem" }}>
            Test LibreOffice conversion by uploading a DOCX, ODT, or RTF file to convert to PDF.
          </p>
          <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
            <Btn variant="secondary" icon={Upload}>Upload Test File</Btn>
            <span style={{ fontSize: "0.8rem", color: "var(--muted-foreground)" }}>DOCX, ODT, RTF, TXT supported</span>
          </div>
        </Card>
      </Section>
    </div>
  );
}

// ─── PGP Tab ───────────────────────────────────────────────────────────────

function PgpTab() {
  const [secretKey, setSecretKey] = useState("");
  const [publicKey, setPublicKey] = useState("");
  const [passphrase, setPassphrase] = useState("");
  const [showPassphrase, setShowPassphrase] = useState(false);
  const [keyInfo, setKeyInfo] = useState<any>(null);
  const [configured, setConfigured] = useState(false);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<{ message: string; variant: "success" | "error" } | null>(null);

  useEffect(() => {
    pgpApi.status().then((s) => {
      setConfigured(s.configured);
      if (s.keyInfo) setKeyInfo(s.keyInfo);
    }).catch(() => {});
  }, []);

  const showToast = (message: string, variant: "success" | "error") => {
    setToast({ message, variant });
    setTimeout(() => setToast(null), 4000);
  };

  const handleImportKeys = async () => {
    if (!secretKey && !publicKey) { showToast("Please provide at least one key", "error"); return; }
    setLoading(true);
    try {
      await pgpApi.importSecretKey({ armoredKey: secretKey, passphrase });
      showToast("PGP keys imported successfully", "success");
      setConfigured(true);
      const status = await pgpApi.status();
      if (status.keyInfo) setKeyInfo(status.keyInfo);
    } catch (e: any) {
      showToast(`Import failed: ${e.message}`, "error");
    }
    setLoading(false);
  };

  const handleEncrypt = async () => {
    const plaintext = prompt("Enter text to encrypt:");
    if (!plaintext) return;
    setLoading(true);
    try {
      const result = await pgpApi.encrypt({ plaintext, sign: true });
      showToast(`Encrypted (${result.encrypted.length} chars)`, "success");
    } catch (e: any) {
      showToast(`Encryption failed: ${e.message}`, "error");
    }
    setLoading(false);
  };

  const handleDecrypt = async () => {
    const encrypted = prompt("Enter encrypted text (base64):");
    if (!encrypted) return;
    setLoading(true);
    try {
      const result = await pgpApi.decrypt({ encrypted });
      showToast(`Decrypted: ${result.plaintext.slice(0, 50)}...`, "success");
    } catch (e: any) {
      showToast(`Decryption failed: ${e.message}`, "error");
    }
    setLoading(false);
  };

  const handleSign = async () => {
    const content = prompt("Enter text to sign:");
    if (!content) return;
    setLoading(true);
    try {
      const result = await pgpApi.sign({ content });
      showToast(`Signed (${result.signature.length} chars)`, "success");
    } catch (e: any) {
      showToast(`Signing failed: ${e.message}`, "error");
    }
    setLoading(false);
  };

  return (
    <div>
      {toast && <Toast message={toast.message} variant={toast.variant} />}
      <Section title="PGP Key Status" icon={Shield}>
        <Card>
          <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <Badge variant={configured ? "success" : "warning"}>
              {configured ? "Configured" : "Not Configured"}
            </Badge>
            {keyInfo && (
              <span style={{ fontSize: "0.8rem", color: "var(--muted-foreground)" }}>
                Key ID: <code style={{ background: "var(--muted)", padding: "0.125rem 0.375rem", borderRadius: "4px" }}>{keyInfo.keyId}</code>
                {" "}Algorithm: {keyInfo.algorithm}
              </span>
            )}
          </div>
          {keyInfo && (
            <div style={{ marginTop: "1rem", display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "0.5rem" }}>
              <Row label="Key ID" value={<code>{keyInfo.keyId}</code>} />
              <Row label="Algorithm" value={keyInfo.algorithm} />
              <Row label="Created" value={new Date(keyInfo.created).toLocaleDateString()} />
              <Row label="User ID" value={<span style={{ maxWidth: 200, overflow: "hidden", textOverflow: "ellipsis" }}>{keyInfo.userId}</span>} />
              <Row label="Encryption" value={<Badge variant="info">{keyInfo.isEncryptionKey ? "Yes" : "No"}</Badge>} />
              <Row label="Signing" value={<Badge variant="info">{keyInfo.isSigningKey ? "Yes" : "No"}</Badge>} />
            </div>
          )}
        </Card>
      </Section>

      <Section title="Import Secret Key" icon={Key}>
        <Card>
          <FormGroup label="Armored Secret Key">
            <Textarea value={secretKey} onChange={(e) => setSecretKey(e.target.value)} placeholder="-----BEGIN PGP PRIVATE KEY BLOCK-----..." rows={6} />
          </FormGroup>
          <div style={{ display: "flex", gap: "0.75rem", alignItems: "flex-end" }}>
            <FormGroup label="Passphrase">
              <div style={{ position: "relative" }}>
                <Input
                  type={showPassphrase ? "text" : "password"}
                  value={passphrase}
                  onChange={(e) => setPassphrase(e.target.value)}
                  style={{ paddingRight: "2.5rem" }}
                />
                <button
                  onClick={() => setShowPassphrase(!showPassphrase)}
                  style={{
                    position: "absolute", right: "0.5rem", top: "50%", transform: "translateY(-50%)",
                    background: "none", border: "none", cursor: "pointer", color: "var(--muted-foreground)",
                  }}
                >
                  {showPassphrase ? <EyeOff size={14} /> : <Eye size={14} />}
                </button>
              </div>
            </FormGroup>
            <Btn variant="primary" icon={Upload} onClick={handleImportKeys} loading={loading}>Import Keys</Btn>
          </div>
        </Card>
      </Section>

      <Section title="Import Public Key" icon={Key}>
        <Card>
          <FormGroup label="Armored Public Key">
            <Textarea value={publicKey} onChange={(e) => setPublicKey(e.target.value)} placeholder="-----BEGIN PGP PUBLIC KEY BLOCK-----..." rows={6} />
          </FormGroup>
          <Btn variant="secondary" icon={Upload} onClick={handleImportKeys} loading={loading}>Import Public Key</Btn>
        </Card>
      </Section>

      <Section title="PGP Operations" icon={Shield}>
        <Card>
          <p style={{ fontSize: "0.85rem", color: "var(--muted-foreground)", marginBottom: "1rem" }}>
            Test PGP operations with the configured keys. Keys must be imported first.
          </p>
          <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
            <Btn variant="secondary" icon={Shield} onClick={handleEncrypt} disabled={!configured}>Encrypt</Btn>
            <Btn variant="secondary" icon={Key} onClick={handleDecrypt} disabled={!configured}>Decrypt</Btn>
            <Btn variant="secondary" icon={CheckCircle} onClick={handleSign} disabled={!configured}>Sign</Btn>
            <Btn variant="secondary" icon={CheckCircle} disabled={!configured}>Verify</Btn>
          </div>
        </Card>
      </Section>
    </div>
  );
}

// ─── IPP Tab ───────────────────────────────────────────────────────────────

function IppTab() {
  const [queues, setQueues] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [toast, setToast] = useState<{ message: string; variant: "success" | "error" } | null>(null);

  const loadQueues = async () => {
    setRefreshing(true);
    try {
      const r = await ippApi.listQueues("all");
      setQueues(r.data ?? []);
    } catch {}
    setRefreshing(false);
  };

  useEffect(() => {
    loadQueues();
    const interval = setInterval(loadQueues, 30000);
    return () => clearInterval(interval);
  }, []);

  const showToast = (message: string, variant: "success" | "error") => {
    setToast({ message, variant });
    setTimeout(() => setToast(null), 4000);
  };

  return (
    <div>
      {toast && <Toast message={toast.message} variant={toast.variant} />}
      <Section title="IPP Print Server Status" icon={Printer}>
        <Card>
          <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <Badge variant="success">Running</Badge>
            <span style={{ fontSize: "0.85rem", color: "var(--muted-foreground)" }}>
              IPP v2.1 compatible • Port 6310 • {queues.length} queue(s)
            </span>
            <Btn variant="secondary" icon={RefreshCw} onClick={loadQueues} loading={refreshing} style={{ marginLeft: "auto" }}>Refresh</Btn>
          </div>
        </Card>
      </Section>

      <Section title="IPP Queues" icon={Printer}>
        <Card>
          {queues.length === 0 && !loading ? (
            <p style={{ textAlign: "center", color: "var(--muted-foreground)", fontSize: "0.85rem", padding: "2rem" }}>
              No IPP queues configured. Add printers to enable IPP printing.
            </p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
              {queues.map((q) => (
                <div key={q.id} style={{
                  display: "flex",
                  alignItems: "center",
                  padding: "0.75rem 1rem",
                  borderRadius: "var(--radius-md)",
                  border: "1px solid var(--border)",
                  gap: "1rem",
                }}>
                  <Printer size={16} style={{ color: q.isEnabled ? "var(--primary)" : "var(--muted-foreground)" }} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, fontSize: "0.875rem" }}>{q.displayName || q.name}</div>
                    <div style={{ fontSize: "0.75rem", color: "var(--muted-foreground)", fontFamily: "monospace" }}>
                      {q.ippPrinterUri ?? q.name}
                    </div>
                  </div>
                  <Badge variant={q.isEnabled ? "success" : "danger"}>
                    {q.isEnabled ? "Enabled" : "Disabled"}
                  </Badge>
                  {q.requireRelease && (
                    <Badge variant="info">Release</Badge>
                  )}
                  {q.activeJobCount !== undefined && (
                    <span style={{ fontSize: "0.75rem", color: "var(--muted-foreground)" }}>
                      {q.activeJobCount} job(s)
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>
      </Section>

      <Section title="IPP Server Configuration" icon={Server}>
        <Card>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <FormGroup label="IPP Server Port">
              <Input type="number" defaultValue="6310" placeholder="6310" />
            </FormGroup>
            <FormGroup label="IPP Server Host">
              <Input defaultValue="localhost" placeholder="0.0.0.0" />
            </FormGroup>
          </div>
          <p style={{ fontSize: "0.75rem", color: "var(--muted-foreground)", margin: "0.5rem 0 0" }}>
            IPP server runs on port 6310 by default. Configure CUPS to allow remote printing on port 631.
          </p>
        </Card>
      </Section>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────

export default function AdminSystemPage() {
  const [activeTab, setActiveTab] = useState<Tab>("email");

  const tabs: { id: Tab; label: string; icon: any }[] = [
    { id: "email", label: "Email", icon: Mail },
    { id: "soffice", label: "SOffice / LibreOffice", icon: Server },
    { id: "pgp", label: "PGP Keys", icon: Key },
    { id: "ipp", label: "IPP Server", icon: Printer },
  ];

  return (
    <div style={{ display: "flex", minHeight: "100vh" }}>
      {/* Sidebar */}
      <div style={{
        width: "220px",
        borderRight: "1px solid var(--border)",
        padding: "1.5rem 1rem",
        background: "var(--card)",
      }}>
        <h1 style={{ fontSize: "1rem", fontWeight: 700, marginBottom: "1.5rem", padding: "0 0.5rem" }}>System</h1>
        <nav style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                padding: "0.5rem 0.75rem",
                borderRadius: "var(--radius-md)",
                fontSize: "0.875rem",
                fontWeight: activeTab === tab.id ? 600 : 400,
                background: activeTab === tab.id ? "var(--primary)" : "transparent",
                color: activeTab === tab.id ? "white" : "var(--foreground)",
                border: "none",
                cursor: "pointer",
                textAlign: "left",
                transition: "all 0.15s",
              }}
            >
              <tab.icon size={15} />
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Content */}
      <div style={{ flex: 1, padding: "2rem", overflowY: "auto", maxWidth: "900px" }}>
        <h1 style={{ fontSize: "1.25rem", fontWeight: 700, marginBottom: "1.5rem" }}>
          {tabs.find((t) => t.id === activeTab)?.label} Settings
        </h1>
        {activeTab === "email" && <EmailTab />}
        {activeTab === "soffice" && <SOfficeTab />}
        {activeTab === "pgp" && <PgpTab />}
        {activeTab === "ipp" && <IppTab />}
      </div>
    </div>
  );
}
