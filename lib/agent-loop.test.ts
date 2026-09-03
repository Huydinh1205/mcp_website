import { describe, expect, test, vi } from "vitest";
import {
  runAgentLoop,
  type DiscoveredTool,
  type LlmResponse,
  type LoopEvent,
  type ToolRegistry,
} from "@/lib/agent-loop";

const TOOLS: DiscoveredTool[] = [
  { name: "search_products", description: "find products", parameters: { type: "object" } },
  { name: "submit_offer", description: "make an offer", parameters: { type: "object" } },
];

function fakeRegistry(over: Partial<ToolRegistry> = {}): ToolRegistry {
  return {
    listTools: () => TOOLS,
    callTool: vi.fn(async (name: string) => ({ echo: name })),
    ...over,
  };
}

/** Returns a fake `llmTurn` that yields the queued responses in order. */
function scriptedLlm(responses: LlmResponse[]) {
  const calls: { tools: unknown[] }[] = [];
  let i = 0;
  const fn = vi.fn(async (_messages: unknown[], tools: unknown[]) => {
    calls.push({ tools });
    return responses[Math.min(i++, responses.length - 1)];
  });
  return Object.assign(fn, { calls });
}

const text = (content: string): LlmResponse => ({ content, toolCalls: [] });
const call = (name: string, args: unknown = {}): LlmResponse => ({
  content: null,
  toolCalls: [{ id: `c${name}`, name, arguments: args }],
});

describe("runAgentLoop", () => {
  test("executes a requested tool, feeds the result back, stops on a no-tool reply", async () => {
    const registry = fakeRegistry();
    const llm = scriptedLlm([call("search_products", { query: "keyboard" }), text("done shopping")]);

    const events = await runAgentLoop({
      systemPrompt: "you are a buyer",
      goal: "buy a keyboard",
      registry,
      llmTurn: llm,
    });

    expect(registry.callTool).toHaveBeenCalledWith("search_products", { query: "keyboard" });
    expect(events.map((e) => e.type)).toEqual([
      "tool_call",
      "tool_result",
      "assistant",
      "done",
    ]);
    expect(events.at(-1)).toMatchObject({ type: "done", reason: "no_tool_call" });
  });

  test("unknown tool name -> error event, aborts, never touches the registry", async () => {
    const registry = fakeRegistry();
    const llm = scriptedLlm([call("delete_everything")]);

    const events = await runAgentLoop({
      systemPrompt: "x",
      goal: "y",
      registry,
      llmTurn: llm,
    });

    expect(registry.callTool).not.toHaveBeenCalled();
    expect(events.some((e) => e.type === "error")).toBe(true);
    expect(events.at(-1)).toMatchObject({ type: "done", reason: "error" });
  });

  test("llm turn throws -> error event, loop ends with reason error", async () => {
    const llm = vi.fn(async () => {
      throw new Error("agent turn failed: 401");
    });

    const events = await runAgentLoop({
      systemPrompt: "x",
      goal: "y",
      registry: fakeRegistry(),
      llmTurn: llm,
    });

    expect(events.some((e) => e.type === "error")).toBe(true);
    expect(events.at(-1)).toMatchObject({ type: "done", reason: "error" });
  });

  test("model never stops -> loop ends at maxSteps with reason round_cap", async () => {
    const registry = fakeRegistry();
    const llm = scriptedLlm([call("search_products")]); // always asks for a tool

    const events = await runAgentLoop({
      systemPrompt: "x",
      goal: "y",
      registry,
      llmTurn: llm,
      maxSteps: 3,
    });

    expect(events.at(-1)).toMatchObject({ type: "done", reason: "round_cap" });
    expect(events.filter((e) => e.type === "tool_call")).toHaveLength(3);
  });

  test("passes the discovered tool schemas into every llm turn", async () => {
    const llm = scriptedLlm([text("hi")]);
    await runAgentLoop({ systemPrompt: "x", goal: "y", registry: fakeRegistry(), llmTurn: llm });

    const names = (llm.calls[0].tools as { name: string }[]).map((t) => t.name);
    expect(names).toEqual(["search_products", "submit_offer"]);
  });

  test("streams events through onEvent in the same order as the return value", async () => {
    const seen: LoopEvent[] = [];
    const llm = scriptedLlm([call("submit_offer", { price: 10 }), text("ok")]);

    const events = await runAgentLoop({
      systemPrompt: "x",
      goal: "y",
      registry: fakeRegistry(),
      llmTurn: llm,
      onEvent: (e) => seen.push(e),
    });

    expect(seen).toEqual(events);
  });
});
