# Changelog

本项目使用面向用户的变更记录。格式参考 Keep a Changelog，版本号在首次发布前保持 `Unreleased`。

## Unreleased

### Added

- 建立产品、架构、Agent 协议、模型接入、安全、测试和开源协作文档。
- 建立根目录与模块级 `AGENTS.md`。
- 建立 ADR、GitHub Issue 和 Pull Request 模板。
- 建立共享 Zod/TypeScript 协议包和契约测试。
- 建立 Fastify 编排服务、模型路由、OpenAI adapter、设备认证和任务取消接口。
- 增加 WebSocket 任务事件流、确认接口、技能 manifest 校验和 Android Compose App 骨架。
- 增加 AccessibilityService UI 观察、页面风险分类、取消门禁、无障碍驱动与 Shizuku UserService。
- 修复 HyperOS 下 Shizuku UserService 进程名导致的崩溃，并在 Redmi K60 上完成真机权限验收。
- 将可见界面切换为中文，并增加产品命名提案。

### Changed

- 将确认策略调整为风险分级：普通低风险消息可自动发送，支付与敏感信息必须人工处理。
- 项目正式命名为 Personal Jarvis，产品范围调整为开发者专用 Android Agent 设备。

### Security

- 明确 API 密钥、截图、验证码、支付信息和 `FLAG_SECURE` 页面边界。
