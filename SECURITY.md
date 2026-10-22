# Security Policy

## Current Status

Personal Jarvis 尚未公开发布，当前没有受支持的生产版本。不要在公开 Issue 中披露密钥、聊天内容、账号信息或可利用细节。

## Reporting

仓库公开发布前，维护者必须启用 GitHub Private Vulnerability Reporting，并在本节补充唯一有效的私密报告入口。在此之前，不得将仓库设为公开。

报告至少包含：

- 受影响版本和 Android 设备/系统版本；
- 前置权限与配置；
- 可复现步骤；
- 影响范围；
- 是否涉及消息误发、敏感数据外传、权限提升或支付风险；
- 建议的缓解方式。

## Security Invariants

- 模型无法绕过本地策略直接执行动作。
- 没有唯一明确收件人时不得自动发送消息。
- 支付、验证码、密码、银行卡和 `FLAG_SECURE` 内容不得上云或自动操作。
- API 密钥只存在于服务端密钥环境。
- 用户取消任务后，任何迟到的模型动作必须被拒绝。
- 日志、指标和调试产物不得包含原始聊天、截图或身份凭据。

完整威胁模型和测试要求见 `docs/05-security-privacy.md`。
