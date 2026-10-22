# Changelog

本项目使用面向用户的变更记录。格式参考 Keep a Changelog，版本号在首次发布前保持 `Unreleased`。

## Unreleased

### Added

- 建立产品、架构、Agent 协议、模型接入、安全、测试和开源协作文档。
- 建立根目录与模块级 `AGENTS.md`。
- 建立 ADR、GitHub Issue 和 Pull Request 模板。

### Changed

- 将确认策略调整为风险分级：普通低风险消息可自动发送，支付与敏感信息必须人工处理。
- 项目正式命名为 Personal Jarvis，产品范围调整为开发者专用 Android Agent 设备。

### Security

- 明确 API 密钥、截图、验证码、支付信息和 `FLAG_SECURE` 页面边界。
