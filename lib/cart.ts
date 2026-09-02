"use client";

import { useEffect, useState } from "react";
import { authedFetch } from "@/lib/auth";

const KEY = "mcp_cart";
const EVT = "cart-change";

export interface CartItem {
  product_id: string;
  name: string;
  price: number;
  image_url?: string | null;
  seller_name?: string;
  quantity: number;
}

function read(): CartItem[] {
  try {
    const raw = localStorage.getItem(KEY);
    const v = raw ? JSON.parse(raw) : [];
    return Array.isArray(v) ? v : [];
  } catch {
    return [];
  }
}

function write(items: CartItem[]) {
  try {
    localStorage.setItem(KEY, JSON.stringify(items));
  } catch {
    /* private mode / quota — cart just won't persist */
  }
  window.dispatchEvent(new Event(EVT));
}

export function getCart(): CartItem[] {
  return read();
}

export function cartCount(): number {
  return read().reduce((n, i) => n + i.quantity, 0);
}

export function addToCart(item: Omit<CartItem, "quantity">, qty = 1) {
  const items = read();
  const found = items.find((i) => i.product_id === item.product_id);
  if (found) found.quantity += qty;
  else items.push({ ...item, quantity: qty });
  write(items);
}

export function setQuantity(productId: string, qty: number) {
  let items = read();
  if (qty <= 0) items = items.filter((i) => i.product_id !== productId);
  else items = items.map((i) => (i.product_id === productId ? { ...i, quantity: qty } : i));
  write(items);
}

export function removeFromCart(productId: string) {
  write(read().filter((i) => i.product_id !== productId));
}

export function clearCart() {
  write([]);
}

/** { items, count, subtotal } — re-renders on any cart change (this tab or another). */
export function useCart() {
  const [items, setItems] = useState<CartItem[]>([]);
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
    count: items.reduce((n, i) => n + i.quantity, 0),
    subtotal: items.reduce((s, i) => s + i.price * i.quantity, 0),
  };
}

export interface CheckoutLine {
  product_id: string;
  order_id?: string;
  total?: number;
  error?: string;
}

export async function checkout(items: CartItem[]): Promise<CheckoutLine[]> {
  const res = await authedFetch("/api/orders/checkout", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      items: items.map((i) => ({ product_id: i.product_id, quantity: i.quantity })),
    }),
  });
  const j = await res.json().catch(() => ({}));
  if (!res.ok || j.error) throw new Error(j.error ?? "checkout failed");
  return (j.lines ?? []) as CheckoutLine[];
}
