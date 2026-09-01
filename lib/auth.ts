"use client";

import { useEffect, useState } from "react";
import { API_BASE } from "@/lib/api";

const KEY = "mcp_auth";

export interface AuthUser {
  userId: string;
  name: string;
  email: string;
  role: "buyer" | "seller";
  token: string;
}

export function getAuth(): AuthUser | null {
  try {
    const s = localStorage.getItem(KEY);
    return s ? (JSON.parse(s) as AuthUser) : null;
  } catch {
    return null;
  }
}

export function setAuth(u: AuthUser) {
  localStorage.setItem(KEY, JSON.stringify(u));
  window.dispatchEvent(new Event("auth-change"));
}

export function clearAuth() {
  localStorage.removeItem(KEY);
  window.dispatchEvent(new Event("auth-change"));
}

/** Reactive current-user hook. */
export function useAuth(): AuthUser | null {
  const [u, setU] = useState<AuthUser | null>(null);
  useEffect(() => {
    setU(getAuth());
    const h = () => setU(getAuth());
    window.addEventListener("auth-change", h);
    window.addEventListener("storage", h);
    return () => {
      window.removeEventListener("auth-change", h);
      window.removeEventListener("storage", h);
    };
  }, []);
  return u;
}

/** fetch with the Bearer token attached. `path` is relative to the API base. */
export async function authedFetch(
  path: string,
  init: RequestInit = {},
): Promise<Response> {
  const a = getAuth();
  const headers = new Headers(init.headers);
  if (a) headers.set("Authorization", `Bearer ${a.token}`);
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const res = await fetch(url, { ...init, headers });
  if (res.status === 401) clearAuth();
  return res;
}

export async function login(email: string, password: string): Promise<AuthUser> {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const j = await res.json();
  if (!res.ok) throw new Error(j.error ?? "login failed");
  return finish(j, email);
}

export async function register(
  name: string,
  email: string,
  password: string,
  role: "buyer" | "seller",
): Promise<AuthUser> {
  const res = await fetch(`${API_BASE}/api/auth/register`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ name, email, password, role }),
  });
  const j = await res.json();
  if (!res.ok) throw new Error(j.error ?? "register failed");
  return finish(j, email, name);
}

async function finish(
  j: { token: string; role: "buyer" | "seller"; userId: string },
  email: string,
  fallbackName?: string,
): Promise<AuthUser> {
  const me = await fetch(`${API_BASE}/api/auth/me`, {
    headers: { Authorization: `Bearer ${j.token}` },
  }).then((r) => (r.ok ? r.json() : null));
  const u: AuthUser = {
    userId: j.userId,
    name: me?.name ?? fallbackName ?? email,
    email,
    role: j.role,
    token: j.token,
  };
  setAuth(u);
  return u;
}
