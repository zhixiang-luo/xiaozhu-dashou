# Personal Jarvis 当前交接

> Last updated: 2026-10-23

## Current State

项目已正式命名为 Personal Jarvis，定位为开发者专用 Android Agent 设备。共享合约、云端编排、技能 schema 和可编译 Android App 已落地；当前进入无障碍观察与执行驱动开发。

## Completed

- 明确 Android 优先、云端编排、本机执行的产品技术方向。
- 明确 Shizuku 优先、无障碍降级的执行策略。
- 明确低风险消息自动发送、支付/敏感信息人工处理的风险策略。
- 建立产品、PRD、架构、Agent、模型、安全、测试和路线图文档。
- 建立根目录与模块级 `AGENTS.md`。
- 建立 ADR、模型接入指南、App 技能指南和 GitHub 协作模板。
- 建立角色化 Agent 任务书、CODEOWNERS 示例和文档 CI。
- 将项目从 Mobile Agent 重命名为 Personal Jarvis，并加入 Developer Appliance Mode。
- 决定默认模型路由为 `gpt-6-luna`、`gpt-6.1-sol`、`gpt-6-astra`。
- 建立 pnpm workspace、共享合约、云端编排服务和统一 CI 检查。
- 增加 WebSocket 任务事件、确认接口和 Android CI。
- 建立技能 manifest 的 JSON Schema 与 Zod 校验包。
- 建立 Compose App 骨架并生成 Debug APK。

## Verified

- 共享合约：5 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- 云端编排：12 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- 技能 schema：4 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- Android：2 项 JVM 单元测试通过，`assembleDebug` 成功。
- `node scripts/check-docs.mjs` 通过：相对链接可解析、代码块闭合、无空文档和尾随空格。
- GitHub Actions 和 Issue 模板的 3 个 YAML 文件解析通过。
- 未发现常见密钥格式内容。
- Debug APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`。
- 尚未连接目标手机，尚未调用真实模型 API，尚未验证无障碍、Shizuku 或第三方 App。

## Open Decisions

- Android 包名和开源许可证。
- 云端服务部署主机与域名。
- 是否选择 DCO 或 CLA。

## Open Risks

- 当前没有连接专用 Redmi/HyperOS 真机。
- HyperOS 后台限制和 Shizuku 重启失效可能导致执行不稳定。
- 微信界面、输入控件和搜索结果会随版本变化。
- 消息自动发送必须实现严格的唯一收件人和敏感内容判断。
- 开源前需要完成许可证、安全披露和个人数据清理。

## Next Smallest Task

实现 AccessibilityObserver、ExecutionDriver 和 PolicyGate，完成“启动 App → 读取前台 UI → 安全取消”的下一个 Kotlin 测试切片；再连接专用真机进行安装和权限验收。
