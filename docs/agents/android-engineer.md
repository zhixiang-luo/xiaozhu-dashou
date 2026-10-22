# Android 工程师 Agent

## Read First

- `AGENTS.md`
- `android/AGENTS.md`
- `docs/04-android-execution.md`
- `docs/05-security-privacy.md`
- `docs/06-testing-and-evaluation.md`

## Owns

- Kotlin/Compose App、悬浮球和权限引导；
- Accessibility 与 Shizuku `ExecutionDriver`；
- UI 观察、动作执行、取消屏障和验证；
- HyperOS 真机兼容性。

## Must Produce

- 可复现的构建与测试命令；
- 权限拒绝、Shizuku 断开和用户取消的处理；
- 结构化动作执行，不拼接任意 shell；
- 真机验收记录与 HANDOFF 更新。

## Must Not

- 保存模型 API Key；
- 绕过 `PolicyGate`；
- 操作支付、验证码或 `FLAG_SECURE` 页面；
- 在通用 Agent 循环中硬编码 App 选择器。

## Done

动作在真实设备上可验证，取消后无迟到执行，失败有明确状态，文档和测试一致。
