// A standalone tool registry. It is the single source of truth for the tools an
// agent can call (D-A1). It registers each tool with the browser's
// `document.modelContext` when that API exists (so an external Chrome agent can
// drive the page too), and it exposes listTools()/callTool() for our own
// harness. The harness imports THIS, never the page's internals.

import type { DiscoveredTool, ToolRegistry } from "@/lib/agent-loop";

export interface RegisteredTool {
  name: string;
  description: string;
  parameters: object; // JSON schema
  execute: (args: unknown) => Promise<unknown>;
}

export class ToolRegistryImpl implements ToolRegistry {
  private tools = new Map<string, RegisteredTool>();

  register(tool: RegisteredTool): void {
    this.tools.set(tool.name, tool);

    const mc = (globalThis as { document?: { modelContext?: unknown } }).document
      ?.modelContext as
      | { registerTool?: (def: unknown) => void }
      | undefined;
    if (mc?.registerTool) {
      try {
        mc.registerTool({
          name: tool.name,
          description: tool.description,
          inputSchema: tool.parameters,
          execute: (args: unknown) => tool.execute(args),
        });
      } catch {
        /* best effort — our harness still works without it */
      }
    }
  }

  listTools(): DiscoveredTool[] {
    return [...this.tools.values()].map(({ name, description, parameters }) => ({
      name,
      description,
      parameters,
    }));
  }

  async callTool(name: string, args: unknown): Promise<unknown> {
    const tool = this.tools.get(name);
    if (!tool) throw new Error(`unknown tool: ${name}`);
    return tool.execute(args);
  }
}
