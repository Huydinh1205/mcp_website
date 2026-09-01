"use client";

// Minimal seller dashboard. US1 runs the seller as a server-side responder, so
// this view is read-only: pick a seller, watch incoming negotiations. US2 adds
// the seller WebMCP tools + a live seller agent + a ConfirmModal here.

import { useEffect, useState } from "react";
import { DealView } from "@/app/components/DealView";
import { useNegotiationFeed } from "@/lib/useNegotiationFeed";
import { API_BASE } from "@/lib/api";

interface SellerOption {
  id: string;
  name: string;
}

export default function SellerDashboard() {
  const [sellers, setSellers] = useState<SellerOption[]>([]);
  const [sellerId, setSellerId] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/sellers`)
      .then((r) => r.json())
      .then((data: SellerOption[]) => {
        setSellers(data);
        if (data[0]) setSellerId(data[0].id);
      })
      .catch(() => setSellers([]));
  }, []);

  // Feed is buyer-scoped today; passing null returns all negotiations.
  const negotiations = useNegotiationFeed(sellerId ? "__all__" : null);

  return (
    <main className="wrap">
      <h1>Seller dashboard</h1>
      <section className="panel">
        <label>
          Acting as{" "}
          <select
            value={sellerId ?? ""}
            onChange={(e) => setSellerId(e.target.value)}
          >
            {sellers.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <p className="muted">
          US1: the seller side runs server-side (SELLER_MODE=server). This view is
          read-only until US2.
        </p>
      </section>

      <section className="panel">
        <h2>Incoming negotiations</h2>
        {negotiations.length === 0 ? (
          <p className="muted">Nothing yet.</p>
        ) : (
          negotiations.map((n) => (
            <DealView key={n.negotiationId} negotiation={n} />
          ))
        )}
      </section>
    </main>
  );
}
