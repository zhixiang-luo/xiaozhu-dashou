import { describe, expect, it } from "vitest";
import { TaskContextSchema } from "./task.js";

describe("TaskContextSchema", () => {
  it("accepts calendar and recent task context", () => {
    const context = TaskContextSchema.parse({
      currentTime: "2026-10-23T05:00:00.000Z",
      timeZone: "Asia/Shanghai",
      currentPackage: "com.tencent.mm",
      screenSummary: "文件传输助手",
      upcomingEvents: [
        {
          title: "和猪哥开会",
          startTime: "2026-10-24T15:00:00+08:00",
          endTime: "2026-10-24T16:00:00+08:00",
          timeZone: "Asia/Shanghai"
        }
      ],
      recentTasks: [
        {
          summary: "创建和猪哥开会的日程",
          occurredAt: "2026-10-23T04:50:00.000Z"
        }
      ],
      relevantMemory: [],
      contacts: [{ alias: "朱哥", canonicalName: "猪哥" }],
      preferences: [{ key: "reply_tone", value: "自然、简短" }]
    });

    expect(context.upcomingEvents[0]?.title).toBe("和猪哥开会");
  });

  it("rejects oversized context arrays", () => {
    const upcomingEvents = Array.from({ length: 21 }, (_, index) => ({
      title: `事件${index}`,
      startTime: "2026-10-24T15:00:00+08:00",
      endTime: "2026-10-24T16:00:00+08:00",
      timeZone: "Asia/Shanghai"
    }));

    expect(() =>
      TaskContextSchema.parse({
        currentTime: "2026-10-23T05:00:00.000Z",
        timeZone: "Asia/Shanghai",
        upcomingEvents,
        recentTasks: [],
        relevantMemory: [],
        contacts: [],
        preferences: []
      })
    ).toThrow();
  });

  it("accepts the current WeChat conversation and messages", () => {
    const context = TaskContextSchema.parse({
      currentTime: "2026-10-23T06:30:00.000Z",
      timeZone: "Asia/Shanghai",
      currentPackage: "com.tencent.mm",
      currentConversation: {
        title: "文件传输助手",
        appPackage: "com.tencent.mm",
        capturedAt: "2026-10-23T06:30:00.000Z",
        recentMessages: [
          {
            direction: "incoming",
            text: "明天记得把文件发我"
          }
        ]
      },
      upcomingEvents: [],
      recentTasks: [],
      relevantMemory: [],
      contacts: [],
      preferences: []
    });

    expect(context.currentConversation?.title).toBe("文件传输助手");
    expect(
      context.currentConversation?.recentMessages[0]?.text
    ).toBe("明天记得把文件发我");
  });
});
