import { expect, it } from "vitest";
import { TaskCoordinator } from "./coordinator.js";

it("rejects actions from a cancelled generation", async () => {
  const coordinator = new TaskCoordinator();
  const task = coordinator.create({ instruction: "play music" });
  const cancelled = coordinator.cancel(task.id);

  await expect(
    coordinator.acceptAction(task.id, {
      actionId: "a1",
      taskId: task.id,
      taskGeneration: task.generation,
      expectedState: "home",
      timeoutMs: 1000,
      type: "observe"
    })
  ).rejects.toThrow("stale_action");

  expect(cancelled.state).toBe("cancelled");
});
