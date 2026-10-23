# 微信当前会话显式回复 Implementation Plan

> 状态：暂停实验，未合并 `main`。本计划保留已完成的工程探索和真机失败原因，不再继续
> 按原方式推进。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用户明确要求时，结合微信当前会话生成回复，并在两次会话校验后安全发送。

**Architecture:** Android 用 AccessibilityService 截图和 ML Kit 中文 OCR 生成结构化会话上下文；Orchestrator 让 DeepSeek 生成最终回复并转换成专用发送动作；Android 专用 adapter 在输入前和发送后重新 OCR 验证。

**Tech Stack:** Kotlin、Android AccessibilityService、ML Kit Chinese Text Recognition、TypeScript、zod、Vitest、JUnit。

**Spec:** `docs/superpowers/specs/2026-10-23-wechat-explicit-reply-design.md`

## Global Constraints

- 只有用户明确要求回复时才允许调用当前会话回复。
- 输入文字前必须验证当前会话标题。
- 发送后必须验证 outgoing 消息。
- 发送失败不自动重试。
- 截图和聊天正文不写服务端日志或长期记忆。
- 支付、验证码、密码和账号安全页面立即停止。

---

### Task 1: OCR screen parser

**Files:**
- Create: `android/app/src/main/kotlin/com/personaljarvis/wechat/WechatScreenParser.kt`
- Create: `android/app/src/test/kotlin/com/personaljarvis/wechat/WechatScreenParserTest.kt`

**Interfaces:**
- Produces `WechatOcrLine(text, left, top, right, bottom)`
- Produces `WechatMessage(direction, text)`
- Produces `WechatScreenState(title, recentMessages, lines)`
- Produces `WechatScreenParser.parse(width, height, lines): WechatScreenState?`

- [ ] **Step 1: Write failing parser tests**

```kotlin
@Test
fun extractsChatTitleAndMessages() {
    val state = WechatScreenParser.parse(
        1080,
        2400,
        listOf(
            line("文件传输助手", 390, 146, 680, 195),
            line("对方：你好", 190, 366, 760, 420),
            line("我：收到", 520, 610, 900, 664)
        )
    )

    assertEquals("文件传输助手", state?.title)
    assertEquals(
        listOf(
            WechatMessage(Direction.INCOMING, "对方：你好"),
            WechatMessage(Direction.OUTGOING, "我：收到")
        ),
        state?.recentMessages
    )
}

@Test
fun rejectsWechatConversationList() {
    val state = WechatScreenParser.parse(
        1080,
        2400,
        listOf(
            line("微信（1）", 430, 146, 650, 195),
            line("通讯录", 100, 2300, 200, 2350)
        )
    )

    assertNull(state)
}
```

- [ ] **Step 2: Run parser tests and observe RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.personaljarvis.wechat.WechatScreenParserTest`

Expected: compilation failure because `WechatScreenParser` does not exist.

- [ ] **Step 3: Implement the parser**

Use normalized screen regions:

```kotlin
const val TITLE_TOP = 0.045f
const val TITLE_BOTTOM = 0.105f
const val CHAT_TOP = 0.11f
const val CHAT_BOTTOM = 0.90f
const val BOTTOM_NAV_TOP = 0.94f
```

- [ ] **Step 4: Run parser tests and observe GREEN**

Run: `./gradlew :app:testDebugUnitTest --tests com.personaljarvis.wechat.WechatScreenParserTest`

Expected: all parser tests pass.

### Task 2: Android OCR context

**Files:**
- Modify: `android/app/build.gradle.kts`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/observe/AccessibilityObserver.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/wechat/WechatConversationReader.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/memory/TaskContextPayload.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/memory/ContextBuilder.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/ui/FloatingBubbleService.kt`
- Modify: `android/app/src/test/kotlin/com/personaljarvis/memory/TaskContextPayloadTest.kt`

**Interfaces:**
- `AccessibilityObserver.captureScreenshot(): Bitmap?`
- `WechatConversationReader.read(): WechatScreenState?`
- `TaskContextPayload.currentConversation: CurrentConversationPayload?`

- [ ] **Step 1: Add a failing JSON test**

```kotlin
@Test
fun serializesCurrentWechatConversation() {
    val payload = basePayload().copy(
        currentConversation = CurrentConversationPayload(
            title = "文件传输助手",
            recentMessages = listOf(
                WechatMessagePayload(Direction.INCOMING, "你好")
            ),
            capturedAt = "2026-10-23T06:30:00Z"
        )
    )

    val json = JSONObject(payload.toJson()).getJSONObject("currentConversation")

    assertEquals("文件传输助手", json.getString("title"))
    assertEquals("你好", json.getJSONArray("recentMessages")
        .getJSONObject(0).getString("text"))
}
```

- [ ] **Step 2: Run the test and observe RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.personaljarvis.memory.TaskContextPayloadTest`

Expected: failure because current-conversation fields do not exist.

- [ ] **Step 3: Implement screenshot, ML Kit OCR and JSON**

Add `com.google.mlkit:text-recognition-chinese:16.0.1`, capture API 30 screenshots,
convert OCR lines to `WechatOcrLine`, and inject the parsed context only when the foreground
package is `com.tencent.mm`.

- [ ] **Step 4: Run Android tests and build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: all tests pass and APK builds.

### Task 3: Shared context and explicit reply intent

**Files:**
- Modify: `packages/contracts/src/task.ts`
- Modify: `packages/contracts/src/task.test.ts`
- Modify: `packages/contracts/src/action.ts`
- Modify: `packages/contracts/src/action.test.ts`
- Modify: `services/orchestrator/src/providers/deepseek/client.ts`
- Modify: `services/orchestrator/src/providers/deepseek/client.test.ts`
- Modify: `services/orchestrator/src/skills/translate-intent.ts`
- Modify: `services/orchestrator/src/skills/translate-intent.test.ts`

**Interfaces:**
- Produces `TaskContext.currentConversation`
- Produces action `wechat_reply_current { conversationTitle, message }`
- Explicit-reply route requires context title match and explicit language

- [ ] **Step 1: Write failing contract and translator tests**

```typescript
it("builds an explicit current-conversation reply", async () => {
  const actions = await translateIntent(
    {
      ...task,
      instruction: "我不知道怎么回，帮我直接回一下这段对话",
      context: wechatContext()
    },
    {
      kind: "execute",
      skillId: "wechat.send-message",
      arguments: {
        contactName: "文件传输助手",
        message: "这份文件我看到了，晚点给你回复。"
      }
    },
    loadDefaultSkillCatalog(),
    { allowWechatAutoSend: true }
  );

  expect(actions).toEqual([
    expect.objectContaining({
      type: "wechat_reply_current",
      conversationTitle: "文件传输助手",
      message: "这份文件我看到了，晚点给你回复。"
    })
  ]);
});
```

Also add a test that `"帮我发一句测试"` with the same context returns `ask_user`.

- [ ] **Step 2: Run tests and observe RED**

Run: `pnpm --filter @personal-jarvis/contracts test && pnpm --filter @personal-jarvis/orchestrator test`

Expected: failure because the context field and action do not exist.

- [ ] **Step 3: Implement schema, prompt and translator**

The prompt receives `currentConversation.recentMessages` as untrusted data. For an explicit
reply request it must produce the final message as `arguments.message`, not echo the user's
instruction.

- [ ] **Step 4: Run tests and observe GREEN**

Run: `pnpm --filter @personal-jarvis/contracts test && pnpm --filter @personal-jarvis/orchestrator test`

Expected: all tests pass.

### Task 4: Guarded Android sender and real-device acceptance

**Files:**
- Modify: `android/app/src/main/kotlin/com/personaljarvis/policy/RuntimeModels.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/execute/AccessibilityDriver.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/execute/ShizukuUserServiceBridge.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/execute/RoutedExecutionDriver.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/execute/WechatReplyDriver.kt`
- Create: `android/app/src/test/kotlin/com/personaljarvis/execute/WechatReplyDriverTest.kt`
- Modify: `android/app/src/main/kotlin/com/personaljarvis/ui/FloatingBubbleService.kt`
- Modify: `docs/10-acceptance-record.md`
- Modify: `docs/HANDOFF.md`

**Interfaces:**
- `WechatReplyUi.read(): WechatScreenState?`
- `WechatReplyUi.focusInput(): Boolean`
- `WechatReplyUi.paste(text): Boolean`
- `WechatReplyUi.tapSend(): Boolean`
- `WechatReplyDriver.execute(action): DriverResult`

- [ ] **Step 1: Write failing sender tests**

```kotlin
@Test
fun refusesInputWhenConversationTitleChanged() {
    val ui = FakeWechatReplyUi(
        screens = mutableListOf(chat("别的会话"))
    )

    val result = driver(ui).execute(
        replyAction(
            conversationTitle = "文件传输助手",
            message = "收到"
        )
    )

    assertEquals(
        DriverResult.Failure("wechat_conversation_mismatch"),
        result
    )
    assertEquals(0, ui.focusCalls)
}
```

Add tests for missing draft and missing sent outgoing message.

- [ ] **Step 2: Run sender tests and observe RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.personaljarvis.execute.WechatReplyDriverTest`

Expected: compilation failure because the sender does not exist.

- [ ] **Step 3: Implement sender and wire routing**

Route `WECHAT_REPLY_CURRENT` to the specialized adapter. Use screenshot OCR before focus,
after focus, after paste, and after send. Never retry automatically.

- [ ] **Step 4: Run Android verification**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: all Android tests pass and APK builds.

- [ ] **Step 5: Real-device acceptance on file-transfer-helper**

Open `文件传输助手`, install the new APK, grant permissions, restart the service and send:

```text
我不知道怎么回，帮我直接回一下这段对话
```

Verify the title is unchanged, the generated reply appears as an outgoing message, and a
second instruction without explicit reply wording does not send.

- [ ] **Step 6: Update acceptance docs and commit**

Record the APK hash, model behavior, OCR title, draft and sent-message verification, then run:

```bash
pnpm check
git diff --check
git add README.md android docs packages services skills
git commit -m "feat: add guarded WeChat explicit replies"
```
