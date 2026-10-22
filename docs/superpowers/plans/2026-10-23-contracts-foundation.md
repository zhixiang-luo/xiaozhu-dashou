# Contracts Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 Android、Orchestrator、模型 adapter 和技能共用的版本化协议与自动化测试。

**Architecture:** `@personal-jarvis/contracts` 使用 Zod 定义运行时 schema，并从 schema 推导 TypeScript 类型。动作采用可判别联合，任务事件采用带版本和任务代次的事件包络。所有 schema 同时服务于类型检查、服务端请求验证和测试向量。

**Tech Stack:** Node.js 22、pnpm workspace、TypeScript strict、Zod、Vitest。

**Spec:** `docs/01-prd.md`, `docs/02-system-architecture.md`, `docs/03-agent-and-model-integration.md`

> Implementation status: Completed and verified on 2026-10-23.

## Global Constraints

- Android `minSdk` 为 30。
- Node.js 最低版本为 22.6，仓库使用 `.nvmrc` 固定主版本。
- 协议包名为 `@personal-jarvis/contracts`。
- 任务状态为 `queued`、`observing`、`planning`、`acting`、`verifying`、`awaiting_confirmation`、`completed`、`failed`、`cancelled`。
- 动作只允许 `observe`、`open_app`、`tap`、`swipe`、`input_text`、`key`、`media_control`、`ask_user`、`finish`。
- 风险决定只允许 `allow`、`confirm`、`block`。
- 默认模型路由为 `gpt-6-luna`、`gpt-6.1-sol`、`gpt-6-astra`，但协议不得包含供应商私有字段。
- 凭据字段不得进入动作、观察或长期记忆 schema。

---

### Task 1: Workspace and package scaffold

**Files:**
- Create: `package.json`
- Create: `pnpm-workspace.yaml`
- Create: `tsconfig.base.json`
- Create: `packages/contracts/package.json`
- Create: `packages/contracts/tsconfig.json`
- Create: `packages/contracts/vitest.config.ts`

**Interfaces:**
- Produces: pnpm workspace root and package `@personal-jarvis/contracts`.

- [ ] **Step 1: Create workspace metadata**

```json
{
  "name": "personal-jarvis",
  "private": true,
  "packageManager": "pnpm@11.25.0",
  "scripts": {
    "check:docs": "node scripts/check-docs.mjs",
    "test": "pnpm -r test",
    "typecheck": "pnpm -r typecheck"
  }
}
```

- [ ] **Step 2: Create package metadata**

```json
{
  "name": "@personal-jarvis/contracts",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "exports": {
    ".": "./src/index.ts"
  },
  "scripts": {
    "test": "vitest run",
    "typecheck": "tsc --noEmit"
  }
}
```

- [ ] **Step 3: Install dependencies**

Run: `pnpm add -D typescript zod vitest -w`

Expected: lockfile created; no warnings.

- [ ] **Step 4: Verify workspace exists**

Run: `pnpm -r list --depth -1`

Expected: `@personal-jarvis/contracts` listed once.

### Task 2: Task and action schemas

**Files:**
- Create: `packages/contracts/src/action.ts`
- Create: `packages/contracts/src/task.ts`
- Create: `packages/contracts/src/index.ts`
- Test: `packages/contracts/src/action.test.ts`

**Interfaces:**
- Produces: `TaskStateSchema`, `ActionSchema`, `TaskState`, `AssistantAction`.
- `ActionSchema` parses only the nine allowed action variants.

- [ ] **Step 1: Write the failing tests**

```ts
import { describe, expect, it } from "vitest";
import { ActionSchema } from "./action.js";

describe("ActionSchema", () => {
  it("accepts a normalized tap", () => {
    const result = ActionSchema.parse({
      type: "tap",
      actionId: "a1",
      taskId: "t1",
      taskGeneration: 1,
      expectedState: "wechat.chat",
      timeoutMs: 4000,
      target: { x: 0.5, y: 0.8 }
    });
    expect(result.type).toBe("tap");
  });

  it("rejects unsupported tools", () => {
    expect(() => ActionSchema.parse({
      type: "shell",
      actionId: "a1"
    })).toThrow();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pnpm --filter @personal-jarvis/contracts test`

Expected: FAIL because `action.ts` or `ActionSchema` does not exist.

- [ ] **Step 3: Implement the minimal schemas**

```ts
import { z } from "zod";

const ActionEnvelope = z.object({
  actionId: z.string().min(1),
  taskId: z.string().min(1),
  taskGeneration: z.number().int().nonnegative(),
  expectedState: z.string().min(1),
  timeoutMs: z.number().int().positive().max(30_000)
});

export const ActionSchema = z.discriminatedUnion("type", [
  ActionEnvelope.extend({ type: z.literal("observe") }),
  ActionEnvelope.extend({ type: z.literal("open_app"), packageName: z.string().min(1) }),
  ActionEnvelope.extend({
    type: z.literal("tap"),
    target: z.union([
      z.object({ nodeId: z.string().min(1) }),
      z.object({ x: z.number().min(0).max(1), y: z.number().min(0).max(1) })
    ])
  }),
  ActionEnvelope.extend({
    type: z.literal("swipe"),
    direction: z.enum(["up", "down", "left", "right"]),
    distance: z.number().min(0).max(1)
  }),
  ActionEnvelope.extend({ type: z.literal("input_text"), text: z.string() }),
  ActionEnvelope.extend({ type: z.literal("key"), key: z.string().min(1) }),
  ActionEnvelope.extend({
    type: z.literal("media_control"),
    command: z.enum(["play", "pause", "next", "previous"])
  }),
  ActionEnvelope.extend({ type: z.literal("ask_user"), question: z.string().min(1) }),
  ActionEnvelope.extend({ type: z.literal("finish"), summary: z.string().min(1) })
]);

export type AssistantAction = z.infer<typeof ActionSchema>;
```

- [ ] **Step 4: Run tests**

Run: `pnpm --filter @personal-jarvis/contracts test`

Expected: PASS.

### Task 3: Observation and risk contracts

**Files:**
- Create: `packages/contracts/src/observation.ts`
- Create: `packages/contracts/src/policy.ts`
- Create: `packages/contracts/src/policy.test.ts`
- Modify: `packages/contracts/src/index.ts`

**Interfaces:**
- Produces: `UiNodeSchema`, `ObservationSchema`, `RiskDecisionSchema`.

- [ ] **Step 1: Write the failing risk test**

```ts
import { expect, it } from "vitest";
import { RiskDecisionSchema } from "./policy.js";

it("accepts only allow, confirm, and block", () => {
  expect(RiskDecisionSchema.parse({
    decision: "block",
    reason: "payment_screen",
    actionId: "a1"
  }).decision).toBe("block");
  expect(() => RiskDecisionSchema.parse({
    decision: "maybe",
    reason: "unknown",
    actionId: "a1"
  })).toThrow();
});
```

- [ ] **Step 2: Verify failure**

Run: `pnpm --filter @personal-jarvis/contracts test -- policy.test.ts`

Expected: FAIL because `RiskDecisionSchema` is missing.

- [ ] **Step 3: Implement observation and risk schemas**

```ts
export const RiskDecisionSchema = z.object({
  actionId: z.string().min(1),
  decision: z.enum(["allow", "confirm", "block"]),
  reason: z.string().min(1)
});

export const UiNodeSchema = z.object({
  nodeId: z.string().min(1),
  role: z.string().min(1),
  text: z.string().optional(),
  contentDescription: z.string().optional(),
  bounds: z.object({
    left: z.number(), top: z.number(), right: z.number(), bottom: z.number()
  }),
  clickable: z.boolean(),
  editable: z.boolean()
});

export const ObservationSchema = z.object({
  observationId: z.string().min(1),
  taskId: z.string().min(1),
  foregroundPackage: z.string().min(1),
  pageTitle: z.string().optional(),
  screenHash: z.string().min(1),
  nodes: z.array(UiNodeSchema),
  screenshotRef: z.string().optional()
});
```

- [ ] **Step 4: Run all contract tests**

Run: `pnpm --filter @personal-jarvis/contracts test`

Expected: PASS.

### Task 4: Model provider and task event contracts

**Files:**
- Create: `packages/contracts/src/model.ts`
- Create: `packages/contracts/src/task-event.ts`
- Create: `packages/contracts/src/model.test.ts`
- Modify: `packages/contracts/src/index.ts`

**Interfaces:**
- Produces: `ModelRoleSchema`, `ProviderCapabilitiesSchema`, `TaskEventSchema`.

- [ ] **Step 1: Write failing event tests**

```ts
it("rejects a cancelled task event without generation", () => {
  expect(() => TaskEventSchema.parse({
    eventId: "e1",
    taskId: "t1",
    type: "cancelled"
  })).toThrow();
});

it("accepts all four provider roles", () => {
  expect(ModelRoleSchema.array().parse([
    "planner",
    "vision_grounder",
    "speech_to_text",
    "text_to_speech"
  ])).toHaveLength(4);
});
```

- [ ] **Step 2: Verify failure**

Run: `pnpm --filter @personal-jarvis/contracts test -- model.test.ts`

Expected: FAIL because schemas are missing.

- [ ] **Step 3: Implement model and event contracts**

```ts
export const ModelRoleSchema = z.enum([
  "planner",
  "vision_grounder",
  "speech_to_text",
  "text_to_speech"
]);

export const ProviderCapabilitiesSchema = z.object({
  roles: z.array(ModelRoleSchema),
  supportsToolCalling: z.boolean(),
  supportsStructuredOutput: z.boolean(),
  supportsImages: z.boolean(),
  contextWindow: z.number().int().positive(),
  regions: z.array(z.string())
});

export const TaskEventSchema = z.object({
  eventId: z.string().min(1),
  taskId: z.string().min(1),
  taskGeneration: z.number().int().nonnegative(),
  type: z.enum(["state", "observation", "action", "confirmation", "result"])
});
```

- [ ] **Step 4: Run full package verification**

Run: `pnpm --filter @personal-jarvis/contracts test && pnpm --filter @personal-jarvis/contracts typecheck`

Expected: both commands PASS.

- [ ] **Step 5: Commit**

```bash
git add package.json pnpm-workspace.yaml tsconfig.base.json pnpm-lock.yaml packages/contracts
git commit -m "feat(contracts): add versioned agent protocols"
```
