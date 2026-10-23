# 火山引擎实时语音接入设计

> 状态：已确认，实施中
> 日期：2026-10-23

## 目标

替换 Android 系统 `SpeechRecognizer` 的短句识别路径，让用户按住悬浮球期间持续采集
16 kHz、单声道、16-bit PCM，通过本地编排服务转发到火山引擎双向流式 ASR，实时显示
累计中文文本，松手后提交最终完整文本。

## 非目标

- 不把火山 API Key 放进 Android APK。
- 不在本次接入中实现唤醒词、TTS 或声纹识别。
- 不尝试识别支付、验证码和账号安全页面。
- 不为多个 ASR 供应商建立完整插件市场；只保留可替换的网关接口边界。

## 架构

```text
长按悬浮球
  -> AudioRecord 持续采集 PCM16
  -> WebSocket ws://127.0.0.1:4100/v1/voice/stream
  -> orchestrator 校验设备 Token
  -> 火山 /api/v3/sauc/bigmodel
  <- ready / partial / final / error
  -> 悬浮球实时字幕
  -> 松手发送 stop，等待 final
  -> 现有 /v1/tasks 规划与手机执行
```

服务端密钥只从 `VOLCENGINE_ASR_API_KEY` 和可选
`VOLCENGINE_ASR_RESOURCE_ID` 环境变量读取。Android 只连接回环地址或 HTTPS。

## 录音时序

1. `ACTION_DOWN` 立即启动 `AudioRecord`，先把 PCM 放入内存缓冲。
2. 达到长按阈值后连接网关；连接期间继续采集。
3. WebSocket 就绪后发送缓冲音频和后续音频，悬浮球显示实时累计文本。
4. `ACTION_UP` 停止录音并发送 `{"type":"stop"}`。
5. 网关等待火山最终包；Android 收到 `final` 后沿用现有任务提交链路。
6. 连接或识别失败时明确提示；若已经存在部分文字，允许在最终超时后用当前累计文本收尾。
7. 单次录音硬上限 30 秒，超限主动停止并提交当前文本。

## 协议

客户端到网关：

- WebSocket 二进制帧：PCM16LE 音频块。
- 文本帧：`{"type":"stop"}`。
- 握手头：`Authorization: Bearer <device-token>`。

网关到客户端：

- `{"type":"ready"}`
- `{"type":"partial","text":"累计文本"}`
- `{"type":"final","text":"完整文本"}`
- `{"type":"error","code":"...","message":"中文错误信息"}`

火山返回的 `result.text` 是累计文本，客户端替换字幕，不拼接分段。

## 失败策略

- 6 秒内未连接：提示“实时语音连接超时”，停止本轮。
- 松手后 6 秒内未收到 `final`：有累计文本则按累计文本提交，否则提示未听清。
- 火山断连：返回 `VOLCANO_CLOSED`，不把空结果当作成功。
- 网关未配置 Key：连接前返回配置错误，不在服务启动时阻塞普通任务接口。
- 网络中断：本轮明确失败，用户重新按住即可；下一次只开一条新连接。

## 安全

- 保留现有“支付、验证码、登录、权限页由本人处理”的本地策略。
- 音频只在内存中流转，不写入文件、日志、数据库或任务记录。
- 服务端日志不记录转写正文和音频内容。
- WebSocket 使用现有 device Token 鉴权，不允许匿名连接。

## 验证

- TypeScript 单测覆盖帧编解码、初始请求、最终状态和鉴权拒绝。
- 使用生成的 16 kHz PCM 测试音频完成一次真实火山端到端转写。
- Android JVM 单测覆盖连接缓冲、累计字幕、停止和失败状态。
- Redmi K60 真机长按说长句，观察到实时字幕持续增长，松手后完整文本进入任务规划。
- 全仓 `pnpm check`、Android 单测和 Debug APK 构建通过。
