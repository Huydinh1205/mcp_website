"use client";

import type { LiveNegotiation } from "@/lib/useNegotiationFeed";

const STATUS_LABEL: Record<string, string> = {
  open: "opening",
  countered: "negotiating",
  buyer_accepted: "deal pending — awaiting confirmation",
  seller_accepted: "deal pending — awaiting confirmation",
  confirmed: "order placed",
  rejected: "no deal",
};

export function DealView({
  negotiation,
  listPrice,
}: {
  negotiation: LiveNegotiation;
  listPrice?: number;
}) {
  const n = negotiation;
  const thinking = n.status === "countered";
  const saved =
    listPrice && listPrice > n.currentPrice
      ? Math.round(((listPrice - n.currentPrice) / listPrice) * 100)
      : null;

  return (
    <div className="deal">
      <div className="deal__head">
        <strong>{n.name}</strong>
        <span className={`pill pill--${n.status}`}>
          {STATUS_LABEL[n.status] ?? n.status}
          {thinking && n.lastActor === "buyer" ? " · seller agent thinking…" : ""}
          {thinking && n.lastActor === "seller" ? " · buyer agent thinking…" : ""}
        </span>
      </div>

      <ol className="ladder">
        {n.history.map((r) => (
          <li key={r.roundNumber} className={`ladder__row ladder__row--${r.author}`}>
            <span className="ladder__who">{r.author}</span>
            <span className="ladder__price">{r.proposedPrice.toFixed(2)}</span>
            <span className="ladder__msg">{r.message}</span>
          </li>
        ))}
      </ol>

      <div className="deal__terms">
        {n.quantity > 1 ? <span className="tag">qty {n.quantity}</span> : null}
        {n.freebiesCost > 0 ? (
          <span className="tag">+ free add-ons (${n.freebiesCost.toFixed(2)} value)</span>
        ) : null}
        {n.freeShipping ? <span className="tag">+ free shipping</span> : null}
      </div>

      <div className="deal__foot">
        current price <strong>{n.currentPrice.toFixed(2)}</strong> · round {n.currentRound}
        {saved != null ? (
          <span className="saved"> · agent saved you ~{saved}% off list</span>
        ) : null}
      </div>
    </div>
  );
}
