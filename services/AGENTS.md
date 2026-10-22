# 云端服务模块规则

先阅读根目录 `AGENTS.md`、`docs/02-system-architecture.md`、`docs/03-agent-and-model-integration.md` 和 `docs/05-security-privacy.md`。

## 边界

- Orchestrator 只规划和返回契约动作，不直接控制手机。
- 供应商 SDK、模型 ID 和响应格式只能存在于对应 adapter 目录。
- 通用 Agent 循环不得出现供应商或具体 App 的条件分支。
- 所有模型输出必须经过 schema 校验、超时和本地可执行性约束。

## 模型接入

- 新供应商遵循 `docs/guides/adding-model-provider.md`。
- 每次调用记录 provider、model、adapter 和 prompt 版本，不记录原始敏感内容。
- 超时、限流、取消和能力不支持必须有明确错误。
- 模型版本或 Prompt 变化必须更新兼容矩阵和评测。

## 数据与安全

- 使用最小字段和短期保留。
- 不默认保存截图、聊天正文或联系人。
- API Key 只来自服务端密钥环境。
- 请求体、WebSocket 消息和模型响应设置大小限制。
- 日志、指标和错误响应不得包含敏感数据。

## 验证

运行模块类型检查、单元测试、adapter 契约测试和受影响集成测试。跨服务协议变化必须同时更新 `packages/contracts` 和 Android 调用方。
