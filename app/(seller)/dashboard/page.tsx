"use client";

// Seller dashboard (US2/US3). Pick a seller, run the seller agent (same
// tool-agnostic harness as the buyer, seller persona + seller WebMCP tools).
// When the seller agent accepts, the seller HUMAN confirms here — the order
// finalizes only once the buyer has confirmed too (D-A4).
//
// Requires SELLER_MODE=browser on the backend so the buyer path does not also
// auto-run the server-side responder.

import { useCallback, useEffect, useRef, useState } from "react";
import { runAgentLoop, type LoopEvent } from "@/lib/agent-loop";
import { browserLlmTurn } from "@/lib/browserLlm";
import { buildSellerRegistry } from "@/lib/webmcp/seller-tools";
import { sellerAgentPrompt } from "@/lib/personas";
import { useNegotiationFeed } from "@/lib/useNegotiationFeed";
import { DealView } from "@/app/components/DealView";
import { ConfirmModal } from "@/app/components/ConfirmModal";
import { TakeoverControls } from "@/app/components/TakeoverControls";
import { API_BASE } from "@/lib/api";

interface SellerOption {
  id: string;
  name: string;
  rating: number;
}

const NEEDS_SELLER = "countered";

export default function SellerDashboard() {
  const [sellers, setSellers] = useState<SellerOption[]>([]);
  const [sellerId, setSellerId] = useState<string | null>(null);
  const [autoRun, setAutoRun] = useState(false);
  const [running, setRunning] = useState(false);
  const [events, setEvents] = useState<LoopEvent[]>([]);
  const [tokens, setTokens] = useState<Record<string, string>>({});
  const [confirmed, setConfirmed] = useState<Record<string, string>>({});
  const [paused, setPaused] = useState<Record<string, boolean>>({});
  const runningRef = useRef(false);

  useEffect(() => {
    fetch(`${API_BASE}/api/sellers`)
      .then((r) => r.json())
      .then((data: SellerOption[]) => {
        setSellers(data);
        if (data[0]) setSellerId(data[0].id);
      })
      .catch(() => setSellers([]));
  }, []);

  const negotiations = useNegotiationFeed(sellerId ? { seller: sellerId } : null);

  const runOnce = useCallback(async () => {
    if (!sellerId || runningRef.current) return;
    runningRef.current = true;
    setRunning(true);
    try {
      await runAgentLoop({
        systemPrompt: sellerAgentPrompt(),
        goal: "Handle every offer waiting for you right now, then stop.",
        registry: buildSellerRegistry({ sellerId }),
        llmTurn: browserLlmTurn,
        maxSteps: 16,
        onEvent: (e) => {
          setEvents((prev) => [...prev.slice(-40), e]);
          if (
            e.type === "tool_result" &&
            e.name === "respond_to_offer" &&
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
      runningRef.current = false;
      setRunning(false);
    }
  }, [sellerId]);

  // Auto-respond: when enabled, kick the agent whenever an offer is on our side.
  useEffect(() => {
    if (!autoRun) return;
    const pending = negotiations.some(
      (n) => n.status === NEEDS_SELLER && n.lastActor === "buyer" && !paused[n.negotiationId],
    );
    if (pending && !runningRef.current) void runOnce();
  }, [autoRun, negotiations, paused, runOnce]);

  return (
    <main className="wrap">
      <h1>Seller dashboard</h1>

      <section className="panel">
        <label>
          Acting as{" "}
          <select value={sellerId ?? ""} onChange={(e) => setSellerId(e.target.value)}>
            {sellers.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} (rating {s.rating})
              </option>
            ))}
          </select>
        </label>
        <div>
          <button onClick={runOnce} disabled={running || !sellerId}>
            {running ? "Agent running…" : "Run agent once"}
          </button>{" "}
          <label>
            <input
              type="checkbox"
              checked={autoRun}
              onChange={(e) => setAutoRun(e.target.checked)}
            />{" "}
            Auto-respond to new offers
          </label>
        </div>
        <p className="muted">
          Needs <code>SELLER_MODE=browser</code> on the backend.
        </p>
      </section>

      <section className="panel">
        <h2>Negotiations</h2>
        {negotiations.length === 0 ? (
          <p className="muted">Nothing yet.</p>
        ) : (
          negotiations.map((n) => (
            <div key={n.negotiationId}>
              <DealView negotiation={n} />
              {sellerId ? (
                <TakeoverControls
                  side="seller"
                  negotiation={n}
                  sessionId={sellerId}
                  onToken={(id, t) => setTokens((p) => ({ ...p, [id]: t }))}
                  onActive={(id, active) =>
                    setPaused((p) => ({ ...p, [id]: active }))
                  }
                />
              ) : null}
              {n.status === "seller_accepted" &&
              tokens[n.negotiationId] &&
              !confirmed[n.negotiationId] ? (
                <ConfirmModal
                  title={n.name}
                  price={n.currentPrice}
                  confirmToken={tokens[n.negotiationId]}
                  onDone={(res) =>
                    setConfirmed((p) => ({ ...p, [n.negotiationId]: res.status }))
                  }
                />
              ) : null}
              {confirmed[n.negotiationId] ? (
                <p className="ok">Sale {confirmed[n.negotiationId]} ✓</p>
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
                <span className="muted"> {JSON.stringify(e.result).slice(0, 160)}</span>
              ) : null}
            </li>
          ))}
        </ol>
      </section>
    </main>
  );
}
