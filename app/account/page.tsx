"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth, authedFetch } from "@/lib/auth";
import { Icon } from "@/app/components/Icon";

const STATES = ["NSW", "VIC", "QLD", "WA", "SA", "TAS", "ACT", "NT"];

interface Profile {
  name: string;
  email: string;
  role: string;
  phone: string | null;
  street_address: string | null;
  suburb: string | null;
  state: string | null;
  postcode: string | null;
  full_address: string | null;
}

export default function AccountPage() {
  const user = useAuth();
  const router = useRouter();
  const [p, setP] = useState<Profile | null>(null);
  const [saved, setSaved] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (user === null) return;
    if (!user) {
      router.replace("/login?next=/account");
      return;
    }
    authedFetch("/api/auth/me")
      .then((r) => r.json())
      .then((j) => setP(j.error ? null : j));
  }, [user, router]);

  if (!user) return <main className="wrap"><p className="muted">Redirecting…</p></main>;
  if (!p) return <main className="wrap"><p className="muted">Loading…</p></main>;

  const set = (k: keyof Profile, v: string) => {
    setP({ ...p, [k]: v });
    setSaved(false);
  };

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr(null);
    setBusy(true);
    try {
      const res = await authedFetch("/api/auth/me", {
        method: "PUT",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          name: p.name,
          phone: p.phone ?? "",
          street_address: p.street_address ?? "",
          suburb: p.suburb ?? "",
          state: p.state ?? "",
          postcode: p.postcode ?? "",
        }),
      });
      const j = await res.json();
      if (!res.ok) setErr(j.error ?? "save failed");
      else {
        setP(j);
        setSaved(true);
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="wrap narrow">
      <h1><Icon name="user" size={20} /> Account</h1>

      <form className="panel" onSubmit={save}>
        <label>Name<input value={p.name} onChange={(e) => set("name", e.target.value)} required /></label>
        <label>Email<input value={p.email} disabled /></label>
        <label>Phone<input value={p.phone ?? ""} onChange={(e) => set("phone", e.target.value)} placeholder="04xx xxx xxx" /></label>

        <h2 style={{ marginTop: 8 }}><Icon name="truck" size={16} /> Shipping address</h2>
        <label>Street address<input value={p.street_address ?? ""} onChange={(e) => set("street_address", e.target.value)} placeholder="12 Example St" /></label>
        <label>Suburb<input value={p.suburb ?? ""} onChange={(e) => set("suburb", e.target.value)} placeholder="Sydney" /></label>
        <label>
          State
          <select value={p.state ?? ""} onChange={(e) => set("state", e.target.value)}>
            <option value="">—</option>
            {STATES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
        <label>Postcode<input value={p.postcode ?? ""} onChange={(e) => set("postcode", e.target.value)} placeholder="2000" inputMode="numeric" /></label>

        <button type="submit" disabled={busy}>{busy ? "Saving…" : "Save"}</button>
        {saved ? <p className="ok"><Icon name="check" size={14} /> Saved</p> : null}
        {err ? <p className="err">{err === "BAD_STATE" ? "Pick a valid AU state" : err === "BAD_POSTCODE" ? "Postcode must be 4 digits" : err}</p> : null}
      </form>

      <p className="muted small">
        This address is used as the delivery destination when you buy now or check out a cart.
      </p>
    </main>
  );
}
