import { describe, expect, it } from "vitest";
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
});
