# 小猪搭手

<p align="center">
  <img src="design/brand/exports/brand-lockup.png" alt="小猪搭手：猪哥，暖科技任务猪" width="760" />
</p>

小猪搭手是一个运行在开发者专用 Android 手机上的个人 Agent。用户通过持续语音或悬浮入口发起任务，小猪搭手观察屏幕、通知和个人上下文，规划并执行日常操作；账号、支付、验证码和身份验证由本人完成。

> 项目正式名称：小猪搭手。开发期 Android 包名暂时保留 `com.personaljarvis`，正式发布前再决定最终包名。

## 当前状态

项目已正式成立，当前进入 **首批真实 App 技能开发阶段**。共享协议、云端编排和 Android App 已经建立；“语音 → 任务服务 → 本机打开系统设置”开发版闭环已在 Redmi K60 / HyperOS 真机通过验收。常驻唤醒、正式 TTS 和微信/网易云等第三方 App 自动化尚未接入。

当前可以运行共享合约和云端编排测试：

```bash
nvm use
pnpm install
pnpm check
```

Android 构建：

```bash
cd android
JAVA_HOME=/opt/homebrew/opt/openjdk@17 \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 小猪搭手 1.0 目标

- 常驻唤醒词、按住说话和点击悬浮球。
- 持续观察屏幕、通知、剪贴板与任务上下文。
- 打开已配置的 App，并跨 App 传递信息。
- 在微信中自动回复普通私聊消息。
- 在网易云音乐中搜索并播放歌曲，支持系统媒体控制。
- 地图搜索与导航、日历、提醒、闹钟、文件和截图检索。
- Shizuku 优先、Android 无障碍降级；不要求 Root。
- 普通消息直接发送；账号、支付、验证码和身份验证交还本人。
- 云端模型通过统一供应商接口接入，支持团队并行开发多个模型。

## 文档入口

- [文档索引](docs/README.md)
- [产品宪章](docs/00-product-charter.md)
- [V1 产品需求](docs/01-prd.md)
- [系统架构](docs/02-system-architecture.md)
- [Agent 与模型接入](docs/03-agent-and-model-integration.md)
- [Android 执行层](docs/04-android-execution.md)
- [安全与隐私](docs/05-security-privacy.md)
- [测试与评测](docs/06-testing-and-evaluation.md)
- [开发路线图](docs/07-roadmap.md)
- [文档治理](docs/08-documentation-process.md)
- [开源准备](docs/09-open-source-readiness.md)
- [验收记录](docs/10-acceptance-record.md)
- [专用设备配置](docs/11-device-setup.md)
- [产品命名提案](docs/12-naming-proposal.md)
- [App 技能架构](docs/14-app-skill-architecture.md)
- [角色、唤醒与声音设计](docs/13-brand-character-and-voice.md)
- [当前交接状态](docs/HANDOFF.md)
- [Agent 角色说明](docs/agents/README.md)

## 计划中的仓库结构

```text
xiaozhu-dashou/
├── AGENTS.md
├── packages/contracts/       共享协议与类型
├── services/orchestrator/    云端编排服务
├── android/                  Kotlin Android 客户端
├── skills/                   App 操作技能
├── design/                   品牌角色、状态稿和导出资源
├── docs/                     产品、架构、ADR 与协作文档
└── .github/                  PR 与 Issue 模板
```

## 参与开发

先阅读根目录 `AGENTS.md`、`docs/README.md` 和对应模块的 `AGENTS.md`。新增模型、App 技能或跨模块协议的流程分别见：

- [新增模型供应商](docs/guides/adding-model-provider.md)
- [新增 App 技能](docs/guides/adding-app-skill.md)
- [录制 App 技能](docs/guides/recording-app-skill.md)
- [多 Agent 与团队协作](docs/guides/agent-collaboration.md)

## 当前已验证

- 共享合约通过 5 项 Vitest 测试和 TypeScript 严格类型检查。
- 云端编排通过 14 项 Vitest 测试和 TypeScript 严格类型检查，包含开发版任务规划器。
- 技能 manifest 包通过 7 项测试和 TypeScript 严格类型检查，覆盖结构化步骤、输入、风险和 4 个随仓技能清单。
- Android 24 项 JVM 测试通过，Debug APK 构建成功，路径为 `android/app/build/outputs/apk/debug/app-debug.apk`。
- 已接入 AccessibilityService、页面风险门禁、取消代次和 Shizuku UserService。
- Redmi K60 / Android 15 / HyperOS OS3.0 真机已验证安装、无障碍读取与 Shizuku 授权。
- 真机可见界面已切换为中文，当前标题为正式名称“小猪搭手”。
- 悬浮球已启动；短按可打开主界面，长按可启动语音识别会话。
- 按住时悬浮球会展开为“正在听…”，带脉冲和触感；未开口不会提前报错，松手后才等待最终识别结果。
- 语音文字已接入 `/v1/tasks`；“打开设置/微信/网易云/高德”可由开发版规划器返回并执行 `open_app` 动作。
- 已建立微信、网易云、高德及跨 App 的结构化技能清单；真实选择器仍需按设备录制校准。
- 真机已确认端到端链路可用：长按说“打开设置”后，任务服务返回动作并由本机自动打开系统设置。
- 文档、链接和 YAML 结构检查通过。
- 尚未执行微信、网易云、高德等真实 App 自动化任务，结构化清单仍需真机校准。

## 许可证

许可证尚未选择。决策完成前不得把仓库公开为正式开源项目；候选方案和发布阻塞项见 [开源准备](docs/09-open-source-readiness.md)。
