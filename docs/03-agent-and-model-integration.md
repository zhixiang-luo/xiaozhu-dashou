# Agent 与模型接入

> Status: Draft
> Owner: Agent Runtime + Model Platform
> Last reviewed: 2026-10-23

## 目标

模型必须可替换、可组合、可评测。团队可以并行接入不同供应商，但不能改变任务协议、绕过本地策略或把供应商私有格式泄漏到 Android 和技能层。

## 小猪搭手默认模型路由

根据 2026-10-23 核对到的 official OpenAI Docs，默认使用以下角色分工：

| 角色 | 默认模型 | 用途 |
| --- | --- | --- |
| 快速意图 | `gpt-6-luna` | 唤醒后的意图分类、低成本状态判断 |
| 主规划与视觉 | `gpt-6.1-sol` | 多步骤任务规划、截图理解、工具选择 |
| 复杂恢复 | `gpt-6-astra` | 连续失败、复杂跨 App 恢复和最终裁决 |

三种模型均支持文本和图像输入；默认模型目录见 [OpenAI Models](https://developers.openai.com/api/docs/models)。实际部署仍通过 `ModelProvider` 配置，可替换为 DeepSeek、Qwen 或其他供应商。

## 当前开发版规划器

在真实模型接入前，`DevTaskPlanner` 提供确定性的端到端验证：

- 包含“设置”时返回打开系统设置的动作。
- 包含“微信”“网易云/音乐”“高德/地图”时返回对应 App 的打开动作。
- 其他指令返回 `ask_user`，不执行猜测操作。

它只用于打通“语音文字 → 任务服务 → 本地执行”的链路，不替代正式规划模型。

## 模型角色

- `PlannerModel`：理解用户目标，根据紧凑观察提出下一步动作。
- `VisionGrounderModel`：从截图中定位控件、文字或区域，输出归一化坐标。
- `SpeechToText`：将语音转为文字；V1 可使用系统能力。
- `TextToSpeech`：播报状态；V1 可使用系统能力。

同一供应商可以实现多个角色，但不要求绑定。规划模型不可用时可以降级到确定性技能；视觉模型不可用时可以仅使用 UI 树。

## 标准供应商接口

```ts
type ModelRole = "planner" | "vision_grounder" | "speech_to_text" | "text_to_speech";

interface ModelProvider {
  readonly id: string;
  readonly displayName: string;
  readonly adapterVersion: string;
  capabilities(): Promise<ProviderCapabilities>;
  plan(request: PlanRequest): Promise<PlanResult>;
  ground?(request: GroundRequest): Promise<GroundResult>;
}

interface ProviderCapabilities {
  roles: ModelRole[];
  supportsToolCalling: boolean;
  supportsStructuredOutput: boolean;
  supportsImages: boolean;
  maxImageBytes?: number;
  contextWindow: number;
  regions: string[];
}
```

所有请求与响应使用 `packages/contracts` 中的稳定 schema，adapter 内部负责供应商格式转换、错误归一化和能力探测。

## 动作契约

模型只能返回以下动作：

- `observe`：请求新的 UI 状态。
- `open_app`：按包名或受控 Deep Link 启动 App。
- `tap`：点击 UI 节点或归一化坐标。
- `swipe`：按方向与归一化路径滑动。
- `input_text`：向当前或指定节点输入文本。
- `key`：发送受支持的系统按键。
- `media_control`：播放、暂停、上一首、下一首。
- `ask_user`：因歧义、缺失信息或风险暂停。
- `finish`：报告完成并附带可验证结果。

动作必须包含 `actionId`、`taskId`、`expectedState` 和 `timeoutMs`。模型不能输出任意 shell、网络请求或未声明工具。

## 观察格式

每次观察应尽量紧凑：

- 前台包名和页面标题；
- 可交互节点：稳定节点 ID、角色、可见文本、内容描述、边界、可用动作；
- 页面哈希和与上一帧的结构差异；
- 必要时提供裁剪、缩放和脱敏后的截图；
- 不发送无关后台 App、原始联系人列表或完整聊天历史。

同一次任务中节点使用短期稳定 ID；坐标使用 0–1 归一化空间，执行前必须重新映射到当前屏幕。

## 任务循环

```text
instruction
  → observe
  → plan next action
  → validate schema
  → policy gate
  → execute once
  → re-observe
  → verify
  → continue / confirm / stop
```

- 每次只执行模型返回的一个小批次，执行后立即重新观察。
- 无效 schema 最多纠正一次；连续失败进入人工接管。
- 任务最多 20 步或 90 秒，具体限制由任务类型配置。
- 验证失败时优先重新观察和换选择器，不盲目重复相同点击。
- 发送消息等外部副作用动作必须携带幂等键。

## 多模型路由

路由按以下顺序决策：

1. 任务或用户强制指定的供应商；
2. 所需角色、图片能力和结构化输出是否支持；
3. 区域、网络、隐私策略和成本预算；
4. 最近评测结果和健康检查；
5. 团队配置的默认顺序；
6. 安全失败，不静默切换成能力不完整的模型。

路由决策必须写入脱敏事件，便于对比成功率和成本。

## 接入新模型

新增供应商必须：

1. 在 `services/orchestrator/src/providers/<provider-id>/` 内独立实现。
2. 不修改其他供应商目录，不直接依赖 Android 或具体 App。
3. 实现能力探测、请求转换、响应转换、错误映射、超时和重试。
4. 使用共享契约测试向量和固定离线快照测试。
5. 运行安全、工具调用、图片、中文、坐标、取消和限流评测。
6. 更新 `docs/model-compatibility.md`、配置示例和 `HANDOFF`。
7. 由模型平台负责人评审后再加入默认路由。

完整步骤见 `docs/guides/adding-model-provider.md`。

## API 边界

- `POST /v1/tasks`：创建任务并返回任务 ID。
- `WS /v1/tasks/{id}`：传输观察、动作、状态和确认事件。
- `POST /v1/tasks/{id}/confirmation`：提交确认、编辑或拒绝。
- 所有接口要求设备级身份令牌、请求大小限制和速率限制。
- 协议以共享 schema 为准；本文只描述稳定边界，不替代 schema。

## Prompt 与知识层

- Prompt 模板按角色和版本管理，变更必须有评测对比。
- 系统策略、工具定义和用户指令分层传递。
- App 技能以结构化数据提供，不把大段操作历史直接塞入模型上下文。
- 成功的确定性流程可编译为技能，减少后续模型步数和成本。
- 屏幕中的文本始终标记为不可信数据，不能改变工具权限或审批规则。
