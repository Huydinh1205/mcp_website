"use client";

// Buyer's orders + delivery tracking. Orders come from buy-now or a confirmed
// negotiation; each has a Delivery row with a tracking number + ETA.

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/auth";
import { myOrders, type Order } from "@/lib/orders";
import { money, ago } from "@/lib/format";
import { ReviewForm } from "@/app/components/ReviewForm";
import { Icon } from "@/app/components/Icon";

const STEPS = [
  { key: "PENDING", label: "Order placed", icon: "check" as const },
  { key: "IN_TRANSIT", label: "In transit", icon: "package" as const },
  { key: "OUT_FOR_DELIVERY", label: "Out for delivery", icon: "truck" as const },
  { key: "DELIVERED", label: "Delivered", icon: "check" as const },
];
const LABEL: Record<string, string> = {
  PENDING: "Order placed",
  IN_TRANSIT: "In transit",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  FAILED: "Delivery failed",
};

function Tracker({ status }: { status: string }) {
  const at = Math.max(0, STEPS.findIndex((s) => s.key === status));
  const failed = status === "FAILED";
  return (
    <div className={`track ${failed ? "track--failed" : ""}`}>
      {STEPS.map((s, i) => (
        <div key={s.key} className={`track__step ${i <= at ? "on" : ""}`}>
          <span className="track__dot"><Icon name={s.icon} size={11} /></span>
          <span className="track__label">{s.label}</span>
        </div>
      ))}
    </div>
  );
}

export default function OrdersPage() {
  const user = useAuth();
  const router = useRouter();
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [reviewing, setReviewing] = useState<string | null>(null);

  useEffect(() => {
    if (user === null) return;
    if (!user || user.role !== "buyer") {
      router.replace("/login?next=/orders");
      return;
    }
    myOrders().then(setOrders);
  }, [user, router]);

  if (!user || user.role !== "buyer") {
    return <main className="wrap"><p className="muted">Redirecting…</p></main>;
  }

  return (
    <main className="wrap">
      <h1>My orders</h1>
      {orders === null ? (
        <p className="muted">Loading…</p>
      ) : orders.length === 0 ? (
        <p className="muted">
          No orders yet. <Link href="/catalog">Browse the catalog</Link> — add to cart, or send an agent to negotiate.
        </p>
      ) : (
        <div className="olist">
          {orders.map((o) => (
            <div key={o.order_id} className="ocard">
              <div className="ocard__head">
                {o.image_url ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img className="ocard__img" src={o.image_url} alt={o.name} />
                ) : null}
                <div className="ocard__meta">
                  <Link href={o.product_id ? `/product/${o.product_id}` : "#"} className="ocard__name">
                    {o.name}
                  </Link>
                  <div className="muted small">
                    Order #{o.order_id} · {o.ordered_at ? ago(o.ordered_at) : "—"} ·{" "}
                    {o.negotiated ? "negotiated" : "bought at list price"}
                  </div>
                  <div className="ocard__nums">
                    <span>Qty {o.quantity}</span>
                    <span className="dot">·</span>
                    <span>${money(o.unit_price)} each</span>
                    <span className="dot">·</span>
                    <strong>${money(o.total)} total</strong>
                  </div>
                </div>
                <span className={`obadge obadge--${o.delivery_status.toLowerCase()}`}>
                  {LABEL[o.delivery_status] ?? o.delivery_status}
                </span>
              </div>

              <Tracker status={o.delivery_status} />

              <div className="ocard__foot">
                <span className="muted small">
                  {o.tracking_number ? <>Tracking <code>{o.tracking_number}</code></> : "No tracking yet"}
                  {o.estimated_date ? ` · ETA ${new Date(o.estimated_date).toLocaleDateString()}` : ""}
                </span>
                {o.reviewed ? (
                  <span className="chip">Reviewed ✓</span>
                ) : o.product_id ? (
                  <button className="linkbtn" onClick={() => setReviewing(reviewing === o.order_id ? null : o.order_id)}>
                    {reviewing === o.order_id ? "Cancel" : "Write a review"}
                  </button>
                ) : null}
              </div>

              {reviewing === o.order_id && o.product_id ? (
                <ReviewForm
                  productId={o.product_id}
                  negotiationId={o.negotiation_id}
                  onDone={() => {
                    setReviewing(null);
                    myOrders().then(setOrders);
                  }}
                />
              ) : null}
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
