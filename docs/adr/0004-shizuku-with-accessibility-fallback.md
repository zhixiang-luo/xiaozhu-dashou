# ADR-0004: Shizuku 优先、无障碍降级

> Status: Accepted
> Date: 2026-10-23
> Owners: Android + Security

## Context

Android 无障碍可以读取 UI 并执行部分手势，但文本输入和跨控件操作在不同 ROM 上不稳定。Shizuku 可通过用户主动授权的 ADB 级能力提高稳定性，但可能在重启后失效。

## Decision

首版执行层使用 `ExecutionDriver` 抽象。Shizuku 可用时作为增强执行通道；不可用时降级为 `AccessibilityDriver`。不使用 Root。

## Consequences

- 用户需要一次性 Shizuku 配置，门槛高于普通 App。
- 同一动作需要在两个 driver 上验证行为和失败语义。
- Shizuku 不是强依赖；断开时应安全降级或停止。
- 所有输入必须结构化生成，禁止任意 shell 命令。

## Alternatives Considered

- 只用无障碍：安装门槛低，但输入和手势稳定性较差。
- Root：控制能力最强，但不适合可分发产品或普通用户。

## Migration / Follow-up

后续可以加入其他安全 driver，但必须保持动作契约、取消屏障和风险策略一致。
