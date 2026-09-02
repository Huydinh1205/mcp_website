"use client";

// Storefront product page: gallery, price block, and reviews SPLIT PER SHOP.
// Every seller carrying this model is its own listing with its own reviews;
// each shop row expands to that shop's rating breakdown + review list. The
// top section merges every shop's reviews, tagged with the shop name.

import { use, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { API_BASE } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { Stars } from "@/app/components/Stars";
import { compact, money, ago } from "@/lib/format";
import { buyNow } from "@/lib/orders";
import { addToCart } from "@/lib/cart";
import { Icon } from "@/app/components/Icon";

interface Review {
  rating: number;
  comment: string | null;
  reviewer: string;
  verified: boolean;
  created_at: string;
  seller_name?: string;
}
interface SellerRow {
  product_id: string;
  seller_name: string;
  seller_rating: number;
  price: number;
  shipping_cost: number;
  rating_avg: number;
  rating_count: number;
  rating_breakdown: number[]; // [5★,4★,3★,2★,1★]
  reviews: Review[];
}
interface Detail {
  product_id: string;
  name: string;
  description?: string | null;
  remaining?: number;
  category: string | null;
  price: number;
  compare_at_price?: number | null;
  discount_pct?: number;
  rating_avg?: number;
  rating_count?: number;
  sold_count?: number;
  free_shipping?: boolean;
  image_url?: string | null;
  rating_breakdown?: number[]; // merged [5★,4★,3★,2★,1★]
  sellers: SellerRow[];
  avg_rating: number;
  reviews: Review[]; // merged across shops
}

function initials(name: string) {
  return name.split(/\s+/).map((w) => w[0]).join("").slice(0, 2).toUpperCase();
}

function ReviewItem({ r, showShop }: { r: Review; showShop: boolean }) {
  return (
    <div className="rev">
      <div className="rev__avatar">{initials(r.reviewer)}</div>
      <div className="rev__body">
        <div className="rev__top">
          <strong>{r.reviewer}</strong>
          {showShop && r.seller_name ? <span className="chip">{r.seller_name}</span> : null}
          {r.verified ? <span className="rev__verified">Verified purchase</span> : null}
          <span className="muted small">{ago(r.created_at)}</span>
        </div>
        <Stars value={r.rating} />
        {r.comment ? <p className="rev__text">{r.comment}</p> : null}
      </div>
    </div>
  );
}

function Breakdown({
  bd,
  active,
  onPick,
}: {
  bd: number[];
  active: number;
  onPick: (s: number) => void;
}) {
  const total = bd.reduce((a, b) => a + b, 0) || 1;
  return (
    <div className="rsummary__bars">
      {[5, 4, 3, 2, 1].map((s, i) => {
        const c = bd[i] ?? 0;
        return (
          <button
            key={s}
            className={`rbar ${active === s ? "rbar--on" : ""}`}
            onClick={() => onPick(active === s ? 0 : s)}
          >
            <span className="rbar__label">{s}★</span>
            <span className="rbar__track">
              <span className="rbar__fill" style={{ width: `${(c / total) * 100}%` }} />
            </span>
            <span className="rbar__count">{c}</span>
          </button>
        );
      })}
    </div>
  );
}

export default function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const user = useAuth();
  const [d, setD] = useState<Detail | null>(null);
  const [star, setStar] = useState(0); // 0 = all — filters the merged list
  const [openShops, setOpenShops] = useState<Record<string, boolean>>({});
  const [shopStar, setShopStar] = useState<Record<string, number>>({});
  const [qty, setQty] = useState(1);
  const [buying, setBuying] = useState<string | null>(null);
  const [bought, setBought] = useState<{ orderId: string; total: number } | null>(null);
  const [added, setAdded] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/products/${encodeURIComponent(id)}`)
      .then((r) => r.json())
      .then((j) => setD(j.error ? null : j))
      .catch(() => setD(null));
  }, [id]);

  const shownReviews = useMemo(
    () => (d?.reviews ?? []).filter((r) => star === 0 || r.rating === star),
    [d, star],
  );

  if (!d) return <main className="wrap"><p className="muted">Loading…</p></main>;

  const rating = d.rating_avg ?? d.avg_rating ?? 0;
  const total = d.rating_count ?? d.reviews.length;
  const bd = d.rating_breakdown ?? [0, 0, 0, 0, 0];
  const hasDisc = (d.discount_pct ?? 0) > 0 && d.compare_at_price;

  const needBuyer = () => {
    if (!user || user.role !== "buyer") {
      router.push(`/login?next=/product/${d.product_id}`);
      return false;
    }
    return true;
  };

  const negotiate = (s: SellerRow) => {
    if (!needBuyer()) return;
    const goal = `Negotiate the best deal for "${d.name}" from ${s.seller_name} (listed ${s.price}), quantity ${qty}. Use quantity, free add-ons, free shipping and coupons where they help.`;
    router.push(`/?goal=${encodeURIComponent(goal)}`);
  };

  const buy = async (productId: string) => {
    if (!needBuyer()) return;
    setBuying(productId);
    try {
      const r = await buyNow(productId, qty);
      setBought({ orderId: r.order_id, total: r.total });
    } catch (e) {
      alert(e instanceof Error ? e.message : "buy failed");
    } finally {
      setBuying(null);
    }
  };

  const addCart = (s: SellerRow) => {
    addToCart(
      {
        product_id: s.product_id,
        name: d.name,
        price: s.price,
        image_url: d.image_url ?? null,
        seller_name: s.seller_name,
      },
      qty,
    );
    setAdded(s.product_id);
    setTimeout(() => setAdded(null), 1500);
  };

  return (
    <main className="wrap wide">
      <div className="pdp">
        <div className="pdp__gallery">
          {d.image_url ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img className="pdp__img" src={d.image_url} alt={d.name} />
          ) : null}
        </div>

        <div className="pdp__info">
          <h1>{d.name}</h1>
          <div className="pdp__rateline">
            <Stars value={rating} size={16} />
            <strong>{rating.toFixed(1)}</strong>
            <span className="dot">·</span>
            <span>{compact(total)} ratings</span>
            <span className="dot">·</span>
            <span>{compact(d.sold_count ?? 0)} sold</span>
            <span className="dot">·</span>
            <span>{d.sellers.length} shops</span>
          </div>

          <div className="pricebox">
            <span className="pricebox__now">${money(d.price)}</span>
            {hasDisc ? (
              <>
                <span className="pricebox__was">${money(d.compare_at_price!)}</span>
                <span className="pricebox__disc">-{d.discount_pct}%</span>
              </>
            ) : null}
          </div>

          <div className="pdp__tags">
            {d.category ? <span className="chip">{d.category}</span> : null}
            {d.free_shipping ? <span className="chip chip--ship">Free shipping</span> : null}
            {typeof d.remaining === "number" ? (
              <span className="chip">{d.remaining} in stock</span>
            ) : null}
          </div>

          <div className="buybar">
            <label className="qty">
              Qty
              <button type="button" onClick={() => setQty((q) => Math.max(1, q - 1))}>−</button>
              <input
                type="number"
                min={1}
                value={qty}
                onChange={(e) => setQty(Math.max(1, Number(e.target.value) || 1))}
              />
              <button type="button" onClick={() => setQty((q) => q + 1)}>+</button>
            </label>
            <button
              className="buynow"
              disabled={buying === d.product_id}
              onClick={() => buy(d.product_id)}
            >
              <Icon name="bolt" size={15} />
              {buying === d.product_id ? "Placing…" : `Buy now · $${money(d.price * qty)}`}
            </button>
            <button className="secondary" onClick={() => addCart(d.sellers[0])}>
              <Icon name={added === d.sellers[0]?.product_id ? "check" : "cart"} size={15} />
              {added === d.sellers[0]?.product_id ? "Added" : "Add to cart"}
            </button>
            <button className="secondary" onClick={() => negotiate(d.sellers[0])}>
              <Icon name="handshake" size={15} />
              Negotiate
            </button>
          </div>

          {bought ? (
            <p className="ok">
              Order #{bought.orderId} placed · ${money(bought.total)}.{" "}
              <a href="/orders">Track it →</a>
            </p>
          ) : null}

          {d.description ? (
            <p className="pdp__desc">{d.description}</p>
          ) : (
            <p className="muted small">
              Buy now at list price, or send an agent to a shop below to negotiate the real deal
              (quantity, freebies, free shipping, coupons).
            </p>
          )}
        </div>
      </div>

      <section className="panel">
        <h2>Shops carrying this item ({d.sellers.length})</h2>
        {d.sellers.map((s) => {
          const open = !!openShops[s.product_id];
          const sStar = shopStar[s.product_id] ?? 0;
          const revs = s.reviews.filter((r) => sStar === 0 || r.rating === sStar);
          return (
            <div key={s.product_id} className="shoprow">
              <div className="shoprow__head">
                <div>
                  <strong>{s.seller_name}</strong>
                  <div className="shoprow__meta">
                    <Stars value={s.rating_avg} /> {s.rating_avg.toFixed(1)} ·{" "}
                    {compact(s.rating_count)} ratings ·{" "}
                    {s.shipping_cost <= 0 ? "free shipping" : `+$${money(s.shipping_cost)} shipping`}
                  </div>
                </div>
                <div className="shoprow__right">
                  <span className="shoprow__price">${money(s.price)}</span>
                  <button
                    className="buynow"
                    disabled={buying === s.product_id}
                    onClick={() => buy(s.product_id)}
                  >
                    {buying === s.product_id ? "…" : "Buy now"}
                  </button>
                  <button className="secondary" onClick={() => addCart(s)}>
                    {added === s.product_id ? "Added ✓" : "Add to cart"}
                  </button>
                  <button className="secondary" onClick={() => negotiate(s)}>
                    {user?.role === "buyer" ? "Negotiate" : "Log in"}
                  </button>
                </div>
              </div>
              <button
                className="shoprow__toggle"
                onClick={() =>
                  setOpenShops((p) => ({ ...p, [s.product_id]: !p[s.product_id] }))
                }
              >
                {open ? "Hide" : "Show"} this shop&apos;s reviews ({s.reviews.length})
              </button>
              {open ? (
                <div className="shoprow__reviews">
                  <Breakdown
                    bd={s.rating_breakdown ?? [0, 0, 0, 0, 0]}
                    active={sStar}
                    onPick={(v) => setShopStar((p) => ({ ...p, [s.product_id]: v }))}
                  />
                  <div className="reviews">
                    {revs.length === 0 ? (
                      <p className="muted">No reviews {sStar ? `with ${sStar}★` : "yet"}.</p>
                    ) : (
                      revs.map((r, i) => <ReviewItem key={i} r={r} showShop={false} />)
                    )}
                  </div>
                </div>
              ) : null}
            </div>
          );
        })}
      </section>

      <section className="panel">
        <h2>All reviews · {d.sellers.length} shops</h2>
        <div className="rsummary">
          <div className="rsummary__score">
            <div className="big">{rating.toFixed(1)}</div>
            <Stars value={rating} size={16} />
            <div className="muted small">{compact(total)} ratings</div>
          </div>
          <Breakdown bd={bd} active={star} onPick={setStar} />
        </div>

        <div className="reviews">
          {shownReviews.length === 0 ? (
            <p className="muted">No reviews {star ? `with ${star}★` : "yet"}.</p>
          ) : (
            shownReviews.map((r, i) => <ReviewItem key={i} r={r} showShop />)
          )}
        </div>
      </section>
    </main>
  );
}
