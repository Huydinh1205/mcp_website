"use client";

import { useState } from "react";
import { authedFetch } from "@/lib/auth";

export function ConfirmModal({
  title,
  price,
  seller,
  confirmToken,
  onDone,
}: {
  title: string;
  price: number;
  seller?: string;
  confirmToken: string;
  onDone: (result: { status: string }) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const confirm = async () => {
    if (busy) return; // double-click guard
    setBusy(true);
    setError(null);
    try {
      const res = await authedFetch("/api/orders/confirm", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ confirm_token: confirmToken }),
      });
      const json = await res.json();
      if (!res.ok) {
        setError(json.error ?? "confirm failed");
        setBusy(false);
        return;
      }
      onDone(json);
    } catch {
      setError("network error");
      setBusy(false);
    }
  };

  return (
    <div className="modal">
      <h3>Confirm purchase</h3>
      <p>
        Buy <strong>{title}</strong>
        {seller ? <> from {seller}</> : null} for{" "}
        <strong>{price.toFixed(2)}</strong>?
      </p>
      <button onClick={confirm} disabled={busy}>
        {busy ? "Confirming…" : "Confirm"}
      </button>
      {error ? <p className="err">{error}</p> : null}
    </div>
  );
}
