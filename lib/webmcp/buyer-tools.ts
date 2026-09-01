// Buyer WebMCP tools. Handlers POST to the Java backend /api/mcp; the backend
// does the applyTurn work. Offers/counters carry a full DEAL (price + quantity +
// freebies + free shipping), not just a number.

import { ToolRegistryImpl } from "@/lib/webmcp/registry";
import { authedFetch } from "@/lib/auth";

const OBJ = (properties: Record<string, unknown>, required: string[] = []) => ({
  type: "object",
  properties,
  required,
  additionalProperties: false,
});

const DEAL_PROPS = {
  quantity: { type: "integer", minimum: 1, description: "units in the deal (e.g. 2 for buy-one-get-one)" },
  freebies: {
    type: "array",
    items: { type: "string" },
    description: "product_ids the seller throws in for free (from list_addons)",
  },
  free_shipping: { type: "boolean" },
};

export function buildBuyerRegistry(): ToolRegistryImpl {
  const reg = new ToolRegistryImpl();

  const call = async (tool: string, args: unknown): Promise<unknown> => {
    const res = await authedFetch("/api/mcp", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ tool, args }),
    });
    return res.json().catch(() => ({}));
  };

  reg.register({
    name: "search_products",
    description:
      "Search the catalog. Optional filters: max_price, min_seller_rating, category.",
    parameters: OBJ(
      {
        query: { type: "string" },
        max_price: { type: "number" },
        min_seller_rating: { type: "number" },
        category: { type: "string" },
      },
      ["query"],
    ),
    execute: (a) => call("search_products", a),
  });

  reg.register({
    name: "get_product",
    description: "Details for one product, incl. any open negotiation you have for it.",
    parameters: OBJ({ product_id: { type: "string" } }, ["product_id"]),
    execute: (a) => call("get_product", a),
  });

  reg.register({
    name: "list_addons",
    description:
      "Cheap items the seller of this product could throw in for free. Use their product_ids as `freebies` in an offer.",
    parameters: OBJ({ product_id: { type: "string" } }, ["product_id"]),
    execute: (a) => call("list_addons", a),
  });

  reg.register({
    name: "list_coupons",
    description: "Valid coupon codes for this product right now.",
    parameters: OBJ({ product_id: { type: "string" } }, ["product_id"]),
    execute: (a) => call("list_coupons", a),
  });

  reg.register({
    name: "apply_coupon",
    description:
      "Apply a coupon to a negotiation. Lowers the final price at checkout; does not change the negotiated base.",
    parameters: OBJ(
      { negotiation_id: { type: "string" }, code: { type: "string" } },
      ["negotiation_id", "code"],
    ),
    execute: (a) => call("apply_coupon", a),
  });

  reg.register({
    name: "submit_offer",
    description:
      "Open a negotiation with an opening deal (price + optional quantity / freebies / free shipping). Returns the seller's response.",
    parameters: OBJ(
      { product_id: { type: "string" }, price: { type: "number" }, message: { type: "string" }, ...DEAL_PROPS },
      ["product_id", "price"],
    ),
    execute: (a) => call("submit_offer", a),
  });

  reg.register({
    name: "counter_offer",
    description:
      "Counter in an existing negotiation with a full deal. Pass round_seen from the latest state you read.",
    parameters: OBJ(
      {
        negotiation_id: { type: "string" },
        price: { type: "number" },
        round_seen: { type: "integer" },
        message: { type: "string" },
        ...DEAL_PROPS,
      },
      ["negotiation_id", "price", "round_seen"],
    ),
    execute: (a) => call("counter_offer", a),
  });

  reg.register({
    name: "accept_offer",
    description:
      "Accept the seller's current deal. Does NOT place an order — returns a token for the human to confirm.",
    parameters: OBJ(
      { negotiation_id: { type: "string" }, round_seen: { type: "integer" } },
      ["negotiation_id", "round_seen"],
    ),
    execute: (a) => call("accept_offer", a),
  });

  reg.register({
    name: "list_my_offers",
    description: "Current state and full history of all your negotiations.",
    parameters: OBJ({}),
    execute: (a) => call("list_my_offers", a),
  });

  return reg;
}
