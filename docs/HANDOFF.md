# Personal Jarvis 当前交接

> Last updated: 2026-10-23

## Current State

项目已正式命名为 Personal Jarvis，定位为开发者专用 Android Agent 设备。产品范围、模型路由、ADR 和四份子系统实施计划已建立；共享合约与云端编排骨架已开始实现。

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

## Verified

- 共享合约：5 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- 云端编排：8 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- `node scripts/check-docs.mjs` 通过：相对链接可解析、代码块闭合、无空文档和尾随空格。
- GitHub Actions 和 Issue 模板的 3 个 YAML 文件解析通过。
- 未发现常见密钥格式内容。
- 尚未构建 Android APK，尚未连接目标手机，尚未调用真实模型 API。

## Open Decisions

- Android 包名和开源许可证。
- 云端服务部署主机与域名。
- 是否选择 DCO 或 CLA。

## Open Risks

- 本机缺少 Java Runtime 和 Android SDK，Android 构建暂时阻塞。
- HyperOS 后台限制和 Shizuku 重启失效可能导致执行不稳定。
- 微信界面、输入控件和搜索结果会随版本变化。
- 消息自动发送必须实现严格的唯一收件人和敏感内容判断。
- 开源前需要完成许可证、安全披露和个人数据清理。

## Next Smallest Task

安装 JDK 17 和 Android SDK，建立可编译的 Compose App 骨架，完成“启动 App → 观察屏幕 → 取消任务”首个真机垂直切片。
