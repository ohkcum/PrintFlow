interface SkeletonProps {
  className?: string;
  style?: React.CSSProperties;
  width?: string | number;
  height?: string | number;
}

export function Skeleton({ className = "", style, width, height }: SkeletonProps) {
  return (
    <div
      className={`skeleton ${className}`}
      style={{
        width: typeof width === "number" ? `${width}px` : width,
        height: typeof height === "number" ? `${height}px` : height,
        ...style,
      }}
    />
  );
}

export function SkeletonRow({ cols = 5 }: { cols?: number }) {
  return (
    <div style={{ display: "flex", gap: "0.75rem", alignItems: "center", padding: "0.75rem 1rem", borderBottom: "1px solid var(--color-border)" }}>
      <Skeleton width={32} height={32} style={{ borderRadius: "50%", flexShrink: 0 }} />
      <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: "0.375rem" }}>
        <Skeleton height={14} width="60%" />
        <Skeleton height={12} width="40%" />
      </div>
      {Array.from({ length: cols - 1 }).map((_, i) => (
        <Skeleton key={i} height={14} width={60} />
      ))}
    </div>
  );
}

export function SkeletonCard() {
  return (
    <div style={{ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)", padding: "1.25rem" }}>
      <div style={{ display: "flex", gap: "0.75rem", alignItems: "center", marginBottom: "1rem" }}>
        <Skeleton width={40} height={40} style={{ borderRadius: "var(--radius-md)" }} />
        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: "0.375rem" }}>
          <Skeleton height={16} width="70%" />
          <Skeleton height={12} width="40%" />
        </div>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.5rem" }}>
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
            <Skeleton height={10} width="50%" />
            <Skeleton height={13} width="70%" />
          </div>
        ))}
      </div>
    </div>
  );
}

export function SkeletonTable({ rows = 5, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <div style={{ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)", overflow: "hidden" }}>
      {/* Header */}
      <div style={{ display: "flex", gap: "0.75rem", padding: "0.75rem 1rem", borderBottom: "1px solid var(--color-border)", background: "oklch(0.20 0.02 250)" }}>
        {Array.from({ length: cols }).map((_, i) => (
          <Skeleton key={i} height={12} width={i === 0 ? 120 : 70} />
        ))}
      </div>
      {/* Rows */}
      {Array.from({ length: rows }).map((_, r) => (
        <SkeletonRow key={r} cols={cols} />
      ))}
    </div>
  );
}

export function SkeletonStatCard() {
  return (
    <div style={{ background: "var(--color-card)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-lg)", padding: "1.25rem", flex: 1, minWidth: 180 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "0.75rem" }}>
        <Skeleton width={36} height={36} style={{ borderRadius: "var(--radius-md)" }} />
      </div>
      <Skeleton height={28} width="50%" style={{ marginBottom: "0.375rem" }} />
      <Skeleton height={12} width="70%" />
    </div>
  );
}
