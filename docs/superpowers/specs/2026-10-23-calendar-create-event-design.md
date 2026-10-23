# 系统日历创建场景设计

> 状态：已确认
> 日期：2026-10-23

## 目标

用户通过自然语言创建个人日历事件，例如：

> 帮我明天下午三点创建一个和朱哥开会的日程。

DeepSeek 将相对日期解析成带时区的 ISO 时间，Android 使用系统
`CalendarContract` 写入本地日历，并在插入后回读标题与时间完成验证。

## 架构

```text
实时语音
  -> DeepSeek + 当前日期/时区
  -> calendar.create-event Intent
  -> 服务端时间与参数校验
  -> calendar_create_event Action
  -> Android 日历权限
  -> CalendarContract 写入
  -> 回读事件验证
```

## 意图参数

- `title`：必填，1–200 字。
- `startTime`：必填，带时区的 ISO 8601。
- `durationMinutes`：可选，默认 60，范围 1–1440。
- `timeZone`：可选，默认设备时区；首版固定使用 `Asia/Shanghai`。

模型必须使用服务端提示中给出的当前日期和时区，不能直接依赖自身训练时间。

## Android 执行

- 申请 `READ_CALENDAR` 和 `WRITE_CALENDAR`。
- 选择可见、可写、优先生主日历的 CalendarContract 日历。
- 写入 `Events`：标题、开始、结束、时区。
- 插入后按 event ID 回读并验证。
- 无权限、无日历、写入失败或回读不一致时停止。

## 安全与限制

- 首版只创建个人事件，不自动邀请联系人、不发送邮件。
- 明确的用户指令视为本次创建授权。
- 过去时间、结束不晚于开始、超过 24 小时的事件拒绝。
- 日历 Provider 不支持或权限未授权时提示用户，不退回盲点 UI。

## 验收

- “明天下午三点和朱哥开会”返回一个 `calendar_create_event` 动作。
- 模型提供的 ISO 时间被正确转换为 epoch 毫秒。
- Android 写入后能回读标题、开始和结束时间。
- 真机日历 App 能看到新建事件。
