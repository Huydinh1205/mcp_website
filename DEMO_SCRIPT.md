# Demo video script — Agent Market (WebMCP Challenge)

Target length **2:45** (hard cap 3:00). Narrate in **English**.

- Live URL: `https://mcp-website-chi.vercel.app`
- Backend: `https://agent-marketplace-api-gc1w.onrender.com`
- Repo: `https://github.com/Huydinh1205/mcp_website`
- Buyer login: `mai.demo@example.com` / `password`
- Seller login: `keylab.demo@example.com` / `password`

---

## PRE-FLIGHT (do before hitting record)

1. **Chrome 149+** with `chrome://flags/#enable-webmcp-testing` = **Enabled**, relaunched.
   Verify: open Live URL → F12 → Console shows
   `[WebMCP] document.modelContext present · 9/9 tools registered · …`
2. **Warm the backend**: open `https://agent-marketplace-api-gc1w.onrender.com/api/categories`
   in a tab, wait until it returns JSON (Render free tier can sleep ~50s).
3. **Two browser windows**, side by side:
   - **WIN-A** (normal Chrome window) = BUYER. Log in as `mai.demo@example.com`.
   - **WIN-B** (Incognito) = SELLER. Log in as `keylab.demo@example.com` → it lands on `/dashboard`.
4. In WIN-A, pre-open a KeyLab product page in another tab: `…/shop/137` → click any product.
5. Start the screen recorder (OBS / QuickTime / Loom). **Record system + mic audio.**
6. Zoom the browser to ~110–125% so text is readable in the video.

---

## RECORDING — shot by shot

### 0:00 – 0:20 · Intro
| | |
|---|---|
| SCREEN | WIN-A on the homepage. Slowly scroll the "Best selling" / "Top rated" rails. |
| SAY | "This is **Agent Market**, built for the WebMCP challenge. It's a marketplace where a shopper's browser agent and a seller's agent negotiate a real deal — price, quantity, freebies, free shipping, coupons — through tools the page exposes with WebMCP. And a human confirms every order." |

### 0:20 – 1:15 · WebMCP proof (Console)
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

### 1:15 – 1:40 · Buyer agent opens a negotiation
| | |
|---|---|
| SCREEN | WIN-A → switch to the pre-opened KeyLab **product page** → in "Shops carrying this item", click **Negotiate** on the **KeyLab Store** row. Lands on `/agent` with a goal pre-filled. Click **Start agent**. |
| SCREEN | The "Agent log" panel fills: `search_products`, then `submit_offer`. It then waits. |
| SAY | "Now the buyer's agent. I tell it to get the best deal on this robe from KeyLab Store. It searches, then makes an opening offer under my budget — you can see it calling the same tools. In browser-seller mode it now waits for the seller." |

### 1:40 – 2:15 · Seller agent responds, they haggle
| | |
|---|---|
| SCREEN | WIN-B (`/dashboard`) → the incoming offer is listed → tick **Auto-respond**. It posts a counter. |
| SCREEN | WIN-A → click **Start agent** again → buyer counters up. (Repeat B↔A once more if it hasn't settled.) |
| SAY | "The seller's agent picks it up and counters — never below its floor. They go back and forth a couple of rounds and converge." |
| NOTE | If an agent stalls: use the manual **Counter / Accept** buttons on the negotiation card and say "I can take over any turn." |

### 2:15 – 2:40 · Both humans confirm → order
| | |
|---|---|
| SCREEN | Both windows show a **Confirm** dialog (item + final price). Click **Confirm** in WIN-A, then in WIN-B. |
| SCREEN | WIN-A → nav → **My orders** → the order appears with the 4-step delivery tracker + a tracking number. Click **Write a review**, pick 5★, **Submit** → toast. |
| SAY | "Both agents agreed — but nothing is bought until each human confirms. Now there's an order, an invoice, and a delivery I can track. I can review it too." |

### 2:40 – 2:55 · Close
| | |
|---|---|
| SCREEN | Open the GitHub repo page — show the **MIT license** badge in the About box. |
| SAY | "People delegate the haggling. Agents settle the terms through the page's WebMCP tools. The human keeps the final say. The code and MIT license are in the description. Thanks for watching." |

---

## IF SOMETHING BREAKS

| Symptom | Fix on camera / edit later |
|---|---|
| Console prints `document.modelContext absent` | Chrome flag not on / Chrome < 149. Not recordable — fix Chrome first. |
| Catalog stuck "Loading…" | Backend was asleep. Reload after ~50s. (Pre-flight step 2 prevents this.) |
| `webmcpCall` errors | Hard-refresh (Cmd+Shift+R) so the latest deploy loads; retry. |
| Buyer/seller agent does nothing | Click **Start agent** again, or use the manual Counter/Accept buttons. Trim the dead air in editing. |
| Negotiation hits the round cap and rejects | Start a fresh one with a higher opening offer (closer to list price). |

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
   - **Testing Instructions** (private): `mai.demo@example.com` / `password` (buyer), `keylab.demo@example.com` / `password` (seller). Note: open in ChatGPT in-app browser or Chrome 149+ with `chrome://flags/#enable-webmcp-testing`.
5. **Submit.**
6. **Freeze** — no more pushes, no redeploys on Vercel/Render, no Devpost edits until winners are announced (Sep 21).
