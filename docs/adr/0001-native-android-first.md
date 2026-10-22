# ADR-0001: Android 原生优先

> Status: Accepted
> Date: 2026-10-23
> Owners: Architecture + Android

## Context

产品需要读取其他 App 的 UI、保持悬浮入口、通过 Shizuku 执行动作，并管理无障碍、语音和前台服务。这些能力主要存在于 Android 原生层。Personal Jarvis 1.0 使用开发者专用的小米/Redmi 设备。

## Decision

V1 使用 Kotlin 和 Jetpack Compose 构建独立 Android App，`minSdk 30`。iOS、跨平台 UI 和 Root 方案不进入 V1。

## Consequences

- Android 模块可直接封装权限、服务、Shizuku 和 UI 观察。
- 需要维护 Android 专属代码，未来跨平台只能复用协议、技能和云端服务。
- 首发覆盖范围窄，但与真实执行能力的匹配度最高。

## Alternatives Considered

- React Native/Flutter：UI 开发更快，但核心系统能力仍需大量原生模块。
- iOS 优先：无法实现本机跨 App 全局操作，不满足核心目标。
- Root：能力更强，但失去普通设备可用性和安全边界。

## Migration / Follow-up

未来增加 iOS 或桌面端时，通过共享契约、模型网关和任务技能复用非 UI 能力，不强行共享本机执行代码。
