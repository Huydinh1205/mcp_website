"use client";

import { useState } from "react";
import { authedFetch } from "@/lib/auth";
import { toast, toastErr } from "@/lib/toast";

export function ReviewForm({
  productId,
  negotiationId,
  onDone,
}: {
  productId: string;
  negotiationId: string;
  onDone?: () => void;
}) {
  const [rating, setRating] = useState("5");
  const [comment, setComment] = useState("");
  const [done, setDone] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  if (done) return <p className="ok">Review submitted ✓</p>;

  const submit = async () => {
    setErr(null);
    const res = await authedFetch("/api/feedback", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        product_id: productId,
        negotiation_id: negotiationId,
        rating: Number(rating),
        comment,
      }),
    });
    const j = await res.json().catch(() => ({}));
    if (!res.ok) {
      setErr(j.error ?? "failed");
      toastErr(j.error ?? "Review failed");
    } else {
      setDone(true);
      toast("Thanks for the review!", "success");
      onDone?.();
    }
  };

  return (
    <div className="reviewform">
      <select value={rating} onChange={(e) => setRating(e.target.value)}>
        {[5, 4, 3, 2, 1].map((n) => (
          <option key={n} value={n}>★ {n}</option>
        ))}
      </select>
      <input
        placeholder="Leave a review…"
        value={comment}
        onChange={(e) => setComment(e.target.value)}
      />
      <button onClick={submit}>Submit review</button>
      {err ? <span className="err">{err}</span> : null}
    </div>
  );
}
