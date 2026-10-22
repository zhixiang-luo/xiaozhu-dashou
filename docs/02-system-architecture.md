# 系统架构

> Status: Draft
> Owner: Architecture
> Last reviewed: 2026-10-23

## 总体形态

```text
语音/文字/悬浮球
        │
        ▼
Android App ── HTTPS/WSS ──> Orchestrator ──> ModelProvider[]
        │                         │
        │                         └─ Task store / model routing / prompts
        │
        ├─ Observation (UI tree, metadata, redacted screenshot)
        ├─ Local PolicyGate
        ├─ ExecutionDriver
        │    ├─ ShizukuDriver
        │    └─ AccessibilityDriver
        └─ Verification / audit summary
```

手机负责观察、授权、确认、执行、验证和个人记忆；云端只负责规划与视觉理解，不直接接触设备注入接口。Developer Edition 默认可长期保留屏幕与任务上下文。

## 组件职责

### Android App

- Kotlin + Jetpack Compose，计划 `minSdk 30`。
- 主 App 管理任务、权限、白名单、模型/能力状态和设置。
- 悬浮球承载语音入口、实时进度、确认和接管。
- `AccessibilityService` 提供前台 App、控件树和截图能力。
- `ShizukuDriver` 提供增强输入；不可用时切换 `AccessibilityDriver`。
- `PolicyGate` 和 `Redactor` 完全位于本机，决定什么可上传、可执行和必须停止。
- `MemoryStore` 维护屏幕时间线、任务历史、联系人别名、技能和偏好，但排除密码、验证码与支付凭据。

### Orchestrator

- TypeScript + Fastify + PostgreSQL，先使用单机 Docker Compose 部署。
- 维护任务事件、确认状态、会话超时和匿名运行指标。
- 通过 `ModelGateway` 选择规划、视觉定位和可选辅助模型。
- 只返回契约定义的动作，不持有手机控制能力。
- 对模型调用执行超时、重试、版本记录、成本上限和 schema 校验。

### Shared Contracts

- 定义任务状态、观察、动作、错误、确认和供应商能力。
- Android、服务端、评测工具使用同一 schema 和测试向量。
- 任何破坏性协议变化必须提升协议版本或增加兼容迁移。

### Skills

- 存放一个 App 或一组流程的可版本化知识和确定性步骤。
- 通用 Agent 不包含“张三按钮在左下角”一类选择器。
- 技能可选择 Shizuku 或无障碍执行，但必须声明前置条件和结果验证。

## 任务数据流

1. App 将语音转为文字，生成本地 `TaskSession`。
2. App 采集前台包名、紧凑 UI 树和必要截图，并在本地脱敏。
3. Orchestrator 选择规划模型，返回下一步动作或确认请求。
4. App 对动作做 schema、能力、风险和时效校验。
5. 合法动作交给当前 `ExecutionDriver` 执行。
6. App 重新观察并对预期状态做验证，将最小结果回传。
7. 循环直到完成、失败、取消或需要人工处理。

## 状态机

```text
queued
  → observing
  → planning
  → acting / awaiting_confirmation
  → verifying
  → completed | failed | cancelled
```

- `awaiting_confirmation` 只接受与当前动作绑定的确认结果。
- `cancelled` 是终态；之后到达的模型动作必须由动作序号和任务代次拒绝。
- 网络断开时保留本地任务状态，不把超时误判为完成。
- 重复动作使用 `actionId` 和幂等窗口，避免点击后重试造成二次发送。

## 信任边界

1. **远端模型不可信**：只可提出合法动作。
2. **屏幕内容不可信**：可能包含误导或提示词注入。
3. **第三方 App 不稳定**：选择器、广告、登录和更新随时改变。
4. **用户设备是敏感环境**：本地策略优先于模型置信度。
5. **服务端数据最小化**：不默认保存截图、聊天文本和联系人信息。

## 部署

- 开发：Android 真机 + HTTPS 云端编排；可用本地隧道访问开发服务。
- 生产候选：单个 Linux 主机、Docker Compose、PostgreSQL、反向代理与 TLS。
- 密钥由环境变量或托管密钥服务提供；客户端只持有短期设备令牌。
- 任务事件按保留期清理；调试采集需要独立开关和自动过期。
- Developer Edition 默认保留屏幕时间线和任务轨迹，仍对密钥与支付凭据做硬隔离。
