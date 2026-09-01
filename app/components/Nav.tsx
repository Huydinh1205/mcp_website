"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth, clearAuth } from "@/lib/auth";

export function Nav() {
  const user = useAuth();
  const router = useRouter();
  return (
    <nav className="topnav">
      <Link href="/catalog" className="brand">Agent Market</Link>
      <Link href="/catalog">Catalog</Link>
      {user?.role === "buyer" ? <Link href="/">Buyer</Link> : null}
      {user?.role === "seller" ? <Link href="/dashboard">Seller dashboard</Link> : null}
      <span className="hint">Chrome 146+ recommended</span>
      {user ? (
        <>
          <span className="who">{user.name} ({user.role})</span>
          <button
            className="linkbtn"
            onClick={() => {
              clearAuth();
              router.replace("/login");
            }}
          >
            Log out
          </button>
        </>
      ) : (
        <>
          <Link href="/login">Log in</Link>
          <Link href="/register">Register</Link>
        </>
      )}
    </nav>
  );
}
