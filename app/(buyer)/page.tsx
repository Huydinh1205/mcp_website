"use client";

import { useEffect, useMemo, useState } from "react";
import { runAgentLoop, type LoopEvent } from "@/lib/agent-loop";
import { browserLlmTurn } from "@/lib/browserLlm";
import { buildBuyerRegistry } from "@/lib/webmcp/buyer-tools";
import { buildSystemPrompt } from "@/lib/personas";
import { useNegotiationFeed, bestPerProduct } from "@/lib/useNegotiationFeed";
import { DealView } from "@/app/components/DealView";
import { ConfirmModal } from "@/app/components/ConfirmModal";
import { TakeoverControls } from "@/app/components/TakeoverControls";
import { API_BASE } from "@/lib/api";

interface BuyerOption {
  id: string;
  name: string;
  interest: string | null;
  config: {
    maxBudget: number;
    targetPrice: number;
    minSellerRating: number;
    style: string;
  } | null;
}

const ACCEPTED = new Set(["buyer_accepted", "seller_accepted"]);

export default function BuyerPage() {
  const [buyers, setBuyers] = useState<BuyerOption[]>([]);
  const [buyerId, setBuyerId] = useState<string | null>(null);
  const [goal, setGoal] = useState(
    "Find me a 65% Mechanical Keyboard. Compare every seller that has one and get the best price.",
  );
  const [running, setRunning] = useState(false);
  const [events, setEvents] = useState<LoopEvent[]>([]);
  const [tokens, setTokens] = useState<Record<string, string>>({});
  const [placed, setPlaced] = useState<Record<string, string>>({});

  useEffect(() => {
    fetch(`${API_BASE}/api/buyers`)
      .then((r) => r.json())
      .then((data: BuyerOption[]) => {
        setBuyers(data);
        if (data[0]) setBuyerId(data[0].id);
      })
      .catch(() => setBuyers([]));
  }, []);

  const buyer = useMemo(
    () => buyers.find((b) => b.id === buyerId) ?? null,
    [buyers, buyerId],
  );

  const negotiations = useNegotiationFeed(buyerId ? { buyer: buyerId } : null);
  const best = useMemo(() => bestPerProduct(negotiations), [negotiations]);
  const multiSeller = negotiations.length > best.length;

  const captureToken = (e: LoopEvent) => {
    if (
      e.type === "tool_result" &&
      (e.name === "accept_offer" || e.name === "respond_to_offer") &&
      e.result &&
      typeof e.result === "object" &&
      "confirm_token" in e.result &&
      "negotiation_id" in e.result
    ) {
      const r = e.result as { negotiation_id: string; confirm_token: string };
      if (r.confirm_token) {
        setTokens((prev) => ({ ...prev, [r.negotiation_id]: r.confirm_token }));
      }
    }
  };

  const start = async () => {
    if (!buyer?.config || running) return;
    setRunning(true);
    setEvents([]);
    try {
      await runAgentLoop({
        systemPrompt: buildSystemPrompt({
          role: "buyer",
          maxBudget: buyer.config.maxBudget,
          targetPrice: buyer.config.targetPrice,
          minSellerRating: buyer.config.minSellerRating,
          style: buyer.config.style,
        }),
        goal,
        registry: buildBuyerRegistry({ buyerId: buyer.id }),
        llmTurn: browserLlmTurn,
        maxSteps: 16,
        onEvent: (e) => {
          setEvents((prev) => [...prev, e]);
          captureToken(e);
        },
      });
    } finally {
      setRunning(false);
    }
  };

  return (
    <main className="wrap">
      <h1>Buyer</h1>

      <section className="panel">
        <label>
          Acting as{" "}
          <select value={buyerId ?? ""} onChange={(e) => setBuyerId(e.target.value)}>
            {buyers.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
                {b.config
                  ? ` (budget ${b.config.maxBudget}, target ${b.config.targetPrice}, ${b.config.style})`
                  : ""}
              </option>
            ))}
          </select>
        </label>
        <textarea value={goal} onChange={(e) => setGoal(e.target.value)} rows={2} />
        <button onClick={start} disabled={running || !buyer?.config}>
          {running ? "Agent running…" : "Start agent"}
        </button>
      </section>

      {multiSeller ? (
        <section className="panel">
          <h2>Best offer per product</h2>
          {best.map((n) => (
            <div key={n.negotiationId} className="best">
              <strong>{n.name}</strong> — {n.currentPrice.toFixed(2)}{" "}
              <span className="muted">({n.status})</span>
            </div>
          ))}
        </section>
      ) : null}

      <section className="panel">
        <h2>Negotiations</h2>
        {negotiations.length === 0 ? (
          <p className="muted">No negotiations yet.</p>
        ) : (
          negotiations.map((n) => (
            <div key={n.negotiationId}>
              <DealView negotiation={n} />
              {buyerId ? (
                <TakeoverControls
                  side="buyer"
                  negotiation={n}
                  sessionId={buyerId}
                  onToken={(id, t) => setTokens((p) => ({ ...p, [id]: t }))}
                />
              ) : null}
              {ACCEPTED.has(n.status) &&
              tokens[n.negotiationId] &&
              !placed[n.negotiationId] ? (
                <ConfirmModal
                  title={n.name}
                  price={n.currentPrice}
                  confirmToken={tokens[n.negotiationId]}
                  onDone={(res) =>
                    setPlaced((prev) => ({ ...prev, [n.negotiationId]: res.status }))
                  }
                />
              ) : null}
              {placed[n.negotiationId] ? (
                <p className="ok">Order {placed[n.negotiationId]} ✓</p>
              ) : null}
            </div>
          ))
        )}
      </section>

      <section className="panel">
        <h2>Agent log</h2>
        <ol className="log">
          {events.map((e, i) => (
            <li key={i}>
              <code>{e.type}</code> {"name" in e ? <b>{e.name}</b> : null}{" "}
              {"reason" in e ? <i>{e.reason}</i> : null}
              {"message" in e ? <span className="err"> {e.message}</span> : null}
              {"args" in e ? (
                <span className="muted"> {JSON.stringify(e.args)}</span>
              ) : null}
              {"result" in e ? (
                <span className="muted">
                  {" "}
                  {JSON.stringify(e.result).slice(0, 200)}
                </span>
              ) : null}
            </li>
          ))}
        </ol>
      </section>
    </main>
  );
}
