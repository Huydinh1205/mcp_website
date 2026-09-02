"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth, clearAuth } from "@/lib/auth";
import { useCart } from "@/lib/cart";
import { useWishlist } from "@/lib/wishlist";
import { Icon } from "@/app/components/Icon";

export function Nav() {
  const user = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const { count } = useCart();
  const wl = useWishlist();
  const [open, setOpen] = useState(false);

  const close = () => setOpen(false);
  const active = (href: string) => (pathname === href ? "on" : "");

  const links = (
    <>
      <Link href="/" className={active("/")} onClick={close}>Home</Link>
      <Link href="/catalog" className={active("/catalog")} onClick={close}>Catalog</Link>
      {user?.role === "buyer" ? <Link href="/agent" className={active("/agent")} onClick={close}>Buyer agent</Link> : null}
      {user?.role === "buyer" ? <Link href="/orders" className={active("/orders")} onClick={close}>My orders</Link> : null}
      {user?.role === "seller" ? <Link href="/dashboard" className={active("/dashboard")} onClick={close}>Seller dashboard</Link> : null}
    </>
  );

  return (
    <nav className="topnav">
      <Link href="/" className="brand" onClick={close}>Agent Market</Link>

      <button className="navburger" onClick={() => setOpen((v) => !v)} aria-label="Menu">
        <Icon name={open ? "check" : "chevron"} size={18} />
      </button>

      <div className={`navlinks ${open ? "open" : ""}`}>{links}</div>

      <span className="topnav__spacer" />

      <Link href="/wishlist" className="navicon" title="Wishlist" onClick={close}>
        <Icon name="heart" size={18} />
        {wl.count > 0 ? <span className="navcart__badge">{wl.count}</span> : null}
      </Link>
      <Link href="/cart" className="navicon" title="Cart" onClick={close}>
        <Icon name="cart" size={19} />
        {count > 0 ? <span className="navcart__badge">{count}</span> : null}
      </Link>

      {user ? (
        <>
          <Link href="/account" className="navuser" title={user.name} onClick={close}>
            <Icon name="user" size={18} />
            <span className="navuser__name">{user.name}</span>
          </Link>
          <button
            className="linkbtn"
            onClick={() => {
              clearAuth();
              close();
              router.replace("/login");
            }}
          >
            Log out
          </button>
        </>
      ) : (
        <>
          <Link href="/login" onClick={close}>Log in</Link>
          <Link href="/register" onClick={close}>Register</Link>
        </>
      )}
    </nav>
  );
}
