"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth, clearAuth } from "@/lib/auth";
import { useCart } from "@/lib/cart";

export function Nav() {
  const user = useAuth();
  const router = useRouter();
  const { count } = useCart();
  return (
    <nav className="topnav">
      <Link href="/catalog" className="brand">Agent Market</Link>
      <Link href="/catalog">Catalog</Link>
      {user?.role === "buyer" ? <Link href="/">Buyer agent</Link> : null}
      {user?.role === "buyer" ? <Link href="/orders">My orders</Link> : null}
      {user?.role === "seller" ? <Link href="/dashboard">Seller dashboard</Link> : null}
      <Link href="/cart" className="navcart">
        Cart{count > 0 ? <span className="navcart__badge">{count}</span> : null}
      </Link>
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
