"use client";

// A chat-style transcript of a negotiation, live-updating from the polled feed.
// Buyer messages on one side, seller on the other. Shown on both /agent and
// /dashboard so each side can follow what's been exchanged.

import { useEffect, useRef } from "react";
import type { LiveNegotiation } from "@/lib/useNegotiationFeed";

export function NegotiationChat({
  negotiation,
  youAre,
}: {
  negotiation: LiveNegotiation;
  youAre: "buyer" | "seller";
}) {
  const n = negotiation;
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [n.history.length]);

  const nextTurn = n.lastActor === "buyer" ? "seller" : "buyer";
  const done = ["confirmed", "rejected"].includes(n.status);
  const accepted = ["buyer_accepted", "seller_accepted"].includes(n.status);

  return (
    <div className="nchat">
      <div className="nchat__head">
        <span className={`nchat__live ${done ? "off" : ""}`} />
        <strong>{n.name}</strong>
        <span className="muted small">
          round {n.currentRound} · current ${n.currentPrice.toFixed(2)}
        </span>
      </div>

      <div className="nchat__body">
        {n.history.length === 0 ? (
          <p className="muted small">Waiting for the opening offer…</p>
        ) : (
          n.history.map((r) => {
            const mine = r.author === youAre;
            return (
              <div key={r.roundNumber} className={`nbub nbub--${mine ? "me" : "them"} nbub--${r.author}`}>
                <div className="nbub__who">{r.author === "buyer" ? "Buyer agent" : "Seller agent"}</div>
                <div className="nbub__msg">{r.message}</div>
                <div className="nbub__price">${r.proposedPrice.toFixed(2)}</div>
              </div>
            );
          })
        )}

        {n.status === "countered" ? (
          <div className="nchat__typing">
            {nextTurn === youAre ? "your agent’s turn" : `${nextTurn} agent’s turn…`}
          </div>
        ) : accepted ? (
          <div className="nchat__typing nchat__typing--ok">deal agreed — confirm below</div>
        ) : n.status === "confirmed" ? (
          <div className="nchat__typing nchat__typing--ok">order placed ✓</div>
        ) : n.status === "rejected" ? (
          <div className="nchat__typing nchat__typing--no">no deal</div>
        ) : null}

        <div ref={endRef} />
      </div>
    </div>
  );
}
