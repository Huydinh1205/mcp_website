"use client";

import Link from "next/link";
import { Stars } from "@/app/components/Stars";
import { Icon } from "@/app/components/Icon";
import { compact, money } from "@/lib/format";
import { addToCart } from "@/lib/cart";
import { toast } from "@/lib/toast";

export interface CardProduct {
  product_id: string;
  name: string;
  price: number;
  compare_at_price?: number | null;
  discount_pct?: number;
  rating_avg?: number;
  sold_count?: number;
  free_shipping?: boolean;
  image_url?: string | null;
  seller_name?: string;
  shops?: number;
}

export function ProductCard({ p }: { p: CardProduct }) {
  const hasDisc = (p.discount_pct ?? 0) > 0 && p.compare_at_price;

  const quickAdd = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    addToCart({
      product_id: p.product_id,
      name: p.name,
      price: p.price,
      image_url: p.image_url ?? null,
      seller_name: p.seller_name,
    });
    toast("Added to cart", "success");
  };

  return (
    <Link href={`/product/${encodeURIComponent(p.product_id)}`} className="pcard">
      <div className="pcard__media">
        {p.image_url ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={p.image_url} alt={p.name} loading="lazy" />
        ) : null}
        {hasDisc ? <span className="disc">-{p.discount_pct}%</span> : null}
        <button className="pcard__add" title="Add to cart" onClick={quickAdd}>
          <Icon name="cart" size={16} />
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
          {(p.shops ?? 0) > 1 ? <span className="chip">{p.shops} shops</span> : null}
        </div>
      </div>
    </Link>
  );
}
