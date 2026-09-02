"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { login } from "@/lib/auth";

const DEMO = {
  buyer: { email: "mai.demo@example.com", password: "password" },
  seller: { email: "keylab.demo@example.com", password: "password" },
};

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const next = () => new URLSearchParams(window.location.search).get("next");

  const submit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    setErr(null);
    setBusy(true);
    try {
      const u = await login(email, password);
      router.replace(next() ?? (u.role === "seller" ? "/dashboard" : "/"));
    } catch (x) {
      setErr(x instanceof Error ? x.message : "login failed");
      setBusy(false);
    }
  };

  const fill = (r: "buyer" | "seller") => {
    setEmail(DEMO[r].email);
    setPassword(DEMO[r].password);
  };

  return (
    <main className="wrap narrow">
      <h1>Log in</h1>
      <form className="panel" onSubmit={submit}>
        <label>Email<input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required /></label>
        <label>Password<input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required /></label>
        <button type="submit" disabled={busy}>{busy ? "…" : "Log in"}</button>
        {err ? <p className="err">{err}</p> : null}
      </form>

      <section className="panel">
        <h2>Demo accounts (password: <code>password</code>)</h2>
        <button onClick={() => fill("buyer")}>Fill buyer (mai.demo@example.com)</button>{" "}
        <button onClick={() => fill("seller")}>Fill seller (keylab.demo@example.com)</button>
        <p className="muted">Also: long.demo@example.com (buyer). Password: <code>password</code></p>
      </section>

      <p>No account? <Link href="/register">Register</Link></p>
    </main>
  );
}
