# DeepSeek 意图规划器 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 DeepSeek 把自然语言拆成受校验的微信发送技能调用。

**Architecture:** DeepSeek client 只返回 Intent Schema；SkillCatalog 和本地 translator 生成确定性动作；失败时回退 DevTaskPlanner。

**Tech Stack:** TypeScript、Fastify、zod、Vitest、DeepSeek OpenAI-compatible HTTP API。

**Spec:** `docs/superpowers/specs/2026-10-23-deepseek-intent-planner-design.md`

## Global Constraints

- Key 只来自服务端 `DEEPSEEK_API_KEY`。
- 模型输出不得直接包含 Android 坐标。
- 首个自动执行目标是“文件传输助手”任意普通消息。
- 超时 10 秒，失败回退开发版规划器。
- 不记录用户正文和消息参数。

### Task 1: Intent schema and DeepSeek client

**Files:**
- Create: `services/orchestrator/src/model/intent.ts`
- Create: `services/orchestrator/src/providers/deepseek/client.ts`
- Create: `services/orchestrator/src/providers/deepseek/client.test.ts`

**Interfaces:**
- Produces: `AssistantIntentSchema`
- Produces: `DeepSeekIntentClient.infer(instruction, catalog): Promise<AssistantIntent>`

- [ ] **Step 1: Write failing tests for a valid intent and invalid JSON**

Assert `execute` parses with `skillId` and arguments; assert malformed JSON throws
`ProviderError("invalid_action", ...)`.

- [ ] **Step 2: Run the focused test and confirm failure**

Run: `../../node_modules/.bin/vitest run src/providers/deepseek/client.test.ts`
Expected: fail because client/schema do not exist.

- [ ] **Step 3: Implement schema and client**

POST to `/chat/completions` with model `deepseek-chat`, temperature `0`, JSON response
format, bearer key, and 10-second timeout.

- [ ] **Step 4: Run focused tests**

Expected: all pass.

### Task 2: Skill catalog and WeChat translator

**Files:**
- Create: `services/orchestrator/src/skills/catalog.ts`
- Create: `services/orchestrator/src/skills/translate-intent.ts`
- Create: `services/orchestrator/src/skills/translate-intent.test.ts`
- Modify: `services/orchestrator/package.json`

**Interfaces:**
- Consumes: `AssistantIntent`.
- Produces: `translateIntent(task, intent, catalog): Promise<AssistantAction[]>`.

- [ ] **Step 1: Write failing test for “测试成功了”**

Assert the translated plan contains `open_app`, file-assistant tap, input focus,
`input_text` with exact message, and send tap.

- [ ] **Step 2: Run test and confirm failure**

Expected: translator module missing.

- [ ] **Step 3: Implement manifest-backed catalog and translator**

Read the four `manifest.yaml` files through `@personal-jarvis/skills`.
Reject unknown skills, missing fields, long messages and sensitive content.

- [ ] **Step 4: Run translator tests**

Expected: all pass.

### Task 3: Async planner wiring and fallback

**Files:**
- Modify: `services/orchestrator/src/tasks/planner.ts`
- Modify: `services/orchestrator/src/server.ts`
- Modify: `services/orchestrator/src/routes/tasks.test.ts`
- Create: `services/orchestrator/src/tasks/deepseek-planner.ts`
- Create: `services/orchestrator/src/tasks/deepseek-planner.test.ts`
- Modify: `.env.example`

**Interfaces:**
- Produces: `DeepSeekTaskPlanner`.
- Produces: `createTaskPlanner(environment)`.

- [ ] **Step 1: Write failing tests for model success and timeout fallback**

Success returns exact message actions; timeout for “打开设置” falls back to Settings.

- [ ] **Step 2: Run tests and confirm failure**

Expected: DeepSeek planner missing.

- [ ] **Step 3: Make TaskPlanner async and wire provider selection**

Use DeepSeek when key exists, otherwise DevTaskPlanner. Await planner in `/v1/tasks`.

- [ ] **Step 4: Run all orchestrator tests and typecheck**

Expected: all pass.

### Task 4: Real API verification and documentation

**Files:**
- Modify: `docs/HANDOFF.md`
- Modify: `docs/03-agent-and-model-integration.md`
- Modify: `docs/guides/adding-model-provider.md`

**Interfaces:**
- Consumes: local `DEEPSEEK_API_KEY`.

- [ ] **Step 1: POST the user’s natural-language example**

Assert response contains input text `测试成功了`.

- [ ] **Step 2: Run full shared contracts, skills, orchestrator and Android checks.**
- [ ] **Step 3: Commit the DeepSeek planner.**
