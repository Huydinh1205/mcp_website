"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { runAgentLoop, type LoopEvent } from "@/lib/agent-loop";
import { browserLlmTurn } from "@/lib/browserLlm";
import { buildBuyerRegistry } from "@/lib/webmcp/buyer-tools";
import { buildSystemPrompt } from "@/lib/personas";
import { useNegotiationFeed, bestPerProduct } from "@/lib/useNegotiationFeed";
import { useAuth } from "@/lib/auth";
import { DealView } from "@/app/components/DealView";
import { ConfirmModal } from "@/app/components/ConfirmModal";
import { TakeoverControls } from "@/app/components/TakeoverControls";
import { ReviewForm } from "@/app/components/ReviewForm";

const ACCEPTED = new Set(["buyer_accepted", "seller_accepted"]);

// Sensible defaults for a registered buyer (backend BuyerAiConfig defaults match).
const CFG = { maxBudget: 200, targetPrice: 120, minSellerRating: 0, style: "fair" };

export default function BuyerPage() {
  const user = useAuth();
  const router = useRouter();
  const [goal, setGoal] = useState(
    "Find me a 65% Mechanical Keyboard. Compare every seller and get the best total deal — use quantity, free add-ons, free shipping and coupons where they help.",
  );
  const [running, setRunning] = useState(false);
  const [events, setEvents] = useState<LoopEvent[]>([]);
  const [tokens, setTokens] = useState<Record<string, string>>({});
  const [placed, setPlaced] = useState<Record<string, string>>({});

  useEffect(() => {
    const qGoal = new URLSearchParams(window.location.search).get("goal");
    if (qGoal) setGoal(qGoal);
  }, []);

  useEffect(() => {
    if (user === null) return; // still loading
    if (!user || user.role !== "buyer") router.replace("/login?next=/");
  }, [user, router]);

  const negotiations = useNegotiationFeed(user?.role === "buyer" ? "buyer" : null);
  const best = useMemo(() => bestPerProduct(negotiations), [negotiations]);
  const multiSeller = negotiations.length > best.length;

  const onEvent = (e: LoopEvent) => {
    setEvents((prev) => [...prev, e]);
    if (
      e.type === "tool_result" &&
      (e.name === "accept_offer" || e.name === "respond_to_offer") &&
      e.result &&
      typeof e.result === "object" &&
      "confirm_token" in e.result &&
      "negotiation_id" in e.result
    ) {
      const r = e.result as { negotiation_id: string; confirm_token: string };
      if (r.confirm_token) setTokens((p) => ({ ...p, [r.negotiation_id]: r.confirm_token }));
    }
  };

  const start = async () => {
    if (running || user?.role !== "buyer") return;
    setRunning(true);
    setEvents([]);
    try {
      await runAgentLoop({
        systemPrompt: buildSystemPrompt({ role: "buyer", ...CFG }),
        goal,
        registry: buildBuyerRegistry(),
        llmTurn: browserLlmTurn,
        maxSteps: 16,
        onEvent,
      });
    } finally {
      setRunning(false);
    }
  };

  if (!user || user.role !== "buyer") {
    return <main className="wrap"><p className="muted">Redirecting to login…</p></main>;
  }

  return (
    <main className="wrap">
      <h1>Buyer — {user.name}</h1>

      <section className="panel">
        <textarea value={goal} onChange={(e) => setGoal(e.target.value)} rows={3} />
        <button onClick={start} disabled={running}>
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
          <p className="muted">No negotiations yet. Start the agent, or pick a product from the Catalog.</p>
        ) : (
          negotiations.map((n) => (
            <div key={n.negotiationId}>
              <DealView negotiation={n} />
              <TakeoverControls
                side="buyer"
                negotiation={n}
                onToken={(id, t) => setTokens((p) => ({ ...p, [id]: t }))}
              />
              {ACCEPTED.has(n.status) && tokens[n.negotiationId] && !placed[n.negotiationId] ? (
                <ConfirmModal
                  title={n.name}
                  price={n.currentPrice}
                  confirmToken={tokens[n.negotiationId]}
                  onDone={(res) => setPlaced((p) => ({ ...p, [n.negotiationId]: res.status }))}
                />
              ) : null}
              {placed[n.negotiationId] ? (
                <>
                  <p className="ok">Order {placed[n.negotiationId]} ✓</p>
                  {placed[n.negotiationId] === "confirmed" ? (
                    <ReviewForm productId={n.productId} negotiationId={n.negotiationId} />
                  ) : null}
                </>
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
              {"result" in e ? (
                <span className="muted"> {JSON.stringify(e.result).slice(0, 200)}</span>
              ) : null}
            </li>
          ))}
        </ol>
      </section>
    </main>
  );
}
