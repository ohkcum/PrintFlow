import { useState } from "react";
import { useNavigate } from "react-router-dom";

const API_URL = "http://localhost:3001/api/v1";

export default function LoginPage() {
  const navigate = useNavigate();
  const [userName, setUserName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const res = await fetch(`${API_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userName, password }),
      });
      const data = await res.json();

      if (data.success && data.data?.token) {
        localStorage.setItem("printflow_token", data.data.token);
        navigate("/");
      } else {
        setError(data.error?.message ?? "Login failed");
      }
    } catch {
      setError("Network error. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-shell">
      <div className="login-card">
        <div className="login-logo">
          <span className="logo-icon-lg">PF</span>
          <span className="logo-name">PrintFlow</span>
        </div>
        <h2>Sign In</h2>
        <form onSubmit={handleLogin}>
          <div className="form-field">
            <label>Username</label>
            <input
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              placeholder="Enter username"
              required
            />
          </div>
          <div className="form-field">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter password"
              required
            />
          </div>
          {error && <div className="login-error">{error}</div>}
          <button type="submit" className="login-btn" disabled={loading}>
            {loading ? "Signing in..." : "Sign In"}
          </button>
        </form>
      </div>

      <style>{`
        .login-shell {
          min-height: 100vh;
          display: flex;
          align-items: center;
          justify-content: center;
          background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 100%);
        }
        .login-card {
          background: white;
          border-radius: 16px;
          padding: 2.5rem;
          width: 100%;
          max-width: 400px;
          box-shadow: 0 25px 50px rgba(0,0,0,0.3);
        }
        .login-logo {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 0.75rem;
          margin-bottom: 2rem;
        }
        .logo-icon-lg {
          width: 48px;
          height: 48px;
          background: #3b82f6;
          color: white;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 800;
          font-size: 1.25rem;
        }
        .logo-name {
          font-size: 1.5rem;
          font-weight: 700;
          color: #1e293b;
        }
        .login-card h2 {
          text-align: center;
          margin-bottom: 1.5rem;
          font-size: 1.25rem;
          color: #64748b;
        }
        .form-field {
          margin-bottom: 1rem;
        }
        .form-field label {
          display: block;
          font-size: 0.8125rem;
          font-weight: 500;
          color: #475569;
          margin-bottom: 0.375rem;
        }
        .form-field input {
          width: 100%;
          padding: 0.625rem 0.875rem;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          font-size: 0.9375rem;
          outline: none;
          transition: border-color 0.15s;
        }
        .form-field input:focus {
          border-color: #3b82f6;
          box-shadow: 0 0 0 3px rgba(59,130,246,0.1);
        }
        .login-error {
          padding: 0.625rem 0.875rem;
          background: #fef2f2;
          border: 1px solid #fecaca;
          border-radius: 8px;
          color: #dc2626;
          font-size: 0.875rem;
          margin-bottom: 1rem;
        }
        .login-btn {
          width: 100%;
          padding: 0.75rem;
          background: #3b82f6;
          color: white;
          border: none;
          border-radius: 8px;
          font-size: 0.9375rem;
          font-weight: 600;
          cursor: pointer;
        }
        .login-btn:hover {
          background: #2563eb;
        }
        .login-btn:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
}
