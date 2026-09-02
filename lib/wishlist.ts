"use client";

import { useEffect, useState } from "react";
import type { CardProduct } from "@/app/components/ProductCard";

const KEY = "mcp_wishlist";
const EVT = "wishlist-change";

export type WishItem = CardProduct;

function read(): WishItem[] {
  try {
    const v = JSON.parse(localStorage.getItem(KEY) || "[]");
    return Array.isArray(v) ? v : [];
  } catch {
    return [];
  }
}
function write(items: WishItem[]) {
  try {
    localStorage.setItem(KEY, JSON.stringify(items));
  } catch {
    /* ignore */
  }
  window.dispatchEvent(new Event(EVT));
}

export function isWished(productId: string) {
  return read().some((i) => i.product_id === productId);
}

/** Toggle; pass the product so the wishlist page can render it. Returns the new state. */
export function toggleWishlist(p: WishItem): boolean {
  const items = read();
  const has = items.some((i) => i.product_id === p.product_id);
  write(has ? items.filter((i) => i.product_id !== p.product_id) : [...items, p]);
  return !has;
}

export function removeWished(productId: string) {
  write(read().filter((i) => i.product_id !== productId));
}

export function useWishlist() {
  const [items, setItems] = useState<WishItem[]>([]);
  useEffect(() => {
    const sync = () => setItems(read());
    sync();
    window.addEventListener(EVT, sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener(EVT, sync);
      window.removeEventListener("storage", sync);
    };
  }, []);
  return {
    items,
    count: items.length,
    has: (id: string) => items.some((i) => i.product_id === id),
  };
}
