import { describe, expect, it } from "vitest";
import type { TaskContext } from "@personal-jarvis/contracts";
import type { TaskRecord } from "../tasks/coordinator.js";
import { loadDefaultSkillCatalog } from "./catalog.js";
import { translateIntent } from "./translate-intent.js";

const task: TaskRecord = {
  id: "task-1",
  instruction: "帮我到文件传输助手里面去发一句话，测试成功了",
  generation: 0,
  state: "queued",
  createdAt: 1
};

describe("translateIntent", () => {
  it("extracts an arbitrary ordinary message for File Transfer Assistant", async () => {
    const actions = await translateIntent(
      task,
      {
        kind: "execute",
        skillId: "wechat.send-message",
        arguments: {
          contactName: "文件传输助手",
          message: "测试成功了"
        }
      },
      loadDefaultSkillCatalog(),
      { allowWechatAutoSend: true }
    );

    expect(actions).toHaveLength(5);
    expect(actions[0]).toMatchObject({
      type: "restart_app",
      packageName: "com.tencent.mm"
    });
    expect(actions[3]).toMatchObject({
      type: "input_text",
      text: "测试成功了"
    });
    expect(actions[4]).toMatchObject({
      type: "tap",
      target: { x: 0.91, y: 0.59 }
    });
  });

  it("asks for clarification instead of clicking an uncalibrated contact", async () => {
    const actions = await translateIntent(
      task,
      {
        kind: "execute",
        skillId: "wechat.send-message",
        arguments: {
          contactName: "张三",
          message: "你好"
        }
      },
      loadDefaultSkillCatalog(),
      { allowWechatAutoSend: true }
    );

    expect(actions).toHaveLength(1);
    expect(actions[0]).toMatchObject({ type: "ask_user" });
  });

  it("rejects unknown skills", async () => {
    const actions = await translateIntent(
      task,
      {
        kind: "execute",
        skillId: "unknown.skill",
        arguments: {}
      },
      loadDefaultSkillCatalog(),
      { allowWechatAutoSend: true }
    );

    expect(actions[0]).toMatchObject({ type: "ask_user" });
  });

  it("builds an explicit current-conversation reply", async () => {
    const actions = await translateIntent(
      {
        ...task,
        instruction: "我不知道怎么回，帮我直接回一下这段对话",
        context: wechatContext()
      },
      {
        kind: "execute",
        skillId: "wechat.send-message",
        arguments: {
          contactName: "文件传输助手",
          message: "这份文件我看到了，晚点给你回复。"
        }
      },
      loadDefaultSkillCatalog(),
      { allowWechatAutoSend: true }
    );

    expect(actions).toEqual([
      expect.objectContaining({
        type: "wechat_reply_current",
        conversationTitle: "文件传输助手",
        message: "这份文件我看到了，晚点给你回复。"
      })
    ]);
  });

  it("does not use the reply path without explicit reply language", async () => {
    const context = wechatContext();
    const actions = await translateIntent(
      {
        ...task,
        instruction: "帮我发一句测试",
        context: {
          ...context,
          currentConversation: {
            ...context.currentConversation!,
            title: "张三"
          }
        }
      },
      {
        kind: "execute",
        skillId: "wechat.send-message",
        arguments: {
          contactName: "张三",
          message: "测试"
        }
      },
      loadDefaultSkillCatalog(),
      { allowWechatAutoSend: true }
    );

    expect(actions).toHaveLength(1);
    expect(actions[0]).toMatchObject({ type: "ask_user" });
  });

  it("blocks automatic WeChat sending by default", async () => {
    const actions = await translateIntent(
      task,
      {
        kind: "execute",
        skillId: "wechat.send-message",
        arguments: {
          contactName: "文件传输助手",
          message: "测试成功了"
        }
      },
      loadDefaultSkillCatalog(),
      { allowWechatAutoSend: false }
    );

    expect(actions).toHaveLength(1);
    expect(actions[0]).toMatchObject({ type: "ask_user" });
  });

  it("turns natural-language calendar arguments into epoch milliseconds", async () => {
    const actions = await translateIntent(
      {
        ...task,
        instruction: "明天下午三点帮我创建一个和朱哥开会的日程"
      },
      {
        kind: "execute",
        skillId: "calendar.create-event",
        arguments: {
          title: "和朱哥开会",
          startTime: "2026-10-24T15:00:00+08:00",
          durationMinutes: 60,
          timeZone: "Asia/Shanghai"
        }
      },
      loadDefaultSkillCatalog(),
      {
        allowWechatAutoSend: false,
        nowMillis: Date.parse("2026-10-23T08:00:00+08:00")
      }
    );

    expect(actions).toEqual([
      expect.objectContaining({
        type: "calendar_create_event",
        title: "和朱哥开会",
        startTimeMillis: Date.parse("2026-10-24T15:00:00+08:00"),
        endTimeMillis: Date.parse("2026-10-24T16:00:00+08:00"),
        timeZone: "Asia/Shanghai"
      })
    ]);
  });

  it("rejects an invalid calendar duration", async () => {
    const actions = await translateIntent(
      task,
      {
        kind: "execute",
        skillId: "calendar.create-event",
        arguments: {
          title: "超长会议",
          startTime: "2099-10-24T15:00:00+08:00",
          durationMinutes: 1_500,
          timeZone: "Asia/Shanghai"
        }
      },
      loadDefaultSkillCatalog(),
      { allowWechatAutoSend: false }
    );

    expect(actions[0]).toMatchObject({ type: "ask_user" });
  });

  it("builds a calendar list action", async () => {
    const actions = await translateIntent(
      task,
      {
        kind: "execute",
        skillId: "calendar.list-events",
        arguments: {
          rangeStart: "2026-10-24T00:00:00+08:00",
          rangeEnd: "2026-10-25T00:00:00+08:00",
          titleQuery: "朱哥"
        }
      },
      loadDefaultSkillCatalog(),
      {
        allowWechatAutoSend: false,
        nowMillis: Date.parse("2026-10-23T08:00:00+08:00")
      }
    );

    expect(actions[0]).toMatchObject({
      type: "calendar_list_events",
      rangeStartMillis: Date.parse("2026-10-24T00:00:00+08:00"),
      rangeEndMillis: Date.parse("2026-10-25T00:00:00+08:00"),
      titleQuery: "朱哥"
    });
  });

  it("builds a calendar update action", async () => {
    const actions = await translateIntent(
      task,
      {
        kind: "execute",
        skillId: "calendar.update-event",
        arguments: {
          titleQuery: "朱哥开会",
          rangeStart: "2026-10-24T00:00:00+08:00",
          rangeEnd: "2026-10-25T00:00:00+08:00",
          newTitle: "和朱哥改期会议",
          newStartTime: "2026-10-24T16:00:00+08:00",
          newDurationMinutes: 90
        }
      },
      loadDefaultSkillCatalog(),
      {
        allowWechatAutoSend: false,
        nowMillis: Date.parse("2026-10-23T08:00:00+08:00")
      }
    );

    expect(actions[0]).toMatchObject({
      type: "calendar_update_event",
      titleQuery: "朱哥开会",
      newTitle: "和朱哥改期会议",
      newStartTimeMillis: Date.parse("2026-10-24T16:00:00+08:00"),
      newDurationMinutes: 90
    });
  });

  it("builds a calendar delete action", async () => {
    const actions = await translateIntent(
      task,
      {
        kind: "execute",
        skillId: "calendar.delete-event",
        arguments: {
          titleQuery: "朱哥开会",
          rangeStart: "2026-10-24T00:00:00+08:00",
          rangeEnd: "2026-10-25T00:00:00+08:00"
        }
      },
      loadDefaultSkillCatalog(),
      {
        allowWechatAutoSend: false,
        nowMillis: Date.parse("2026-10-23T08:00:00+08:00")
      }
    );

    expect(actions[0]).toMatchObject({
      type: "calendar_delete_event",
      titleQuery: "朱哥开会"
    });
  });
});

function wechatContext(): TaskContext {
  return {
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
  };
}
