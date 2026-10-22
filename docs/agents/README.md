# Agent 角色说明

> Status: Accepted
> Owner: Maintainers
> Last reviewed: 2026-10-23

这些文档是未来多人或子 Agent 并行开发时的角色任务书。它们不替代根目录和模块级 `AGENTS.md`，而是定义每个角色应读什么、交付什么、不能碰什么。

| 角色 | 任务书 | 主要输出 |
| --- | --- | --- |
| 产品负责人 | [product-owner.md](product-owner.md) | 范围、流程、风险体验、验收 |
| 架构负责人 | [architecture-owner.md](architecture-owner.md) | 边界、ADR、共享契约决策 |
| Android 工程师 | [android-engineer.md](android-engineer.md) | 本机观察、执行、权限和 UI |
| 模型平台工程师 | [model-platform-engineer.md](model-platform-engineer.md) | adapter、路由、兼容性和评测 |
| QA 与安全评审 | [qa-security-reviewer.md](qa-security-reviewer.md) | 测试矩阵、威胁模型、发布审计 |
| 文档与发布负责人 | [documentation-release-owner.md](documentation-release-owner.md) | 文档一致性、版本、开源准备 |

## 使用规则

- 启用子 Agent 前，由主 Agent 选择角色并给出明确任务、输出目录和验收条件。
- 所有角色先读根 `AGENTS.md`、`docs/README.md`、`docs/HANDOFF.md` 和对应模块规则。
- 角色说明与用户当前指令冲突时，以用户明确指令为准；但安全与权限边界不能被静默放宽。
- 子 Agent 默认不提交、不推送、不变基，最终集成由主 Agent 完成。
