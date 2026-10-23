# Android 执行层

> Status: Draft
> Owner: Android
> Last reviewed: 2026-10-23

## 技术基线

- Kotlin、Jetpack Compose、`minSdk 30`。
- 首要验收设备：小米/Redmi、Android 11+、HyperOS。
- 权限：无障碍服务、悬浮窗、麦克风、前台服务通知；截图优先使用无障碍能力，必要时才申请屏幕录制。
- Developer Edition 额外启用通知监听、持续屏幕采集、剪贴板历史、默认助手入口和可选自定义输入法。
- 不要求 Root，不读取系统分区，不修改其他 App。

## 执行驱动

```kotlin
interface ExecutionDriver {
    suspend fun capabilities(): DriverCapabilities
    suspend fun observe(): Observation
    suspend fun execute(action: Action): ActionResult
    suspend fun cancel(actionId: String)
}
```

### ShizukuDriver

- 通过 Shizuku 获取受限的 shell 级能力，用于稳定的点击、滑动、按键和输入。
- 实际执行代码运行在 Shizuku UserService 中；只接受结构化参数，不开放任意 shell 接口。
- 所有参数必须由结构化动作生成，禁止拼接用户文本为 shell 命令。
- 调用前校验任务代次、动作 ID、前台 App 和动作时效。
- 用户取消、Shizuku 断开或权限撤销后立即停止新动作。

### AccessibilityDriver

- 使用 `AccessibilityService` 读取前台包名和紧凑 UI 节点树。
- 优先按节点 ID、文本、描述和可操作角色执行。
- 节点不可用时可使用截图定位的归一化坐标，但必须重新映射屏幕尺寸。
- Shizuku 不可用时作为降级路径，并明确告知用户能力可能受限。

### 当前实现

- `FallbackExecutionDriver` 在 Shizuku 不可用时自动选择无障碍驱动。
- `ShizukuUserServiceBridge` 通过 AIDL 执行点击、滑动、文本输入、按键和打开 App。
- `AccessibilityObserver` 只提供前台 UI 树、短期 node ID 和页面哈希。
- `PolicyGate` 在动作进入驱动前阻止登录、支付、验证码、权限和未知安全页面。
- `SafeExecutionRunner` 在取消后递增任务代次，拒绝所有迟到动作。
- `FloatingBubbleService` 提供中文悬浮球；长按 450ms 启动系统语音识别，松手后显示识别文本。
- 录音时悬浮球从圆形展开为“正在听…”胶囊，并触发触感与脉冲；部分识别结果会实时显示。
- 手指仍按住时，系统无语音超时只会静默重启识别；松手后最多等待 1.6 秒获取最终结果，再决定提示成功或未听清。
- 当前只验证语音转文字，尚未把识别结果提交给云端任务服务。
- 悬浮球已接入 `TaskPlannerClient`：语音文字提交到回环地址 `127.0.0.1:4100/v1/tasks`，再把 `open_app` 动作交给 `SafeExecutionRunner`。
- 开发期通过 `adb reverse tcp:4100 tcp:4100` 映射本机服务，不向局域网暴露任务接口。

## 权限与启动

- 首次启动分步解释每项权限的用途、数据流向和拒绝后的可用功能。
- Shizuku 未运行时提供状态与重连说明；不偷偷尝试 Root 或提权。
- 设备重启后 Shizuku 可能失效，App 必须检测并降级，不能卡死或重复执行。
- 使用精确 `<queries>` 或已知包名映射，避免申请不必要的全量应用查询能力。
- 麦克风只在用户主动按住或点击说话时使用。
- 唤醒词检测可常驻；检测和简单意图优先在本地完成。

## UI 观察

- 只采集当前前台 App 的必要节点。
- 节点输出包括 role、text、contentDescription、bounds、clickable、editable、actions 和短期 nodeId。
- 对长列表只取可见区域；滚动前记录页面哈希和目标验证条件。
- 截图优先裁剪当前目标区域；缩小时保留坐标反向映射。
- `FLAG_SECURE`、锁屏、权限弹窗、支付/密码页面和系统安全界面停止自动操作。
- Developer Edition 建立屏幕时间线和页面索引，供跨任务检索；敏感凭据页面只记录发生和暂停原因，不保存内容。

## 输入策略

1. 优先 `ACTION_SET_TEXT` 或等价结构化输入。
2. 不支持时使用受控剪贴板粘贴，但必须恢复用户原剪贴板或明确告知。
3. 仍不支持时使用 Shizuku 输入通道。
4. 任何方式都必须验证输入框最终内容，不重复盲输。

中文输入、表情、组合字符和长文本需要单独测试。

## App 技能

- 技能按 App 和版本声明前置条件、选择器、动作、验证和失败降级。
- 微信技能只操作前台已授权界面，不读取后台消息。
- 网易云技能区分媒体控制和界面搜索播放。
- 选择器优先级：稳定 resource ID > 可访问文本/描述 > 结构关系 > OCR/视觉坐标。
- App 更新导致技能失效时，回退到通用观察和模型规划，并记录待修复技能。

新增技能步骤见 `docs/guides/adding-app-skill.md`。

## 取消与并发

- 同一设备只允许一个前台操作任务；只读系统查询可另行设计。
- 每个动作带 `taskGeneration`、`actionId` 和 `createdAt`。
- 取消后更新 `taskGeneration`，执行器拒绝旧代次动作。
- 外部副作用动作在发送前再次检查前台 App、目标收件人和风险策略。
- 任务结束必须关闭悬浮提示、释放资源和删除临时截图。
- Developer Edition 的长期上下文由独立保留策略管理，不与单次任务临时文件混用。

## 专用设备建议

- 使用独立 Android 手机和测试账号，不登录主要支付、银行或生产身份。
- 通过 ADB 完成一次性 Device Owner、Shizuku 或调试配置；这些能力不是绕过系统安全的理由。
- 手机可长期插电和联网，但仍应保留锁屏、应用验证和设备恢复能力。
- 记录设备型号、系统、Shizuku、微信和高德版本，以复现兼容性问题。

## 真机兼容

- 在 HyperOS 上测试后台限制、权限自动收回、省电策略、悬浮窗和前台服务。
- 每次系统升级或目标 App 更新后，重跑固定真机场景。
- 验收记录包含设备型号、系统版本、App 版本、Shizuku 版本和测试结果。
