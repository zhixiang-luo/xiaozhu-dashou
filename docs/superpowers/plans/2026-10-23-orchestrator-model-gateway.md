# Orchestrator and Model Gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立接收手机观察、调用默认多模型路由并返回经 schema 验证动作的云端服务。

**Architecture:** Fastify 提供设备认证后的任务创建和 WebSocket 事件流。`TaskCoordinator` 维护状态与预算，`ModelGateway` 通过 `ModelProvider` 选择 `gpt-6-luna`、`gpt-6.1-sol` 或 `gpt-6-astra`，OpenAI adapter 只负责格式转换。所有动作在返回前通过共享 schema。

**Tech Stack:** TypeScript、Fastify、WebSocket、PostgreSQL、OpenAI Responses API、Vitest。

**Spec:** `docs/02-system-architecture.md`, `docs/03-agent-and-model-integration.md`, `docs/model-compatibility.md`

> Implementation status: Completed and verified on 2026-10-23.

## Global Constraints

- API Key 只来自服务端环境变量 `OPENAI_API_KEY`。
- 默认主模型 `gpt-6.1-sol`，快速路由 `gpt-6-luna`，复杂恢复 `gpt-6-astra`。
- 模型响应不合法时最多纠正一次；失败后返回 `needs_human`。
- 任务最多 20 步或 90 秒。
- 取消后的 `taskGeneration` 动作一律拒绝。
- 服务端不直接控制手机。

---

### Task 1: Service skeleton

**Files:**
- Create: `services/orchestrator/package.json`
- Create: `services/orchestrator/tsconfig.json`
- Create: `services/orchestrator/src/server.ts`
- Test: `services/orchestrator/src/server.test.ts`

**Interfaces:**
- Produces: `GET /health` returning `{ status: "ok" }`.

- [ ] **Step 1: Write the failing health test**

```ts
import { expect, it } from "vitest";
import { buildServer } from "./server.js";

it("reports health", async () => {
  const app = buildServer();
  const response = await app.inject({ method: "GET", url: "/health" });
  expect(response.statusCode).toBe(200);
  expect(response.json()).toEqual({ status: "ok" });
  await app.close();
});
```

- [ ] **Step 2: Verify failure**

Run: `pnpm --filter @personal-jarvis/orchestrator test`

Expected: FAIL because the service and `buildServer` do not exist.

- [ ] **Step 3: Implement the minimal server**

```ts
import Fastify from "fastify";

export function buildServer() {
  const app = Fastify({ logger: false });
  app.get("/health", async () => ({ status: "ok" }));
  return app;
}
```

- [ ] **Step 4: Verify pass**

Run: `pnpm --filter @personal-jarvis/orchestrator test`

Expected: PASS.

### Task 2: Provider gateway and deterministic routing

**Files:**
- Create: `services/orchestrator/src/model/provider.ts`
- Create: `services/orchestrator/src/model/gateway.ts`
- Create: `services/orchestrator/src/model/router.ts`
- Test: `services/orchestrator/src/model/router.test.ts`

**Interfaces:**
- Produces: `selectModel({ role, attempt, taskType }): ModelRoute`.
- `ModelRoute` is `{ providerId, model, reasoningEffort }`.

- [ ] **Step 1: Write routing tests**

```ts
import { expect, it } from "vitest";
import { selectModel } from "./router.js";

it("uses the cheap model for wake intent", () => {
  expect(selectModel({ role: "planner", attempt: 0, taskType: "intent" }))
    .toEqual({ providerId: "openai", model: "gpt-6-luna", reasoningEffort: "none" });
});

it("uses recovery after two failures", () => {
  expect(selectModel({ role: "planner", attempt: 2, taskType: "ui" }).model)
    .toBe("gpt-6-astra");
});
```

- [ ] **Step 2: Verify failure**

Run: `pnpm --filter @personal-jarvis/orchestrator test -- router.test.ts`

Expected: FAIL because the router does not exist.

- [ ] **Step 3: Implement the router**

```ts
export function selectModel(input: {
  role: "planner" | "vision_grounder";
  attempt: number;
  taskType: "intent" | "ui";
}) {
  if (input.taskType === "intent") {
    return { providerId: "openai", model: "gpt-6-luna", reasoningEffort: "none" };
  }
  if (input.attempt >= 2) {
    return { providerId: "openai", model: "gpt-6-astra", reasoningEffort: "high" };
  }
  return { providerId: "openai", model: "gpt-6.1-sol", reasoningEffort: "medium" };
}
```

- [ ] **Step 4: Verify pass**

Run: `pnpm --filter @personal-jarvis/orchestrator test -- router.test.ts`

Expected: PASS.

### Task 3: OpenAI adapter with injected transport

**Files:**
- Create: `services/orchestrator/src/providers/openai/adapter.ts`
- Create: `services/orchestrator/src/providers/openai/adapter.test.ts`
- Create: `services/orchestrator/src/providers/openai/README.md`

**Interfaces:**
- `OpenAIProvider` implements `ModelProvider`.
- Constructor consumes `{ apiKey, fetchImpl, baseUrl }`.
- `plan(request)` returns an `AssistantAction[]` parsed by shared schema.

- [ ] **Step 1: Write failing adapter test**

```ts
it("rejects a malformed model action", async () => {
  const provider = new OpenAIProvider({
    apiKey: "test",
    baseUrl: "https://example.invalid",
    fetchImpl: async () => new Response(JSON.stringify({
      output: [{ type: "message", content: [{ type: "output_text", text: "{\"type\":\"shell\"}" }] }]
    }), { status: 200 })
  });
  await expect(provider.plan(basePlanRequest)).rejects.toThrow("invalid_action");
});
```

- [ ] **Step 2: Verify failure**

Run: `pnpm --filter @personal-jarvis/orchestrator test -- adapter.test.ts`

Expected: FAIL because adapter is missing.

- [ ] **Step 3: Implement request, error mapping, and schema validation**

The adapter sends `POST {baseUrl}/responses` with the selected model, a strict JSON response schema, compact observation, and tool definitions from `@personal-jarvis/contracts`. It maps network failures to `provider_unavailable`, 429 to `provider_rate_limited`, and invalid output to `invalid_action`. It never logs the API key or raw screenshot body.

- [ ] **Step 4: Verify pass**

Run: `pnpm --filter @personal-jarvis/orchestrator test -- adapter.test.ts`

Expected: PASS.

### Task 4: Task state, cancellation, and WebSocket events

**Files:**
- Create: `services/orchestrator/src/tasks/coordinator.ts`
- Create: `services/orchestrator/src/routes/tasks.ts`
- Create: `services/orchestrator/src/tasks/coordinator.test.ts`
- Create: `services/orchestrator/src/routes/tasks.test.ts`

**Interfaces:**
- Produces: `POST /v1/tasks`, `WS /v1/tasks/:id`, `POST /v1/tasks/:id/confirmation`.
- `cancel(taskId)` increments generation and rejects old actions.

- [ ] **Step 1: Write cancellation test**

```ts
it("rejects actions from a cancelled generation", async () => {
  const coordinator = new TaskCoordinator();
  const task = coordinator.create({ instruction: "play music" });
  const cancelled = coordinator.cancel(task.id);
  await expect(coordinator.acceptAction(task.id, {
    actionId: "a1",
    taskId: task.id,
    taskGeneration: task.generation,
    expectedState: "home",
    timeoutMs: 1000,
    type: "observe"
  })).rejects.toThrow("stale_action");
  expect(cancelled.state).toBe("cancelled");
});
```

- [ ] **Step 2: Verify failure**

Run: `pnpm --filter @personal-jarvis/orchestrator test -- coordinator.test.ts`

Expected: FAIL because coordinator is missing.

- [ ] **Step 3: Implement coordinator, routes, auth hook, and event broadcast**

Use a `Map` for the first version and an in-memory event bus behind interfaces. Add a PostgreSQL adapter only after these contracts pass. Require bearer auth on `/v1/*`. Enforce 20 actions and 90 seconds per task.

- [ ] **Step 4: Run all service tests**

Run: `pnpm --filter @personal-jarvis/orchestrator test && pnpm --filter @personal-jarvis/orchestrator typecheck`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add services/orchestrator pnpm-lock.yaml
git commit -m "feat(orchestrator): add task API and model gateway"
```
