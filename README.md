# Agent-to-Agent Negotiation Marketplace

A multi-vendor marketplace for the OpenAI WebMCP Challenge. A buyer's AI agent
and a seller's AI agent negotiate price through structured tools exposed via
WebMCP (`document.modelContext`); every order requires explicit **human
confirmation** before it is placed.

**Runs in Chrome 146+** (only browser with `document.modelContext`). The app also
works without it — the harness falls back to its own tool registry.

## Layout

| Path | Stack | Role |
|------|-------|------|
| `backend/` | Java 21 · Spring Boot 3.5 · JPA/Hibernate · Flyway · **Microsoft SQL Server** | negotiation state machine, REST API, OpenAI proxy, server-side seller agent |
| `app/`, `lib/` | Next.js 15 · React 19 · TypeScript | WebMCP tool layer, browser agent loop, UI |

The agent loop and WebMCP tool registration **must** be browser TypeScript
(WebMCP is a browser API); everything else is the Java backend.

Design + decisions: `docs/designs/agent-negotiation-marketplace.md` ·
Spec Kit artifacts: `specs/001-agent-negotiation-marketplace/`

---

## Run it

### 1. Database (Microsoft SQL Server)

```bash
docker compose up -d
docker compose exec mssql /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'Your_strong_Passw0rd' -C -Q "CREATE DATABASE marketplace"
```
Or point at Azure SQL / any MSSQL — set `SPRING_DATASOURCE_URL` (see
`backend/.env.example`). Flyway (`V1__init.sql`) creates the tables on first boot;
`SeedRunner` loads demo data (5 sellers, ~30 products, 3 buyers) if the DB is empty.

### 2. Backend

```bash
cd backend
export SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:1433;databaseName=marketplace;encrypt=true;trustServerCertificate=true"
export SPRING_DATASOURCE_USERNAME=sa
export SPRING_DATASOURCE_PASSWORD='Your_strong_Passw0rd'
export OPENAI_API_KEY=sk-...
./mvnw spring-boot:run        # → http://localhost:8080  (mvnw downloads Maven on first run)
```

### 3. Frontend

```bash
cp .env.local.example .env.local      # NEXT_PUBLIC_API_BASE defaults to :8080
npm install
npm run dev                           # http://localhost:3000
```

### Try it

- **Buyer** (`/`): pick a buyer, keep or edit the goal, **Start agent**. The agent
  searches, offers, and counters against the server-side seller. When it accepts,
  a **Confirm** button appears — click it to place the order.
- **Seller dashboard** (`/dashboard`): pick a seller, **Run agent** (or toggle
  **Auto-respond**). The seller agent handles incoming offers via its own WebMCP
  tools; when it accepts, the seller confirms here.

**`SELLER_MODE`** (backend env):
- `server` (default) — the seller side is a deterministic server-side responder (US1). The buyer flow works in one window.
- `browser` — the seller side is a live WebMCP agent you run from `/dashboard` (US2/US3). Open two Chrome windows: buyer on `/`, seller on `/dashboard`, start both agents. Each human confirms their side.

US4 (compare sellers): give the buyer agent a goal like "compare every seller" — it opens one negotiation per seller and the buyer page shows a *best offer per product* panel.
US5 (manual takeover): every negotiation card has a **Take over** toggle to counter / accept / reject by hand through the same tools.

## Tests

| where | command | count |
|-------|---------|-------|
| backend | `cd backend && mvn test` | **39** — state machine (12), tokens (5), feed (5), optimistic write (5), two-sided confirm (5), `@SpringBootTest` boot + REST + server-seller e2e (4), browser-seller / seller-scoping / two-sided-confirm (3) |
| frontend | `npm test` | **16** — agent harness incl. unknown-tool abort (5), persona prompts (9), best-offer helper (2) |
| frontend | `npm run build` | production build |

All core logic was built test-first (see the design doc + git history).

## Architecture

```
Chrome (buyer page)
  buildBuyerRegistry() ── 6 WebMCP tools ──► POST  {API}/api/mcp
  runAgentLoop() ─ POST {API}/api/agent/turn ─► OpenAI (key server-side only)
        │
        ▼   Java / Spring Boot  (backend/)
  McpController ─► NegotiationService.commitTurn ─► OffersService.applyTurn
                        │                              (the ONLY transition logic)
                        ├─ optimistic write: UPDATE ... WHERE current_round = :seen
                        └─ SELLER_MODE=server ─► SellerResponderService (heuristic)
        │
        ▼
  Microsoft SQL Server  (JPA + Flyway)   ── single source of truth
        ▲
  GET  {API}/api/negotiations?since=<cursor>   ◄── useNegotiationFeed polls 1.5s
  POST {API}/api/orders/confirm  ◄── only path that finalizes an order;
                                     needs BOTH buyer + seller confirmation
```

## Status

US1–US5 are implemented. Still open: the Playwright two-window E2E (`e2e/two-window.spec.ts` is a stub), a persona eval, unit coverage for the browser harness edge branches, and the 9 non-demo ERD tables (`TODOS.md`).
