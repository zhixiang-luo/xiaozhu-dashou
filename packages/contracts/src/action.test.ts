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

  it("accepts a controlled app restart before a calibrated workflow", () => {
    const result = ActionSchema.parse({
      type: "restart_app",
      actionId: "a1",
      taskId: "t1",
      taskGeneration: 1,
      expectedState: "wechat.home",
      expectedPackage: "com.tencent.mm",
      timeoutMs: 10000,
      packageName: "com.tencent.mm"
    });

    expect(result.type).toBe("restart_app");
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
