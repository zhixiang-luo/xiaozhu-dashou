import { describe, expect, it } from "vitest";
import type { AssistantIntent } from "../model/intent.js";
import { loadDefaultSkillCatalog } from "../skills/catalog.js";
import type { TaskRecord } from "./coordinator.js";
import { DeepSeekTaskPlanner } from "./deepseek-planner.js";

function task(instruction: string): TaskRecord {
  return {
    id: "task-1",
    instruction,
    generation: 0,
    state: "queued",
    createdAt: 1
  };
}

describe("DeepSeekTaskPlanner", () => {
  it("uses the model intent to build the phone action sequence", async () => {
    const intent: AssistantIntent = {
      kind: "execute",
      skillId: "wechat.send-message",
      arguments: {
        contactName: "文件传输助手",
        message: "测试成功了"
      }
    };
    const planner = new DeepSeekTaskPlanner(
      { infer: async () => intent },
      loadDefaultSkillCatalog(),
      undefined,
      { allowWechatAutoSend: true }
    );

    const actions = await planner.plan(
      task("帮我到文件传输助手里面去发一句话，测试成功了")
    );

    expect(actions[3]).toMatchObject({
      type: "input_text",
      text: "测试成功了"
    });
  });

  it("falls back to the development planner when the model fails", async () => {
    const planner = new DeepSeekTaskPlanner({
      infer: async () => {
        throw new Error("timeout");
      }
    }, loadDefaultSkillCatalog());

    const actions = await planner.plan(task("打开设置"));

    expect(actions[0]).toMatchObject({
      type: "open_app",
      packageName: "com.android.settings"
    });
  });

  it("falls back when the model marks a built-in command as unsupported", async () => {
    const planner = new DeepSeekTaskPlanner({
      infer: async () => ({
        kind: "unsupported",
        reason: "技能目录里没有设置"
      })
    }, loadDefaultSkillCatalog());

    const actions = await planner.plan(task("打开设置"));

    expect(actions[0]).toMatchObject({
      type: "open_app",
      packageName: "com.android.settings"
    });
  });
});
