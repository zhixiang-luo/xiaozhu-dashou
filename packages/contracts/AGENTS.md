# 共享契约模块规则

先阅读根目录 `AGENTS.md` 和 `docs/03-agent-and-model-integration.md`。

## 职责

这里只放 Android、Orchestrator、评测工具共同使用的稳定协议、DTO、枚举和 schema。不得依赖 Android、数据库、网络或某一模型供应商。

## 修改规则

- 任何字段、枚举和状态变化必须有兼容性说明。
- 破坏性变化提升协议版本或提供明确迁移路径。
- 每次 schema 变化同时增加合法、非法、边界和旧版本测试向量。
- 不把供应商私有响应字段放入共享契约。
- 不把特定 App 的选择器或业务步骤放入共享契约。

## Review

共享契约由架构负责人和受影响调用方共同 Review。Android、服务和模型 adapter 不得各自私自定义同名但不兼容的结构。
