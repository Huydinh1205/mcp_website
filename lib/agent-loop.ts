// Tool-agnostic agent harness (D-A1, D-A5). It discovers tools from a registry
// (in the browser: document.modelContext), runs a hand-rolled chat-completions
// loop through an injected `llmTurn` (in the app: POST /api/agent/turn), and
// executes tool calls back through the registry. It imports nothing from the
// marketplace pages. Deterministic and fully unit-testable via fakes.

export interface DiscoveredTool {
  name: string;
  description: string;
  parameters: object; // JSON schema
}

export interface ToolRegistry {
  listTools(): DiscoveredTool[];
  callTool(name: string, args: unknown): Promise<unknown>;
}

export interface LlmToolCall {
  id: string;
  name: string;
  arguments: unknown;
}

export interface LlmResponse {
  content: string | null;
  toolCalls: LlmToolCall[];
}

export type LlmTurn = (
  messages: ChatMessage[],
  tools: DiscoveredTool[],
) => Promise<LlmResponse>;

export interface ChatMessage {
  role: "system" | "user" | "assistant" | "tool";
  content: string;
  toolCallId?: string;
  /** Set on an assistant message that triggered tool calls — the subsequent
   *  "tool" messages' toolCallId refer back into this list. Required by
   *  providers (Gemini's OpenAI-compat layer included) to resolve which
   *  function a tool result belongs to. */
  toolCalls?: LlmToolCall[];
  /** Set on a "tool" message: the name of the tool that produced it. */
  name?: string;
}

export type LoopEvent =
  | { type: "tool_call"; name: string; args: unknown }
  | { type: "tool_result"; name: string; result: unknown }
  | { type: "assistant"; content: string }
  | { type: "error"; message: string }
  | { type: "done"; reason: "no_tool_call" | "round_cap" | "error" };

export interface RunOptions {
  systemPrompt: string;
  goal: string;
  registry: ToolRegistry;
  llmTurn: LlmTurn;
  /** Hard stop on the number of llm turns. Default 8 (≈ ROUND_CAP rounds x2 + slack). */
  maxSteps?: number;
  onEvent?: (event: LoopEvent) => void;
}

export async function runAgentLoop(opts: RunOptions): Promise<LoopEvent[]> {
  const { systemPrompt, goal, registry, llmTurn, onEvent } = opts;
  const maxSteps = opts.maxSteps ?? 8;

  const tools = registry.listTools();
  const known = new Set(tools.map((t) => t.name));
  const events: LoopEvent[] = [];
  const emit = (event: LoopEvent) => {
    events.push(event);
    onEvent?.(event);
  };

  const messages: ChatMessage[] = [
    { role: "system", content: systemPrompt },
    { role: "user", content: goal },
  ];

  for (let step = 0; step < maxSteps; step++) {
    let response: LlmResponse;
    try {
      response = await llmTurn(messages, tools);
    } catch (e) {
      // A failed turn (network, 401 from a stale token, upstream 5xx) must surface
      // as an event — otherwise the caller's `await` just rejects and the UI shows
      // nothing at all.
      emit({
        type: "error",
        message: `llm turn failed: ${e instanceof Error ? e.message : String(e)}`,
      });
      emit({ type: "done", reason: "error" });
      return events;
    }

    if (response.toolCalls.length === 0) {
      if (response.content) {
        emit({ type: "assistant", content: response.content });
        messages.push({ role: "assistant", content: response.content });
      }
      emit({ type: "done", reason: "no_tool_call" });
      return events;
    }

    messages.push({
      role: "assistant",
      content: response.content ?? "",
      toolCalls: response.toolCalls,
    });

    for (const tc of response.toolCalls) {
      if (!known.has(tc.name)) {
        emit({ type: "error", message: `unknown tool: ${tc.name}` });
        emit({ type: "done", reason: "error" });
        return events;
      }

      emit({ type: "tool_call", name: tc.name, args: tc.arguments });
      let result: unknown;
      try {
        result = await registry.callTool(tc.name, tc.arguments);
      } catch (e) {
        emit({
          type: "error",
          message: `tool ${tc.name} threw: ${e instanceof Error ? e.message : String(e)}`,
        });
        emit({ type: "done", reason: "error" });
        return events;
      }
      emit({ type: "tool_result", name: tc.name, result });
      messages.push({
        role: "tool",
        toolCallId: tc.id,
        name: tc.name,
        content: JSON.stringify(result ?? null),
      });
    }
  }

  emit({ type: "done", reason: "round_cap" });
  return events;
}
