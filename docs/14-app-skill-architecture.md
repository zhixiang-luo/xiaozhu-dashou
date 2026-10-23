# App 技能架构

> Status: Draft
> Owner: Agent Runtime + Android
> Last reviewed: 2026-10-23

## 通用还是单独实现

正确方案是两层，而不是二选一：

### 通用能力层

所有 App 共享一套基础动作：

- 观察当前页面和 UI 树。
- 点击、滑动和输入。
- 打开 App。
- 提取页面文字。
- 验证页面和结果。
- 处理等待、取消和失败。

模型和视觉能力也属于通用层。它们负责理解陌生页面，但不能绕过本机策略。

### App 技能层

每个 App 用清单声明自己的业务入口和选择器，例如：

```yaml
id: wechat.send-message
appPackages: [com.tencent.mm]
steps:
  - id: open-wechat
    action: open_app
    package: com.tencent.mm
  - id: send-message
    action: send
    target: { text: 发送 }
```

技能通常只需要清单和数据，不需要单独写 Android 代码。只有当 App 提供特殊系统能力或官方 Deep Link 时才添加专用 adapter。

系统日历使用 `system_provider` 能力，直接走 Android `CalendarContract`。这仍然由
技能声明输入和验证点，但执行层不进行坐标点击：

```yaml
id: calendar.create-event
capabilities: [system_provider]
steps:
  - action: create_calendar_event
    titleKey: title
    startTimeKey: startTime
    durationKey: durationMinutes
```

### 跨 App 技能

跨 App 工作流通过 `invoke_skill` 组合已有技能：

```yaml
- id: navigate
  action: invoke_skill
  skill: amap.navigate
  inputs:
    destination: "{{destination}}"
```

## 选择器和降级

按稳定性排序：

1. 官方 Deep Link/公开接口。
2. 稳定 resource ID。
3. 可见文本和 contentDescription。
4. 结构关系。
5. OCR。
6. 视觉坐标。

前四种属于通用技能运行时；OCR 和视觉坐标用于陌生页面或技能失效后的兜底。

## 录制与维护

- 录制材料用于生成技能草稿。
- 草稿必须经过真机回放和校准才能启用。
- App 版本变化后重跑技能测试。
- 未验证的选择器不能进入默认执行路径。

录制步骤见 [录制 App 技能](guides/recording-app-skill.md)。
