# Personal Jarvis

Personal Jarvis 是一个运行在开发者专用 Android 手机上的个人 Agent。用户通过持续语音或悬浮入口发起任务，Jarvis 观察屏幕、通知和个人上下文，规划并执行日常操作；账号、支付、验证码和身份验证由本人完成。

> 项目正式名称：Personal Jarvis。Android 包名和开源许可证仍待发布前确定。

## 当前状态

项目已正式成立，当前进入 **首个 Android 垂直切片阶段**。共享协议、云端编排和可编译 Android App 已经建立；尚未连接专用真机验证系统权限和真实 App 操作。

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

## Personal Jarvis 1.0 目标

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
- [当前交接状态](docs/HANDOFF.md)
- [Agent 角色说明](docs/agents/README.md)

## 计划中的仓库结构

```text
personal-jarvis/
├── AGENTS.md
├── packages/contracts/       共享协议与类型
├── services/orchestrator/    云端编排服务
├── android/                  Kotlin Android 客户端
├── skills/                   App 操作技能
├── docs/                     产品、架构、ADR 与协作文档
└── .github/                  PR 与 Issue 模板
```

## 参与开发

先阅读根目录 `AGENTS.md`、`docs/README.md` 和对应模块的 `AGENTS.md`。新增模型、App 技能或跨模块协议的流程分别见：

- [新增模型供应商](docs/guides/adding-model-provider.md)
- [新增 App 技能](docs/guides/adding-app-skill.md)
- [多 Agent 与团队协作](docs/guides/agent-collaboration.md)

## 当前已验证

- 共享合约通过 5 项 Vitest 测试和 TypeScript 严格类型检查。
- 云端编排通过 12 项 Vitest 测试和 TypeScript 严格类型检查，包含 WebSocket 事件流和确认绑定。
- 技能 manifest 包通过 4 项测试和 TypeScript 严格类型检查。
- Android 14 项 JVM 测试通过，Debug APK 构建成功，路径为 `android/app/build/outputs/apk/debug/app-debug.apk`。
- 已接入 AccessibilityService、页面风险门禁、取消代次和 Shizuku UserService。
- Redmi K60 / Android 15 / HyperOS OS3.0 真机已验证安装、无障碍读取与 Shizuku 授权。
- 文档、链接和 YAML 结构检查通过。
- 尚未执行微信、网易云、高德等真实 App 自动化任务。

## 许可证

许可证尚未选择。决策完成前不得把仓库公开为正式开源项目；候选方案和发布阻塞项见 [开源准备](docs/09-open-source-readiness.md)。
