import { expect, it } from "vitest";
import type { PlanRequest } from "../../model/provider.js";
import { OpenAIProvider } from "./adapter.js";

const basePlanRequest: PlanRequest = {
  taskId: "task-1",
  taskGeneration: 1,
  role: "planner",
  instruction: "Open the music app",
  model: "gpt-6.1-sol"
};

it("rejects a malformed model action", async () => {
  const provider = new OpenAIProvider({
    apiKey: "test-key",
    baseUrl: "https://example.invalid",
    fetchImpl: async () =>
      new Response(
        JSON.stringify({
          output_text: JSON.stringify({ type: "shell", actionId: "a1" })
        }),
        { status: 200, headers: { "content-type": "application/json" } }
      )
  });

  await expect(provider.plan(basePlanRequest)).rejects.toThrow(
    "invalid_action"
  );
});
