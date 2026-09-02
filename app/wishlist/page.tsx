"use client";

import Link from "next/link";
import { useWishlist } from "@/lib/wishlist";
import { ProductCard } from "@/app/components/ProductCard";
import { Icon } from "@/app/components/Icon";

export default function WishlistPage() {
  const { items, count } = useWishlist();

  return (
    <main className="wrap wide">
      <h1><Icon name="heart" size={20} /> Wishlist {count > 0 ? `(${count})` : ""}</h1>
      {items.length === 0 ? (
        <p className="muted">
          Nothing saved yet. Tap the ♥ on a product to save it here.{" "}
          <Link href="/catalog">Browse the catalog</Link>.
        </p>
      ) : (
        <div className="pgrid">
          {items.map((p) => (
            <ProductCard key={p.product_id} p={p} />
          ))}
        </div>
      )}
    </main>
  );
}
