"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  ShoppingCart,
  RefreshCw,
  Plus,
  Minus,
  Trash2,
  Edit,
  DollarSign,
  TrendingUp,
  Loader2,
  X,
  Printer,
  CheckCircle,
  AlertCircle,
  CreditCard,
  Banknote,
} from "lucide-react";
import {
  posApi,
  usersApi,
  type PosItem,
  type PosPurchase,
  type PosSummary,
} from "@/lib/api";
import { SkeletonTable } from "@/components/ui/Skeleton";

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

// ─── Stat Card ─────────────────────────────────────────────────────────────────

function StatCard({
  icon: Icon,
  label,
  value,
  color,
}: {
  icon: React.ElementType;
  label: string;
  value: string | number;
  color: string;
}) {
  return (
    <div
      style={{
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-lg)",
        padding: "1rem",
        display: "flex",
        alignItems: "center",
        gap: "0.75rem",
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
          flexShrink: 0,
        }}
      >
        <Icon size={20} style={{ color }} />
      </div>
      <div>
        <div
          style={{
            fontSize: "0.7rem",
            color: "var(--color-muted-foreground)",
            fontWeight: 500,
          }}
        >
          {label}
        </div>
        <div
          style={{
            fontSize: "1.25rem",
            fontWeight: 700,
            fontFamily: "var(--font-mono)",
          }}
        >
          {value}
        </div>
      </div>
    </div>
  );
}

// ─── POS Terminal Tab ──────────────────────────────────────────────────────────

type CartItem = {
  item: PosItem;
  quantity: number;
};

function POSTerminalTab({
  onToast,
}: {
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [items, setItems] = useState<PosItem[]>([]);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [summary, setSummary] = useState<PosSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [selling, setSelling] = useState(false);
  const [completedSale, setCompletedSale] = useState<PosPurchase | null>(null);
  const [paymentType, setPaymentType] = useState("CASH");
  const [comment, setComment] = useState("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [itemsRes, summaryRes] = await Promise.all([
        posApi.items(),
        posApi.summary(),
      ]);
      setItems(itemsRes.data?.items ?? []);
      setSummary(summaryRes.data);
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setLoading(false);
    }
  }, [onToast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  function addToCart(item: PosItem) {
    setCart((prev) => {
      const existing = prev.find((c) => c.item.id === item.id);
      if (existing) {
        return prev.map((c) =>
          c.item.id === item.id ? { ...c, quantity: c.quantity + 1 } : c,
        );
      }
      return [...prev, { item, quantity: 1 }];
    });
  }

  function updateQuantity(itemId: number, delta: number) {
    setCart((prev) => {
      return prev
        .map((c) =>
          c.item.id === itemId
            ? { ...c, quantity: Math.max(0, c.quantity + delta) }
            : c,
        )
        .filter((c) => c.quantity > 0);
    });
  }

  function removeFromCart(itemId: number) {
    setCart((prev) => prev.filter((c) => c.item.id !== itemId));
  }

  function clearCart() {
    setCart([]);
    setComment("");
    setPaymentType("CASH");
  }

  const total = cart.reduce(
    (sum, c) => sum + Number(c.item.cost) * c.quantity,
    0,
  );

  async function completeSale() {
    if (cart.length === 0) {
      onToast("Cart is empty", "error");
      return;
    }

    setSelling(true);
    try {
      const result = await posApi.sell({
        items: cart.map((c) => ({ itemId: c.item.id, quantity: c.quantity })),
        paymentType,
        comment: comment || undefined,
      });
      setCompletedSale(result.data?.purchase ?? null);
      clearCart();
      onToast("Sale completed successfully!", "success");
      fetchData();
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setSelling(false);
    }
  }

  const PAYMENT_TYPES = [
    { value: "CASH", label: "Cash", icon: Banknote },
    { value: "CARD", label: "Card", icon: CreditCard },
    { value: "ACCOUNT", label: "Account", icon: DollarSign },
    { value: "ONLINE", label: "Online", icon: Printer },
  ];

  return (
    <div>
      {/* Summary Stats */}
      {summary && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
            gap: "0.75rem",
            marginBottom: "1.5rem",
          }}
        >
          <StatCard
            icon={DollarSign}
            label="Today's Revenue"
            value={`$${summary.todayRevenue.toFixed(2)}`}
            color="oklch(0.72 0.18 145)"
          />
          <StatCard
            icon={ShoppingCart}
            label="Today's Sales"
            value={summary.todaySales}
            color="oklch(0.72 0.18 250)"
          />
          <StatCard
            icon={TrendingUp}
            label="Total Revenue"
            value={`$${summary.totalRevenue.toFixed(2)}`}
            color="oklch(0.75 0.15 85)"
          />
          <StatCard
            icon={CheckCircle}
            label="Total Sales"
            value={summary.totalSales}
            color="oklch(0.72 0.18 280)"
          />
        </div>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 380px",
          gap: "1.5rem",
          alignItems: "start",
        }}
      >
        {/* Items Grid */}
        <div>
          <h3
            style={{
              fontSize: "0.95rem",
              fontWeight: 700,
              marginBottom: "1rem",
            }}
          >
            Items
          </h3>
          {loading ? (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(150px, 1fr))",
                gap: "0.75rem",
              }}
            >
              {Array.from({ length: 8 }).map((_, i) => (
                <div
                  key={i}
                  style={{
                    background: "var(--color-card)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-lg)",
                    padding: "1.25rem",
                    height: "100px",
                    animation: "pulse 1.5s infinite",
                  }}
                />
              ))}
            </div>
          ) : items.length === 0 ? (
            <div
              style={{
                padding: "3rem",
                textAlign: "center",
                background: "var(--color-card)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-lg)",
                color: "var(--color-muted-foreground)",
              }}
            >
              No items available
            </div>
          ) : (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(150px, 1fr))",
                gap: "0.75rem",
              }}
            >
              {items.map((item) => (
                <button
                  key={item.id}
                  onClick={() => addToCart(item)}
                  disabled={!item.isActive}
                  style={{
                    background: "var(--color-card)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-lg)",
                    padding: "1.25rem",
                    cursor: item.isActive ? "pointer" : "not-allowed",
                    opacity: item.isActive ? 1 : 0.5,
                    transition: "all 0.15s",
                    textAlign: "left",
                  }}
                >
                  <div
                    style={{
                      fontSize: "0.85rem",
                      fontWeight: 600,
                      marginBottom: "0.5rem",
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {item.name}
                  </div>
                  {item.category && (
                    <div
                      style={{
                        fontSize: "0.7rem",
                        color: "var(--color-muted-foreground)",
                        marginBottom: "0.5rem",
                      }}
                    >
                      {item.category}
                    </div>
                  )}
                  <div
                    style={{
                      fontSize: "1.1rem",
                      fontWeight: 700,
                      fontFamily: "var(--font-mono)",
                      color: "oklch(0.75 0.15 85)",
                    }}
                  >
                    ${Number(item.cost).toFixed(2)}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Cart Panel */}
        <div
          style={{
            background: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-xl)",
            padding: "1.25rem",
            position: "sticky",
            top: "80px",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              marginBottom: "1rem",
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
              <ShoppingCart size={18} />
              Cart ({cart.length})
            </h3>
            {cart.length > 0 && (
              <button
                onClick={clearCart}
                style={{
                  ...btnStyle("ghost"),
                  padding: "0.25rem 0.5rem",
                  fontSize: "0.75rem",
                }}
              >
                Clear
              </button>
            )}
          </div>

          {cart.length === 0 ? (
            <div
              style={{
                padding: "2rem 1rem",
                textAlign: "center",
                color: "var(--color-muted-foreground)",
                fontSize: "0.875rem",
              }}
            >
              Cart is empty
              <br />
              <span style={{ fontSize: "0.75rem" }}>
                Click items to add them
              </span>
            </div>
          ) : (
            <>
              {/* Cart Items */}
              <div
                style={{
                  maxHeight: "240px",
                  overflowY: "auto",
                  marginBottom: "1rem",
                }}
              >
                {cart.map((c) => (
                  <div
                    key={c.item.id}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "0.625rem 0",
                      borderBottom: "1px solid var(--color-border)",
                    }}
                  >
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div
                        style={{
                          fontSize: "0.85rem",
                          fontWeight: 600,
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {c.item.name}
                      </div>
                      <div
                        style={{
                          fontSize: "0.75rem",
                          color: "var(--color-muted-foreground)",
                          fontFamily: "var(--font-mono)",
                        }}
                      >
                        ${Number(c.item.cost).toFixed(2)} each
                      </div>
                    </div>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.375rem",
                      }}
                    >
                      <button
                        onClick={() => updateQuantity(c.item.id, -1)}
                        style={{
                          width: "28px",
                          height: "28px",
                          borderRadius: "var(--radius-sm)",
                          border: "1px solid var(--color-border)",
                          background: "transparent",
                          cursor: "pointer",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          color: "var(--color-foreground)",
                        }}
                      >
                        <Minus size={12} />
                      </button>
                      <span
                        style={{
                          fontFamily: "var(--font-mono)",
                          fontSize: "0.875rem",
                          fontWeight: 600,
                          minWidth: "20px",
                          textAlign: "center",
                        }}
                      >
                        {c.quantity}
                      </span>
                      <button
                        onClick={() => updateQuantity(c.item.id, 1)}
                        style={{
                          width: "28px",
                          height: "28px",
                          borderRadius: "var(--radius-sm)",
                          border: "1px solid var(--color-border)",
                          background: "transparent",
                          cursor: "pointer",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          color: "var(--color-foreground)",
                        }}
                      >
                        <Plus size={12} />
                      </button>
                      <button
                        onClick={() => removeFromCart(c.item.id)}
                        style={{
                          width: "28px",
                          height: "28px",
                          borderRadius: "var(--radius-sm)",
                          border: "none",
                          background: "transparent",
                          cursor: "pointer",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          color: "oklch(0.65 0.22 25)",
                          marginLeft: "0.25rem",
                        }}
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {/* Total */}
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  padding: "0.75rem 0",
                  borderTop: "2px solid var(--color-border)",
                  marginBottom: "1rem",
                }}
              >
                <span
                  style={{ fontSize: "0.95rem", fontWeight: 700 }}
                >
                  Total
                </span>
                <span
                  style={{
                    fontSize: "1.25rem",
                    fontWeight: 700,
                    fontFamily: "var(--font-mono)",
                    color: "oklch(0.75 0.15 85)",
                  }}
                >
                  ${total.toFixed(2)}
                </span>
              </div>

              {/* Payment Type */}
              <div style={{ marginBottom: "1rem" }}>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.8rem",
                    fontWeight: 600,
                    marginBottom: "0.5rem",
                  }}
                >
                  Payment Type
                </label>
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
                    gap: "0.5rem",
                  }}
                >
                  {PAYMENT_TYPES.map((pt) => {
                    const Icon = pt.icon;
                    const active = paymentType === pt.value;
                    return (
                      <button
                        key={pt.value}
                        onClick={() => setPaymentType(pt.value)}
                        style={{
                          padding: "0.5rem",
                          borderRadius: "var(--radius-md)",
                          border: active
                            ? "1px solid var(--color-primary)"
                            : "1px solid var(--color-border)",
                          background: active
                            ? "oklch(0.72 0.18 250 / 0.1)"
                            : "transparent",
                          color: active
                            ? "var(--color-primary)"
                            : "var(--color-foreground)",
                          cursor: "pointer",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          gap: "0.375rem",
                          fontSize: "0.8rem",
                          fontWeight: active ? 600 : 400,
                        }}
                      >
                        <Icon size={14} />
                        {pt.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Comment */}
              <div style={{ marginBottom: "1rem" }}>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.8rem",
                    fontWeight: 600,
                    marginBottom: "0.5rem",
                  }}
                >
                  Comment (optional)
                </label>
                <input
                  type="text"
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Add a note..."
                  style={{
                    width: "100%",
                    padding: "0.5rem 0.75rem",
                    background: "var(--color-input)",
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                    color: "var(--color-foreground)",
                    fontSize: "0.875rem",
                    outline: "none",
                  }}
                />
              </div>

              {/* Complete Sale Button */}
              <button
                onClick={completeSale}
                disabled={selling}
                style={{
                  ...btnStyle("primary"),
                  width: "100%",
                  justifyContent: "center",
                  opacity: selling ? 0.7 : 1,
                }}
              >
                {selling ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    Processing...
                  </>
                ) : (
                  <>
                    <CheckCircle size={16} />
                    Complete Sale
                  </>
                )}
              </button>
            </>
          )}
        </div>
      </div>

      {/* Receipt Modal */}
      {completedSale && (
        <Modal
          title={`Receipt #${completedSale.receiptNumber}`}
          onClose={() => setCompletedSale(null)}
        >
          <div style={{ textAlign: "center" }}>
            <div
              style={{
                width: "60px",
                height: "60px",
                borderRadius: "50%",
                background: "oklch(0.72 0.18 145 / 0.12)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                margin: "0 auto 1rem",
              }}
            >
              <CheckCircle
                size={32}
                style={{ color: "oklch(0.72 0.18 145)" }}
              />
            </div>
            <div
              style={{
                fontSize: "0.8rem",
                color: "var(--color-muted-foreground)",
                marginBottom: "1rem",
              }}
            >
              {new Date(completedSale.createdAt).toLocaleString()}
            </div>

            {/* Receipt Items */}
            <div
              style={{
                background: "var(--color-background)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                padding: "1rem",
                marginBottom: "1rem",
                textAlign: "left",
              }}
            >
              {completedSale.items.map((item, i) => (
                <div
                  key={i}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    padding: "0.375rem 0",
                    borderBottom:
                      i < completedSale.items.length - 1
                        ? "1px solid var(--color-border)"
                        : "none",
                    fontSize: "0.875rem",
                  }}
                >
                  <span>
                    {item.quantity}x {item.name}
                  </span>
                  <span style={{ fontFamily: "var(--font-mono)" }}>
                    ${(item.unitCost * item.quantity).toFixed(2)}
                  </span>
                </div>
              ))}
            </div>

            {/* Total */}
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                fontSize: "1.1rem",
                fontWeight: 700,
                padding: "0.75rem 0",
                borderTop: "2px solid var(--color-border)",
              }}
            >
              <span>Total</span>
              <span style={{ fontFamily: "var(--font-mono)", color: "oklch(0.75 0.15 85)" }}>
                ${completedSale.totalCost.toFixed(2)}
              </span>
            </div>

            <div
              style={{
                fontSize: "0.75rem",
                color: "var(--color-muted-foreground)",
                marginTop: "0.5rem",
              }}
            >
              Payment: {completedSale.paymentType}
            </div>

            <button
              onClick={() => setCompletedSale(null)}
              style={{ ...btnStyle("primary"), marginTop: "1.25rem" }}
            >
              <ShoppingCart size={16} /> New Sale
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ─── POS Items Tab ─────────────────────────────────────────────────────────────

function POSItemsTab({
  onToast,
}: {
  onToast: (msg: string, type: "success" | "error") => void;
}) {
  const [items, setItems] = useState<PosItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editItem, setEditItem] = useState<PosItem | null>(null);
  const [formData, setFormData] = useState({
    name: "",
    cost: "",
    category: "",
  });
  const [saving, setSaving] = useState(false);

  const fetchItems = useCallback(async () => {
    setLoading(true);
    try {
      const result = await posApi.itemsAll();
      setItems(result.data?.items ?? []);
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setLoading(false);
    }
  }, [onToast]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  function openCreate() {
    setEditItem(null);
    setFormData({ name: "", cost: "", category: "" });
    setShowModal(true);
  }

  function openEdit(item: PosItem) {
    setEditItem(item);
    setFormData({
      name: item.name,
      cost: String(item.cost),
      category: item.category ?? "",
    });
    setShowModal(true);
  }

  async function handleSave() {
    const cost = parseFloat(formData.cost);
    if (!formData.name || isNaN(cost) || cost < 0) {
      onToast("Please enter a valid name and cost", "error");
      return;
    }

    setSaving(true);
    try {
      if (editItem) {
        await posApi.updateItem(editItem.id, {
          name: formData.name,
          cost,
          category: formData.category || undefined,
        });
        onToast("Item updated", "success");
      } else {
        await posApi.createItem({
          name: formData.name,
          cost,
          category: formData.category || undefined,
        });
        onToast("Item created", "success");
      }
      setShowModal(false);
      fetchItems();
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(item: PosItem) {
    if (!confirm(`Delete "${item.name}"?`)) return;
    try {
      await posApi.deleteItem(item.id);
      onToast("Item deleted", "success");
      fetchItems();
    } catch (e: any) {
      onToast(e.message, "error");
    }
  }

  return (
    <div>
      {/* Toolbar */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "1rem",
        }}
      >
        <h3 style={{ fontSize: "0.95rem", fontWeight: 700 }}>
          POS Items ({items.length})
        </h3>
        <button onClick={openCreate} style={btnStyle("primary")}>
          <Plus size={16} /> New Item
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
        ) : items.length === 0 ? (
          <div
            style={{
              padding: "3rem",
              textAlign: "center",
              color: "var(--color-muted-foreground)",
            }}
          >
            No items yet. Create one to get started.
          </div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid var(--color-border)" }}>
                  {["Name", "Category", "Cost", "Status", "Actions"].map((h) => (
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
                {items.map((item) => (
                  <tr
                    key={item.id}
                    style={{ borderBottom: "1px solid var(--color-border)" }}
                  >
                    <td style={{ padding: "0.75rem 1rem", fontWeight: 600 }}>
                      {item.name}
                    </td>
                    <td
                      style={{
                        padding: "0.75rem 1rem",
                        fontSize: "0.85rem",
                        color: "var(--color-muted-foreground)",
                      }}
                    >
                      {item.category ?? "—"}
                    </td>
                    <td
                      style={{
                        padding: "0.75rem 1rem",
                        fontFamily: "var(--font-mono)",
                        fontSize: "0.875rem",
                        color: "oklch(0.75 0.15 85)",
                      }}
                    >
                      ${Number(item.cost).toFixed(2)}
                    </td>
                    <td style={{ padding: "0.75rem 1rem" }}>
                      <span
                        style={{
                          padding: "2px 8px",
                          borderRadius: "999px",
                          fontSize: "0.7rem",
                          fontWeight: 600,
                          background: item.isActive
                            ? "oklch(0.72 0.18 145 / 0.12)"
                            : "oklch(0.65 0.22 25 / 0.12)",
                          color: item.isActive
                            ? "oklch(0.72 0.18 145)"
                            : "oklch(0.65 0.22 25)",
                        }}
                      >
                        {item.isActive ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td style={{ padding: "0.75rem 1rem" }}>
                      <div style={{ display: "flex", gap: "0.375rem" }}>
                        <button
                          onClick={() => openEdit(item)}
                          style={{
                            padding: "0.375rem 0.625rem",
                            background: "transparent",
                            border: "1px solid var(--color-border)",
                            borderRadius: "var(--radius-md)",
                            color: "var(--color-muted-foreground)",
                            cursor: "pointer",
                            fontSize: "0.8rem",
                            display: "flex",
                            alignItems: "center",
                            gap: "4px",
                          }}
                        >
                          <Edit size={12} />
                        </button>
                        <button
                          onClick={() => handleDelete(item)}
                          style={{
                            padding: "0.375rem 0.625rem",
                            background: "transparent",
                            border: "1px solid var(--color-border)",
                            borderRadius: "var(--radius-md)",
                            color: "oklch(0.65 0.22 25)",
                            cursor: "pointer",
                            fontSize: "0.8rem",
                            display: "flex",
                            alignItems: "center",
                            gap: "4px",
                          }}
                        >
                          <Trash2 size={12} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create/Edit Modal */}
      {showModal && (
        <Modal
          title={editItem ? "Edit Item" : "New Item"}
          onClose={() => setShowModal(false)}
        >
          <div
            style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
          >
            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  marginBottom: "0.375rem",
                }}
              >
                Name *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) =>
                  setFormData({ ...formData, name: e.target.value })
                }
                placeholder="Item name"
                style={{
                  width: "100%",
                  padding: "0.5rem 0.75rem",
                  background: "var(--color-input)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                  fontSize: "0.875rem",
                  outline: "none",
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
                Cost *
              </label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={formData.cost}
                onChange={(e) =>
                  setFormData({ ...formData, cost: e.target.value })
                }
                placeholder="0.00"
                style={{
                  width: "100%",
                  padding: "0.5rem 0.75rem",
                  background: "var(--color-input)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                  fontSize: "0.875rem",
                  fontFamily: "var(--font-mono)",
                  outline: "none",
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
                Category (optional)
              </label>
              <input
                type="text"
                value={formData.category}
                onChange={(e) =>
                  setFormData({ ...formData, category: e.target.value })
                }
                placeholder="e.g. Beverages, Snacks"
                style={{
                  width: "100%",
                  padding: "0.5rem 0.75rem",
                  background: "var(--color-input)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  color: "var(--color-foreground)",
                  fontSize: "0.875rem",
                  outline: "none",
                }}
              />
            </div>
            <div
              style={{
                display: "flex",
                gap: "0.75rem",
                justifyContent: "flex-end",
                marginTop: "0.5rem",
              }}
            >
              <button
                onClick={() => setShowModal(false)}
                style={btnStyle("secondary")}
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                style={{ ...btnStyle("primary"), opacity: saving ? 0.7 : 1 }}
              >
                {saving ? "Saving..." : editItem ? "Save Changes" : "Create Item"}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function AdminPOSPage() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<"terminal" | "items">("terminal");
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
    { id: "terminal" as const, label: "POS Terminal", icon: ShoppingCart },
    { id: "items" as const, label: "POS Items", icon: Edit },
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
          Point of Sale
        </h1>
        <p
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.875rem",
            marginTop: "0.25rem",
          }}
        >
          Process sales, manage items, and track revenue
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
      {activeTab === "terminal" && <POSTerminalTab onToast={showToast} />}
      {activeTab === "items" && <POSItemsTab onToast={showToast} />}

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
