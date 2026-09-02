"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth, clearAuth } from "@/lib/auth";
import { useCart } from "@/lib/cart";
import { Icon } from "@/app/components/Icon";

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

      <span className="topnav__spacer" />

      <Link href="/cart" className="navcart" title="Cart">
        <Icon name="cart" size={19} />
        {count > 0 ? <span className="navcart__badge">{count}</span> : null}
      </Link>

      {user ? (
        <>
          <Link href="/account" className="navuser" title={user.name}>
            <Icon name="user" size={18} />
            <span className="navuser__name">{user.name}</span>
          </Link>
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
