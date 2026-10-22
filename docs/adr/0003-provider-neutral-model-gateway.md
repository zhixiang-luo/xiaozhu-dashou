# ADR-0003: 模型供应商中立网关

> Status: Accepted
> Date: 2026-10-23
> Owners: Model Platform

## Context

项目需要支持多个规划、视觉、语音模型，并允许不同团队并行开发。直接调用某家 SDK 会把供应商私有字段扩散到 Android、评测和技能层。

## Decision

所有模型通过标准 `ModelProvider` 接口接入，角色分为 planner、vision_grounder、speech_to_text 和 text_to_speech。供应商 adapter 位于独立目录，统一映射到共享 schema 和错误模型。

## Consequences

- 新增模型不改 Android 和通用任务协议。
- 路由、评测、成本、限流和健康检查可以跨供应商比较。
- adapter 需要更多兼容性测试和格式转换代码。
- 不支持的供应商能力必须显式声明，不能静默降级。

## Alternatives Considered

- 单供应商 SDK：首个版本快，但切换和团队并行成本高。
- 在业务代码中按模型散落条件分支：短期方便，长期难以测试和维护。

## Migration / Follow-up

首个 adapter 完成后立即建立契约测试，第二个 adapter 必须复用同一测试套件，防止抽象只适配一家供应商。
