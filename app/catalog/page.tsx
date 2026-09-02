"use client";

// Storefront listing: search, sidebar filters, sort, paginated product grid.

import { useEffect, useMemo, useState } from "react";
import { API_BASE } from "@/lib/api";
import { Icon } from "@/app/components/Icon";
import { ProductCard, type CardProduct } from "@/app/components/ProductCard";
import { ProductGridSkeleton } from "@/app/components/Skeleton";

interface Row extends CardProduct {
  seller_name: string;
}

const SORTS = [
  ["", "Relevance"],
  ["sold", "Best selling"],
  ["rating", "Top rated"],
  ["price_asc", "Price: low to high"],
  ["price_desc", "Price: high to low"],
] as const;

const RATINGS = [4.5, 4, 3.5, 3];
const PAGE = 24;

export default function CatalogPage() {
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [minRating, setMinRating] = useState("");
  const [sort, setSort] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [rows, setRows] = useState<Row[]>([]);
  const [loading, setLoading] = useState(true);
  const [shown, setShown] = useState(PAGE);
  const [filtersOpen, setFiltersOpen] = useState(false);

  // hydrate filters from the URL (?category=, ?sort=, ?q=)
  useEffect(() => {
    const sp = new URLSearchParams(window.location.search);
    if (sp.get("category")) setCategory(sp.get("category")!);
    if (sp.get("sort")) setSort(sp.get("sort")!);
    if (sp.get("q")) setQ(sp.get("q")!);
  }, []);

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
    setShown(PAGE);
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

  const visible = cards.slice(0, shown);
  const activeFilters =
    (category ? 1 : 0) + (maxPrice ? 1 : 0) + (minRating ? 1 : 0);

  const Filters = (
    <aside className="fside">
      <div className="fside__group">
        <h3>Category</h3>
        <button className={`fopt ${category === "" ? "on" : ""}`} onClick={() => setCategory("")}>
          All categories
        </button>
        {categories.map((c) => (
          <button
            key={c}
            className={`fopt ${category === c ? "on" : ""}`}
            onClick={() => setCategory(category === c ? "" : c)}
          >
            {c}
          </button>
        ))}
      </div>
      <div className="fside__group">
        <h3>Max price</h3>
        <input
          type="number"
          placeholder="Any"
          value={maxPrice}
          onChange={(e) => setMaxPrice(e.target.value)}
        />
      </div>
      <div className="fside__group">
        <h3>Rating</h3>
        <button className={`fopt ${minRating === "" ? "on" : ""}`} onClick={() => setMinRating("")}>
          Any
        </button>
        {RATINGS.map((r) => (
          <button
            key={r}
            className={`fopt ${minRating === String(r) ? "on" : ""}`}
            onClick={() => setMinRating(minRating === String(r) ? "" : String(r))}
          >
            <Icon name="star" size={12} fill /> {r} & up
          </button>
        ))}
      </div>
      {activeFilters > 0 ? (
        <button
          className="linkbtn"
          onClick={() => {
            setCategory("");
            setMaxPrice("");
            setMinRating("");
          }}
        >
          Clear filters
        </button>
      ) : null}
    </aside>
  );

  return (
    <main className="wrap wide">
      <div className="hero">
        <div>
          <h1 className="hero__title">Catalog</h1>
          <p className="hero__sub">
            {loading ? "Loading…" : `${cards.length}+ products`} · add to cart, or send an agent to negotiate.
          </p>
        </div>
        <div className="searchbar">
          <Icon name="search" size={16} />
          <input placeholder="Search products…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
      </div>

      <div className="listing-head">
        <button
          className="filtertoggle"
          onClick={() => setFiltersOpen((v) => !v)}
        >
          <Icon name="tag" size={14} /> Filters{activeFilters ? ` (${activeFilters})` : ""}
        </button>
        <label className="sort">
          Sort
          <select value={sort} onChange={(e) => setSort(e.target.value)}>
            {SORTS.map(([v, l]) => (
              <option key={v} value={v}>{l}</option>
            ))}
          </select>
        </label>
      </div>

      <div className={`catlayout ${filtersOpen ? "catlayout--open" : ""}`}>
        {Filters}
        <div className="catmain">
          {loading ? (
            <ProductGridSkeleton n={12} />
          ) : cards.length === 0 ? (
            <p className="muted">No products match.</p>
          ) : (
            <>
              <div className="pgrid">
                {visible.map((p) => (
                  <ProductCard key={p.product_id} p={p} />
                ))}
              </div>
              {shown < cards.length ? (
                <div className="loadmore">
                  <button className="ghostbtn" onClick={() => setShown((s) => s + PAGE)}>
                    Load more ({cards.length - shown} left)
                  </button>
                </div>
              ) : null}
            </>
          )}
        </div>
      </div>
    </main>
  );
}
