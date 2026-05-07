import { useState, useEffect } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useNavigate } from "react-router-dom";

const API_URL = "http://localhost:3001/api/v1";

function getToken(): string | null {
  return localStorage.getItem("printflow_token");
}

function getHeaders(): Record<string, string> {
  const token = getToken();
  return token
    ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" }
    : { "Content-Type": "application/json" };
}

async function apiFetch<T = any>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: { ...getHeaders(), ...((options.headers as Record<string, string>) ?? {}) },
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.error?.message ?? "Request failed");
  return data;
}

export default function App() {
  const navigate = useNavigate();
  const [page, setPage] = useState<string>("dashboard");

  useEffect(() => {
    const token = getToken();
    if (!token) {
      navigate("/login");
    }
  }, [navigate]);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-logo">
          <span className="logo-icon">PF</span>
          <span className="logo-text">PrintFlow</span>
        </div>
        <nav className="sidebar-nav">
          <button className={page === "dashboard" ? "active" : ""} onClick={() => setPage("dashboard")}>
            Dashboard
          </button>
          <button className={page === "documents" ? "active" : ""} onClick={() => setPage("documents")}>
            Documents
          </button>
          <button className={page === "pos" ? "active" : ""} onClick={() => setPage("pos")}>
            POS Terminal
          </button>
          <button className={page === "release" ? "active" : ""} onClick={() => setPage("release")}>
            QR Release
          </button>
          <button className={page === "settings" ? "active" : ""} onClick={() => setPage("settings")}>
            Settings
          </button>
        </nav>
        <div className="sidebar-footer">
          <button
            className="logout-btn"
            onClick={() => {
              localStorage.removeItem("printflow_token");
              navigate("/login");
            }}
          >
            Logout
          </button>
        </div>
      </aside>

      <main className="main-content">
        {page === "dashboard" && <DashboardPage />}
        {page === "documents" && <DocumentsPage />}
        {page === "pos" && <PosPage />}
        {page === "release" && <QrReleasePage />}
        {page === "settings" && <SettingsPage />}
      </main>
    </div>
  );
}

function DashboardPage() {
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiFetch("/reports/summary")
      .then((r: any) => setStats(r.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="page-loading">Loading...</div>;

  return (
    <div className="page">
      <h1>Dashboard</h1>
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-value">{stats?.overview?.totalUsers ?? 0}</div>
          <div className="stat-label">Total Users</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats?.overview?.totalPrintJobs ?? 0}</div>
          <div className="stat-label">Print Jobs</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats?.overview?.totalTransactions ?? 0}</div>
          <div className="stat-label">Transactions</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats?.overview?.totalPrinters ?? 0}</div>
          <div className="stat-label">Printers</div>
        </div>
      </div>
      <div className="section">
        <h2>Top Printers</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th>Printer</th>
              <th>Jobs</th>
              <th>Pages</th>
            </tr>
          </thead>
          <tbody>
            {(stats?.topPrinters ?? []).map((p: any) => (
              <tr key={p.printerName}>
                <td>{p.printerName}</td>
                <td>{p.jobCount}</td>
                <td>{p.totalPages}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function DocumentsPage() {
  const [docs, setDocs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiFetch("/documents?limit=50")
      .then((r: any) => setDocs(r.data.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="page-loading">Loading...</div>;

  return (
    <div className="page">
      <h1>Documents</h1>
      <table className="data-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Pages</th>
            <th>Status</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          {docs.map((d: any) => (
            <tr key={d.id}>
              <td>{d.docName}</td>
              <td>{d.docType}</td>
              <td>{d.pageCount}</td>
              <td><span className="badge">{d.docStatus}</span></td>
              <td>{new Date(d.dateCreated).toLocaleDateString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function PosPage() {
  const [items, setItems] = useState<any[]>([]);
  const [cart, setCart] = useState<Map<number, number>>(new Map());
  const [paymentType, setPaymentType] = useState("CASH");
  const [receipt, setReceipt] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiFetch("/pos/items")
      .then((r: any) => setItems(r.data.items))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const addToCart = (id: number) => {
    const next = new Map(cart);
    next.set(id, (next.get(id) ?? 0) + 1);
    setCart(next);
  };

  const removeFromCart = (id: number) => {
    const next = new Map(cart);
    if ((next.get(id) ?? 0) <= 1) {
      next.delete(id);
    } else {
      next.set(id, (next.get(id) ?? 0) - 1);
    }
    setCart(next);
  };

  const total = items.reduce((sum, item) => sum + (cart.get(item.id) ?? 0) * item.cost, 0);

  const completeSale = async () => {
    if (cart.size === 0) return;
    try {
      const body = {
        items: Array.from(cart.entries()).map(([itemId, quantity]) => ({ itemId, quantity })),
        paymentType,
      };
      const r: any = await apiFetch("/pos/sales", { method: "POST", body: JSON.stringify(body) });
      setReceipt(r.data.purchase);
      setCart(new Map());
    } catch (e: any) {
      alert(e.message);
    }
  };

  if (loading) return <div className="page-loading">Loading...</div>;

  return (
    <div className="page">
      <h1>POS Terminal</h1>
      <div className="pos-layout">
        <div className="pos-items">
          <h3>Items</h3>
          <div className="item-grid">
            {items.map((item) => (
              <div key={item.id} className="item-card" onClick={() => addToCart(item.id)}>
                <div className="item-name">{item.name}</div>
                <div className="item-cost">${item.cost.toFixed(2)}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="pos-cart">
          <h3>Cart</h3>
          {cart.size === 0 && <p className="empty-cart">No items in cart</p>}
          {Array.from(cart.entries()).map(([itemId, qty]) => {
            const item = items.find((i) => i.id === itemId);
            if (!item) return null;
            return (
              <div key={itemId} className="cart-item">
                <span>{item.name}</span>
                <div className="cart-qty">
                  <button onClick={() => removeFromCart(itemId)}>-</button>
                  <span>{qty}</span>
                  <button onClick={() => addToCart(itemId)}>+</button>
                </div>
                <span>${(item.cost * qty).toFixed(2)}</span>
              </div>
            );
          })}
          <div className="cart-total">
            <strong>Total:</strong> <strong>${total.toFixed(2)}</strong>
          </div>
          <select value={paymentType} onChange={(e) => setPaymentType(e.target.value)} className="payment-select">
            <option value="CASH">Cash</option>
            <option value="CARD">Card</option>
            <option value="ACCOUNT">Account</option>
            <option value="ONLINE">Online</option>
          </select>
          <button className="complete-btn" onClick={completeSale} disabled={cart.size === 0}>
            Complete Sale
          </button>
          {receipt && (
            <div className="receipt">
              <h4>Receipt #{receipt.receiptNumber}</h4>
              {receipt.items.map((i: any) => (
                <div key={i.itemIndex} className="receipt-item">
                  {i.quantity}x {i.name} - ${(i.unitCost * i.quantity).toFixed(2)}
                </div>
              ))}
              <div className="receipt-total">Total: ${receipt.totalCost.toFixed(2)}</div>
              <button className="new-sale-btn" onClick={() => setReceipt(null)}>New Sale</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function QrReleasePage() {
  const [uuid, setUuid] = useState("");
  const [doc, setDoc] = useState<any>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [released, setReleased] = useState(false);

  const validate = async () => {
    if (!uuid.trim()) return;
    setLoading(true);
    setError("");
    setDoc(null);
    setReleased(false);
    try {
      const r: any = await apiFetch(`/qr/validate/${uuid}`);
      if (r.data.valid) {
        setDoc(r.data);
      } else {
        setError(r.data.message);
      }
    } catch (e: any) {
      setError("QR code not found");
    } finally {
      setLoading(false);
    }
  };

  const release = async () => {
    try {
      await apiFetch("/qr/release", { method: "POST", body: JSON.stringify({ uuid }) });
      setReleased(true);
    } catch (e: any) {
      setError(e.message);
    }
  };

  return (
    <div className="page">
      <h1>Scan &amp; Print</h1>
      <div className="qr-release">
        <input
          className="uuid-input"
          placeholder="Enter document UUID"
          value={uuid}
          onChange={(e) => setUuid(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && validate()}
        />
        <button className="validate-btn" onClick={validate} disabled={loading}>
          {loading ? "Checking..." : "Validate"}
        </button>
        {error && <div className="error-msg">{error}</div>}
        {doc && !released && (
          <div className="doc-info">
            <h3>{doc.docName}</h3>
            <p>Pages: {doc.pageCount}</p>
            <p>Status: {doc.status}</p>
            <button className="release-btn" onClick={release}>
              Release Document
            </button>
          </div>
        )}
        {released && <div className="success-msg">Document released successfully!</div>}
      </div>
    </div>
  );
}

function SettingsPage() {
  return (
    <div className="page">
      <h1>Settings</h1>
      <div className="settings-section">
        <h3>Tauri Desktop Client</h3>
        <p>This is a native desktop client for PrintFlow. It provides offline document storage, local printing, and system tray integration.</p>
        <div className="settings-info">
          <p><strong>Version:</strong> 0.1.0</p>
          <p><strong>API:</strong> {API_URL}</p>
        </div>
      </div>
    </div>
  );
}
