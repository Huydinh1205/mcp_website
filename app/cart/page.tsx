"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/auth";
import { useCart, setQuantity, removeFromCart, clearCart, checkout } from "@/lib/cart";
import { money } from "@/lib/format";
import { toast, toastErr } from "@/lib/toast";
import { Icon } from "@/app/components/Icon";

export default function CartPage() {
  const user = useAuth();
  const router = useRouter();
  const { items, count, subtotal } = useCart();
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const doCheckout = async () => {
    if (!user || user.role !== "buyer") {
      router.push("/login?next=/cart");
      return;
    }
    setBusy(true);
    setErr(null);
    try {
      const lines = await checkout(items);
      const ok = lines.filter((l) => !l.error).length;
      const failed = lines.length - ok;
      clearCart();
      if (ok) toast(`${ok} order${ok === 1 ? "" : "s"} placed`, "success");
      if (failed) toastErr(`${failed} item(s) failed`);
      router.push("/orders");
    } catch (e) {
      const m = e instanceof Error ? e.message : "checkout failed";
      setErr(m);
      toastErr(m);
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="wrap">
      <h1>Cart {count > 0 ? `(${count})` : ""}</h1>

      {items.length === 0 ? (
        <p className="muted">
          Your cart is empty. <Link href="/catalog">Browse the catalog</Link>.
        </p>
      ) : (
        <>
          <div className="clist">
            {items.map((i) => (
              <div key={i.product_id} className="crow">
                {i.image_url ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img className="crow__img" src={i.image_url} alt={i.name} />
                ) : (
                  <div className="crow__img" />
                )}
                <div className="crow__meta">
                  <Link href={`/product/${i.product_id}`} className="crow__name">{i.name}</Link>
                  {i.seller_name ? <div className="muted small">{i.seller_name}</div> : null}
                  <div className="crow__price">${money(i.price)} each</div>
                </div>
                <div className="crow__qty">
                  <button onClick={() => setQuantity(i.product_id, i.quantity - 1)}>−</button>
                  <input
                    type="number"
                    min={1}
                    value={i.quantity}
                    onChange={(e) => setQuantity(i.product_id, Math.max(1, Number(e.target.value) || 1))}
                  />
                  <button onClick={() => setQuantity(i.product_id, i.quantity + 1)}>+</button>
                </div>
                <div className="crow__line">${money(i.price * i.quantity)}</div>
                <button
                  className="linkbtn crow__rm"
                  onClick={() => {
                    removeFromCart(i.product_id);
                    toast("Removed from cart");
                  }}
                >
                  <Icon name="trash" size={13} /> Remove
                </button>
              </div>
            ))}
          </div>

          <div className="csum">
            <div className="csum__row">
              <span>Subtotal ({count} item{count === 1 ? "" : "s"})</span>
              <strong>${money(subtotal)}</strong>
            </div>
            <p className="muted small">
              Buying at list price. Want a better deal? Close the cart and send an agent to
              negotiate a product instead.
            </p>
            <div className="csum__actions">
              <button onClick={doCheckout} disabled={busy}>
                {busy ? "Placing orders…" : `Checkout · $${money(subtotal)}`}
              </button>
              <button className="secondary" onClick={() => clearCart()}>Clear cart</button>
            </div>
            {err ? <p className="err">{err}</p> : null}
          </div>
        </>
      )}
    </main>
  );
}
