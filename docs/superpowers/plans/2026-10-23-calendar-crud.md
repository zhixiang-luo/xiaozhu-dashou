# 系统日历 CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过自然语言查询、创建、修改和删除唯一匹配的系统日历事件。

**Architecture:** DeepSeek 输出通用日历操作 Intent；本地转换器校验时间、标题和修改字段；Android 专用 CalendarContract 驱动执行并回读。

**Tech Stack:** TypeScript、zod、Vitest、Kotlin、CalendarContract、JUnit。

**Spec:** `docs/superpowers/specs/2026-10-23-calendar-crud-design.md`

## Global Constraints

- 修改和删除必须有标题查询词。
- 多个匹配或重复日程必须停止。
- 查询范围最多一年，事件最长 24 小时。
- 所有日期使用显式 `Asia/Shanghai` 配置。
- 每个外部副作用动作执行后必须回读验证。

### Task 1: Action contracts and skill catalog

**Files:**
- Modify: `packages/contracts/src/action.ts`
- Modify: `packages/contracts/src/action.test.ts`
- Modify: `packages/skills/src/manifest.ts`
- Modify: `skills/schema/skill-manifest.schema.json`
- Create: `skills/calendar/list-events/manifest.yaml`
- Create: `skills/calendar/update-event/manifest.yaml`
- Create: `skills/calendar/delete-event/manifest.yaml`

**Interfaces:**
- Produces action types `calendar_list_events`, `calendar_update_event`,
  `calendar_delete_event`.
- Keeps `calendar_create_event`.

- [x] **Step 1: Add failing contract tests for list, update and delete.**
- [x] **Step 2: Implement action and skill schemas.**
- [x] **Step 3: Run contract and skill tests.**

### Task 2: Natural-language calendar routing

**Files:**
- Modify: `services/orchestrator/src/skills/translate-intent.ts`
- Modify: `services/orchestrator/src/skills/translate-intent.test.ts`
- Modify: `services/orchestrator/src/providers/deepseek/client.ts`
- Modify: `services/orchestrator/src/providers/deepseek/client.test.ts`

**Interfaces:**
- Produces validated range, title-query and update arguments.

- [x] **Step 1: Add failing list/update/delete translation tests.**
- [x] **Step 2: Implement date-range parsing and field validation.**
- [x] **Step 3: Run orchestrator tests and typecheck.**

### Task 3: Android CalendarContract CRUD

**Files:**
- Modify: `android/app/src/main/kotlin/com/personaljarvis/policy/RuntimeModels.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/policy/SafeExecutionRunner.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/voice/TaskPlannerClient.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/ui/FloatingBubbleService.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/execute/CalendarEventRequest.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/execute/CalendarEventWriter.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/execute/RoutedExecutionDriver.kt`
- Modify: `android/app/src/test/kotlin/com/personaljarvis/execute/CalendarEventRequestTest.kt`

**Interfaces:**
- Consumes all calendar action types.
- Produces query/update/delete operations with read-back verification and result summaries.

- [x] **Step 1: Add failing request tests for list, update and delete.**
- [x] **Step 2: Implement request validation and action parsing.**
- [x] **Step 3: Implement unique event lookup and CRUD operations.**
- [x] **Step 4: Run Android tests and build.**

### Task 4: Real device verification

**Files:**
- Modify: `docs/10-acceptance-record.md`
- Modify: `docs/HANDOFF.md`

- [x] **Step 1: Create a temporary event.**
- [x] **Step 2: Query it and verify the returned title/time.**
- [x] **Step 3: Update its time and verify read-back.**
- [x] **Step 4: Delete it and verify it no longer exists.**
- [x] **Step 5: Record results and commit.**
