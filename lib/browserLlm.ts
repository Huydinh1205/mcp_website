// Client-safe LlmTurn: delegates to the server proxy (Bearer-authed) so the
// OpenAI SDK and key never enter the browser bundle.

import type { ChatMessage, DiscoveredTool, LlmResponse } from "@/lib/agent-loop";
import { authedFetch } from "@/lib/auth";

export const browserLlmTurn = async (
  messages: ChatMessage[],
  tools: DiscoveredTool[],
): Promise<LlmResponse> => {
  const res = await authedFetch("/api/agent/turn", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ messages, tools }),
  });
  if (!res.ok) throw new Error(`agent turn failed: ${res.status}`);
  return (await res.json()) as LlmResponse;
};
