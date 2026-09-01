"use client";

// Polls GET /api/negotiations and merges the deltas into a live map (D-A3).

import { useEffect, useRef, useState } from "react";
import { API_BASE } from "@/lib/api";

export interface FeedRound {
  roundNumber: number;
  author: string;
  proposedPrice: number;
  message: string;
}

export interface LiveNegotiation {
  negotiationId: string;
  status: string;
  lastActor: string | null;
  currentRound: number;
  currentPrice: number;
  updatedAt: number;
  history: FeedRound[];
}

function mergeHistory(existing: FeedRound[], tail: FeedRound[]): FeedRound[] {
  const byNumber = new Map(existing.map((r) => [r.roundNumber, r]));
  for (const r of tail) byNumber.set(r.roundNumber, r);
  return [...byNumber.values()].sort((a, b) => a.roundNumber - b.roundNumber);
}

export function useNegotiationFeed(
  buyerId: string | null,
  intervalMs = 1500,
): LiveNegotiation[] {
  const [map, setMap] = useState<Record<string, LiveNegotiation>>({});
  const cursor = useRef("");

  useEffect(() => {
    if (!buyerId) return;
    setMap({});
    cursor.current = "";
    let active = true;

    const tick = async () => {
      try {
        // "__all__" = every negotiation (seller dashboard until US2 scoping).
        const scope =
          buyerId === "__all__"
            ? ""
            : `&buyer=${encodeURIComponent(buyerId)}`;
        const res = await fetch(
          `${API_BASE}/api/negotiations?since=${encodeURIComponent(cursor.current)}${scope}`,
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
  }, [buyerId, intervalMs]);

  return Object.values(map).sort((a, b) => b.updatedAt - a.updatedAt);
}
