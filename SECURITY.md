# Security Policy

## Supported Version

当前处于 `0.1.x` Developer Edition 阶段，仅支持经过校准的开发设备和 Android
版本。不要在生产账号或主力支付手机上使用未经验证的版本。

## Reporting

请使用仓库的 GitHub Private Vulnerability Reporting 私密报告漏洞。不要在公开
Issue 中披露密钥、聊天内容、账号信息、设备标识或可利用细节。

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
