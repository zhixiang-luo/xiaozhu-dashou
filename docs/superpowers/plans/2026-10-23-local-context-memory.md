# 手机本地背景记忆 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在每次 DeepSeek 规划前加入手机本地的未来日历、近期任务、联系人和偏好背景。

**Architecture:** Android SQLite 持久化事件与偏好，ContextBuilder 实时聚合日历和本地记忆，`/v1/tasks` 携带结构化上下文，DeepSeek adapter 将其加入 Prompt。

**Tech Stack:** Kotlin SQLiteOpenHelper、CalendarContract、TypeScript、zod、Vitest、JUnit。

**Spec:** `docs/superpowers/specs/2026-10-23-local-context-memory-design.md`

## Global Constraints

- 上下文是辅助信息，日历修改和删除仍实时回查。
- 不持久化密码、验证码、支付凭据或安全页面内容。
- 上下文数组有数量上限。
- 手机 SQLite 是第一版记忆事实来源。
- 不引入向量数据库。

### Task 1: Shared TaskContext contract

**Files:**
- Modify: `packages/contracts/src/task.ts`
- Modify: `packages/contracts/src/task.test.ts` or create
- Modify: `services/orchestrator/src/server.ts`
- Modify: `services/orchestrator/src/tasks/coordinator.ts`
- Modify: `services/orchestrator/src/model/intent.ts`
- Modify: `services/orchestrator/src/providers/deepseek/client.ts`

**Interfaces:**
- Produces `TaskContextSchema`.
- `/v1/tasks` accepts `{ instruction, context? }`.
- Prompt context includes `appContext`.

- [x] **Step 1: Write failing schema and prompt tests.**
- [x] **Step 2: Implement shared context contract and prompt integration.**
- [x] **Step 3: Run contract and orchestrator tests.**

### Task 2: Android MemoryStore and ContextBuilder

**Files:**
- Create: `android/app/src/main/kotlin/com/personaljarvis/memory/MemoryStore.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/memory/ContextBuilder.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/memory/TaskContextPayload.kt`
- Create: `android/app/src/test/kotlin/com/personaljarvis/memory/TaskContextPayloadTest.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/ui/FloatingBubbleService.kt`

**Interfaces:**
- Produces `MemoryStore.recordEvent`, `recentEvents`, `contacts`, `preferences`.
- Produces `ContextBuilder.build(): TaskContextPayload`.
- Produces `TaskContextPayload.toJson()`.

- [x] **Step 1: Write failing JSON serialization test.**
- [x] **Step 2: Implement SQLite schema and event access.**
- [x] **Step 3: Implement calendar/current screen context aggregation.**
- [x] **Step 4: Run Android tests and build.**

### Task 3: Task transport and task journal

**Files:**
- Modify: `android/app/src/main/kotlin/com/personaljarvis/voice/TaskPlannerClient.kt`
- Modify: `android/app/src/test/kotlin/com/personaljarvis/voice/TaskPlannerClientTest.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/ui/FloatingBubbleService.kt`

**Interfaces:**
- `TaskPlannerClient.submit(instruction, context)`.
- HTTP body contains optional `context`.
- Every planned task is written to MemoryStore.

- [x] **Step 1: Write failing request-body test.**
- [x] **Step 2: Implement context transport and task journal writes.**
- [x] **Step 3: Run Android and server verification.**

### Task 4: Real device context verification

**Files:**
- Modify: `docs/10-acceptance-record.md`
- Modify: `docs/HANDOFF.md`

- [x] **Step 1: Create a test calendar event.**
- [x] **Step 2: Query “我明天下午有什么安排” and verify task request contains the event.**
- [x] **Step 3: Restart App and confirm recent task memory survives.**
