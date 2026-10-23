import { randomUUID } from "node:crypto";
import type { AssistantAction } from "@personal-jarvis/contracts";
import type { TaskRecord } from "./coordinator.js";

export interface TaskPlanner {
  plan(task: TaskRecord): AssistantAction[];
}

export class DevTaskPlanner implements TaskPlanner {
  plan(task: TaskRecord): AssistantAction[] {
    const packageName = resolvePackage(task.instruction);
    if (packageName) {
      return [
        {
          type: "open_app",
          actionId: randomUUID(),
          taskId: task.id,
          taskGeneration: task.generation,
          expectedState: "app.opened",
          timeoutMs: 10_000,
          packageName
        }
      ];
    }

    return [
      {
        type: "ask_user",
        actionId: randomUUID(),
        taskId: task.id,
        taskGeneration: task.generation,
        expectedState: "user_input",
        timeoutMs: 30_000,
        question: "这个任务还没有接入开发版规划器，请换一个已支持的指令。"
      }
    ];
  }
}

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
