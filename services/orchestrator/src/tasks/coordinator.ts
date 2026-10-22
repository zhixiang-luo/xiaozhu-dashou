import { randomUUID } from "node:crypto";
import type { AssistantAction, TaskState } from "@personal-jarvis/contracts";

export interface TaskRecord {
  id: string;
  instruction: string;
  state: TaskState;
  generation: number;
  createdAt: number;
}

export class TaskCoordinator {
  private readonly tasks = new Map<string, TaskRecord>();

  create(input: { instruction: string }): TaskRecord {
    const task: TaskRecord = {
      id: randomUUID(),
      instruction: input.instruction,
      state: "queued",
      generation: 0,
      createdAt: Date.now()
    };
    this.tasks.set(task.id, task);
    return task;
  }

  cancel(taskId: string): TaskRecord {
    const task = this.requireTask(taskId);
    const cancelled: TaskRecord = {
      ...task,
      state: "cancelled",
      generation: task.generation + 1
    };
    this.tasks.set(taskId, cancelled);
    return cancelled;
  }

  async acceptAction(
    taskId: string,
    action: AssistantAction
  ): Promise<AssistantAction> {
    const task = this.requireTask(taskId);

    if (action.taskId !== taskId || action.taskGeneration !== task.generation) {
      throw new Error("stale_action");
    }

    if (task.state === "cancelled" || task.state === "completed") {
      throw new Error("stale_action");
    }

    return action;
  }

  private requireTask(taskId: string): TaskRecord {
    const task = this.tasks.get(taskId);
    if (!task) {
      throw new Error("task_not_found");
    }
    return task;
  }
}
