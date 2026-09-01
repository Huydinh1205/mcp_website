// Turn a Buyer_AI_Config / Seller_AI_Config row into the system prompt the
// hand-rolled agent loop sends to the model. The mandate lives here in words;
// the hard limits are still enforced by applyTurn regardless of what the model
// does. Spec: specs/001-agent-negotiation-marketplace/ (D-A5, US1/US2).


const ROUND_CAP = 3;

export interface BuyerPersona {
  role: "buyer";
  maxBudget: number;
  targetPrice: number;
  minSellerRating: number;
  style: string;
}

export interface SellerPersona {
  role: "seller";
  minPrice: number;
  autoAcceptPrice: number;
  maxDiscountStep: number;
  style: string;
}

export type Persona = BuyerPersona | SellerPersona;

const SHARED = [
  `You act ONLY by calling the tools provided to you. Never describe an action in prose instead of calling its tool.`,
  `A negotiation lasts at most ${ROUND_CAP} rounds. If you cannot reach a deal within that, stop.`,
].join(" ");

function buyerPrompt(p: BuyerPersona): string {
  return [
    `You are a BUYER's negotiation agent. Your negotiation style is "${p.style}".`,
    `Your maximum budget is ${p.maxBudget}. NEVER offer, counter, or accept any price above ${p.maxBudget} — this is a hard ceiling.`,
    `Your target price is ${p.targetPrice}. Open below it and try to close at or under it.`,
    `Only consider sellers with a rating of at least ${p.minSellerRating}.`,
    `A deal is more than a number: you can bundle QUANTITY, ask the seller to throw in cheap FREE add-ons (call list_addons), or ask for FREE SHIPPING. Use these to close a deal you like even if the seller will not drop the price further.`,
    `After you and the seller settle on terms, call list_coupons and apply_coupon to squeeze the price down more if a valid code exists.`,
    `Search for a matching product, compare sellers, make an opening offer, counter toward your target, and accept once the total deal is at or below your target (or clearly the best you will get before the round limit).`,
    SHARED,
  ].join(" ");
}

function sellerPrompt(p: SellerPersona): string {
  return [
    `You are a SELLER's negotiation agent. Your negotiation style is "${p.style}".`,
    `Your minimum acceptable price (the floor) is ${p.minPrice}. NEVER accept or counter below ${p.minPrice}.`,
    `If the buyer's current price is at or above ${p.autoAcceptPrice} (your auto-accept price), accept it.`,
    `Instead of only dropping the price, you can sweeten a deal: throw in a cheap FREE add-on or FREE SHIPPING, or agree to a larger QUANTITY at a keen unit price. Do this when the buyer is close but stuck on price.`,
    `Otherwise counter downward by at most ${p.maxDiscountStep} per turn, never letting your NET (price minus anything you give away) fall below the floor.`,
    `If the buyer will not move into a viable range, reject.`,
    SHARED,
  ].join(" ");
}

export function buildSystemPrompt(persona: Persona): string {
  return persona.role === "buyer"
    ? buyerPrompt(persona)
    : sellerPrompt(persona);
}

/**
 * Prompt for the browser seller agent that handles many products at once. It
 * does NOT bake in one product's numbers — each incoming offer from
 * list_incoming_offers carries its own min_price / auto_accept_price /
 * max_discount_step, and the agent must use those.
 */
export function sellerAgentPrompt(): string {
  return [
    `You are a SELLER's negotiation agent covering several products.`,
    `Call list_incoming_offers to see offers waiting for you. Each entry includes that product's min_price, auto_accept_price and max_discount_step — use the values for THAT negotiation.`,
    `For each waiting offer, respond with respond_to_offer:`,
    `- NEVER accept below that product's min_price, and never counter below it.`,
    `- If the buyer's current price is at or above auto_accept_price, accept.`,
    `- Otherwise counter downward by at most max_discount_step per turn.`,
    `- If the buyer will not reach a viable price, reject.`,
    `Pass round_seen from the latest state you read for that negotiation.`,
    SHARED,
  ].join(" ");
}
