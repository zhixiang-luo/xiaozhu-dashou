# Architecture Decision Records

ADR 记录重要、长期且不容易逆转的决策。它们保存当时的上下文和后果，不追求永远正确。

## 状态

- `Proposed`：正在评审。
- `Accepted`：当前实现应遵循。
- `Superseded`：已被新 ADR 替代，必须链接替代文档。
- `Deprecated`：不再推荐，但尚未完全移除。

## 列表

| ADR | 决策 | 状态 |
| --- | --- | --- |
| [0001](0001-native-android-first.md) | Android 原生优先 | Accepted |
| [0002](0002-cloud-orchestration-local-execution.md) | 云端编排、本地执行 | Accepted |
| [0003](0003-provider-neutral-model-gateway.md) | 模型供应商中立网关 | Accepted |
| [0004](0004-shizuku-with-accessibility-fallback.md) | Shizuku 优先、无障碍降级 | Accepted |
| [0005](0005-risk-based-confirmation.md) | 风险分级确认 | Accepted |
| [0006](0006-developer-appliance-mode.md) | 开发者专用设备模式 | Accepted |

## 模板

```md
# ADR-NNNN: Title

> Status: Proposed
> Date: YYYY-MM-DD
> Owners: Role

## Context
## Decision
## Consequences
## Alternatives Considered
## Migration / Follow-up
```
