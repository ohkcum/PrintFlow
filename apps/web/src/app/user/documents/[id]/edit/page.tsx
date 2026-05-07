"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Save, RotateCcw, Loader2, CheckCircle, AlertCircle, Printer } from "lucide-react";
import { PageEditor, PageEditorToolbar, type CanvasEditorHandle } from "@/components/editor/PageEditor";
import { documentsApi, printersApi, type DocIn, type Printer as PrinterType } from "@/lib/api";

const API_URL = process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001";

export default function DocumentEditorPage() {
  const router = useRouter();
  const params = useParams();
  const docId = parseInt(params.id as string, 10);
  const editorRef = useRef<CanvasEditorHandle | null>(null);

  const [doc, setDoc] = useState<DocIn | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [canvasSize, setCanvasSize] = useState({ width: 800, height: 1100 });

  const token = typeof window !== "undefined" ? localStorage.getItem("printflow_token") : null;
  useEffect(() => { if (!token) router.push("/login"); }, [router, token]);

  useEffect(() => {
    if (!docId || isNaN(docId)) { router.push("/user"); return; }

    documentsApi.get(docId)
      .then((d) => {
        setDoc(d);
        // Build thumbnail URL from file path
        const baseUrl = API_URL.replace("3001", "3001");
        setImageUrl(`${baseUrl}/api/v1/documents/${docId}/thumbnail`);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [docId, router]);

  async function handleSave() {
    if (!editorRef.current || !doc) return;
    setError("");
    setSaving(true);

    try {
      const editor = editorRef.current;
      const hasText = editor.hasTextObjects();

      // Prefer SVG for shapes, JSON for text
      const svg = editor.toSVG();
      const json = editor.toJSON();

      // Save to server
      const res = await fetch(`${API_URL}/api/v1/documents/${docId}/overlay`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          svgBase64: btoa(svg),
          jsonBase64: hasText ? btoa(json) : null,
        }),
      });

      const data = await res.json();
      if (!data.success) throw new Error(data.error?.message ?? "Save failed");

      setSuccess("Overlay saved!");
      setTimeout(() => setSuccess(""), 3000);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  function handleReset() {
    if (!confirm("Reset to original? This will clear all annotations.")) return;
    editorRef.current?.clear();
  }

  if (loading) {
    return (
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", padding: "4rem" }}>
        <Loader2 size={24} style={{ animation: "spin 1s linear infinite", color: "var(--color-muted-foreground)" }} />
      </div>
    );
  }

  if (!doc) {
    return (
      <div style={{ textAlign: "center", padding: "3rem" }}>
        <AlertCircle size={40} style={{ color: "oklch(0.65 0.22 25)", margin: "0 auto 1rem" }} />
        <p style={{ color: "var(--color-muted-foreground)" }}>Document not found</p>
      </div>
    );
  }

  return (
    <div style={{ minHeight: "100vh", background: "var(--color-background)" }}>
      {/* Header */}
      <header style={{
        background: "oklch(0.14 0.02 250 / 0.8)",
        backdropFilter: "blur(12px)",
        borderBottom: "1px solid var(--color-border)",
        position: "sticky",
        top: 0,
        zIndex: 20,
      }}>
        <div style={{ maxWidth: "1400px", margin: "0 auto", padding: "0 1.5rem", height: "56px", display: "flex", alignItems: "center", gap: "1rem" }}>
          <Link href="/user" style={{ display: "inline-flex", alignItems: "center", gap: "0.375rem", padding: "0.375rem 0.75rem", background: "transparent", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-muted-foreground)", textDecoration: "none", fontSize: "0.85rem" }}>
            <ArrowLeft size={16} />
          </Link>

          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: "0.95rem" }}>{doc.docName}</div>
            <div style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)" }}>{doc.pageCount} pages · {doc.docStatus}</div>
          </div>

          {error && (
            <div style={{ display: "flex", alignItems: "center", gap: "0.375rem", color: "oklch(0.65 0.22 25)", fontSize: "0.8rem" }}>
              <AlertCircle size={14} /> {error}
            </div>
          )}
          {success && (
            <div style={{ display: "flex", alignItems: "center", gap: "0.375rem", color: "oklch(0.72 0.18 145)", fontSize: "0.8rem" }}>
              <CheckCircle size={14} /> {success}
            </div>
          )}

          <button onClick={handleReset} style={{ display: "inline-flex", alignItems: "center", gap: "0.375rem", padding: "0.5rem 1rem", background: "transparent", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", color: "var(--color-muted-foreground)", fontSize: "0.85rem", cursor: "pointer" }}>
            <RotateCcw size={14} /> Reset
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            style={{ display: "inline-flex", alignItems: "center", gap: "0.375rem", padding: "0.5rem 1.25rem", background: saving ? "oklch(0.72 0.18 145 / 0.5)" : "oklch(0.72 0.18 145)", color: "white", border: "none", borderRadius: "var(--radius-md)", fontSize: "0.85rem", fontWeight: 600, cursor: saving ? "not-allowed" : "pointer" }}
          >
            {saving ? <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} /> : <Save size={14} />}
            {saving ? "Saving..." : "Save Overlay"}
          </button>
        </div>
      </header>

      {/* Editor Area */}
      <div style={{ maxWidth: "1400px", margin: "0 auto", padding: "1.5rem" }}>
        {/* Toolbar */}
        <PageEditorToolbar editorRef={editorRef} />

        {/* Canvas wrapper with scroll */}
        <div style={{
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-lg)",
          padding: "1rem",
          overflow: "auto",
          maxHeight: "calc(100vh - 200px)",
        }}>
          <PageEditor
            ref={editorRef}
            canvasId={`doc-editor-${docId}`}
            width={canvasSize.width}
            height={canvasSize.height}
            backgroundImage={imageUrl}
            readonly={false}
            onAfterRender={(count) => {/* toolbar updates via polling */}}
          />
        </div>

        {/* Page size selector */}
        <div style={{ display: "flex", gap: "0.75rem", marginTop: "1rem", alignItems: "center" }}>
          <span style={{ fontSize: "0.8rem", color: "var(--color-muted-foreground)" }}>Page size:</span>
          {[
            { label: "A4", w: 794, h: 1123 },
            { label: "Letter", w: 816, h: 1056 },
            { label: "A3", w: 1123, h: 1587 },
            { label: "Custom", w: 800, h: 1100 },
          ].map(({ label, w, h }) => (
            <button
              key={label}
              onClick={() => {
                setCanvasSize({ width: w, height: h });
                editorRef.current?.setCanvasSize(w, h);
              }}
              style={{
                padding: "0.25rem 0.75rem",
                background: canvasSize.width === w ? "oklch(0.72 0.18 250 / 0.1)" : "var(--color-input)",
                border: `1px solid ${canvasSize.width === w ? "var(--color-primary)" : "var(--color-border)"}`,
                borderRadius: "999px",
                color: canvasSize.width === w ? "var(--color-primary)" : "var(--color-muted-foreground)",
                fontSize: "0.75rem",
                fontWeight: canvasSize.width === w ? 600 : 400,
                cursor: "pointer",
              }}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
