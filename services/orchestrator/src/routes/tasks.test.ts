import { afterEach, expect, it, vi } from "vitest";
import { TaskCoordinator } from "../tasks/coordinator.js";
import { buildServer } from "../server.js";
import type { TaskPlanner } from "../tasks/planner.js";

afterEach(() => {
  vi.unstubAllEnvs();
});

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

it("uses DEVICE_TOKEN from the environment when no option is supplied", async () => {
  vi.stubEnv("DEVICE_TOKEN", "environment-secret");
  const app = buildServer();
  const response = await app.inject({
    method: "POST",
    url: "/v1/tasks",
    headers: { authorization: "Bearer environment-secret" },
    payload: { instruction: "打开设置" }
  });

  expect(response.statusCode).toBe(201);
  await app.close();
});

it("passes task context to the planner", async () => {
  let receivedContext: unknown;
  const planner: TaskPlanner = {
    plan: async (task) => {
      receivedContext = task.context;
      return [
        {
          type: "ask_user",
          actionId: "context-action",
          taskId: task.id,
          taskGeneration: task.generation,
          expectedState: "user_input",
          timeoutMs: 30_000,
          question: "ok"
        }
      ];
    }
  };
  const app = buildServer({ deviceToken: "secret", planner });
  const response = await app.inject({
    method: "POST",
    url: "/v1/tasks",
    headers: { authorization: "Bearer secret" },
    payload: {
      instruction: "那个会怎么安排",
      context: {
        currentTime: "2026-10-23T05:00:00.000Z",
        timeZone: "Asia/Shanghai",
        upcomingEvents: [],
        recentTasks: [
          {
            summary: "创建和猪哥开会的日程",
            occurredAt: "2026-10-23T04:50:00.000Z"
          }
        ],
        relevantMemory: [],
        contacts: [],
        preferences: []
      }
    }
  });

  expect(response.statusCode).toBe(201);
  expect(receivedContext).toMatchObject({
    recentTasks: [
      {
        summary: "创建和猪哥开会的日程"
      }
    ]
  });
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
    task: {
      instruction: "play music",
      state: "queued"
    }
  });
  await app.close();
});

it("plans opening Android settings", async () => {
  const app = buildServer({ deviceToken: "secret" });
  const response = await app.inject({
    method: "POST",
    url: "/v1/tasks",
    headers: { authorization: "Bearer secret" },
    payload: { instruction: "打开设置" }
  });

  expect(response.statusCode).toBe(201);
  expect(response.json().actions[0]).toMatchObject({
    type: "open_app",
    packageName: "com.android.settings"
  });
  await app.close();
});

it("asks the user when the development planner does not support a task", async () => {
  const app = buildServer({ deviceToken: "secret" });
  const response = await app.inject({
    method: "POST",
    url: "/v1/tasks",
    headers: { authorization: "Bearer secret" },
    payload: { instruction: "帮我做一件还没接入的事情" }
  });

  expect(response.statusCode).toBe(201);
  expect(response.json().actions[0]).toMatchObject({
    type: "ask_user"
  });
  await app.close();
});

it("plans the calibrated WeChat file-assistant message workflow", async () => {
  const app = buildServer({
    deviceToken: "secret",
    allowWechatAutoSend: true
  });
  const response = await app.inject({
    method: "POST",
    url: "/v1/tasks",
    headers: { authorization: "Bearer secret" },
    payload: { instruction: "给文件传输助手发小猪搭手技能测试" }
  });

  expect(response.statusCode).toBe(201);
  expect(response.json().actions).toEqual([
    expect.objectContaining({
      type: "restart_app",
      packageName: "com.tencent.mm"
    }),
    expect.objectContaining({ type: "tap", target: { x: 0.32, y: 0.26 } }),
    expect.objectContaining({ type: "tap", target: { x: 0.5, y: 0.93 } }),
    expect.objectContaining({
      type: "input_text",
      text: "小猪搭手技能测试"
    }),
    expect.objectContaining({ type: "tap", target: { x: 0.91, y: 0.59 } })
  ]);
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
