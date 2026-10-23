# 新增 App 技能

> Status: Draft
> Owner: Android + Agent Runtime
> Last reviewed: 2026-10-23

App 技能把高频、可验证的手机流程从通用模型的自由点击变成可测试、可版本化的确定性流程。

通用观察、点击、输入和验证能力只需要实现一次；App 技能通常只添加结构化清单。只有 MediaSession、官方 Deep Link 或特殊系统能力才需要额外 Kotlin adapter。详见 [App 技能架构](../14-app-skill-architecture.md)。

## 适用条件

- 用例高频，且能定义清楚的开始和结束状态；
- 有稳定的 UI 语义或官方 Deep Link/API；
- 可以明确判断目标是否正确；
- 风险策略和人工接管点能够定义。

单次探索流程不要立即做成技能，先在评测集验证价值。

## 目录约定

计划结构：

```text
skills/<skill-id>/
├── manifest.yaml
├── README.md
├── selectors/
├── fixtures/
└── tests/
```

示例 manifest：

```yaml
id: wechat.send-message
version: 0.1.0
displayName: 微信发送普通消息
appPackages:
  - com.tencent.mm
supportedVersions:
  - 8.0.79
prerequisites:
  - wechat.foreground
capabilities:
  - accessibility
  - shizuku
risk:
  sideEffect: external_communication
  autoSend: unique_recipient
  handoffOn:
    - payment
    - otp
inputs:
  contactName:
    type: text
    required: false
  message:
    type: text
    required: true
verification:
  - intended_conversation_visible
  - sent_message_visible
steps:
  - id: open-wechat
    action: open_app
    package: com.tencent.mm
  - id: send-message
    action: send
    target:
      text: 发送
```

字段以 `@personal-jarvis/skills` 和 `skills/schema/skill-manifest.schema.json` 为准。

## 通用能力与 App 代码

以下通常只需要 manifest：

- 打开 App；
- 按文本、描述、resource ID 或角色点击；
- 输入参数；
- 提取页面文字；
- 等待和验证；
- 调用其他 App 技能组成跨 App 流程。

以下需要额外 adapter：

- MediaSession 播放控制；
- 官方 Deep Link 参数构造；
- 特殊输入法或复杂文件选择；
- App 专门的安全页面识别。

## 流程要求

每个技能必须定义：

1. 前置条件和禁止页面；
2. 输入参数，例如联系人、消息文本或歌曲名；
3. 稳定选择器与可接受的版本范围；
4. 每一步可观察的预期状态；
5. 成功后验证；
6. 歧义、页面变化、网络和登录失败时的降级；
7. 是否产生外部副作用及幂等策略；
8. 哪些步骤需要用户确认或交还。

## 选择器优先级

1. 官方 Deep Link/App Intent；
2. 稳定 resource ID；
3. 可访问文本和 contentDescription；
4. 邻近结构和可访问性角色；
5. OCR；
6. 视觉坐标。

坐标是最后手段，必须记录屏幕比例并重新映射；不能把一次截图像素写死。

## 录制现有流程

运行：

```bash
./scripts/capture-skill-session.sh wechat.send-message
```

按 [录制 App 技能](recording-app-skill.md) 的步骤操作。录制完成后由控制器生成或更新 manifest，再进行回放校准。

## 安全要求

- 微信收件人必须唯一确定；同名时停止。
- 支付、验证码、密码和 `FLAG_SECURE` 页面不进入技能自动执行。
- 自动发送动作使用任务 ID 与消息哈希构成幂等键。
- 发送前再次观察目标会话，发送后验证消息气泡。
- 技能失败不得重试导致重复发送；先观察外部副作用是否已经发生。

## 测试

- 目标 App 的最低和最高支持版本；
- 两种以上屏幕尺寸；
- Shizuku 和无障碍驱动；
- 正常、空状态、登录、广告、网络失败和权限拒绝；
- 同名联系人/歌曲歧义；
- App 更新导致选择器失效时是否安全停止。

## 发布

- 更新技能 README、版本和兼容矩阵。
- 加入评测集并运行 V1 回归。
- 更新 `HANDOFF`。
- 未达到安全门槛的技能不得进入默认自动执行。
