# 小猪搭手当前交接

> Last updated: 2026-10-23

## Current State

项目已正式命名为小猪搭手，定位为开发者专用 Android Agent 设备。Redmi K60 真机已完成安装、无障碍观察、Shizuku UserService、悬浮语音和开发版任务规划链路；当前进入首批真实 App 技能开发。

## Completed

- 明确 Android 优先、云端编排、本机执行的产品技术方向。
- 明确 Shizuku 优先、无障碍降级的执行策略。
- 明确低风险消息自动发送、支付/敏感信息人工处理的风险策略。
- 建立产品、PRD、架构、Agent、模型、安全、测试和路线图文档。
- 建立根目录与模块级 `AGENTS.md`。
- 建立 ADR、模型接入指南、App 技能指南和 GitHub 协作模板。
- 建立角色化 Agent 任务书、CODEOWNERS 示例和文档 CI。
- 将项目从 Mobile Agent 重命名为 Personal Jarvis，后续正式定名小猪搭手，并加入 Developer Appliance Mode。
- 决定默认模型路由为 `gpt-6-luna`、`gpt-6.1-sol`、`gpt-6-astra`。
- 建立 pnpm workspace、共享合约、云端编排服务和统一 CI 检查。
- 增加 WebSocket 任务事件、确认接口和 Android CI。
- 建立技能 manifest 的 JSON Schema 与 Zod 校验包。
- 建立 Compose App 骨架并生成 Debug APK。
- 接入 AccessibilityService、页面风险分类、取消代次门禁和无障碍执行驱动。
- 接入 Shizuku Provider、AIDL UserService 和 Shizuku/无障碍自动回退。
- 在 Redmi K60 / Android 15 / HyperOS OS3.0 上完成安装与权限验收。
- 修复 HyperOS 下 Shizuku UserService 缺少进程名导致的启动崩溃。
- 将 App 可见按钮和状态切换为中文，并在真机验证页面读取显示中文结果。
- 建立中文悬浮球：短按回到主界面，长按启动系统语音识别并显示临时转写结果。
- 接入 `TaskPlannerClient` 和开发版任务规划器，支持把“打开设置/微信/网易云/高德”转换为本机动作。

## Verified

- 共享合约：5 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- 云端编排：12 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- 技能 schema：4 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- Android：24 项 JVM 单元测试通过，`assembleDebug` 成功。
- `node scripts/check-docs.mjs` 通过：相对链接可解析、代码块闭合、无空文档和尾随空格。
- GitHub Actions 和 Issue 模板的 3 个 YAML 文件解析通过。
- 未发现常见密钥格式内容。
- Debug APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`。
- 真机屏幕读取返回 `com.personaljarvis: NORMAL, 4 nodes`。
- Shizuku 服务端以 shell UID 运行，小猪搭手 UserService 进程 `com.personaljarvis:jarvis-actions` 已启动。
- 中文界面验证通过：当前应用、页面类型和控件数量的结果均为中文。
- 悬浮球前台服务已运行，系统窗口层确认 168×168 的悬浮控件存在。
- 短按悬浮球已在真机验证可以打开小猪搭手；长按手势能启动语音会话且进程不崩溃。
- 录音状态增加“正在听…”展开、触感、脉冲和实时部分文字；按住等待期间不会提前提示未听清。
- 本地任务服务已验证为“打开设置”返回 `open_app / com.android.settings` 动作。
- `adb reverse tcp:4100 tcp:4100` 已建立，手机通过回环地址访问任务服务。
- 设备所有者完成端到端真机验收：语音说“打开设置”后，系统设置 App 被自动打开。
- 设备所有者已完成新一轮真机体验测试，确认录音状态反馈与中文转写效果可接受。
- 尚未调用真实模型 API，尚未执行第三方 App 任务。

## Open Decisions

- Android 包名和开源许可证。
- 云端服务部署主机与域名。
- 是否选择 DCO 或 CLA。
- 正式产品名称仍待从命名提案中选择。

## Open Risks

- 当前没有连接专用 Redmi/HyperOS 真机。
- HyperOS 后台限制和 Shizuku 重启失效可能导致执行不稳定。
- 微信界面、输入控件和搜索结果会随版本变化。
- 消息自动发送必须实现严格的唯一收件人和敏感内容判断。
- 开源前需要完成许可证、安全披露和个人数据清理。

## Next Smallest Task

开始微信与网易云音乐的第一批真实 App 技能，并保持“先技能、后视觉兜底”的执行顺序。
