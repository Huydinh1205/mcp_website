"use client";

// WebMCP: register the storefront's tools on document.modelContext so an agent
// in the browser (ChatGPT in-app browser / Chrome with WebMCP) can shop and
// start a negotiation on ANY page. Ordering still needs an explicit human
// click — the agent fills the cart / opens a negotiation, the person confirms.
//
// This mirrors the challenge's example:
//   document.modelContext.registerTool({ name, description, inputSchema, execute })

import { useEffect } from "react";
import { API_BASE } from "@/lib/api";
import { getAuth, authedFetch } from "@/lib/auth";
import { addToCart, getCart, setQuantity } from "@/lib/cart";
import { toast } from "@/lib/toast";

type ToolDef = {
  name: string;
  description: string;
  inputSchema: object;
  execute: (args: Record<string, unknown>) => Promise<unknown>;
};

const obj = (properties: Record<string, unknown>, required: string[] = []) => ({
  type: "object",
  properties,
  required,
  additionalProperties: false,
});

async function getJSON(path: string) {
  const r = await fetch(`${API_BASE}${path}`);
  return r.json();
}

const TOOLS: ToolDef[] = [
  {
    name: "search_products",
    description:
      "Search the product catalog. Returns up to ~60 products with price, rating, seller and image.",
    inputSchema: obj(
      {
        query: { type: "string", description: "free-text search" },
        category: { type: "string" },
        max_price: { type: "number" },
        min_rating: { type: "number" },
        sort: {
          type: "string",
          enum: ["", "sold", "rating", "price_asc", "price_desc"],
        },
      },
      ["query"],
    ),
    execute: async (a) => {
      const p = new URLSearchParams();
      if (a.query) p.set("q", String(a.query));
      if (a.category) p.set("category", String(a.category));
      if (a.max_price != null) p.set("maxPrice", String(a.max_price));
      if (a.min_rating != null) p.set("minRating", String(a.min_rating));
      if (a.sort) p.set("sort", String(a.sort));
      const rows = await getJSON(`/api/products?${p}`);
      return Array.isArray(rows) ? rows.slice(0, 40) : rows;
    },
  },
  {
    name: "get_product",
    description: "Full detail for one product: description, price, every shop selling it, reviews.",
    inputSchema: obj({ product_id: { type: "string" } }, ["product_id"]),
    execute: (a) => getJSON(`/api/products/${encodeURIComponent(String(a.product_id))}`),
  },
  {
    name: "list_categories",
    description: "List the product categories available in the catalog.",
    inputSchema: obj({}),
    execute: () => getJSON(`/api/categories`),
  },
  {
    name: "add_to_cart",
    description:
      "Add a product to the shopping cart at list price. Does NOT place an order — the person reviews the cart and checks out.",
    inputSchema: obj(
      { product_id: { type: "string" }, quantity: { type: "integer", minimum: 1 } },
      ["product_id"],
    ),
    execute: async (a) => {
      const d = await getJSON(`/api/products/${encodeURIComponent(String(a.product_id))}`);
      if (!d || d.error) return { error: "product not found" };
      const qty = Math.max(1, Number(a.quantity) || 1);
      addToCart(
        {
          product_id: String(d.product_id),
          name: d.name,
          price: d.price,
          image_url: d.image_url ?? null,
          seller_name: d.sellers?.[0]?.seller_name,
        },
        qty,
      );
      toast(`Agent added ${qty}× ${d.name} to cart`, "success");
      return { ok: true, cart: getCart(), note: "Ask the person to review the cart and press Checkout." };
    },
  },
  {
    name: "view_cart",
    description: "Show what is currently in the shopping cart and the subtotal.",
    inputSchema: obj({}),
    execute: async () => {
      const items = getCart();
      return { items, subtotal: items.reduce((s, i) => s + i.price * i.quantity, 0) };
    },
  },
  {
    name: "update_cart_quantity",
    description: "Change the quantity of a cart line (0 removes it).",
    inputSchema: obj(
      { product_id: { type: "string" }, quantity: { type: "integer", minimum: 0 } },
      ["product_id", "quantity"],
    ),
    execute: async (a) => {
      setQuantity(String(a.product_id), Number(a.quantity) || 0);
      return { ok: true, cart: getCart() };
    },
  },
  {
    name: "list_coupons",
    description: "Valid coupon codes for a product right now.",
    inputSchema: obj({ product_id: { type: "string" } }, ["product_id"]),
    execute: async (a) => {
      if (!getAuth()) return { error: "sign in as a buyer to check coupons" };
      const r = await authedFetch("/api/mcp", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ tool: "list_coupons", args: { product_id: a.product_id } }),
      });
      return r.json();
    },
  },
  {
    name: "start_negotiation",
    description:
      "Open a negotiation with an opening offer for one product listing (needs a signed-in buyer). " +
      "The seller (or their agent) responds; the person confirms the final deal. " +
      "price is the deal total; optionally quantity / free_shipping.",
    inputSchema: obj(
      {
        product_id: { type: "string" },
        price: { type: "number" },
        quantity: { type: "integer", minimum: 1 },
        free_shipping: { type: "boolean" },
        message: { type: "string" },
      },
      ["product_id", "price"],
    ),
    execute: async (a) => {
      if (!getAuth()) return { error: "sign in as a buyer first (top-right → Log in)" };
      const r = await authedFetch("/api/mcp", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ tool: "submit_offer", args: a }),
      });
      const j = await r.json();
      toast("Agent opened a negotiation", "info");
      return j;
    },
  },
  {
    name: "list_my_orders",
    description: "The signed-in buyer's orders with delivery status and tracking.",
    inputSchema: obj({}),
    execute: async () => {
      if (!getAuth()) return { error: "sign in first" };
      const r = await authedFetch("/api/orders");
      return r.json();
    },
  },
];

export function StorefrontTools() {
  useEffect(() => {
    const mc = (window as unknown as { document?: { modelContext?: { registerTool?: (d: unknown) => void } } })
      .document?.modelContext;

    let registered = 0;
    if (mc?.registerTool) {
      for (const t of TOOLS) {
        try {
          mc.registerTool({
            name: t.name,
            description: t.description,
            inputSchema: t.inputSchema,
            execute: (args: unknown) => t.execute((args ?? {}) as Record<string, unknown>),
          });
          registered++;
        } catch {
          /* best effort */
        }
      }
    }

    // Debug hook: inspect / invoke the exact registered tools from DevTools console.
    //   webmcpTools()                         -> list
    //   webmcpCall("search_products",{query:"robe"})
    const w = window as unknown as Record<string, unknown>;
    w.__webmcpTools = TOOLS;
    w.webmcpTools = () =>
      TOOLS.map((t) => ({ name: t.name, description: t.description, inputSchema: t.inputSchema }));
    w.webmcpCall = (name: string, args?: Record<string, unknown>) => {
      const t = TOOLS.find((x) => x.name === name);
      if (!t) throw new Error(`no tool: ${name}`);
      return t.execute(args ?? {});
    };
    console.info(
      `[WebMCP] document.modelContext ${mc ? "present" : "absent"} · ${registered}/${TOOLS.length} tools registered · ` +
        `console: webmcpTools(), webmcpCall("search_products",{query:"robe"})`,
    );
  }, []);
  return null;
}
