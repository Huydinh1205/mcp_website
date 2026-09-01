"use client";

import { useEffect, useMemo, useState } from "react";
import { runAgentLoop, type LoopEvent } from "@/lib/agent-loop";
import { browserLlmTurn } from "@/lib/browserLlm";
import { buildBuyerRegistry } from "@/lib/webmcp/buyer-tools";
import { buildSystemPrompt } from "@/lib/personas";
import { useNegotiationFeed } from "@/lib/useNegotiationFeed";
import { DealView } from "@/app/components/DealView";
import { ConfirmModal } from "@/app/components/ConfirmModal";
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
  const [goal, setGoal] = useState("Find me a 65% mechanical keyboard under 60.");
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

  const negotiations = useNegotiationFeed(buyerId);

  const start = async () => {
    if (!buyer?.config || running) return;
    setRunning(true);
    setEvents([]);
    const registry = buildBuyerRegistry({ buyerId: buyer.id });
    const systemPrompt = buildSystemPrompt({
      role: "buyer",
      maxBudget: buyer.config.maxBudget,
      targetPrice: buyer.config.targetPrice,
      minSellerRating: buyer.config.minSellerRating,
      style: buyer.config.style,
    });

    try {
      await runAgentLoop({
        systemPrompt,
        goal,
        registry,
        llmTurn: browserLlmTurn,
        maxSteps: 12,
        onEvent: (e) => {
          setEvents((prev) => [...prev, e]);
          if (
            e.type === "tool_result" &&
            e.name === "accept_offer" &&
            e.result &&
            typeof e.result === "object" &&
            "confirm_token" in e.result &&
            "negotiation_id" in e.result
          ) {
            const r = e.result as { negotiation_id: string; confirm_token: string };
            setTokens((prev) => ({ ...prev, [r.negotiation_id]: r.confirm_token }));
          }
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
          <select
            value={buyerId ?? ""}
            onChange={(e) => setBuyerId(e.target.value)}
          >
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
        <textarea
          value={goal}
          onChange={(e) => setGoal(e.target.value)}
          rows={2}
        />
        <button onClick={start} disabled={running || !buyer?.config}>
          {running ? "Agent running…" : "Start agent"}
        </button>
      </section>

      <section className="panel">
        <h2>Negotiations</h2>
        {negotiations.length === 0 ? (
          <p className="muted">No negotiations yet.</p>
        ) : (
          negotiations.map((n) => (
            <div key={n.negotiationId}>
              <DealView negotiation={n} />
              {ACCEPTED.has(n.status) &&
              tokens[n.negotiationId] &&
              !placed[n.negotiationId] ? (
                <ConfirmModal
                  title={n.negotiationId.slice(0, 8)}
                  price={n.currentPrice}
                  confirmToken={tokens[n.negotiationId]}
                  onDone={(res) =>
                    setPlaced((prev) => ({
                      ...prev,
                      [n.negotiationId]: res.status,
                    }))
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
              <code>{e.type}</code>{" "}
              {"name" in e ? <b>{e.name}</b> : null}{" "}
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
