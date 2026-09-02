"use client";

import { useEffect, useState } from "react";
import { TOAST_EVENT, type ToastMsg } from "@/lib/toast";
import { Icon } from "@/app/components/Icon";

export function Toaster() {
  const [items, setItems] = useState<ToastMsg[]>([]);

  useEffect(() => {
    const onToast = (e: Event) => {
      const m = (e as CustomEvent<ToastMsg>).detail;
      setItems((prev) => [...prev, m]);
      setTimeout(() => setItems((prev) => prev.filter((x) => x.id !== m.id)), 2600);
    };
    window.addEventListener(TOAST_EVENT, onToast);
    return () => window.removeEventListener(TOAST_EVENT, onToast);
  }, []);

  return (
    <div className="toaster" aria-live="polite">
      {items.map((m) => (
        <div key={m.id} className={`toast toast--${m.kind}`}>
          <Icon name={m.kind === "error" ? "shield" : "check"} size={15} />
          <span>{m.text}</span>
        </div>
      ))}
    </div>
  );
}
