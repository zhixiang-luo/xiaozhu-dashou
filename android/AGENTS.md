# Android 模块规则

先阅读根目录 `AGENTS.md`、`docs/04-android-execution.md` 和 `docs/05-security-privacy.md`。

## 边界

- 这是 Kotlin/Jetpack Compose Android 原生模块。
- 不得保存模型 API Key。
- 不得执行未经共享 schema 校验和 `PolicyGate` 批准的动作。
- 不得把微信、网易云等选择器写入通用 Agent 循环。

## 执行

- Shizuku 和 Accessibility 必须实现同一 `ExecutionDriver` 行为契约。
- 参数必须结构化生成，禁止把用户文本拼接成 shell 命令。
- 每个动作检查任务代次、actionId、时效和前台 App。
- 用户取消后不得执行旧动作。
- 所有外部副作用动作必须可验证并避免重复。

## 权限与隐私

- 新权限必须说明最低必要性、拒绝后的功能和测试。
- 密码、OTP、支付与 `FLAG_SECURE` 页面必须停止。
- 截图默认只存在内存；调试保存需要显式开关和过期清理。
- 日志不得包含聊天正文、联系人、剪贴板或 Token。

## 验证

实现后运行模块构建、单元测试和受影响真机测试。UI、悬浮窗、麦克风、无障碍、Shizuku 或系统服务变化必须在 HyperOS 真机验证，并在 `HANDOFF` 记录设备与版本。
