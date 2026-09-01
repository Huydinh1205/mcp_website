"use client";

// US5 — a human takes over their side of a negotiation and acts manually,
// through the same WebMCP tools the agent uses (via POST /api/mcp).

import { useState } from "react";
import { API_BASE } from "@/lib/api";
import type { LiveNegotiation } from "@/lib/useNegotiationFeed";

type Side = "buyer" | "seller";

async function mcp(tool: string, args: unknown, session: unknown) {
  const res = await fetch(`${API_BASE}/api/mcp`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ tool, args, session }),
  });
  return res.json().catch(() => ({}));
}

export function TakeoverControls({
  side,
  negotiation,
  sessionId,
  onToken,
  onActive,
}: {
  side: Side;
  negotiation: LiveNegotiation;
  sessionId: string;
  onToken?: (negotiationId: string, token: string) => void;
  onActive?: (negotiationId: string, active: boolean) => void;
}) {
  const [open, setOpen] = useState(false);
  const [price, setPrice] = useState<string>(
    String(negotiation.currentPrice || ""),
  );
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const n = negotiation;
  const session =
    side === "buyer" ? { buyerId: sessionId } : { sellerId: sessionId };

  const toggle = (v: boolean) => {
    setOpen(v);
    onActive?.(n.negotiationId, v);
  };

  const run = async (tool: string, extra: Record<string, unknown>) => {
    setBusy(true);
    setMsg(null);
    const r = await mcp(
      tool,
      { negotiation_id: n.negotiationId, round_seen: n.currentRound, ...extra },
      session,
    );
    setBusy(false);
    if (r?.error) {
      setMsg(String(r.error));
      return;
    }
    if (r?.confirm_token) onToken?.(n.negotiationId, r.confirm_token);
    setMsg("sent");
  };

  const terminal = n.status === "confirmed" || n.status === "rejected";
  if (terminal) return null;

  return (
    <div className="takeover">
      <label className="takeover__toggle">
        <input
          type="checkbox"
          checked={open}
          onChange={(e) => toggle(e.target.checked)}
        />{" "}
        Take over ({side})
      </label>
      {open ? (
        <div className="takeover__body">
          <input
            type="number"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            placeholder="price"
          />
          {side === "buyer" ? (
            <>
              <button disabled={busy} onClick={() => run("counter_offer", { price: Number(price) })}>
                Counter
              </button>
              <button disabled={busy} onClick={() => run("accept_offer", {})}>
                Accept
              </button>
            </>
          ) : (
            <>
              <button
                disabled={busy}
                onClick={() => run("respond_to_offer", { action: "counter", price: Number(price) })}
              >
                Counter
              </button>
              <button
                disabled={busy}
                onClick={() => run("respond_to_offer", { action: "accept" })}
              >
                Accept
              </button>
              <button
                disabled={busy}
                onClick={() => run("respond_to_offer", { action: "reject" })}
              >
                Reject
              </button>
            </>
          )}
          {msg ? <span className="takeover__msg">{msg}</span> : null}
        </div>
      ) : null}
    </div>
  );
}
