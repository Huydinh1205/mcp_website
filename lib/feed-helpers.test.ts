import { describe, expect, test } from "vitest";
import { bestPerProduct, type LiveNegotiation } from "@/lib/useNegotiationFeed";

const n = (id: string, name: string, price: number): LiveNegotiation => ({
  negotiationId: id,
  productId: "p-" + name,
  name,
  status: "countered",
  lastActor: "seller",
  currentRound: 1,
  currentPrice: price,
  updatedAt: 1,
  history: [],
});

describe("bestPerProduct (US4)", () => {
  test("keeps the cheapest negotiation per product name", () => {
    const best = bestPerProduct([
      n("a", "65% Keyboard", 58),
      n("b", "65% Keyboard", 52),
      n("c", "65% Keyboard", 55),
      n("d", "USB Cable", 9),
    ]);
    const byName = Object.fromEntries(best.map((x) => [x.name, x.negotiationId]));
    expect(byName["65% Keyboard"]).toBe("b");
    expect(byName["USB Cable"]).toBe("d");
    expect(best).toHaveLength(2);
  });

  test("empty in, empty out", () => {
    expect(bestPerProduct([])).toEqual([]);
  });
});
