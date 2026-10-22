import { expect, it } from "vitest";
import { TaskCoordinator } from "../tasks/coordinator.js";
import { buildServer } from "../server.js";

it("requires a device token for task routes", async () => {
  const app = buildServer({ deviceToken: "secret" });
  const response = await app.inject({
    method: "POST",
    url: "/v1/tasks",
    payload: { instruction: "play music" }
  });

  expect(response.statusCode).toBe(401);
  await app.close();
});

it("creates a queued task for an authorized device", async () => {
  const app = buildServer({ deviceToken: "secret" });
  const response = await app.inject({
    method: "POST",
    url: "/v1/tasks",
    headers: { authorization: "Bearer secret" },
    payload: { instruction: "play music" }
  });

  expect(response.statusCode).toBe(201);
  expect(response.json()).toMatchObject({
    instruction: "play music",
    state: "queued"
  });
  await app.close();
});

it("cancels an existing task", async () => {
  const coordinator = new TaskCoordinator();
  const task = coordinator.create({ instruction: "reply to Alice" });
  const app = buildServer({ deviceToken: "secret", coordinator });
  const response = await app.inject({
    method: "POST",
    url: `/v1/tasks/${task.id}/cancel`,
    headers: { authorization: "Bearer secret" }
  });

  expect(response.statusCode).toBe(200);
  expect(response.json()).toMatchObject({
    id: task.id,
    state: "cancelled",
    generation: 1
  });
  await app.close();
});
