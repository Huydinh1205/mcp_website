"use client";

// Storefront product page: gallery + sticky buy box, per-shop reviews.

import { use, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { API_BASE } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { Stars } from "@/app/components/Stars";
import { Icon } from "@/app/components/Icon";
import { compact, money, ago } from "@/lib/format";
import { addToCart } from "@/lib/cart";
import { toast } from "@/lib/toast";
import { useWishlist, toggleWishlist } from "@/lib/wishlist";
import { Skeleton } from "@/app/components/Skeleton";

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
  seller_id?: string;
  seller_name: string;
  seller_rating: number;
  price: number;
  shipping_cost: number;
  rating_avg: number;
  rating_count: number;
  rating_breakdown: number[];
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
  rating_breakdown?: number[];
  sellers: SellerRow[];
  avg_rating: number;
  reviews: Review[];
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

function Breakdown({ bd, active, onPick }: { bd: number[]; active: number; onPick: (s: number) => void }) {
  const total = bd.reduce((a, b) => a + b, 0) || 1;
  return (
    <div className="rsummary__bars">
      {[5, 4, 3, 2, 1].map((s, i) => {
        const c = bd[i] ?? 0;
        return (
          <button key={s} className={`rbar ${active === s ? "rbar--on" : ""}`} onClick={() => onPick(active === s ? 0 : s)}>
            <span className="rbar__label">{s}★</span>
            <span className="rbar__track"><span className="rbar__fill" style={{ width: `${(c / total) * 100}%` }} /></span>
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
  const wl = useWishlist();
  const [d, setD] = useState<Detail | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [star, setStar] = useState(0);
  const [openShops, setOpenShops] = useState<Record<string, boolean>>({});
  const [shopStar, setShopStar] = useState<Record<string, number>>({});
  const [qty, setQty] = useState(1);
  const [gimg, setGimg] = useState(0);

  useEffect(() => {
    fetch(`${API_BASE}/api/products/${encodeURIComponent(id)}`)
      .then((r) => r.json())
      .then((j) => (j.error ? setNotFound(true) : setD(j)))
      .catch(() => setNotFound(true));
  }, [id]);

  const gallery = useMemo(() => {
    if (!d?.image_url) return [];
    const main = d.image_url;
    // derive matching thumbnails by nudging the seed/lock so they stay on-topic
    const variants = [1, 2, 3].map((k) =>
      /[?&]lock=/.test(main)
        ? main.replace(/([?&]lock=)([^&]+)/, `$1$2${k}`)
        : `https://picsum.photos/seed/pv${d.product_id}-${k}/600/700`,
    );
    return [main, ...variants];
  }, [d]);

  const shownReviews = useMemo(
    () => (d?.reviews ?? []).filter((r) => star === 0 || r.rating === star),
    [d, star],
  );

  if (notFound) return <main className="wrap"><p className="muted">Product not found. <Link href="/catalog">Back to catalog</Link></p></main>;
  if (!d) {
    return (
      <main className="wrap wide">
        <div className="pdp">
          <div className="pdp__gallery"><Skeleton h={420} r={16} /></div>
          <div className="pdp__info">
            <Skeleton h={26} w="80%" />
            <Skeleton h={16} w="50%" style={{ marginTop: 12 }} />
            <Skeleton h={40} w="40%" style={{ marginTop: 16 }} />
            <Skeleton h={44} style={{ marginTop: 20 }} />
          </div>
        </div>
      </main>
    );
  }

  const rating = d.rating_avg ?? d.avg_rating ?? 0;
  const total = d.rating_count ?? d.reviews.length;
  const bd = d.rating_breakdown ?? [0, 0, 0, 0, 0];
  const hasDisc = (d.discount_pct ?? 0) > 0 && d.compare_at_price;
  const wished = wl.has(d.product_id);

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
    router.push(`/agent?goal=${encodeURIComponent(goal)}`);
  };

  const addCart = (s: SellerRow) => {
    addToCart(
      { product_id: s.product_id, name: d.name, price: s.price, image_url: d.image_url ?? null, seller_name: s.seller_name },
      qty,
    );
    toast(`Added ${qty} to cart`, "success");
  };

  return (
    <main className="wrap wide">
      <nav className="crumbs">
        <Link href="/">Home</Link>
        <Icon name="chevron" size={12} />
        <Link href={d.category ? `/catalog?category=${encodeURIComponent(d.category)}` : "/catalog"}>
          {d.category ?? "Catalog"}
        </Link>
        <Icon name="chevron" size={12} />
        <span className="muted">{d.name}</span>
      </nav>

      <div className="pdp">
        <div className="pdp__gallery">
          <div className="pdp__main">
            {gallery[gimg] ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img className="pdp__img" src={gallery[gimg]} alt={d.name} />
            ) : null}
            <button
              className={`pdp__heart ${wished ? "on" : ""}`}
              title={wished ? "Remove from wishlist" : "Save"}
              onClick={() => {
                const now = toggleWishlist({
                  product_id: d.product_id,
                  name: d.name,
                  price: d.price,
                  compare_at_price: d.compare_at_price,
                  discount_pct: d.discount_pct,
                  rating_avg: rating,
                  sold_count: d.sold_count,
                  free_shipping: d.free_shipping,
                  image_url: d.image_url ?? null,
                });
                toast(now ? "Saved to wishlist" : "Removed from wishlist");
              }}
            >
              <Icon name="heart" size={18} fill={wished} />
            </button>
          </div>
          {gallery.length > 1 ? (
            <div className="pdp__thumbs">
              {gallery.map((g, i) => (
                <button
                  key={i}
                  className={`pdp__thumb ${i === gimg ? "on" : ""}`}
                  onClick={() => setGimg(i)}
                >
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img src={g} alt="" />
                </button>
              ))}
            </div>
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
            {d.free_shipping ? <span className="chip chip--ship"><Icon name="truck" size={12} /> Free shipping</span> : null}
            {typeof d.remaining === "number" ? <span className="chip">{d.remaining} in stock</span> : null}
          </div>

          <div className="buybar">
            <label className="qty">
              Qty
              <button type="button" onClick={() => setQty((q) => Math.max(1, q - 1))}>−</button>
              <input type="number" min={1} value={qty} onChange={(e) => setQty(Math.max(1, Number(e.target.value) || 1))} />
              <button type="button" onClick={() => setQty((q) => q + 1)}>+</button>
            </label>
            <button className="buynow" onClick={() => addCart(d.sellers[0])}>
              <Icon name="cart" size={15} /> Add to cart · ${money(d.price * qty)}
            </button>
            <button className="secondary" onClick={() => negotiate(d.sellers[0])}>
              <Icon name="handshake" size={15} /> Negotiate
            </button>
          </div>

          {d.description ? (
            <p className="pdp__desc">{d.description}</p>
          ) : (
            <p className="muted small">
              Add to cart at list price, or send an agent to a shop below to negotiate (quantity, freebies, free shipping, coupons).
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
                  {s.seller_id ? (
                    <Link href={`/shop/${s.seller_id}`}><strong>{s.seller_name}</strong></Link>
                  ) : (
                    <strong>{s.seller_name}</strong>
                  )}
                  <div className="shoprow__meta">
                    <Stars value={s.rating_avg} /> {s.rating_avg.toFixed(1)} ·{" "}
                    {compact(s.rating_count)} ratings ·{" "}
                    {s.shipping_cost <= 0 ? "free shipping" : `+$${money(s.shipping_cost)} shipping`}
                  </div>
                </div>
                <div className="shoprow__right">
                  <span className="shoprow__price">${money(s.price)}</span>
                  <button className="buynow" onClick={() => addCart(s)}>
                    <Icon name="cart" size={14} /> Add to cart
                  </button>
                  <button className="secondary" onClick={() => negotiate(s)}>
                    {user?.role === "buyer" ? "Negotiate" : "Log in"}
                  </button>
                </div>
              </div>
              <button
                className="shoprow__toggle"
                onClick={() => setOpenShops((p) => ({ ...p, [s.product_id]: !p[s.product_id] }))}
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
