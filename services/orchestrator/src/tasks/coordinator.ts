import { randomUUID } from "node:crypto";
import type {
  AssistantAction,
  TaskContext,
  TaskEvent,
  TaskState
} from "@personal-jarvis/contracts";

export interface TaskRecord {
  id: string;
  instruction: string;
  state: TaskState;
  generation: number;
  createdAt: number;
  context?: TaskContext | undefined;
}

export class TaskCoordinator {
  private readonly tasks = new Map<string, TaskRecord>();
  private readonly listeners = new Map<
    string,
    Set<(event: TaskEvent) => void>
  >();
  private readonly pendingConfirmations = new Map<string, Set<string>>();

  create(input: {
    instruction: string;
    context?: TaskContext | undefined;
  }): TaskRecord {
    const task: TaskRecord = {
      id: randomUUID(),
      instruction: input.instruction,
      state: "queued",
      generation: 0,
      createdAt: Date.now(),
      ...(input.context ? { context: input.context } : {})
    };
    this.tasks.set(task.id, task);
    this.emitState(task);
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
    this.emitState(cancelled);
    return cancelled;
  }

  get(taskId: string): TaskRecord {
    return this.requireTask(taskId);
  }

  requestConfirmation(taskId: string, actionId: string): void {
    const task = this.requireTask(taskId);
    const actions = this.pendingConfirmations.get(taskId) ?? new Set();
    actions.add(actionId);
    this.pendingConfirmations.set(taskId, actions);

    const updated: TaskRecord = {
      ...task,
      state: "awaiting_confirmation"
    };
    this.tasks.set(taskId, updated);
    this.emitState(updated);
  }

  confirm(
    taskId: string,
    actionId: string,
    decision: "approve" | "reject"
  ): { actionId: string; decision: "approve" | "reject" } {
    this.requireTask(taskId);
    const actions = this.pendingConfirmations.get(taskId);
    if (!actions?.delete(actionId)) {
      throw new Error("confirmation_not_pending");
    }
    if (actions.size === 0) {
      this.pendingConfirmations.delete(taskId);
    }
    return { actionId, decision };
  }

  subscribe(
    taskId: string,
    listener: (event: TaskEvent) => void
  ): () => void {
    this.requireTask(taskId);
    const taskListeners = this.listeners.get(taskId) ?? new Set();
    taskListeners.add(listener);
    this.listeners.set(taskId, taskListeners);

    return () => {
      taskListeners.delete(listener);
      if (taskListeners.size === 0) {
        this.listeners.delete(taskId);
      }
    };
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

  private emitState(task: TaskRecord): void {
    const event: TaskEvent = {
      eventId: randomUUID(),
      taskId: task.id,
      taskGeneration: task.generation,
      type: "state",
      state: task.state
    };

    for (const listener of this.listeners.get(task.id) ?? []) {
      listener(event);
    }
  }
}
