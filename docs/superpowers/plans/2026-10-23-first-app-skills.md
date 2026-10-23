# First App Skills Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付小猪搭手的首批真实任务：微信自动回复、网易云音乐播放、高德导航和从微信地址到高德的跨 App 流程。

**Architecture:** 每个技能声明包名、支持版本、前置条件、动作、验证和降级。通用 Agent 只负责选择和组合技能；App 特定选择器保存在技能目录。所有外部副作用动作使用幂等键并在执行前重新观察。

**Tech Stack:** YAML manifest、Kotlin skill interpreter、共享 contracts、Vitest contract tests、HyperOS 真机验收。

**Spec:** `docs/01-prd.md`, `docs/guides/adding-app-skill.md`, `docs/06-testing-and-evaluation.md`

> Implementation status: Platform-neutral manifest schema and validator completed. Android loader and App skills remain pending.

## Global Constraints

- 微信普通私聊在目标明确时可直接发送。
- 同名联系人、异常页面和不可验证结果必须暂停。
- 支付、验证码、密码和账号安全步骤不得自动执行。
- App 更新后选择器失效时必须停止，不得盲点旧坐标。
- 每个技能在 Shizuku 和无障碍两种模式下测试。

---

### Task 1: Skill manifest and loader

**Files:**
- Create: `skills/schema/skill-manifest.schema.json`
- Create: `android/app/src/main/kotlin/com/personaljarvis/skills/SkillLoader.kt`
- Create: `android/app/src/test/kotlin/com/personaljarvis/skills/SkillLoaderTest.kt`

**Interfaces:**
- Produces: `SkillLoader.load(id): SkillManifest`.
- Invalid package names, missing verification, or missing risk policy fail loading.

- [ ] **Step 1: Write failing loader test**

```kotlin
@Test
fun rejectsSkillWithoutVerification() {
    val result = loader.parse(manifestWithoutVerification)
    assertTrue(result.isFailure)
}
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests '*SkillLoaderTest'`

Expected: FAIL because loader and schema do not exist.

- [ ] **Step 3: Implement schema and loader**

Required fields are `id`, `version`, `appPackages`, `prerequisites`, `capabilities`, `risk`, `verification`, and `steps`.

- [ ] **Step 4: Verify**

Run: `./gradlew :app:testDebugUnitTest --tests '*SkillLoaderTest'`

Expected: PASS.

### Task 2: WeChat send-message skill

**Files:**
- Create: `skills/wechat/send-message/manifest.yaml`
- Create: `skills/wechat/send-message/README.md`
- Create: `skills/wechat/send-message/tests/cases.json`

**Interfaces:**
- Input: `{ contactName: string | null, message: string }`.
- Success: latest outgoing bubble equals message in the intended conversation.

- [ ] **Step 1: Add failing fixtures**

Fixtures cover one unique contact, duplicate contacts, current conversation, wrong foreground app, and login screen.

- [ ] **Step 2: Implement manifest steps**

Steps: verify foreground/launch, choose current or search target, validate unique result, open chat, input text, tap send, verify outgoing bubble. Search ambiguity invokes `ask_user`.

- [ ] **Step 3: Run contract validation**

Run: `pnpm --filter @personal-jarvis/contracts test && ./gradlew :app:testDebugUnitTest --tests '*SkillLoaderTest'`

Expected: PASS.

### Task 3: NetEase Cloud Music skill

**Files:**
- Create: `skills/netease-music/play-song/manifest.yaml`
- Create: `skills/netease-music/play-song/README.md`
- Create: `skills/netease-music/play-song/tests/cases.json`

**Interfaces:**
- Input: `{ query: string }`.
- Success: target song appears in now-playing state and playback status is playing.

- [ ] **Step 1: Add failing fixtures**

Fixtures cover unique song, multiple versions, no result, login wall, advertisement, and unavailable copyright.

- [ ] **Step 2: Implement media and UI paths**

Play/pause/next/previous use system MediaSession. Search/play uses the skill UI path and verifies playback.

- [ ] **Step 3: Verify**

Run: `pnpm --filter @personal-jarvis/contracts test && ./gradlew :app:testDebugUnitTest --tests '*SkillLoaderTest'`

Expected: PASS.

### Task 4: Amap navigation and cross-app handoff

**Files:**
- Create: `skills/amap/navigate/manifest.yaml`
- Create: `skills/wechat-to-amap-navigate/manifest.yaml`
- Create: `skills/wechat-to-amap-navigate/README.md`
- Create: `skills/wechat-to-amap-navigate/tests/cases.json`

**Interfaces:**
- WeChat-to-Amap input: `{ conversation: string | null }`.
- Success: extracted address is shown in Amap destination field and navigation starts.

- [ ] **Step 1: Add failing extraction fixtures**

Fixtures include one clear address, multiple addresses, no address, ambiguous recipient, and an Amap login/permission prompt.

- [ ] **Step 2: Implement sequential skills**

Read latest allowed conversation context, extract one address, invoke Amap package with destination, verify destination, start navigation, and report route state.

- [ ] **Step 3: Verify and run physical acceptance**

Run 20 trials each for WeChat, NetEase, and WeChat-to-Amap on the target Redmi. Record results in `docs/10-acceptance-record.md`; zero wrong-recipient and zero credential automation are release blockers.

- [ ] **Step 4: Commit**

```bash
git add skills docs/10-acceptance-record.md
git commit -m "feat(skills): add first personal jarvis workflows"
```
