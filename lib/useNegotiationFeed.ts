"use client";

// Polls GET /api/negotiations and merges the deltas into a live map (D-A3).

import { useEffect, useRef, useState } from "react";
import { authedFetch } from "@/lib/auth";

export interface FeedRound {
  roundNumber: number;
  author: string;
  proposedPrice: number;
  message: string;
}

export interface LiveNegotiation {
  negotiationId: string;
  productId: string;
  name: string;
  quantity: number;
  freebiesCost: number;
  freeShipping: boolean;
  status: string;
  lastActor: string | null;
  currentRound: number;
  currentPrice: number;
  updatedAt: number;
  history: FeedRound[];
}

export type FeedScope = "buyer" | "seller" | null;

function mergeHistory(existing: FeedRound[], tail: FeedRound[]): FeedRound[] {
  const byNumber = new Map(existing.map((r) => [r.roundNumber, r]));
  for (const r of tail) byNumber.set(r.roundNumber, r);
  return [...byNumber.values()].sort((a, b) => a.roundNumber - b.roundNumber);
}

export function useNegotiationFeed(
  scope: FeedScope,
  intervalMs = 1500,
): LiveNegotiation[] {
  const [map, setMap] = useState<Record<string, LiveNegotiation>>({});
  const cursor = useRef("");
  const key = scope ?? "";

  useEffect(() => {
    if (!scope) return;
    setMap({});
    cursor.current = "";
    let active = true;

    const tick = async () => {
      try {
        const res = await authedFetch(
          `/api/negotiations?since=${encodeURIComponent(cursor.current)}`,
        );
        const data = await res.json();
        cursor.current = data.cursor ?? cursor.current;
        if (!active) return;
        setMap((prev) => {
          const next = { ...prev };
          for (const n of data.negotiations ?? []) {
            const existing = next[n.negotiationId];
            next[n.negotiationId] = {
              negotiationId: n.negotiationId,
              productId: n.productId ?? "",
              name: n.name ?? n.negotiationId.slice(0, 8),
              quantity: n.quantity ?? 1,
              freebiesCost: n.freebiesCost ?? 0,
              freeShipping: !!n.freeShipping,
              status: n.status,
              lastActor: n.lastActor,
              currentRound: n.currentRound,
              currentPrice: n.currentPrice,
              updatedAt: n.updatedAt,
              history: mergeHistory(existing?.history ?? [], n.historyTail ?? []),
            };
          }
          return next;
        });
      } catch {
        /* transient; next tick retries */
      }
    };

    tick();
    const id = setInterval(tick, intervalMs);
    return () => {
      active = false;
      clearInterval(id);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, intervalMs]);

  return Object.values(map).sort((a, b) => b.updatedAt - a.updatedAt);
}

/** US4: for each product, the negotiation with the lowest current price. */
export function bestPerProduct(negotiations: LiveNegotiation[]): LiveNegotiation[] {
  const best = new Map<string, LiveNegotiation>();
  for (const n of negotiations) {
    const key = n.name || n.productId;
    const cur = best.get(key);
    if (!cur || n.currentPrice < cur.currentPrice) best.set(key, n);
  }
  return [...best.values()];
}
