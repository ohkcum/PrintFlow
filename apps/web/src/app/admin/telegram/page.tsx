"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  MessageSquare,
  CheckCircle,
  AlertCircle,
  Loader2,
  RefreshCw,
  Send,
  Search,
  Link,
  Unlink,
  ToggleLeft,
  ToggleRight,
} from "lucide-react";
import { telegramApi, usersApi } from "@/lib/api";

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

function Toast({
  message,
  type,
}: {
  message: string;
  type: "success" | "error" | "info";
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
          type === "success"
            ? "oklch(0.72 0.18 145)"
            : type === "error"
              ? "oklch(0.65 0.22 25)"
              : "var(--color-primary)",
        color: "white",
        fontSize: "0.875rem",
        fontWeight: 600,
        boxShadow: "var(--shadow-lg)",
        animation: "fadeIn 0.2s ease",
        maxWidth: "350px",
      }}
    >
      {message}
    </div>
  );
}

// ─── Types ─────────────────────────────────────────────────────────────────────

type TelegramStatus = {
  configured: boolean;
  enabled: boolean;
  botUsername: string | null;
};

type UserLink = {
  userId: number;
  userName: string;
  fullName: string;
  telegramId: string | null;
};

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function AdminTelegramPage() {
  const router = useRouter();
  const [status, setStatus] = useState<TelegramStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [botToken, setBotToken] = useState("");
  const [botUsername, setBotUsername] = useState("");

  // Test message
  const [testTelegramId, setTestTelegramId] = useState("");
  const [testMessage, setTestMessage] = useState("");
  const [sending, setSending] = useState(false);

  // User linking
  const [users, setUsers] = useState<UserLink[]>([]);
  const [userSearch, setUserSearch] = useState("");
  const [filteredUsers, setFilteredUsers] = useState<UserLink[]>([]);
  const [selectedUser, setSelectedUser] = useState<UserLink | null>(null);
  const [linkTelegramId, setLinkTelegramId] = useState("");
  const [linking, setLinking] = useState(false);
  const [webhookUrl, setWebhookUrl] = useState("");

  const [toast, setToast] = useState<{
    message: string;
    type: "success" | "error" | "info";
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
    if (toast) {
      const t = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(t);
    }
  }, [toast]);

  function showToast(msg: string, type: "success" | "error" | "info") {
    setToast({ message: msg, type });
  }

  async function loadStatus() {
    setLoading(true);
    try {
      const result = await telegramApi.status();
      setStatus(result.data);
      // Set webhook URL based on current origin
      if (typeof window !== "undefined") {
        setWebhookUrl(
          `${window.location.origin}/api/v1/telegram/webhook`,
        );
      }
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function loadUsers() {
    try {
      const result = await usersApi.list({ limit: 100 });
      const userList: UserLink[] = (result.data?.data ?? []).map((u: any) => ({
        userId: u.id,
        userName: u.userName,
        fullName: u.fullName,
        telegramId: null,
      }));
      setUsers(userList);
      setFilteredUsers(userList.slice(0, 20));
    } catch (e: any) {
      // silently fail
    }
  }

  useEffect(() => {
    loadStatus();
    loadUsers();
  }, []);

  // Filter users by search
  useEffect(() => {
    if (!userSearch.trim()) {
      setFilteredUsers(users.slice(0, 20));
    } else {
      const q = userSearch.toLowerCase();
      setFilteredUsers(
        users
          .filter(
            (u) =>
              u.userName.toLowerCase().includes(q) ||
              u.fullName.toLowerCase().includes(q),
          )
          .slice(0, 20),
      );
    }
  }, [userSearch, users]);

  async function handleSaveSettings() {
    setSaving(true);
    try {
      // Save bot token to localStorage for demo
      localStorage.setItem("telegram_bot_token", botToken);
      showToast("Telegram settings saved", "success");
      await loadStatus();
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setSaving(false);
    }
  }

  async function handleToggle(enabled: boolean) {
    setSaving(true);
    try {
      localStorage.setItem("telegram_enabled", String(enabled));
      await loadStatus();
      showToast(
        `Telegram notifications ${enabled ? "enabled" : "disabled"}`,
        "success",
      );
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setSaving(false);
    }
  }

  async function handleSendTest() {
    if (!testTelegramId.trim() || !testMessage.trim()) {
      showToast("Please enter both Telegram ID and message", "error");
      return;
    }
    setSending(true);
    try {
      await telegramApi.send({
        telegramId: testTelegramId,
        message: testMessage,
      });
      showToast("Test message sent!", "success");
      setTestMessage("");
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setSending(false);
    }
  }

  async function handleLinkUser() {
    if (!selectedUser || !linkTelegramId.trim()) {
      showToast("Please select a user and enter a Telegram ID", "error");
      return;
    }
    setLinking(true);
    try {
      await telegramApi.link({
        userId: selectedUser.userId,
        telegramId: linkTelegramId,
      });
      showToast(
        `Linked ${selectedUser.fullName} to ${linkTelegramId}`,
        "success",
      );
      setLinkTelegramId("");
      setSelectedUser(null);
      setUserSearch("");
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setLinking(false);
    }
  }

  async function handleUnlinkUser(userId: number) {
    setLinking(true);
    try {
      await telegramApi.unlink(userId);
      showToast("User unlinked from Telegram", "success");
    } catch (e: any) {
      showToast(e.message, "error");
    } finally {
      setLinking(false);
    }
  }

  async function sendTestNotification(actionType: string) {
    try {
      await telegramApi.send({
        telegramId: testTelegramId || "test",
        message: `Test notification: ${actionType} - ${new Date().toLocaleString()}`,
      });
      showToast(`Test ${actionType} notification sent`, "success");
    } catch (e: any) {
      showToast(e.message, "error");
    }
  }

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
          Telegram Notifications
        </h1>
        <p
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.875rem",
            marginTop: "0.25rem",
          }}
        >
          Configure bot settings and user Telegram linking for notifications
        </p>
      </div>

      {loading ? (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: "1rem",
          }}
        >
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-lg)",
                padding: "1.5rem",
                height: "150px",
                animation: "pulse 1.5s infinite",
              }}
            />
          ))}
        </div>
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: "1.5rem",
            alignItems: "start",
          }}
        >
          {/* Left Column */}
          <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
            {/* Bot Status Card */}
            <div
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-xl)",
                padding: "1.5rem",
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  marginBottom: "1.25rem",
                }}
              >
                <h3
                  style={{
                    fontSize: "0.95rem",
                    fontWeight: 700,
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  <MessageSquare
                    size={18}
                    style={{ color: "oklch(0.72 0.18 250)" }}
                  />
                  Bot Status
                </h3>
                <button
                  onClick={loadStatus}
                  style={{ ...btnStyle("ghost"), padding: "0.375rem 0.625rem" }}
                >
                  <RefreshCw size={14} />
                </button>
              </div>

              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "1rem",
                  marginBottom: "1.25rem",
                  padding: "1rem",
                  background: "var(--color-background)",
                  borderRadius: "var(--radius-md)",
                }}
              >
                <div
                  style={{
                    width: "48px",
                    height: "48px",
                    borderRadius: "12px",
                    background:
                      status?.configured && status?.enabled
                        ? "oklch(0.72 0.18 145 / 0.12)"
                        : "oklch(0.75 0.15 85 / 0.12)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  {status?.configured && status?.enabled ? (
                    <CheckCircle
                      size={24}
                      style={{ color: "oklch(0.72 0.18 145)" }}
                    />
                  ) : (
                    <AlertCircle
                      size={24}
                      style={{ color: "oklch(0.75 0.15 85)" }}
                    />
                  )}
                </div>
                <div>
                  <div style={{ fontWeight: 700, fontSize: "0.95rem" }}>
                    {status?.botUsername
                      ? `@${status.botUsername}`
                      : "Bot not configured"}
                  </div>
                  <div
                    style={{
                      fontSize: "0.8rem",
                      color: "var(--color-muted-foreground)",
                    }}
                  >
                    {status?.configured
                      ? status?.enabled
                        ? "Active and enabled"
                        : "Configured but disabled"
                      : "Not configured"}
                  </div>
                </div>
              </div>

              {/* Enable/Disable Toggle */}
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  padding: "0.75rem 1rem",
                  background: "var(--color-background)",
                  borderRadius: "var(--radius-md)",
                  marginBottom: "1.25rem",
                }}
              >
                <div>
                  <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>
                    Enable Notifications
                  </div>
                  <div
                    style={{
                      fontSize: "0.75rem",
                      color: "var(--color-muted-foreground)",
                    }}
                  >
                    Send Telegram notifications to users
                  </div>
                </div>
                <button
                  onClick={() => handleToggle(!status?.enabled)}
                  disabled={!status?.configured || saving}
                  style={{
                    background: "none",
                    border: "none",
                    cursor: status?.configured ? "pointer" : "not-allowed",
                    padding: "0",
                    opacity: status?.configured ? 1 : 0.5,
                  }}
                >
                  {status?.enabled ? (
                    <ToggleRight
                      size={36}
                      style={{ color: "oklch(0.72 0.18 145)" }}
                    />
                  ) : (
                    <ToggleLeft
                      size={36}
                      style={{ color: "var(--color-muted-foreground)" }}
                    />
                  )}
                </button>
              </div>

              {/* Bot Token Input */}
              <div style={{ marginBottom: "1rem" }}>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.8rem",
                    fontWeight: 600,
                    marginBottom: "0.5rem",
                  }}
                >
                  Bot Token
                </label>
                <input
                  type="password"
                  value={botToken}
                  onChange={(e) => setBotToken(e.target.value)}
                  placeholder="123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
                  style={{
                    width: "100%",
                    padding: "0.5rem 0.75rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                    color: "var(--color-foreground)",
                    fontSize: "0.8rem",
                    outline: "none",
                    fontFamily: "var(--font-mono)",
                  }}
                />
              </div>

              <button
                onClick={handleSaveSettings}
                disabled={saving}
                style={{ ...btnStyle("primary"), width: "100%", justifyContent: "center", opacity: saving ? 0.7 : 1 }}
              >
                {saving ? (
                  <>
                    <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} />
                    Saving...
                  </>
                ) : (
                  <>
                    <CheckCircle size={16} /> Save Settings
                  </>
                )}
              </button>
            </div>

            {/* Webhook URL */}
            <div
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-xl)",
                padding: "1.5rem",
              }}
            >
              <h3
                style={{
                  fontSize: "0.95rem",
                  fontWeight: 700,
                  marginBottom: "1rem",
                }}
              >
                Webhook URL
              </h3>
              <div
                style={{
                  padding: "0.75rem",
                  background: "var(--color-background)",
                  borderRadius: "var(--radius-md)",
                  fontFamily: "var(--font-mono)",
                  fontSize: "0.8rem",
                  color: "var(--color-muted-foreground)",
                  wordBreak: "break-all",
                  marginBottom: "0.75rem",
                }}
              >
                {webhookUrl || "Set up webhook URL in your bot configuration"}
              </div>
              <div
                style={{
                  fontSize: "0.75rem",
                  color: "var(--color-muted-foreground)",
                }}
              >
                Use this URL to set the Telegram bot webhook for receiving updates.
                Click save after configuring the bot token.
              </div>
            </div>
          </div>

          {/* Right Column */}
          <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
            {/* Test Message */}
            <div
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-xl)",
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
                <Send size={18} style={{ color: "oklch(0.72 0.18 145)" }} />
                Test Message
              </h3>
              <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.8rem",
                      fontWeight: 600,
                      marginBottom: "0.375rem",
                    }}
                  >
                    Telegram Chat ID
                  </label>
                  <input
                    type="text"
                    value={testTelegramId}
                    onChange={(e) => setTestTelegramId(e.target.value)}
                    placeholder="e.g. 123456789"
                    style={{
                      width: "100%",
                      padding: "0.5rem 0.75rem",
                      background: "var(--color-input)",
                      border: "1px solid var(--color-border)",
                      borderRadius: "var(--radius-md)",
                      color: "var(--color-foreground)",
                      fontSize: "0.875rem",
                      outline: "none",
                      fontFamily: "var(--font-mono)",
                    }}
                  />
                </div>
                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.8rem",
                      fontWeight: 600,
                      marginBottom: "0.375rem",
                    }}
                  >
                    Message
                  </label>
                  <textarea
                    value={testMessage}
                    onChange={(e) => setTestMessage(e.target.value)}
                    placeholder="Enter test message..."
                    rows={3}
                    style={{
                      width: "100%",
                      padding: "0.5rem 0.75rem",
                      background: "var(--color-input)",
                      border: "1px solid var(--color-border)",
                      borderRadius: "var(--radius-md)",
                      color: "var(--color-foreground)",
                      fontSize: "0.875rem",
                      outline: "none",
                      resize: "vertical",
                    }}
                  />
                </div>
                <button
                  onClick={handleSendTest}
                  disabled={sending}
                  style={{ ...btnStyle("primary"), width: "100%", justifyContent: "center", opacity: sending ? 0.7 : 1 }}
                >
                  {sending ? (
                    <>
                      <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} />
                      Sending...
                    </>
                  ) : (
                    <>
                      <Send size={16} /> Send Test Message
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* User Linking */}
            <div
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-xl)",
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
                <Link size={18} style={{ color: "oklch(0.75 0.15 85)" }} />
                User Linking
              </h3>
              <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                {/* User search */}
                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.8rem",
                      fontWeight: 600,
                      marginBottom: "0.375rem",
                    }}
                  >
                    Select User
                  </label>
                  <div style={{ position: "relative" }}>
                    <Search
                      size={16}
                      style={{
                        position: "absolute",
                        left: "0.75rem",
                        top: "50%",
                        transform: "translateY(-50%)",
                        color: "var(--color-muted-foreground)",
                      }}
                    />
                    <input
                      type="text"
                      value={userSearch}
                      onChange={(e) => {
                        setUserSearch(e.target.value);
                        setSelectedUser(null);
                      }}
                      placeholder="Search users..."
                      style={{
                        width: "100%",
                        padding: "0.5rem 0.75rem 0.5rem 2.25rem",
                        background: "var(--color-input)",
                        border: "1px solid var(--color-border)",
                        borderRadius: "var(--radius-md)",
                        color: "var(--color-foreground)",
                        fontSize: "0.875rem",
                        outline: "none",
                      }}
                    />
                  </div>

                  {/* User list dropdown */}
                  {(userSearch || filteredUsers.length > 0) && !selectedUser && (
                    <div
                      style={{
                        position: "absolute",
                        top: "100%",
                        left: 0,
                        right: 0,
                        background: "var(--color-card)",
                        border: "1px solid var(--color-border)",
                        borderRadius: "var(--radius-md)",
                        marginTop: "0.25rem",
                        maxHeight: "200px",
                        overflowY: "auto",
                        zIndex: 50,
                        boxShadow: "var(--shadow-lg)",
                      }}
                    >
                      {filteredUsers.length === 0 ? (
                        <div
                          style={{
                            padding: "1rem",
                            textAlign: "center",
                            color: "var(--color-muted-foreground)",
                            fontSize: "0.875rem",
                          }}
                        >
                          No users found
                        </div>
                      ) : (
                        filteredUsers.map((u) => (
                          <button
                            key={u.userId}
                            onClick={() => {
                              setSelectedUser(u);
                              setUserSearch(u.fullName);
                            }}
                            style={{
                              width: "100%",
                              padding: "0.625rem 0.875rem",
                              background: "transparent",
                              border: "none",
                              borderBottom:
                                filteredUsers.indexOf(u) <
                                filteredUsers.length - 1
                                  ? "1px solid var(--color-border)"
                                  : "none",
                              cursor: "pointer",
                              textAlign: "left",
                              color: "var(--color-foreground)",
                            }}
                          >
                            <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>
                              {u.fullName}
                            </div>
                            <div
                              style={{
                                fontSize: "0.75rem",
                                color: "var(--color-muted-foreground)",
                              }}
                            >
                              @{u.userName}
                              {u.telegramId && (
                                <span
                                  style={{
                                    marginLeft: "0.5rem",
                                    color: "oklch(0.72 0.18 145)",
                                  }}
                                >
                                  Linked
                                </span>
                              )}
                            </div>
                          </button>
                        ))
                      )}
                    </div>
                  )}
                </div>

                {/* Selected user display */}
                {selectedUser && (
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "0.75rem",
                      background: "var(--color-background)",
                      borderRadius: "var(--radius-md)",
                    }}
                  >
                    <div>
                      <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>
                        {selectedUser.fullName}
                      </div>
                      <div
                        style={{
                          fontSize: "0.75rem",
                          color: "var(--color-muted-foreground)",
                        }}
                      >
                        @{selectedUser.userName}
                      </div>
                    </div>
                    <button
                      onClick={() => {
                        setSelectedUser(null);
                        setUserSearch("");
                      }}
                      style={{
                        background: "none",
                        border: "none",
                        cursor: "pointer",
                        color: "var(--color-muted-foreground)",
                        padding: "4px",
                      }}
                    >
                      ×
                    </button>
                  </div>
                )}

                {/* Telegram ID input */}
                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.8rem",
                      fontWeight: 600,
                      marginBottom: "0.375rem",
                    }}
                  >
                    Telegram Chat ID
                  </label>
                  <input
                    type="text"
                    value={linkTelegramId}
                    onChange={(e) => setLinkTelegramId(e.target.value)}
                    placeholder="User's Telegram chat ID"
                    style={{
                      width: "100%",
                      padding: "0.5rem 0.75rem",
                      background: "var(--color-input)",
                      border: "1px solid var(--color-border)",
                      borderRadius: "var(--radius-md)",
                      color: "var(--color-foreground)",
                      fontSize: "0.875rem",
                      outline: "none",
                      fontFamily: "var(--font-mono)",
                    }}
                  />
                </div>

                {/* Link/Unlink buttons */}
                <div style={{ display: "flex", gap: "0.5rem" }}>
                  <button
                    onClick={handleLinkUser}
                    disabled={!selectedUser || !linkTelegramId || linking}
                    style={{
                      ...btnStyle("primary"),
                      flex: 1,
                      justifyContent: "center",
                      opacity:
                        !selectedUser || !linkTelegramId || linking ? 0.6 : 1,
                    }}
                  >
                    {linking ? (
                      <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} />
                    ) : (
                      <Link size={14} />
                    )}
                    Link User
                  </button>
                  {selectedUser && (
                    <button
                      onClick={() => handleUnlinkUser(selectedUser.userId)}
                      disabled={linking}
                      style={{ ...btnStyle("danger") }}
                    >
                      <Unlink size={14} />
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Action Notifications */}
            <div
              style={{
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-xl)",
                padding: "1.5rem",
              }}
            >
              <h3
                style={{
                  fontSize: "0.95rem",
                  fontWeight: 700,
                  marginBottom: "1rem",
                }}
              >
                Send Test Notification
              </h3>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "0.5rem",
                }}
              >
                {[
                  { label: "Document Uploaded", type: "DOC_UPLOAD" },
                  { label: "Print Job Done", type: "PRINT_COMPLETE" },
                  { label: "Low Balance", type: "LOW_BALANCE" },
                  { label: "Ticket Created", type: "TICKET_CREATED" },
                ].map((action) => (
                  <button
                    key={action.type}
                    onClick={() => sendTestNotification(action.type)}
                    style={{
                      padding: "0.625rem 0.75rem",
                      background: "var(--color-background)",
                      border: "1px solid var(--color-border)",
                      borderRadius: "var(--radius-md)",
                      cursor: "pointer",
                      fontSize: "0.8rem",
                      color: "var(--color-foreground)",
                      textAlign: "center",
                    }}
                  >
                    {action.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
