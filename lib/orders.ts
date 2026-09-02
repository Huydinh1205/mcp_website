"use client";

import { authedFetch } from "@/lib/auth";

export interface Order {
  order_id: string;
  negotiation_id: string;
  product_id: string | null;
  name: string;
  image_url: string | null;
  quantity: number;
  unit_price: number;
  total: number;
  negotiated: boolean;
  order_status: string;      // PENDING_PAYMENT | PAID | ...
  delivery_status: string;   // PENDING | IN_TRANSIT | OUT_FOR_DELIVERY | DELIVERED | FAILED
  tracking_number: string | null;
  estimated_date: string | null;
  ordered_at: string | null;
  reviewed: boolean;
}

export async function buyNow(productId: string, quantity: number) {
  const res = await authedFetch("/api/orders/buy-now", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ product_id: productId, quantity }),
  });
  const j = await res.json().catch(() => ({}));
  if (!res.ok || j.error) throw new Error(j.error ?? "buy failed");
  return j as { order_id: string; negotiation_id: string; total: number };
}

export async function myOrders(): Promise<Order[]> {
  const res = await authedFetch("/api/orders");
  const j = await res.json().catch(() => []);
  return Array.isArray(j) ? j : [];
}
