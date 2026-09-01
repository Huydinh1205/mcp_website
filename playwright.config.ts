import { defineConfig, devices } from "@playwright/test";

// T033 two-window E2E. Needs the app + a seeded DB running on :3000.
export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  use: {
    baseURL: "http://localhost:3000",
    ...devices["Desktop Chrome"],
  },
  webServer: {
    command: "npm run dev",
    url: "http://localhost:3000",
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
