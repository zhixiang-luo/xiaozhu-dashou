# 模型兼容性矩阵

> Status: Draft
> Owner: Model Platform
> Last reviewed: 2026-10-23

当前尚未接入和评测模型。下表记录小猪搭手的默认候选路由；实际支持状态必须由兼容性测试更新，不能仅根据官方能力目录标记为已通过。

| Provider ID | 角色 | 模型/版本 | 工具调用 | 图片 | 中文 | 坐标定位 | 评测状态 | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| openai-fast | planner | `gpt-6-luna` | 官方支持 | 支持 | 待测 | 待测 | Not run | Model Platform |
| openai-main | planner + vision | `gpt-6.1-sol` | 官方支持 | 支持 | 待测 | 待测 | Not run | Model Platform |
| openai-recovery | planner | `gpt-6-astra` | 官方支持 | 支持 | 待测 | 待测 | Not run | Model Platform |

## 状态定义

- `Not run`：adapter 或评测尚未完成。
- `Experimental`：可手动启用，不进入默认路由。
- `Supported`：通过当前兼容性套件，可进入维护路由。
- `Deprecated`：仍保留兼容，但不得新增依赖。

## 必须记录

- adapter 版本与最终测试提交；
- 模型 ID、区域、上下文和图片限制；
- prompt 版本；
- 固定评测集版本；
- 成功率、P95 延迟、每任务成本和限流表现；
- 已知失败场景和降级策略。

模型版本变化视为兼容性变化，必须更新本表并重跑相关评测。
