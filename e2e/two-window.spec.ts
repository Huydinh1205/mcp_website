import { test, expect } from "@playwright/test";

// T033 — two-window happy path. STUB: fill in once US3 (browser seller agent)
// lands. For US1 this can be reduced to a single-window buyer run.
//
// Skeleton of the intended assertions:
//   1. buyer picks an identity, starts the agent
//   2. a negotiation reaches buyer_accepted / seller_accepted
//   3. NO order row exists yet
//   4. buyer clicks Confirm  -> order pending
//   5. seller clicks Confirm -> order confirmed, both *_confirmed_at set
//   6. a second Confirm click still yields exactly one order

test.skip("buyer agent negotiates and the order needs two human confirms", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByRole("button", { name: /start agent/i }).click();
  await expect(page.locator(".pill--buyer_accepted, .pill--seller_accepted")).toBeVisible({
    timeout: 45_000,
  });
  // ... confirm flow assertions
});
