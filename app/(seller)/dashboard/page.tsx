"use client";

// Seller dashboard (US2/US3). The seller agent (same harness, seller persona +
// seller WebMCP tools) handles incoming offers; the seller HUMAN confirms here.
// Needs SELLER_MODE=browser on the backend.

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { runAgentLoop, type LoopEvent } from "@/lib/agent-loop";
import { browserLlmTurn } from "@/lib/browserLlm";
import { buildSellerRegistry } from "@/lib/webmcp/seller-tools";
import { sellerAgentPrompt } from "@/lib/personas";
import { useNegotiationFeed } from "@/lib/useNegotiationFeed";
import { useAuth } from "@/lib/auth";
import { DealView } from "@/app/components/DealView";
import { ConfirmModal } from "@/app/components/ConfirmModal";
import { TakeoverControls } from "@/app/components/TakeoverControls";

export default function SellerDashboard() {
  const user = useAuth();
  const router = useRouter();
  const [autoRun, setAutoRun] = useState(false);
  const [running, setRunning] = useState(false);
  const [events, setEvents] = useState<LoopEvent[]>([]);
  const [tokens, setTokens] = useState<Record<string, string>>({});
  const [confirmed, setConfirmed] = useState<Record<string, string>>({});
  const [paused, setPaused] = useState<Record<string, boolean>>({});
  const runningRef = useRef(false);

  useEffect(() => {
    if (user === null) return;
    if (!user || user.role !== "seller") router.replace("/login?next=/dashboard");
  }, [user, router]);

  const negotiations = useNegotiationFeed(user?.role === "seller" ? "seller" : null);

  const runOnce = useCallback(async () => {
    if (runningRef.current || user?.role !== "seller") return;
    runningRef.current = true;
    setRunning(true);
    try {
      await runAgentLoop({
        systemPrompt: sellerAgentPrompt(),
        goal: "Handle every offer waiting for you right now, then stop.",
        registry: buildSellerRegistry(),
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
  }, [user]);

  useEffect(() => {
    if (!autoRun) return;
    const pending = negotiations.some(
      (n) => n.status === "countered" && n.lastActor === "buyer" && !paused[n.negotiationId],
    );
    if (pending && !runningRef.current) void runOnce();
  }, [autoRun, negotiations, paused, runOnce]);

  if (!user || user.role !== "seller") {
    return <main className="wrap"><p className="muted">Redirecting to login…</p></main>;
  }

  return (
    <main className="wrap">
      <h1>Seller — {user.name}</h1>

      <section className="panel">
        <button onClick={runOnce} disabled={running}>
          {running ? "Agent running…" : "Run agent once"}
        </button>{" "}
        <label>
          <input type="checkbox" checked={autoRun} onChange={(e) => setAutoRun(e.target.checked)} />{" "}
          Auto-respond to new offers
        </label>
        <p className="muted">Needs <code>SELLER_MODE=browser</code> on the backend.</p>
      </section>

      <section className="panel">
        <h2>Negotiations</h2>
        {negotiations.length === 0 ? (
          <p className="muted">Nothing yet.</p>
        ) : (
          negotiations.map((n) => (
            <div key={n.negotiationId}>
              <DealView negotiation={n} />
              <TakeoverControls
                side="seller"
                negotiation={n}
                onToken={(id, t) => setTokens((p) => ({ ...p, [id]: t }))}
                onActive={(id, active) => setPaused((p) => ({ ...p, [id]: active }))}
              />
              {n.status === "seller_accepted" &&
              tokens[n.negotiationId] &&
              !confirmed[n.negotiationId] ? (
                <ConfirmModal
                  title={n.name}
                  price={n.currentPrice}
                  confirmToken={tokens[n.negotiationId]}
                  onDone={(res) => setConfirmed((p) => ({ ...p, [n.negotiationId]: res.status }))}
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
