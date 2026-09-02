"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { API_BASE } from "@/lib/api";
import { Stars } from "@/app/components/Stars";
import { Icon } from "@/app/components/Icon";
import { compact } from "@/lib/format";
import { ProductCard, type CardProduct } from "@/app/components/ProductCard";
import { ProductGridSkeleton } from "@/app/components/Skeleton";

interface Shop {
  seller_id: string;
  name: string;
  rating: number;
  total_ratings: number;
  product_count: number;
  products: CardProduct[];
}

export default function ShopPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [s, setS] = useState<Shop | null>(null);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    fetch(`${API_BASE}/api/shops/${encodeURIComponent(id)}`)
      .then((r) => r.json())
      .then((j) => (j.error ? setMissing(true) : setS(j)))
      .catch(() => setMissing(true));
  }, [id]);

  if (missing) return <main className="wrap"><p className="muted">Shop not found. <Link href="/catalog">Catalog</Link></p></main>;

  return (
    <main className="wrap wide">
      <nav className="crumbs">
        <Link href="/">Home</Link>
        <Icon name="chevron" size={12} />
        <span className="muted">{s?.name ?? "Shop"}</span>
      </nav>

      <section className="shophead">
        <div className="shophead__avatar">
          <Icon name="tag" size={22} />
        </div>
        <div>
          <h1>{s?.name ?? "…"}</h1>
          {s ? (
            <div className="shophead__meta">
              <Stars value={s.rating} size={15} /> <strong>{s.rating.toFixed(1)}</strong>
              <span className="dot">·</span>
              <span>{compact(s.total_ratings)} ratings</span>
              <span className="dot">·</span>
              <span>{s.product_count} products</span>
            </div>
          ) : null}
        </div>
      </section>

      {!s ? (
        <ProductGridSkeleton n={8} />
      ) : s.products.length === 0 ? (
        <p className="muted">This shop has no products listed.</p>
      ) : (
        <div className="pgrid">
          {s.products.map((p) => (
            <ProductCard key={p.product_id} p={p} />
          ))}
        </div>
      )}
    </main>
  );
}
