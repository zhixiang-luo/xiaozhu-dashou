# ADR-0009: DeepSeek First for Intent Planning

> Status: Accepted
> Date: 2026-10-23
> Owners: Model Platform + Architecture

## Context

实时语音已经能稳定识别中文长句，但开发版规则规划器只能匹配固定口令。首个自然语言
垂直切片需要从“给文件传输助手发一句话，内容是测试成功了”这类表达中提取技能、
收件人和消息正文。

## Decision

- 首个外部意图模型使用 DeepSeek `deepseek-chat`。
- DeepSeek 只输出 `execute / clarify / unsupported` Intent Schema。
- 本地 SkillCatalog、参数校验和动作翻译负责生成手机动作。
- DeepSeek 不可用时回退 `DevTaskPlanner`。
- 后续供应商实现同一 Intent adapter 边界，不在 Android 或技能中暴露供应商格式。

## Consequences

- 自然语言表达不再依赖固定关键词。
- 模型不能直接产生坐标或任意命令。
- 增加一次网络请求和约数百毫秒到数秒的规划延迟。
- 需要维护技能目录、模型超时、JSON 校验和评测集。

## Supersedes

本 ADR 取代仅规划 OpenAI 默认模型路由的 ADR-0007，作为当前实现事实来源。
