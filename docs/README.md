# 文档索引

仓库中的文档是产品、技术和协作事实来源。开始工作前先读根目录 `AGENTS.md`，再按任务读取本页列出的文档。

## 阅读顺序

1. [产品宪章](00-product-charter.md)
2. [V1 产品需求](01-prd.md)
3. [系统架构](02-system-architecture.md)
4. [当前交接](HANDOFF.md)
5. 与任务相关的专项文档和 ADR

## 核心文档

| 文档 | 内容 | 主要更新者 |
| --- | --- | --- |
| [00-product-charter.md](00-product-charter.md) | 用户、原则、边界和成功标准 | 产品负责人 |
| [01-prd.md](01-prd.md) | 小猪搭手 1.0 用户流程、行为与验收 | 产品 + Android |
| [02-system-architecture.md](02-system-architecture.md) | 组件、数据流、信任边界和部署 | 架构负责人 |
| [03-agent-and-model-integration.md](03-agent-and-model-integration.md) | 任务循环、工具协议、模型供应商接口 | 模型运行时负责人 |
| [04-android-execution.md](04-android-execution.md) | 无障碍、Shizuku、权限和技能执行 | Android 负责人 |
| [05-security-privacy.md](05-security-privacy.md) | 威胁模型、脱敏、审批和日志 | 安全负责人 |
| [06-testing-and-evaluation.md](06-testing-and-evaluation.md) | 单元、集成、真机和模型评测 | QA + 模块负责人 |
| [07-roadmap.md](07-roadmap.md) | 里程碑、依赖和退出条件 | 项目负责人 |
| [08-documentation-process.md](08-documentation-process.md) | 文档状态、ADR 和变更流程 | 文档负责人 |
| [09-open-source-readiness.md](09-open-source-readiness.md) | 许可证、治理和安全发布清单 | 维护者 |
| [10-acceptance-record.md](10-acceptance-record.md) | 真机、App 版本和发布验收记录 | QA |
| [11-device-setup.md](11-device-setup.md) | 专用手机、ADB 和 Shizuku 配置 | Android |
| [12-naming-proposal.md](12-naming-proposal.md) | 正式产品名称候选和改名清单 | Product |
| [14-app-skill-architecture.md](14-app-skill-architecture.md) | 通用能力与具体 App 技能的分层 | Agent Runtime + Android |
| [13-brand-character-and-voice.md](13-brand-character-and-voice.md) | 猪哥形象、悬浮状态、唤醒词和 TTS 声音方向 | Product + Design |
| [15-character-ip-bible.md](15-character-ip-bible.md) | 全身母版、产品变体、3D 前置条件和产权清单 | Product + Design |
| [HANDOFF.md](HANDOFF.md) | 最近交付、验证和下一步 | 当次主 Agent/开发者 |
| [model-compatibility.md](model-compatibility.md) | 已接入模型、能力和评测状态 | 模型负责人 |
| [release/v0.1.0-checklist.md](release/v0.1.0-checklist.md) | 首次公开发布门槛和发布状态 | 维护者 |

## 专题目录

- `adr/`：不可轻易推翻的架构与产品决策。
- `guides/`：新增模型、App 技能、团队协作和发布流程。
- `superpowers/plans/`：按子系统拆分的可执行开发计划。
- `superpowers/specs/`：已确认的跨模块设计规格。
- `release/`：版本发布检查。
- `../design/brand/`：品牌角色、Android 图标和六态交互源文件。
- `../design/ip/`：猪哥全身、表情、手势和后续 3D 母版资产。
- [agents/](agents/README.md)：按角色拆分的 Agent 任务说明；实际启动时仍需引用本页和模块规则。

## 文档状态

每份规格文档开头使用统一状态块：

```text
> Status: Draft | Accepted | Superseded
> Owner: 角色或团队
> Last reviewed: YYYY-MM-DD
```

`Draft` 表示仍可改变，`Accepted` 表示实现应遵循，`Superseded` 必须链接替代文档。ADR 一旦接受不直接改写结论；新决策通过新 ADR 取代。

## 事实优先级

1. 共享契约、测试和迁移。
2. 已接受 ADR 和架构文档。
3. PRD 与专项规格。
4. 原型和设计稿。
5. `HANDOFF.md` 的阶段信息。

代码与文档冲突时，在同一 PR 中修正冲突，不允许让下一个开发者猜测。
