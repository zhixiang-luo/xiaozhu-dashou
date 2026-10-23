import { randomUUID } from "node:crypto";
import type { AssistantAction } from "@personal-jarvis/contracts";
import type { TaskRecord } from "./coordinator.js";

export interface TaskPlanner {
  plan(task: TaskRecord): Promise<AssistantAction[]>;
}

export class DevTaskPlanner implements TaskPlanner {
  constructor(
    private readonly options: { allowWechatAutoSend: boolean } = {
      allowWechatAutoSend: false
    }
  ) {}

  async plan(task: TaskRecord): Promise<AssistantAction[]> {
    if (
      task.instruction.includes("文件传输助手") &&
      task.instruction.includes("小猪搭手技能测试")
    ) {
      if (!this.options.allowWechatAutoSend) {
        return Promise.resolve([
          {
            type: "ask_user",
            actionId: randomUUID(),
            taskId: task.id,
            taskGeneration: task.generation,
            expectedState: "user_input",
            timeoutMs: 30_000,
            question:
              "微信自动发送默认关闭。请由开发者在本地明确开启后再执行。"
          }
        ]);
      }
      return Promise.resolve([
        action(task, {
          type: "restart_app",
          expectedState: "wechat.home",
          expectedPackage: "com.tencent.mm",
          timeoutMs: 10_000,
          packageName: "com.tencent.mm"
        }),
        action(task, {
          type: "tap",
          expectedState: "wechat.file_assistant_visible",
          expectedPackage: "com.tencent.mm",
          timeoutMs: 5_000,
          target: { x: 0.32, y: 0.26 }
        }),
        action(task, {
          type: "tap",
          expectedState: "wechat.chat_input_focused",
          expectedPackage: "com.tencent.mm",
          timeoutMs: 5_000,
          target: { x: 0.5, y: 0.93 }
        }),
        action(task, {
          type: "input_text",
          expectedState: "wechat.message_drafted",
          expectedPackage: "com.tencent.mm",
          timeoutMs: 5_000,
          text: "小猪搭手技能测试"
        }),
        action(task, {
          type: "tap",
          expectedState: "wechat.message_sent",
          expectedPackage: "com.tencent.mm",
          timeoutMs: 5_000,
          target: { x: 0.91, y: 0.59 }
        })
      ]);
    }

    const packageName = resolvePackage(task.instruction);
    if (packageName) {
      return Promise.resolve([
        {
          type: "open_app",
          actionId: randomUUID(),
          taskId: task.id,
          taskGeneration: task.generation,
          expectedState: "app.opened",
          timeoutMs: 10_000,
          packageName
        }
      ]);
    }

    return Promise.resolve([
      {
        type: "ask_user",
        actionId: randomUUID(),
        taskId: task.id,
        taskGeneration: task.generation,
        expectedState: "user_input",
        timeoutMs: 30_000,
        question: "这个任务还没有接入开发版规划器，请换一个已支持的指令。"
      }
    ]);
  }
}

function action(
  task: TaskRecord,
  fields: AssistantActionDraft
): AssistantAction {
  return {
    ...fields,
    actionId: randomUUID(),
    taskId: task.id,
    taskGeneration: task.generation
  } as AssistantAction;
}

type EnvelopeField = "actionId" | "taskId" | "taskGeneration";
type DistributiveOmit<T, K extends PropertyKey> = T extends unknown
  ? Omit<T, K>
  : never;
type AssistantActionDraft = DistributiveOmit<AssistantAction, EnvelopeField>;

function resolvePackage(instruction: string): string | undefined {
  const normalized = instruction.trim().toLowerCase();
  if (normalized.includes("设置")) {
    return "com.android.settings";
  }
  if (normalized.includes("微信")) {
    return "com.tencent.mm";
  }
  if (normalized.includes("网易云") || normalized.includes("音乐")) {
    return "com.netease.cloudmusic";
  }
  if (normalized.includes("高德") || normalized.includes("地图")) {
    return "com.autonavi.minimap";
  }
  return undefined;
}
