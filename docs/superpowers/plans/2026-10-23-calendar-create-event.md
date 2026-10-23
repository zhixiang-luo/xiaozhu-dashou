# 系统日历创建 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用一句自然语言在 Android 系统日历创建并验证一个日程。

**Architecture:** DeepSeek 将自然语言结合当前日期解析为 Intent；服务端转为结构化 Action；Android 专用 CalendarContract 驱动写入并回读。

**Tech Stack:** TypeScript、zod、Vitest、Kotlin、Android CalendarContract、JUnit。

**Spec:** `docs/superpowers/specs/2026-10-23-calendar-create-event-design.md`

## Global Constraints

- 时间必须带 `Asia/Shanghai` 时区。
- 只创建个人事件，不邀请他人或发送邮件。
- 过去时间、非法时长、回读失败立即停止。
- 日历写入不依赖固定坐标。
- 模型输出必须先通过 Intent 和 Skill 参数校验。

### Task 1: Action contract and model time context

**Files:**
- Modify: `packages/contracts/src/action.ts`
- Modify: `packages/contracts/src/action.test.ts`
- Modify: `services/orchestrator/src/providers/deepseek/client.ts`
- Modify: `services/orchestrator/src/providers/deepseek/client.test.ts`
- Modify: `services/orchestrator/src/tasks/deepseek-planner.ts`

**Interfaces:**
- Produces: `ActionSchema` variant `calendar_create_event`.
- Produces: `DeepSeekIntentClient.infer(instruction, catalog, context)` where context
  includes `now` and `timeZone`.

- [ ] **Step 1: Add failing schema test for a calendar event action.**
- [ ] **Step 2: Add failing prompt-context test.**
- [ ] **Step 3: Implement action fields and DeepSeek date context.**
- [ ] **Step 4: Run contract and DeepSeek tests.**

### Task 2: Calendar skill and translation

**Files:**
- Create: `skills/calendar/create-event/manifest.yaml`
- Create: `skills/calendar/create-event/README.md`
- Modify: `services/orchestrator/src/skills/translate-intent.ts`
- Modify: `services/orchestrator/src/skills/translate-intent.test.ts`

**Interfaces:**
- Consumes: `calendar.create-event`.
- Produces: one `calendar_create_event` action with epoch milliseconds.

- [ ] **Step 1: Write failing tests for a valid tomorrow event and invalid duration.**
- [ ] **Step 2: Implement time parsing and duration validation.**
- [ ] **Step 3: Run skills and translation tests.**

### Task 3: Android CalendarContract driver

**Files:**
- Create: `android/app/src/main/kotlin/com/personaljarvis/execute/CalendarEventWriter.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/execute/RoutedExecutionDriver.kt`
- Create: `android/app/src/test/kotlin/com/personaljarvis/execute/CalendarEventRequestTest.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/policy/RuntimeModels.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/policy/PolicyGate.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/voice/TaskPlannerClient.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/ui/FloatingBubbleService.kt`

**Interfaces:**
- Produces: `CalendarEventRequest.from(action): CalendarEventRequest?`.
- Produces: `CalendarEventWriter.create(request): DriverResult`.
- Produces: routed execution for calendar versus general UI actions.

- [ ] **Step 1: Write failing request-validation tests.**
- [ ] **Step 2: Implement action parsing and validation.**
- [ ] **Step 3: Implement CalendarContract insert and read-back verification.**
- [ ] **Step 4: Run Android tests and build.**

### Task 4: Permission UI and real-device acceptance

**Files:**
- Modify: `android/app/src/main/kotlin/com/personaljarvis/MainActivity.kt`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `docs/10-acceptance-record.md`
- Modify: `docs/HANDOFF.md`

**Interfaces:**
- Produces: Chinese calendar permission status and request button.

- [ ] **Step 1: Add READ/WRITE_CALENDAR permission request and status.**
- [ ] **Step 2: Run full repository and Android verification.**
- [ ] **Step 3: Install APK and create a real test event on Redmi K60.**
- [ ] **Step 4: Record event title/time and App/OS versions.**
