"use client";

// Lightweight event-based toasts. Call toast("Added to cart") anywhere;
// <Toaster/> in the root layout renders them.

export type ToastKind = "info" | "success" | "error";
export interface ToastMsg {
  id: number;
  text: string;
  kind: ToastKind;
}

const EVT = "app-toast";
let seq = 0;

export function toast(text: string, kind: ToastKind = "info") {
  if (typeof window === "undefined") return;
  window.dispatchEvent(
    new CustomEvent<ToastMsg>(EVT, { detail: { id: ++seq, text, kind } }),
  );
}
export const toastOk = (t: string) => toast(t, "success");
export const toastErr = (t: string) => toast(t, "error");

export const TOAST_EVENT = EVT;
