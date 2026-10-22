# 多 Agent 与团队协作

> Status: Accepted
> Owner: Maintainers
> Last reviewed: 2026-10-23

## 协作原则

- 契约先行：共享协议先评审合并，再并行实现 Android、服务端和测试。
- 目录所有权：每个 Agent/开发者拥有互不重叠的主要输出目录。
- 事实来自仓库：不根据聊天摘要猜测进度，读取代码、文档和 `HANDOFF`。
- 主 Agent 负责集成：子 Agent 完成局部工作，主 Agent 负责最终 diff、全仓测试和交接。

## 角色

| 角色 | 主要目录 | 主要交付 |
| --- | --- | --- |
| Product | `docs/00-*`, `docs/01-*` | 范围、流程、风险体验、验收 |
| Architecture | `docs/02-*`, `docs/adr/` | 组件边界、ADR、跨模块契约决策 |
| Android | `android/` | 权限、观察、执行、悬浮 UI、真机测试 |
| Agent Runtime | `services/orchestrator/` | 任务循环、策略、Prompt、事件 |
| Model Platform | model adapters、兼容矩阵 | 多模型接入、路由、评测 |
| App Skills | `skills/` | 单 App 确定性流程和选择器 |
| QA/Eval | test suites、验收记录 | 自动测试、真机矩阵、模型对比 |
| Security | `docs/05-*`、安全测试 | 威胁模型、脱敏、权限与发布审计 |
| Docs/Release | docs、GitHub 模板 | 文档一致性、版本、开源准备 |

一人可以承担多个角色，但共享文件仍只有一个当次所有者。

## 并行边界

推荐并行：

- Android 观察与执行；
- Orchestrator 与契约测试；
- 两个独立模型 adapter；
- 微信和网易云两个 App 技能；
- 安全测试与模型评测。

禁止并行修改：

- 同一共享 schema；
- 同一数据库迁移；
- 同一风险策略文件；
- 根配置；
- `docs/HANDOFF.md` 的同一时间段。

## 工作流

1. 主 Agent 明确目标、范围、接口和验收。
2. 先完成或确认共享契约。
3. 各 Agent 在独立目录实现并自测。
4. Agent 报告修改文件、验证命令、结果、风险和待决策项。
5. 主 Agent 复核契约兼容性、风险和文档。
6. 主 Agent 运行全仓检查并更新 `HANDOFF`。
7. 由指定 Reviewer 审查高风险变化。

## 子 Agent 完成报告

```md
## Delivered
## Files Changed
## Verification
## Risks / Open Questions
## Shared Files Touched
## Suggested Next Task
```

`Verification` 必须写实际命令和结果，不能只写“测试通过”。

## 分支与提交

- 每个任务使用独立分支或 worktree。
- 不在一个 PR 混合无关重构。
- 共享契约变化先单独 PR 或作为 PR 的第一个独立提交。
- 子 Agent 默认不提交、不推送、不变基；由主 Agent 明确授权。
- Commit 使用 Conventional Commits。

## 冲突处理

- 两个实现与契约冲突时，以已接受契约为准；契约需要改变时先评审。
- 两个文档冲突时按 `docs/README.md` 的事实优先级处理，并在同一变更中修复。
- 无法安全决定时停止共享文件修改，报告具体冲突和可选方案。

## Definition of Done

- 代码、契约、测试、文档和 `HANDOFF` 一致；
- 模块测试和全仓必需检查通过；
- 高风险路径经独立 Review；
- 没有泄漏密钥或用户数据；
- 主 Agent 已检查最终 diff。
