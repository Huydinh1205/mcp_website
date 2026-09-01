// The 6 buyer WebMCP tools (contracts/webmcp-buyer-tools.md). Each handler is a
// thin POST to /api/mcp; the server does the applyTurn work. Registered via
// ToolRegistryImpl so both our harness and a real browser agent can call them.

import { ToolRegistryImpl } from "@/lib/webmcp/registry";
import { API_BASE } from "@/lib/api";

export interface BuyerSession {
  buyerId: string;
}

const OBJ = (properties: Record<string, unknown>, required: string[] = []) => ({
  type: "object",
  properties,
  required,
  additionalProperties: false,
});

export function buildBuyerRegistry(session: BuyerSession): ToolRegistryImpl {
  const reg = new ToolRegistryImpl();

  const call = async (tool: string, args: unknown): Promise<unknown> => {
    const res = await fetch(`${API_BASE}/api/mcp`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ tool, args, session }),
    });
    const json = await res.json().catch(() => ({}));
    // Return errors as data so the agent can react instead of the loop throwing.
    return json;
  };

  reg.register({
    name: "search_products",
    description:
      "Search the marketplace catalog. Returns products with public asking price and seller rating.",
    parameters: OBJ(
      {
        query: { type: "string", description: "natural-language product description" },
        max_price: { type: "number" },
        min_seller_rating: { type: "number" },
      },
      ["query"],
    ),
    execute: (a) => call("search_products", a),
  });

  reg.register({
    name: "get_product",
    description: "Get details for one product, including any open negotiation you have for it.",
    parameters: OBJ({ product_id: { type: "string" } }, ["product_id"]),
    execute: (a) => call("get_product", a),
  });

  reg.register({
    name: "submit_offer",
    description:
      "Open a negotiation for a product with an opening offer price. Returns the seller's response.",
    parameters: OBJ(
      {
        product_id: { type: "string" },
        price: { type: "number" },
        quantity: { type: "integer", minimum: 1 },
        message: { type: "string" },
      },
      ["product_id", "price"],
    ),
    execute: (a) => call("submit_offer", a),
  });

  reg.register({
    name: "counter_offer",
    description:
      "Send a counter-offer in an existing negotiation. Pass round_seen from the latest state you read.",
    parameters: OBJ(
      {
        negotiation_id: { type: "string" },
        price: { type: "number" },
        round_seen: { type: "integer" },
        message: { type: "string" },
      },
      ["negotiation_id", "price", "round_seen"],
    ),
    execute: (a) => call("counter_offer", a),
  });

  reg.register({
    name: "accept_offer",
    description:
      "Accept the seller's current price. This does NOT place an order — it returns a token for the human to confirm.",
    parameters: OBJ(
      {
        negotiation_id: { type: "string" },
        round_seen: { type: "integer" },
      },
      ["negotiation_id", "round_seen"],
    ),
    execute: (a) => call("accept_offer", a),
  });

  reg.register({
    name: "list_my_offers",
    description:
      "List the current state and full history of all your negotiations.",
    parameters: OBJ({}),
    execute: (a) => call("list_my_offers", a),
  });

  return reg;
}
