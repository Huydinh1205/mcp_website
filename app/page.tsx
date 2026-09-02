"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { API_BASE } from "@/lib/api";
import { Icon } from "@/app/components/Icon";
import { ProductCard, type CardProduct } from "@/app/components/ProductCard";
import { ProductCardSkeleton } from "@/app/components/Skeleton";

function dedupeByName(rows: CardProduct[]) {
  const seen = new Set<string>();
  const out: CardProduct[] = [];
  for (const r of rows) {
    if (seen.has(r.name)) continue;
    seen.add(r.name);
    out.push(r);
  }
  return out;
}

function Rail({ title, href, rows }: { title: string; href: string; rows: CardProduct[] | null }) {
  return (
    <section className="rail">
      <div className="rail__head">
        <h2>{title}</h2>
        <Link href={href} className="rail__more">
          See all <Icon name="chevron" size={14} />
        </Link>
      </div>
      <div className="rail__track">
        {rows === null
          ? Array.from({ length: 6 }, (_, i) => (
              <div key={i} className="rail__item">
                <ProductCardSkeleton />
              </div>
            ))
          : rows.slice(0, 12).map((p) => (
              <div key={p.product_id} className="rail__item">
                <ProductCard p={p} />
              </div>
            ))}
      </div>
    </section>
  );
}

export default function Home() {
  const [best, setBest] = useState<CardProduct[] | null>(null);
  const [top, setTop] = useState<CardProduct[] | null>(null);
  const [cats, setCats] = useState<string[]>([]);

  useEffect(() => {
    fetch(`${API_BASE}/api/products?sort=sold`)
      .then((r) => r.json())
      .then((d) => setBest(dedupeByName(Array.isArray(d) ? d : [])))
      .catch(() => setBest([]));
    fetch(`${API_BASE}/api/products?sort=rating`)
      .then((r) => r.json())
      .then((d) => setTop(dedupeByName(Array.isArray(d) ? d : [])))
      .catch(() => setTop([]));
    fetch(`${API_BASE}/api/categories`).then((r) => r.json()).then(setCats).catch(() => {});
  }, []);

  return (
    <main className="wrap wide home">
      <section className="homehero">
        <div className="homehero__text">
          <h1>Shop smarter with an AI agent that haggles for you</h1>
          <p>
            Buy now at list price, or send an agent to negotiate the real deal — price, quantity,
            free add-ons, free shipping and coupons. You confirm every order.
          </p>
          <div className="homehero__cta">
            <Link href="/catalog" className="btn">Browse catalog</Link>
            <Link href="/agent" className="btn ghostbtn">
              <Icon name="handshake" size={15} /> Send an agent
            </Link>
          </div>
        </div>
        <div className="homehero__cards">
          <div className="wcard"><Icon name="bolt" size={20} /><b>Buy now</b><span>list price, instant order</span></div>
          <div className="wcard"><Icon name="cart" size={20} /><b>Add to cart</b><span>check out several at once</span></div>
          <div className="wcard"><Icon name="handshake" size={20} /><b>Negotiate</b><span>agent haggles, you confirm</span></div>
        </div>
      </section>

      {cats.length > 0 ? (
        <section className="cattiles">
          {cats.map((c) => (
            <Link key={c} href={`/catalog?category=${encodeURIComponent(c)}`} className="cattile">
              <Icon name="tag" size={16} />
              {c}
            </Link>
          ))}
        </section>
      ) : null}

      <Rail title="Best selling" href="/catalog?sort=sold" rows={best} />
      <Rail title="Top rated" href="/catalog?sort=rating" rows={top} />
    </main>
  );
}
