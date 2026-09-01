import { describe, expect, test } from "vitest";
import { buildSystemPrompt, sellerAgentPrompt } from "@/lib/personas";

const buyer = {
  role: "buyer" as const,
  maxBudget: 60,
  targetPrice: 45,
  minSellerRating: 4,
  style: "aggressive",
};

const seller = {
  role: "seller" as const,
  minPrice: 40,
  autoAcceptPrice: 50,
  maxDiscountStep: 8,
  style: "fair",
};

describe("buildSystemPrompt (buyer)", () => {
  test("states the hard budget ceiling and forbids exceeding it", () => {
    const p = buildSystemPrompt(buyer);
    expect(p).toContain("60");
    expect(p.toLowerCase()).toMatch(/never (offer|pay|exceed|go above).*(budget|60)|max(imum)? budget/);
  });

  test("states the target price", () => {
    expect(buildSystemPrompt(buyer)).toContain("45");
  });

  test("passes the minimum seller rating through", () => {
    expect(buildSystemPrompt(buyer)).toContain("4");
  });

  test("carries the negotiation style", () => {
    expect(buildSystemPrompt(buyer).toLowerCase()).toContain("aggressive");
  });

  test("tells the agent to act only through the provided tools", () => {
    expect(buildSystemPrompt(buyer).toLowerCase()).toContain("tool");
  });
});

describe("buildSystemPrompt (seller)", () => {
  test("states the floor and forbids accepting below it", () => {
    const p = buildSystemPrompt(seller);
    expect(p).toContain("40");
    expect(p.toLowerCase()).toMatch(/never accept below|floor|minimum (price|acceptable)/);
  });

  test("includes the auto-accept price and the max discount step", () => {
    const p = buildSystemPrompt(seller);
    expect(p).toContain("50");
    expect(p).toContain("8");
  });

  test("carries the negotiation style", () => {
    expect(buildSystemPrompt(seller).toLowerCase()).toContain("fair");
  });
});

describe("sellerAgentPrompt (multi-product browser agent)", () => {
  test("defers to the per-offer limits returned by the tools", () => {
    const p = sellerAgentPrompt().toLowerCase();
    expect(p).toContain("min_price");
    expect(p).toContain("auto_accept_price");
    expect(p).toContain("max_discount_step");
    expect(p).toContain("never accept below");
    expect(p).toContain("tool");
  });
});
