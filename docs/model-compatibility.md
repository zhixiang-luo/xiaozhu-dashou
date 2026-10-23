# 模型兼容性矩阵

> Status: Accepted
> Owner: Model Platform
> Last reviewed: 2026-10-23

当前已接入火山实时 ASR 和 DeepSeek 意图规划。以下状态只代表现有实现和测试结果，
不代表所有设备、网络和模型版本都已验证。

| Provider ID | 角色 | 模型/版本 | 结构化输出 | 图片 | 中文 | 评测状态 | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| volcano-asr | speech_to_text | `bigmodel` 双向流式 | partial/final 文本 | 否 | 真实 PCM 验证 | Supported | Model Platform |
| deepseek | intent | `deepseek-chat` | JSON Intent | 否 | 自然语言消息提取验证 | Supported | Model Platform |

DeepSeek 当前只处理意图，不进行视觉定位。火山当前只处理语音识别，不承担任务规划。

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
