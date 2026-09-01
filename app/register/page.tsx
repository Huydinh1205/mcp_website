"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { register } from "@/lib/auth";

export default function RegisterPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<"buyer" | "seller">("buyer");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr(null);
    setBusy(true);
    try {
      const u = await register(name, email, password, role);
      router.replace(u.role === "seller" ? "/dashboard" : "/");
    } catch (x) {
      setErr(x instanceof Error ? x.message : "register failed");
      setBusy(false);
    }
  };

  return (
    <main className="wrap narrow">
      <h1>Register</h1>
      <form className="panel" onSubmit={submit}>
        <label>Name<input value={name} onChange={(e) => setName(e.target.value)} required /></label>
        <label>Email<input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required /></label>
        <label>Password (min 6)<input value={password} onChange={(e) => setPassword(e.target.value)} type="password" minLength={6} required /></label>
        <label>
          I am a{" "}
          <select value={role} onChange={(e) => setRole(e.target.value as "buyer" | "seller")}>
            <option value="buyer">Buyer</option>
            <option value="seller">Seller</option>
          </select>
        </label>
        <button type="submit" disabled={busy}>{busy ? "…" : "Create account"}</button>
        {err ? <p className="err">{err}</p> : null}
      </form>
      <p>Have an account? <Link href="/login">Log in</Link></p>
    </main>
  );
}
