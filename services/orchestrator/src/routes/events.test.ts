import { expect, it } from "vitest";
import { TaskCoordinator } from "../tasks/coordinator.js";
import { buildServer } from "../server.js";

it("streams a cancellation event to an authorized websocket", async () => {
  const coordinator = new TaskCoordinator();
  const task = coordinator.create({ instruction: "reply to Alice" });
  const app = buildServer({ deviceToken: "secret", coordinator });
  await app.ready();

  const socket = await app.injectWS(`/v1/tasks/${task.id}/events`, {
    headers: { authorization: "Bearer secret" }
  });

  const message = new Promise<string>((resolve) => {
    socket.once("message", (data: { toString(): string }) =>
      resolve(data.toString())
    );
  });
  coordinator.cancel(task.id);

  expect(JSON.parse(await message)).toMatchObject({
    taskId: task.id,
    taskGeneration: 1,
    type: "state",
    state: "cancelled"
  });

  socket.terminate();
  await app.close();
});

it("accepts a confirmation for an existing task", async () => {
  const coordinator = new TaskCoordinator();
  const task = coordinator.create({ instruction: "send hello" });
  coordinator.requestConfirmation(task.id, "action-1");
  const app = buildServer({ deviceToken: "secret", coordinator });

  const response = await app.inject({
    method: "POST",
    url: `/v1/tasks/${task.id}/confirmation`,
    headers: { authorization: "Bearer secret" },
    payload: {
      actionId: "action-1",
      decision: "approve"
    }
  });

  expect(response.statusCode).toBe(200);
  expect(response.json()).toEqual({
    actionId: "action-1",
    decision: "approve"
  });

  await app.close();
});

it("rejects a confirmation that was not requested for the task", async () => {
  const coordinator = new TaskCoordinator();
  const task = coordinator.create({ instruction: "send hello" });
  const app = buildServer({ deviceToken: "secret", coordinator });

  const response = await app.inject({
    method: "POST",
    url: `/v1/tasks/${task.id}/confirmation`,
    headers: { authorization: "Bearer secret" },
    payload: {
      actionId: "unknown-action",
      decision: "approve"
    }
  });

  expect(response.statusCode).toBe(409);
  await app.close();
});

it("rejects an unauthorized websocket handshake", async () => {
  const coordinator = new TaskCoordinator();
  const task = coordinator.create({ instruction: "reply to Alice" });
  const app = buildServer({ deviceToken: "secret", coordinator });
  await app.ready();

  await expect(
    app.injectWS(`/v1/tasks/${task.id}/events`)
  ).rejects.toThrow();

  await app.close();
});
