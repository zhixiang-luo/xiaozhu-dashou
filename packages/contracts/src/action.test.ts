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

  it("accepts a structured calendar event", () => {
    const result = ActionSchema.parse({
      type: "calendar_create_event",
      actionId: "a1",
      taskId: "t1",
      taskGeneration: 1,
      expectedState: "calendar.event_created",
      timeoutMs: 10000,
      title: "和朱哥开会",
      startTimeMillis: 1792825200000,
      endTimeMillis: 1792828800000,
      timeZone: "Asia/Shanghai"
    });

    expect(result.type).toBe("calendar_create_event");
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
