# 火山引擎实时语音 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用火山双向流式 ASR 替换 Android 系统短句识别，确保长按语音持续显示累计文本并在松手后提交完整结果。

**Architecture:** Android `AudioRecord` 采集 PCM16，经本地 Fastify WebSocket 网关转发火山 `/api/v3/sauc/bigmodel`；火山密钥只存在服务端，客户端只接收 `ready/partial/final/error`。

**Tech Stack:** Kotlin、Android AudioRecord、OkHttp WebSocket、TypeScript、Fastify WebSocket、ws、Vitest、JUnit。

**Spec:** `docs/superpowers/specs/2026-10-23-volcengine-streaming-voice-design.md`

## Global Constraints

- 音频格式固定为 16,000 Hz、单声道、16-bit little-endian PCM。
- 火山 API Key 不得进入 Android APK、测试快照或 Git。
- 单次录音上限 30 秒。
- 最终结果以火山累计 `result.text` 为准。
- 不记录音频或转写正文到日志。
- 保留支付、验证码、账号安全和权限页的人工处理规则。

---

### Task 1: 火山协议与网关

**Files:**
- Create: `services/orchestrator/src/voice/protocol.ts`
- Create: `services/orchestrator/src/voice/volcano-session.ts`
- Create: `services/orchestrator/src/voice/routes.ts`
- Create: `services/orchestrator/src/voice/protocol.test.ts`
- Create: `services/orchestrator/src/voice/routes.test.ts`
- Modify: `services/orchestrator/package.json`
- Modify: `services/orchestrator/src/server.ts`
- Modify: `services/orchestrator/src/main.ts`
- Modify: `.env.example`

**Interfaces:**
- Produces: `encodeVolcanoFrame(messageType, flags, payload): Buffer`
- Produces: `parseVolcanoFrame(buffer): { messageType, flags, payload } | null`
- Produces: `createVolcanoRequest(uid): Buffer`
- Produces: `registerVoiceRoutes(app, options): void`

- [ ] **Step 1: Write protocol failure tests**

```ts
expect(encodeVolcanoFrame(0x02, 0x02, Buffer.from([1, 2]))).toEqual(
  Buffer.from([0x11, 0x22, 0x00, 0x00, 0, 0, 0, 2, 1, 2])
);
expect(parseVolcanoFrame(Buffer.from([1]))).toBeNull();
```

- [ ] **Step 2: Run the protocol test and confirm failure**

Run: `pnpm --filter @personal-jarvis/orchestrator test -- protocol.test.ts`
Expected: fail because `voice/protocol.ts` does not exist.

- [ ] **Step 3: Implement frame and initial request builders**

Use the binary header and JSON payload proven by 声账:

```ts
audio: { format: 'pcm', codec: 'raw', rate: 16_000, bits: 16, channel: 1 }
request: {
  model_name: 'bigmodel',
  enable_itn: true,
  enable_ddc: true,
  enable_punc: true,
  show_utterances: true
}
```

- [ ] **Step 4: Add authenticated WebSocket route and Volcano session**

Route path is `/v1/voice/stream`. Reject missing or incorrect Bearer Token with HTTP 401.
Queue audio while upstream is connecting, send 6,400-byte packets, send final flag `0x02`
on client stop, and emit cumulative text from upstream `result.text`.

- [ ] **Step 5: Run route and protocol tests**

Run: `pnpm --filter @personal-jarvis/orchestrator test -- protocol.test.ts routes.test.ts`
Expected: all tests pass.

### Task 2: Android realtime PCM client

**Files:**
- Create: `android/app/src/main/kotlin/com/personaljarvis/voice/RealtimeVoiceSession.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/voice/OkHttpVoiceStream.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/voice/PcmAudioCapture.kt`
- Create: `android/app/src/test/kotlin/com/personaljarvis/voice/RealtimeVoiceSessionTest.kt`
- Modify: `android/app/build.gradle.kts`

**Interfaces:**
- Produces: `RealtimeVoiceSession.startCapture()`
- Produces: `RealtimeVoiceSession.startStreaming()`
- Produces: `RealtimeVoiceSession.stopAndFinish()`
- Produces: `RealtimeVoiceSession.cancel()`
- Consumes: `VoiceStreamEvent` with `Ready`, `Partial(text)`, `Final(text)`, `Error(message)`.

- [ ] **Step 1: Write failing session tests**

```kotlin
session.startCapture()
session.startStreaming()
session.accept(VoiceStreamEvent.Partial("第一段"))
session.accept(VoiceStreamEvent.Partial("第一段第二句"))
assertEquals("第一段第二句", session.latestText)
```

Also test that `stopAndFinish()` sends one stop only and that a final timeout falls back to
the latest cumulative text.

- [ ] **Step 2: Run the focused test and confirm failure**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*RealtimeVoiceSessionTest'`
Expected: compile failure because the realtime session does not exist.

- [ ] **Step 3: Implement AudioRecord and OkHttp websocket adapters**

Start capture on `ACTION_DOWN`, buffer PCM until the socket opens, send binary chunks,
send `{"type":"stop"}` on release, and wait at most 6 seconds for final.

- [ ] **Step 4: Run Android unit tests**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*RealtimeVoiceSessionTest'`
Expected: pass.

### Task 3: Replace system SpeechRecognizer in floating bubble

**Files:**
- Modify: `android/app/src/main/kotlin/com/personaljarvis/ui/FloatingBubbleService.kt`
- Modify: `android/app/src/main/res/values/strings.xml`
- Delete: `android/app/src/main/kotlin/com/personaljarvis/voice/VoiceRecognitionPolicy.kt`
- Delete: `android/app/src/test/kotlin/com/personaljarvis/voice/VoiceRecognitionPolicyTest.kt`

**Interfaces:**
- Consumes: `RealtimeVoiceSession` from Task 2.
- Produces: partial transcript on the expanded bubble and final transcript to
  `submitToPlanner(instruction)`.

- [ ] **Step 1: Write a failing controller behavior test**

Assert that release submits the latest cumulative text once, and connection failure never
submits an empty task.

- [ ] **Step 2: Run the controller test and confirm failure**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*VoiceSessionControllerTest'`
Expected: fail against the current hold timeout behavior if the new finalization contract is absent.

- [ ] **Step 3: Replace SpeechRecognizer callbacks with RealtimeVoiceSession**

Keep the 450 ms gesture threshold for distinguishing tap from hold, but begin audio capture
on `ACTION_DOWN`. On long press, open the stream. On release, finalize and submit.

- [ ] **Step 4: Run all Android tests and build**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: exit code 0.

### Task 4: Real service and device verification

**Files:**
- Modify: `docs/HANDOFF.md`
- Modify: `docs/10-acceptance-record.md`
- Modify: `docs/02-system-architecture.md`
- Modify: `docs/03-agent-and-model-integration.md`

**Interfaces:**
- Consumes: running orchestrator with `VOLCENGINE_ASR_API_KEY`.
- Produces: recorded target-device evidence and rollback notes.

- [ ] **Step 1: Run a real PCM transcription through the gateway**

Generate a 16 kHz mono PCM sample, connect to `/v1/voice/stream` with the device token,
send the binary audio, send stop, and assert that `partial` arrives before `final`.

- [ ] **Step 2: Install the APK and test on the Redmi K60**

Hold the bubble and say a 15–20 second Chinese instruction with a natural pause. Confirm the
transcript continues after the pause and the final text contains both halves.

- [ ] **Step 3: Run complete verification**

Run:

```bash
CI=true PATH="/Users/luozhixiang/.nvm/versions/node/v22.19.0/bin:$PATH" pnpm check
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: all checks and the APK build pass.
