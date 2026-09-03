# Demo video script — Agent Market (WebMCP Challenge)

Target length **2:45** (hard cap 3:00). Narrate in **English**.

- Live URL: `https://mcp-website-chi.vercel.app`
- Backend: `https://agent-marketplace-api-gc1w.onrender.com`
- Repo: `https://github.com/Huydinh1205/mcp_website`
- Buyer login: `mai.demo@example.com` / `password`
- **Seller is automatic**: backend runs in `SELLER_MODE=server` — a heuristic pricing agent
  responds to every buyer offer on the server. No second window, no seller login.
  (Two-agent variant with a browser seller is kept at the bottom of this file.)

---

## PRE-FLIGHT (do before hitting record)

1. **Chrome 149+** with `chrome://flags/#enable-webmcp-testing` = **Enabled**, relaunched.
   Verify: open Live URL → F12 → Console shows
   `[WebMCP] document.modelContext present · 9/9 tools registered · …`
2. **Warm the backend**: open `https://agent-marketplace-api-gc1w.onrender.com/api/categories`
   in a tab, wait until it returns JSON (Render free tier can sleep ~50s).
3. **Confirm server-seller mode** (once, off camera): the negotiation should progress on a
   single "Start agent" click with no seller window. If you self-host, set `SELLER_MODE=server`.
4. **One Chrome window**, logged in as `mai.demo@example.com`.
   For a clean single-chat view, use a buyer account with **no past negotiations** — register a
   throwaway at `/register` if `mai.demo` is cluttered.
5. Pre-open a KeyLab product page in a second tab: `…/shop/137` → click the
   **Flower Decor Fuzzy Night Robe**.
6. Start the screen recorder (OBS / QuickTime / Loom). **Record system + mic audio.**
7. Zoom the browser to ~110–125% so text is readable in the video.

---

## RECORDING — shot by shot

### 0:00 – 0:20 · Intro
| | |
|---|---|
| SCREEN | Homepage. Slowly scroll the "Best selling" / "Top rated" rails. |
| SAY | "This is **Agent Market**, built for the WebMCP challenge. It's a marketplace where a shopper's browser agent negotiates a real deal — price, quantity, freebies, free shipping, coupons — against the store's own pricing agent, all through tools the page exposes with WebMCP. And a human confirms the order." |

### 0:20 – 1:10 · WebMCP proof (Console)
| | |
|---|---|
| SCREEN | Press **F12** → **Console** tab. Type each line, press Enter, let the result show ~1s. |
| DO | `document.modelContext` |
| DO | `typeof document.modelContext.registerTool`  → `"function"` |
| DO | `webmcpTools()`  → expand the array, hover over `search_products`, `add_to_cart`, `start_negotiation` |
| DO | `await webmcpCall("search_products", { query: "robe" })`  → JSON list of products |
| DO | `await webmcpCall("add_to_cart", { product_id: "3" })`  → `{ ok: true, cart: [...] }` |
| SCREEN | Point the cursor at the **cart badge in the nav** — it now shows **1**, and a toast said "Added to cart". |
| DO | `webmcpCall("view_cart")`  → `{ items:[…], subtotal: … }` |
| SAY | "Every page calls `document.modelContext.registerTool` for nine tools. Here I invoke `search_products`, then `add_to_cart` — and the cart updates. A WebMCP browser, like ChatGPT's in-app browser, calls these exactly the same way. The judge's agent drives the store; it never needs an API key from me." |

### 1:10 – 2:05 · Buyer agent negotiates the deal
| | |
|---|---|
| SCREEN | Switch to the pre-opened robe **product page** → in "Shops carrying this item", click **Negotiate** on the **KeyLab Store** row. Lands on `/agent` with a goal pre-filled. Click **Start agent** — one click, then don't touch it. |
| SCREEN | The "Agent log" fills on its own: `search_products` → `submit_offer` → a seller counter comes **straight back** → `counter_offer` → seller counter → … up to 3 rounds → `apply_coupon` → `accept_offer`. In the "Negotiations" card, buyer and seller chat bubbles alternate and the price moves. |
| SAY | "Now the buyer's agent. I tell it to get the best total deal on this robe from KeyLab Store. It searches, opens an offer under my budget, and the store's pricing agent counters instantly — never below its floor. They trade a few rounds, it pulls in a coupon, and once the total is at or under my target the agent accepts the terms." |

### 2:05 – 2:35 · Human confirms → order
| | |
|---|---|
| SCREEN | A **Confirm** dialog appears (item, final price, what's included). Click **Confirm**. |
| SCREEN | nav → **My orders** → the order appears with the 4-step delivery tracker + a tracking number. Click **Write a review**, pick 5★, **Submit** → toast. |
| SAY | "The agents settled the terms — but nothing is bought until I confirm. Now there's an order, an invoice, and a delivery I can track. I can review it too." |

### 2:35 – 2:50 · Close
| | |
|---|---|
| SCREEN | Open the GitHub repo page — show the **MIT license** badge in the About box. |
| SAY | "People delegate the haggling. The agent settles the terms through the page's WebMCP tools. The human keeps the final say. The code and MIT license are in the description. Thanks for watching." |

---

## IF SOMETHING BREAKS

| Symptom | Fix on camera / edit later |
|---|---|
| Console prints `document.modelContext absent` | Chrome flag not on / Chrome < 149. Not recordable — fix Chrome first. |
| Catalog stuck "Loading…" | Backend was asleep. Reload after ~50s. (Pre-flight step 2 prevents this.) |
| `webmcpCall` errors | Hard-refresh (Cmd+Shift+R) so the latest deploy loads; retry. |
| Buyer agent stalls mid-negotiation | Click **Start agent** again — it resumes from the current state. Or use the manual **Counter / Accept** buttons on the negotiation card ("I can take over any turn"). Trim the dead air in editing. |
| Negotiation hits the round cap and rejects | Start a fresh one with a higher opening offer (closer to list price). |
| Page shows several old negotiations | Log in with a fresh buyer account so only the new chat shows. |

---

## AFTER RECORDING

1. Edit to ≤ 3:00, keep audio. Upload to **YouTube → Public** (not Unlisted).
2. Put the Live URL + YouTube link into `SUBMISSION.md` and `README.md` (replace the `_<paste …>_` placeholders).
3. `git add -A && git commit -m "submission: live URL + demo video" && git push`
4. Devpost form:
   - **Live URL**: `https://mcp-website-chi.vercel.app`
   - **Repo**: `https://github.com/Huydinh1205/mcp_website`
   - **Video**: the YouTube link
   - **Text description**: paste `SUBMISSION.md`
   - **Testing Instructions** (private): `mai.demo@example.com` / `password` (buyer). The store's
     pricing agent responds automatically — no seller login needed. Open in ChatGPT's in-app
     browser or Chrome 149+ with `chrome://flags/#enable-webmcp-testing`.
5. **Submit.**
6. **Freeze** — no more pushes, no redeploys on Vercel/Render, no Devpost edits until winners are announced (Sep 21).

---

## VARIANT — two-agent demo (`SELLER_MODE=browser`)

Use this only if you want to show a **second browser agent** on the seller side. It needs two
separate storage contexts (a normal window + an Incognito window) because both pages share
`localStorage` on the same origin.

- **WIN-A** (normal window) = BUYER, `mai.demo@example.com` → `/agent`.
- **WIN-B** (Incognito) = SELLER, `keylab.demo@example.com` → lands on `/dashboard`, tick
  **Auto-respond to new offers**.
- Buyer clicks **Start agent** → makes an opening offer, then **waits**.
- WIN-B auto-posts a counter; WIN-A buyer counters back; repeat a round or two until it settles.
- **Both** humans get a Confirm dialog — click Confirm in WIN-A, then WIN-B.
- Backend env: `SELLER_MODE=browser` (on Render: service `agent-marketplace-api` → Environment).
