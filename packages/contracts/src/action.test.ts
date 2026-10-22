import { describe, expect, it } from "vitest";
import { ActionSchema } from "./action.js";

describe("ActionSchema", () => {
  it("accepts a normalized tap", () => {
    const result = ActionSchema.parse({
      type: "tap",
      actionId: "a1",
      taskId: "t1",
      taskGeneration: 1,
      expectedState: "wechat.chat",
      timeoutMs: 4000,
      target: { x: 0.5, y: 0.8 }
    });

    expect(result.type).toBe("tap");
  });

  it("rejects unsupported tools", () => {
    expect(() =>
      ActionSchema.parse({
        type: "shell",
        actionId: "a1"
      })
    ).toThrow();
  });
});
