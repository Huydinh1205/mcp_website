"use client";

/** Partial-fill star rating. `value` 0..5. */
export function Stars({ value, size = 14 }: { value: number; size?: number }) {
  const pct = Math.max(0, Math.min(100, (value / 5) * 100));
  return (
    <span
      className="stars"
      role="img"
      aria-label={`${value.toFixed(1)} out of 5`}
      style={{ fontSize: size }}
    >
      <span className="stars__bg">★★★★★</span>
      <span className="stars__fg" style={{ width: `${pct}%` }}>
        ★★★★★
      </span>
    </span>
  );
}
