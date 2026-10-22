# Android Observer and Executor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可在专用 HyperOS 手机上安装的 Jarvis App，完成语音入口、屏幕观察、动作执行、取消和人工接管闭环。

**Architecture:** Kotlin/Compose App 管理任务和权限；前台服务承载悬浮球与语音；AccessibilityService 读取 UI 树；Shizuku 和 Accessibility 实现同一个 `ExecutionDriver`。App 只执行共享 schema 校验和本地 `PolicyGate` 批准的动作。

**Tech Stack:** Kotlin、Jetpack Compose、Coroutines、AccessibilityService、Shizuku API、MediaSession、JUnit、Compose UI Test。

**Spec:** `docs/04-android-execution.md`, `docs/05-security-privacy.md`

> Implementation status: Task 1 completed and APK built. Runtime observation and execution tasks remain pending.

## Global Constraints

- `minSdk 30`，Kotlin，Jetpack Compose。
- 不把 API Key 写入 Android。
- 不执行任意 shell。
- 取消后不得执行任何旧代次动作。
- 账号、支付、验证码、密码和生物识别页面立即暂停。
- Developer Edition 可长期保存屏幕上下文，但凭据内容除外。

---

### Task 1: Android app scaffold

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/kotlin/com/personaljarvis/MainActivity.kt`
- Test: `android/app/src/test/kotlin/com/personaljarvis/AppSmokeTest.kt`

**Interfaces:**
- Produces: application id `com.personaljarvis`, single Activity, Compose app shell.

- [ ] **Step 1: Add Gradle wrapper and module**

Use Gradle 8.10+, Android Gradle Plugin compatible version, and Kotlin Compose plugin. Set `applicationId = "com.personaljarvis"`, `minSdk = 30`, `targetSdk` to installed HyperOS target.

- [ ] **Step 2: Write smoke test before Activity**

```kotlin
@Test
fun appIdentityIsStable() {
    assertEquals("com.personaljarvis", BuildConfig.APPLICATION_ID)
}
```

- [ ] **Step 3: Run failing test**

Run: `./gradlew :app:testDebugUnitTest`

Expected: FAIL because the module or `BuildConfig` is missing.

- [ ] **Step 4: Implement minimal app shell and manifest**

The Activity shows “Personal Jarvis”, permission state, and a start/stop task button. The manifest declares no accessibility, overlay, microphone, notification listener, or screen capture permission until their task adds them.

- [ ] **Step 5: Verify**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS and APK produced.

### Task 2: Observation contract and accessibility service

**Files:**
- Create: `android/app/src/main/res/xml/accessibility_service_config.xml`
- Create: `android/app/src/main/kotlin/com/personaljarvis/observe/AccessibilityObserver.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/observe/UiTreeCompactor.kt`
- Test: `android/app/src/test/kotlin/com/personaljarvis/observe/UiTreeCompactorTest.kt`

**Interfaces:**
- Produces: `observe(): Observation`.
- Node IDs remain stable within one screen hash.

- [ ] **Step 1: Write failing compactor test**

```kotlin
@Test
fun removesInvisibleAndNonInteractiveNodes() {
    val output = UiTreeCompactor().compact(listOf(visibleButton, invisibleText))
    assertEquals(listOf("button-1"), output.map { it.nodeId })
}
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests '*UiTreeCompactorTest'`

Expected: FAIL because compactor is missing.

- [ ] **Step 3: Implement compactor and service**

Collect package name, title, visible interactive nodes, bounds, text, description, actions, and screen hash. Do not capture secure content. Expose one observation request at a time and return `observe_timeout` after 3 seconds.

- [ ] **Step 4: Verify**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS.

### Task 3: Execution drivers and policy gate

**Files:**
- Create: `android/app/src/main/kotlin/com/personaljarvis/execute/ExecutionDriver.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/execute/AccessibilityDriver.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/execute/ShizukuDriver.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/policy/PolicyGate.kt`
- Test: `android/app/src/test/kotlin/com/personaljarvis/policy/PolicyGateTest.kt`

**Interfaces:**
- `ExecutionDriver.execute(action): ActionResult`.
- `PolicyGate.evaluate(action, observation): RiskDecision`.

- [ ] **Step 1: Write failing policy tests**

```kotlin
@Test
fun blocksPaymentPackageEvenForTap() {
    val decision = gate.evaluate(tapAction, paymentObservation)
    assertEquals(RiskDecision.Block("account_or_payment_screen"), decision)
}

@Test
fun rejectsStaleGeneration() {
    val result = runner.execute(action.copy(taskGeneration = 1), currentGeneration = 2)
    assertEquals(ActionResult.Rejected("stale_action"), result)
}
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests '*PolicyGateTest'`

Expected: FAIL because gate and runner do not exist.

- [ ] **Step 3: Implement both drivers and gate**

Shizuku path sends structured arguments only; Accessibility path uses node actions and gesture callbacks. `PolicyGate` stops login, payment, OTP, password, biometric, and `FLAG_SECURE` states. The runner increments a local generation on cancel and rejects stale actions.

- [ ] **Step 4: Verify**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS.

### Task 4: Voice and floating entry

**Files:**
- Create: `android/app/src/main/kotlin/com/personaljarvis/ui/FloatingBubbleService.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/voice/VoiceSessionController.kt`
- Create: `android/app/src/main/kotlin/com/personaljarvis/voice/TaskPlannerClient.kt`
- Test: `android/app/src/test/kotlin/com/personaljarvis/voice/VoiceSessionControllerTest.kt`

**Interfaces:**
- Produces: one active push-to-talk session and task state callbacks.
- Remote client speaks HTTPS/WSS only.

- [ ] **Step 1: Write failing state test**

```kotlin
@Test
fun stopTransitionsListeningToIdle() {
    controller.startPressToTalk()
    controller.stopPressToTalk("给小张说我在路上")
    assertEquals(VoiceState.Submitted("给小张说我在路上"), controller.state.value)
}
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests '*VoiceSessionControllerTest'`

Expected: FAIL because controller is missing.

- [ ] **Step 3: Implement overlay, speech-to-text adapter, TTS, and task client**

The overlay stays above current apps, shows listening/planning/acting/awaiting user states, and provides stop. Wake word remains a separate feature flag disabled until dedicated audio tests pass.

- [ ] **Step 4: Verify**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS.

- [ ] **Step 5: Physical-device acceptance**

Run on a dedicated Redmi/HyperOS device: open App, observe screen, tap a test control, cancel, switch apps, disconnect Shizuku, and confirm no late action.

- [ ] **Step 6: Commit**

```bash
git add android
git commit -m "feat(android): add observer executor and voice shell"
```
