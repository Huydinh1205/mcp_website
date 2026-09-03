// Shared guard around the browser's `document.modelContext.registerTool` (the
// experimental WebMCP API). It's called from several places — StorefrontTools
// mounts once in the root layout, buyer/seller registries get rebuilt on every
// "Start agent" click — and the native API throws InvalidStateError if the same
// tool name is registered twice. We register each name once per page load and
// swallow the (often async) rejection so it doesn't surface as an unhandled
// promise rejection in the console.

export interface NativeToolDef {
  name: string;
  description: string;
  inputSchema: object;
  execute: (args: unknown) => Promise<unknown>;
}

const registeredNames = new Set<string>();

export function registerNativeTool(def: NativeToolDef): void {
  const mc = (globalThis as { document?: { modelContext?: unknown } }).document
    ?.modelContext as { registerTool?: (d: unknown) => unknown } | undefined;
  if (!mc?.registerTool || registeredNames.has(def.name)) return;

  registeredNames.add(def.name);
  try {
    const result = mc.registerTool(def);
    if (result && typeof (result as Promise<unknown>).catch === "function") {
      (result as Promise<unknown>).catch(() => {
        registeredNames.delete(def.name);
      });
    }
  } catch {
    registeredNames.delete(def.name);
  }
}
