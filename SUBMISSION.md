# Agent Negotiation Marketplace — WebMCP Challenge submission

**Live URL:** _<paste after deploy>_
**Repo:** https://github.com/Huydinh1205/mcp_website
**Demo video:** _<paste YouTube link>_

A multi‑vendor storefront (95k+ real products) where a shopper's browser agent
and a seller's agent **negotiate a multi‑dimensional deal** — price, quantity,
free add‑ons, free shipping, coupons — through tools the page exposes with
WebMCP. Every order still needs an explicit **human confirmation**.

---

## Why this is a strong fit for WebMCP

Shopping is the canonical "the agent should just do it on the site I'm already
looking at" task, and negotiation is the part that a plain REST integration
*can't* capture:

- A single price is an algorithm; a **deal** (price × quantity × which freebie ×
  free shipping × which coupon) is a judgement call. That is exactly what an LLM
  agent is good at and what a fixed API is bad at.
- The tools are **stateful and rule‑bounded**: every offer/counter/accept runs
  through one server‑side state machine that enforces the buyer's budget ceiling,
  the seller's price floor, a max discount step, turn order and a round cap. The
  agent proposes; the page's tools guarantee the rules.
- WebMCP lets this happen **in the browser the person is already using**, on the
  product they're already looking at — no copy‑pasting IDs into a separate chat,
  no bespoke API keys, no backend integration by the marketplace's partners.

## How it makes the experience better

- **You stay in control.** The agent can `search_products`, `get_product`,
  `add_to_cart`, `list_coupons`, and `start_negotiation`, but it can never place
  an order. It fills the cart or opens a negotiation; **you** press Checkout /
  Confirm. Human‑in‑the‑loop is structural, not a promise.
- **One place.** "Find me a warm hooded robe under $25, get free shipping, and
  use the best coupon" is a sentence to the agent on the page — it searches,
  compares shops, opens a negotiation, and hands you a deal to confirm.
- **Real negotiation, visible.** The transcript (offer → counter → counter →
  accept) is shown live; you can take over with manual counter/accept buttons at
  any turn.

## What people + agents can do together that was hard before

- **Delegate haggling.** Before: haggling online meant a chat widget and a human
  rep, or it simply didn't exist. Now your agent runs 3 rounds against the
  seller's agent within your budget and hands you the result.
- **Compare *deals*, not prices.** The agent negotiates the same item at several
  shops in parallel and shows the best *total* (after freebies + shipping +
  coupon), which no "sort by price" can do.
- **Agent‑to‑agent commerce with a human veto.** Two browser agents (buyer tab +
  seller tab) settle terms; both humans confirm; only then is an Order + Invoice
  + Delivery created.

## How WebMCP was implemented

- **Registration.** On every page, `app/components/StorefrontTools.tsx` calls
  `document.modelContext.registerTool({ name, description, inputSchema, execute })`
  for the storefront tools (`search_products`, `get_product`, `list_categories`,
  `add_to_cart`, `view_cart`, `update_cart_quantity`, `list_coupons`,
  `start_negotiation`, `list_my_orders`). The `/agent` and `/dashboard` pages add
  the full buyer/seller negotiation tool sets via the same registry
  (`lib/webmcp/registry.ts`, `lib/webmcp/buyer-tools.ts`,
  `lib/webmcp/seller-tools.ts`).
- **Graceful fallback.** `ToolRegistryImpl.register()` registers with
  `document.modelContext` when the API exists; otherwise the same tool objects
  drive an in‑page agent loop (`lib/agent-loop.ts`) so the demo works without a
  WebMCP browser.
- **Execution.** A tool's `execute` calls the Java/Spring backend. State‑changing
  tools go through `POST /api/mcp` → `NegotiationService.commitTurn` →
  `OffersService.applyTurn` (the single, tested mutation path). Confirmations go
  through a signed token to `POST /api/orders/confirm`; the order is finalized
  only when **both** sides have confirmed.
- **Stack.** Next.js 15 / React 19 (WebMCP layer + agent loop) · Java 21 /
  Spring Boot 3.5 / JPA (state machine, REST) · Azure SQL Database · optional
  OpenAI **or** Gemini for the in‑page agent loop (WebMCP mode uses the visiting
  agent's own model — no key needed).

## Try it (as a judge)

1. Open the live URL in ChatGPT's in‑app browser or Chrome with WebMCP enabled.
2. Ask the agent: *"search for a fleece hooded robe under $25 and add the
   cheapest to my cart"* — watch it call `search_products` then `add_to_cart`.
3. Press **Checkout** → **My orders** shows the order + delivery tracking.
4. For negotiation: log in as the demo buyer (`mai.demo@example.com` /
   `password`), open a KeyLab Store product, press **Negotiate**, **Start
   agent**; in a second window log in as `keylab.demo@example.com`, open the
   **Seller dashboard**, tick **Auto‑respond**. Both agents settle; both humans
   **Confirm**.
