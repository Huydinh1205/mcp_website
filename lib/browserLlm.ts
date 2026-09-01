// Client-safe LlmTurn: delegates to the server proxy so the OpenAI SDK (and the
// API key) never enter the browser bundle.

import type { ChatMessage, DiscoveredTool, LlmResponse } from "@/lib/agent-loop";
import { API_BASE } from "@/lib/api";

export const browserLlmTurn = async (
  messages: ChatMessage[],
  tools: DiscoveredTool[],
): Promise<LlmResponse> => {
  const res = await fetch(`${API_BASE}/api/agent/turn`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ messages, tools }),
  });
  if (!res.ok) {
    throw new Error(`agent turn failed: ${res.status}`);
  }
  return (await res.json()) as LlmResponse;
};
