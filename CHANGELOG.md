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
- 正式确认产品名为“小猪搭手”。
- 改进按住说话反馈：录音时展开状态、触感、脉冲和实时部分转写，并延迟未听清提示。
- 增加开发版任务规划器和 Android 任务客户端，接通语音文字到本地打开 App 的端到端链路。
- 完成“语音打开系统设置”的真机端到端验收。
- 增加结构化 App 技能 DSL、微信/网易云/高德/跨 App 技能草稿和技能录制工具。
- 将系统短句识别替换为火山引擎双向流式实时语音，持续采集 16 kHz PCM、显示累计字幕，
  松手后提交完整文本。
- 增加 DeepSeek 意图规划器，可从自然语言抽取微信收件人和任意普通消息正文。
- 增加 App 技能录制、OCR 辅助、中文剪贴板输入和微信文件传输助手动作链。
- 增加公开仓库许可证、品牌边界、行为准则、支持渠道、Dependabot 和 Issue Forms。

### Changed

- 将确认策略调整为风险分级：普通低风险消息可自动发送，支付与敏感信息必须人工处理。
- 项目正式命名为 Personal Jarvis，产品范围调整为开发者专用 Android Agent 设备。

### Security

- 明确 API 密钥、截图、验证码、支付信息和 `FLAG_SECURE` 页面边界。
- DeepSeek 和火山密钥只存在于服务端环境变量，客户端只使用短期设备 Token。
- 非文件传输助手联系人、敏感消息和模型非法输出均停止自动执行。
