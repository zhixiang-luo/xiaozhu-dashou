# 验收记录

> Status: Draft
> Owner: QA
> Last reviewed: 2026-10-23

## 2026-10-23 / build 0.1.0

- Tester: controller agent + device owner
- Device: Redmi K60 (`23013RK75C`)
- Android / HyperOS: Android 15 / HyperOS OS3.0
- Shizuku: 13.6.0 app, server reported version 13.5/adb
- WeChat: not tested
- NetEase Cloud Music: not tested
- Driver mode: Accessibility bound, Shizuku UserService bound

### Passed

- `:app:testDebugUnitTest`
- `:app:assembleDebug`
- 14 JVM tests passed, including UI compaction, page classification, policy blocking, cancellation, and driver fallback.
- 18 JVM tests passed, including the voice session state machine.
- 21 JVM tests passed, including retry/empty-result behavior while holding the voice bubble.
- 24 JVM tests passed, including task response parsing.
- APK installed successfully through ADB.
- Accessibility service bound successfully.
- “Read current screen” returned `com.personaljarvis: NORMAL, 4 nodes`.
- Shizuku permission was granted with “始终允许”.
- Shizuku UserService process `com.personaljarvis:jarvis-actions` is running.
- Chinese UI verified: `当前应用：com.personaljarvis，页面：普通页面，控件：4 个`.
- Floating bubble foreground service started with a 168×168 system overlay.
- Short-tap navigation from the bubble to `MainActivity` verified.
- Long-press gesture started the speech recognition session without crashing.
- APK SHA-256: `03f25b6ed2254e8385ef8b2f4c4063b3da61e1e266700b30cff6f3ba75ef44d9`.
- Latest APK SHA-256: `98af92300a3b49b36b7401328c4cd86f1b43ad2c9af5c6691bc3d6fe7265e84d`.
- Recording UX build SHA-256: `6b727eee5b58b101ac9b0cb19e445c5d0967303ec3ce34a985fe49d5fd41cc4c`.
- Development orchestrator returned an `open_app` action for “打开设置” targeting `com.android.settings`.
- End-to-end task client APK SHA-256: `a997917ade6a735b97e0ea189a9d308616b4cc04df07cbd399636227d395a954`.
- Device owner verified the complete voice-to-action flow: holding the bubble, saying “打开设置”, submitting the task, receiving `open_app`, and opening Android Settings all completed successfully.
- Skill recorder smoke test saved 2 screenshot/UI-tree frame pairs from Redmi K60.
- Installed target versions recorded: WeChat 8.0.79, NetEase Cloud Music 9.6.08, Amap 17.01.0.2012.
- WeChat 8.0.79 returned empty UI hierarchies from both `uiautomator` and the app AccessibilityService; screenshot OCR is required for its skill.
- Vision OCR recovered the chat entry, input/send row, and sent message coordinates from 23 recorded frames.
- Multi-action skill build SHA-256: `d16cce76cc0d65907505135a6a66e6acd3a46951c9eb902a8478442569c40462`.
- Device owner manually verified the revised hold-to-talk UX as acceptable: recording state is visible, partial transcription is understandable, silence while holding no longer triggers a premature failure, and final transcription works.
- Device owner confirmed the Volcengine realtime voice experience has high accuracy and is substantially better than the previous system recognizer.
- A 6.8-second 16 kHz PCM sample produced 16 partial events and a complete final transcript through the real Volcengine gateway.
- DeepSeek `deepseek-chat` parsed the natural-language request
  “帮我到文件传输助手里面去发一句话，比如说，测试成功了” into the exact message
  “测试成功了” and a five-step WeChat action plan.
- Debug APK produced at `android/app/build/outputs/apk/debug/app-debug.apk`.
- DeepSeek planning and realtime voice APK SHA-256:
  `306455bdcec379722e2c71073950477dcf299bedd58358fd294c23964e3592cc`.
- Calendar-enabled APK SHA-256:
  `67e0033889c82302a896e6133ad4215ac9e0685f7bf4552a0911c81284e09523`.
- Real DeepSeek parsed “明天下午三点帮我创建一个和朱哥开会的日程” into
  `2026-10-24 15:00–16:00 Asia/Shanghai` and one `calendar_create_event` action.
- CalendarContract unit/build verification passed; physical calendar write is pending because
  the Redmi device was disconnected.

### Failed

- Initial USB ADB did not expose an interface; wireless debugging pairing succeeded.
- Initial Shizuku UserService binding crashed on HyperOS because `processNameSuffix` was missing; fixed with the explicit `jarvis-actions` suffix and rebuild.
- Accessibility must be re-enabled after APK reinstall in the current prototype.

### Release decision

Eligible for the next vertical slice: floating entry, push-to-talk, and real app actions. Payment and credential flows remain blocked.

## 记录模板

每个版本或目标 App 更新后新增一条记录：

```md
## YYYY-MM-DD / <app-or-build-version>

- Tester:
- Commit:
- Device:
- Android / HyperOS:
- Shizuku:
- WeChat:
- NetEase Cloud Music:
- Driver mode:
- Test account:

### Passed

### Failed

### Manual interventions

### Security observations

### Release decision
```

## 必须记录

- 测试的 Git commit；
- 设备型号、系统版本和 Shizuku 版本；
- 微信、网易云音乐和本 App 版本；
- 每条失败任务的输入、起始页面和错误码；
- 人工接管次数与原因；
- 任何错误收件人、重复发送、敏感信息暴露或迟到动作；
- 是否允许进入下一里程碑。

禁止用真实联系人、真实支付或生产账号填充记录。
