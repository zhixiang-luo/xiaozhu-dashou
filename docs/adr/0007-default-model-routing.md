# ADR-0007: Default OpenAI Model Routing

> Status: Accepted
> Date: 2026-10-23
> Owners: Model Platform

## Context

小猪搭手需要连续语音意图、截图理解、工具调用、复杂任务规划和失败恢复。使用一个高成本模型处理所有步骤会显著增加延迟和费用；使用一个低成本模型又无法可靠处理复杂跨 App 任务。

official OpenAI Docs 当前模型目录显示：`gpt-6-luna` 适合高量低成本任务，`gpt-6.1-sol` 提供接近旗舰能力与更低成本，`gpt-6-astra` 面向最复杂工作；三种模型均支持文本和图像输入，并提供工具能力。

## Decision

默认路由如下：

- 唤醒后的快速意图分类：`gpt-6-luna`，reasoning effort 为 `none`。
- 常规规划、视觉定位和工具选择：`gpt-6.1-sol`，reasoning effort 为 `medium`。
- 连续两次执行失败后的复杂恢复或裁决：`gpt-6-astra`，reasoning effort 为 `high`。

路由通过 `ModelGateway` 和 `ModelProvider` 实现，不进入 Android、共享动作协议或 App 技能。任何角色均可通过配置替换为其他供应商模型。

## Consequences

- 普通请求降低成本并减少等待。
- 复杂恢复仍可调用最强模型。
- 需要维护三套模型配置、兼容性评测和成本指标。
- 默认模型必须通过仓库固定评测后才能从 `Not run` 变为 `Supported`。

## Alternatives Considered

- 全部使用 `gpt-6-astra`：判断能力最强，但延迟和成本不适合高频语音交互。
- 全部使用低成本模型：容易在跨 App 恢复和多窗口判断时失败。
- 立即同时接入多个供应商：提高故障弹性，但首个垂直切片会被 provider 适配分散。

## Migration / Follow-up

第二个供应商接入时必须复用相同 `ModelProvider` 契约和评测集。模型升级或路由变化通过新 ADR 或本 ADR 的明确修订记录，不直接修改默认配置而不留痕。

官方参考：[OpenAI Models](https://developers.openai.com/api/docs/models)
