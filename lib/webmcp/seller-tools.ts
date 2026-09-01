// The 3 seller WebMCP tools (contracts/webmcp-seller-tools.md). Each handler is a
// thin POST to /api/mcp; the Java backend does the applyTurn work. Registered via
// ToolRegistryImpl so both our harness and a real browser agent can call them.

import { ToolRegistryImpl } from "@/lib/webmcp/registry";
import { API_BASE } from "@/lib/api";

export interface SellerSession {
  sellerId: string;
}

const OBJ = (properties: Record<string, unknown>, required: string[] = []) => ({
  type: "object",
  properties,
  required,
  additionalProperties: false,
});

export function buildSellerRegistry(session: SellerSession): ToolRegistryImpl {
  const reg = new ToolRegistryImpl();

  const call = async (tool: string, args: unknown): Promise<unknown> => {
    const res = await fetch(`${API_BASE}/api/mcp`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ tool, args, session }),
    });
    return res.json().catch(() => ({}));
  };

  reg.register({
    name: "list_incoming_offers",
    description:
      "List negotiations waiting for your response. Each entry includes that product's min_price, auto_accept_price and max_discount_step.",
    parameters: OBJ({}),
    execute: (a) => call("list_incoming_offers", a),
  });

  reg.register({
    name: "get_offer_history",
    description: "Get the full round-by-round history of one negotiation.",
    parameters: OBJ({ negotiation_id: { type: "string" } }, ["negotiation_id"]),
    execute: (a) => call("get_offer_history", a),
  });

  reg.register({
    name: "respond_to_offer",
    description:
      "Take your turn on a negotiation: accept, counter, or reject. 'accept' returns a token for the human to confirm; it does NOT finalize a sale. Pass round_seen from the latest state.",
    parameters: OBJ(
      {
        negotiation_id: { type: "string" },
        action: { type: "string", enum: ["accept", "counter", "reject"] },
        price: { type: "number", description: "required when action is 'counter'" },
        round_seen: { type: "integer" },
        message: { type: "string" },
      },
      ["negotiation_id", "action", "round_seen"],
    ),
    execute: (a) => call("respond_to_offer", a),
  });

  return reg;
}
