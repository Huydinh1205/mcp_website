"use client";

// Product detail (US4): every seller carrying this product + reviews + image.
// "Send agent to negotiate" deep-links the buyer page (must be logged in as buyer).

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { API_BASE } from "@/lib/api";
import { useAuth } from "@/lib/auth";

interface SellerRow {
  product_id: string;
  seller_name: string;
  seller_rating: number;
  price: number;
  shipping_cost: number;
}
interface Detail {
  product_id: string;
  name: string;
  category: string | null;
  price: number;
  image_url?: string | null;
  sellers: SellerRow[];
  avg_rating: number;
  reviews: { rating: number; comment: string | null }[];
}

export default function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const user = useAuth();
  const [d, setD] = useState<Detail | null>(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/products/${encodeURIComponent(id)}`)
      .then((r) => r.json())
      .then((j) => setD(j.error ? null : j))
      .catch(() => setD(null));
  }, [id]);

  if (!d) return <main className="wrap"><p className="muted">Loading…</p></main>;

  const negotiate = (seller: SellerRow) => {
    if (!user || user.role !== "buyer") {
      router.push("/login?next=/");
      return;
    }
    const goal = `Negotiate the best deal for "${d.name}" from ${seller.seller_name} (listed ${seller.price}). Use quantity, free add-ons, free shipping and coupons where they help.`;
    router.push(`/?goal=${encodeURIComponent(goal)}`);
  };

  return (
    <main className="wrap">
      <div className="pdp">
        {d.image_url ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img className="pdp__img" src={d.image_url} alt={d.name} />
        ) : null}
        <div>
          <h1>{d.name}</h1>
          <p className="muted">
            {d.category ?? "—"} · list {d.price.toFixed(2)} ·{" "}
            {d.avg_rating > 0 ? `★ ${d.avg_rating}` : "no reviews yet"}
          </p>
        </div>
      </div>

      <section className="panel">
        <h2>Sellers</h2>
        {d.sellers.map((s) => (
          <div key={s.product_id} className="sellerrow">
            <span>
              <strong>{s.seller_name}</strong> · ★ {s.seller_rating} · {s.price.toFixed(2)}{" "}
              <span className="muted">(+{s.shipping_cost.toFixed(2)} ship)</span>
            </span>
            <button onClick={() => negotiate(s)}>
              {user?.role === "buyer" ? "Send agent to negotiate" : "Log in to negotiate"}
            </button>
          </div>
        ))}
      </section>

      <section className="panel">
        <h2>Reviews</h2>
        {d.reviews.length === 0 ? (
          <p className="muted">No reviews yet.</p>
        ) : (
          d.reviews.map((r, i) => (
            <div key={i} className="review">
              ★ {r.rating} {r.comment ? <span>— {r.comment}</span> : null}
            </div>
          ))
        )}
      </section>
    </main>
  );
}
