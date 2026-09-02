"use client";

// Storefront listing: search, category chips, filters, sort, product cards
// with a quick add-to-cart.

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { API_BASE } from "@/lib/api";
import { Stars } from "@/app/components/Stars";
import { Icon } from "@/app/components/Icon";
import { compact, money } from "@/lib/format";
import { addToCart } from "@/lib/cart";

interface Row {
  product_id: string;
  name: string;
  price: number;
  compare_at_price?: number | null;
  discount_pct?: number;
  rating_avg?: number;
  rating_count?: number;
  sold_count?: number;
  free_shipping?: boolean;
  image_url?: string | null;
  seller_name: string;
}

const SORTS = [
  ["", "Relevance"],
  ["sold", "Best selling"],
  ["rating", "Top rated"],
  ["price_asc", "Price: low to high"],
  ["price_desc", "Price: high to low"],
] as const;

export default function CatalogPage() {
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [minRating, setMinRating] = useState("");
  const [sort, setSort] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [rows, setRows] = useState<Row[]>([]);
  const [loading, setLoading] = useState(false);
  const [added, setAdded] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/categories`).then((r) => r.json()).then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (category) params.set("category", category);
    if (maxPrice) params.set("maxPrice", maxPrice);
    if (minRating) params.set("minRating", minRating);
    if (sort) params.set("sort", sort);
    setLoading(true);
    const t = setTimeout(() => {
      fetch(`${API_BASE}/api/products?${params}`)
        .then((r) => r.json())
        .then((d) => setRows(Array.isArray(d) ? d : []))
        .catch(() => setRows([]))
        .finally(() => setLoading(false));
    }, 200);
    return () => clearTimeout(t);
  }, [q, category, maxPrice, minRating, sort]);

  const cards = useMemo(() => {
    const m = new Map<string, Row & { shops: number }>();
    for (const r of rows) {
      const g = m.get(r.name);
      if (!g) m.set(r.name, { ...r, shops: 1 });
      else {
        g.shops += 1;
        if (r.price < g.price) Object.assign(g, r, { shops: g.shops });
      }
    }
    return [...m.values()];
  }, [rows]);

  const quickAdd = (e: React.MouseEvent, p: Row) => {
    e.preventDefault();
    e.stopPropagation();
    addToCart({
      product_id: p.product_id,
      name: p.name,
      price: p.price,
      image_url: p.image_url ?? null,
      seller_name: p.seller_name,
    });
    setAdded(p.product_id);
    setTimeout(() => setAdded(null), 1400);
  };

  return (
    <main className="wrap wide">
      <div className="hero">
        <div>
          <h1 className="hero__title">Shop the catalog</h1>
          <p className="hero__sub">
            Buy now at list price, or send an AI agent to haggle the real deal — price,
            quantity, freebies, free shipping, coupons.
          </p>
        </div>
        <div className="searchbar">
          <Icon name="search" size={16} />
          <input placeholder="Search products…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
      </div>

      {categories.length > 0 ? (
        <div className="chiprow">
          <button className={`chipbtn ${category === "" ? "on" : ""}`} onClick={() => setCategory("")}>
            All
          </button>
          {categories.map((c) => (
            <button
              key={c}
              className={`chipbtn ${category === c ? "on" : ""}`}
              onClick={() => setCategory(category === c ? "" : c)}
            >
              {c}
            </button>
          ))}
        </div>
      ) : null}

      <div className="listing-head">
        <span className="muted small">
          {loading ? "Loading…" : `${cards.length} product${cards.length === 1 ? "" : "s"}`}
        </span>
        <div className="listing-head__right">
          <input
            className="mini"
            type="number"
            placeholder="Max $"
            value={maxPrice}
            onChange={(e) => setMaxPrice(e.target.value)}
          />
          <input
            className="mini"
            type="number"
            step="0.1"
            placeholder="Min ★"
            value={minRating}
            onChange={(e) => setMinRating(e.target.value)}
          />
          <label className="sort">
            Sort
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              {SORTS.map(([v, l]) => (
                <option key={v} value={v}>{l}</option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {!loading && cards.length === 0 ? <p className="muted">No products match.</p> : null}

      <div className="pgrid">
        {cards.map((p) => {
          const hasDisc = (p.discount_pct ?? 0) > 0 && p.compare_at_price;
          return (
            <Link key={p.name} href={`/product/${encodeURIComponent(p.product_id)}`} className="pcard">
              <div className="pcard__media">
                {p.image_url ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={p.image_url} alt={p.name} loading="lazy" />
                ) : null}
                {hasDisc ? <span className="disc">-{p.discount_pct}%</span> : null}
                <button
                  className="pcard__add"
                  title="Add to cart"
                  onClick={(e) => quickAdd(e, p)}
                >
                  <Icon name={added === p.product_id ? "check" : "cart"} size={16} />
                </button>
              </div>
              <div className="pcard__body">
                <div className="pcard__name">{p.name}</div>
                <div className="pcard__price">
                  <span className="now">${money(p.price)}</span>
                  {hasDisc ? <span className="was">${money(p.compare_at_price!)}</span> : null}
                </div>
                <div className="pcard__meta">
                  <Stars value={p.rating_avg ?? 0} />
                  <span>{(p.rating_avg ?? 0).toFixed(1)}</span>
                  <span className="dot">·</span>
                  <span>{compact(p.sold_count ?? 0)} sold</span>
                </div>
                <div className="pcard__foot">
                  {p.free_shipping ? (
                    <span className="chip chip--ship">
                      <Icon name="truck" size={12} /> Free shipping
                    </span>
                  ) : null}
                  {p.shops > 1 ? <span className="chip">{p.shops} shops</span> : null}
                </div>
              </div>
            </Link>
          );
        })}
      </div>
    </main>
  );
}
