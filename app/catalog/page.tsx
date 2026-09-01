"use client";

// Human catalog (US4): browse / search / filter. Click a product to see every
// seller carrying it, then send your agent to negotiate.

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { API_BASE } from "@/lib/api";

interface Row {
  product_id: string;
  name: string;
  price: number;
  image_url?: string | null;
  seller_name: string;
  seller_rating: number;
}

export default function CatalogPage() {
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [minRating, setMinRating] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [rows, setRows] = useState<Row[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetch(`${API_BASE}/api/categories`).then((r) => r.json()).then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (category) params.set("category", category);
    if (maxPrice) params.set("maxPrice", maxPrice);
    if (minRating) params.set("minRating", minRating);
    setLoading(true);
    const t = setTimeout(() => {
      fetch(`${API_BASE}/api/products?${params}`)
        .then((r) => r.json())
        .then((d) => setRows(Array.isArray(d) ? d : []))
        .catch(() => setRows([]))
        .finally(() => setLoading(false));
    }, 200);
    return () => clearTimeout(t);
  }, [q, category, maxPrice, minRating]);

  // group by name so the grid shows one card per product with a "from N sellers"
  const grouped = useMemo(() => {
    const m = new Map<string, { name: string; min: number; count: number; anyId: string; img?: string | null }>();
    for (const r of rows) {
      const g = m.get(r.name);
      if (!g) m.set(r.name, { name: r.name, min: r.price, count: 1, anyId: r.product_id, img: r.image_url });
      else {
        g.count += 1;
        g.min = Math.min(g.min, r.price);
      }
    }
    return [...m.values()].sort((a, b) => a.name.localeCompare(b.name));
  }, [rows]);

  return (
    <main className="wrap">
      <h1>Catalog</h1>

      <section className="panel filters">
        <input placeholder="Search products…" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>
        <input
          type="number"
          placeholder="Max price"
          value={maxPrice}
          onChange={(e) => setMaxPrice(e.target.value)}
        />
        <input
          type="number"
          step="0.1"
          placeholder="Min rating"
          value={minRating}
          onChange={(e) => setMinRating(e.target.value)}
        />
      </section>

      <section className="panel">
        {loading ? <p className="muted">Loading…</p> : null}
        {!loading && grouped.length === 0 ? <p className="muted">No products.</p> : null}
        <div className="grid">
          {grouped.map((g) => (
            <Link key={g.name} href={`/product/${encodeURIComponent(g.anyId)}`} className="card">
              {g.img ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img className="card__img" src={g.img} alt={g.name} />
              ) : null}
              <div className="card__name">{g.name}</div>
              <div className="card__price">from {g.min.toFixed(2)}</div>
              <div className="card__sub">
                {g.count} seller{g.count > 1 ? "s" : ""}
              </div>
            </Link>
          ))}
        </div>
      </section>
    </main>
  );
}
