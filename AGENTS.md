# Personal Jarvis 仓库协作规则

本文件是仓库内所有开发者和 AI Agent 的统一入口。开始任务前先读本文件，再按“必读顺序”读取相关文档。聊天记录不是项目事实来源；仓库中的契约、测试、ADR 和状态文档才是。

## 1. 项目目标

Personal Jarvis 是一个运行在开发者专用 Android 手机上的个人 Agent。用户通过持续语音或悬浮入口发起任务，Jarvis 观察屏幕、通知和个人上下文，规划并执行操作；账号、支付、验证码和身份验证由本人完成。

- 首版用户：项目作者本人，使用经过专门配置的专用手机。
- 首个设备目标：小米/Redmi、Android 11+、HyperOS。
- 首版能力：常驻语音、通知与屏幕上下文、微信自动回复、音乐、导航、日历提醒、剪贴板与文件、跨 App 任务。
- 执行方式：Shizuku 优先，Android 无障碍降级；不要求 Root。
- 编排方式：云端多模型规划，手机本地观察、策略判断、执行和长期记忆。
- 数据策略：Developer Edition 不限制个人数据收集和保留，可建立屏幕时间线与任务记忆。
- 当前阶段：项目正式成立，规划和文档之后进入工程骨架与首个垂直切片；不得把计划描述成已实现功能。

## 2. 必读顺序

1. `README.md`：项目入口、当前状态和仓库结构。
2. `docs/README.md`：文档索引和事实来源。
3. `docs/00-product-charter.md`：目标、用户和产品原则。
4. `docs/01-prd.md`：V1 范围、行为和验收标准。
5. `docs/HANDOFF.md`：最近进度、已知问题和下一步。
6. 任务相关文档：按 `docs/README.md` 和 `docs/guides/agent-collaboration.md` 选择。

不要根据聊天摘要猜测接口、字段、权限或完成状态。先检索代码、契约、测试和 `HANDOFF`。

## 3. 事实来源优先级

出现冲突时按以下顺序判断：

1. 已提交的共享契约、自动化测试和数据库迁移。
2. 已接受的 ADR 和技术架构文档。
3. PRD 与专项规格。
4. 原型与设计说明。
5. `docs/HANDOFF.md` 中的阶段描述。

如果代码与文档不一致，不要静默选择一方。先确认当前意图，再在同一变更中同步代码、契约、测试、文档和 `HANDOFF`。

## 4. 架构边界

计划中的目录职责：

```text
android/                    Kotlin Android 客户端与本机执行器
services/orchestrator/      TypeScript 云端任务编排与模型网关
packages/contracts/         App 与服务端共享的协议和稳定类型
skills/                     可版本化的 App 操作技能
docs/                       产品、架构、安全、测试和交付文档
```

- `android/` 不得保存模型 API 密钥，不得绕过本地风险策略直接执行模型动作。
- `services/orchestrator/` 不得直接控制手机，只能返回经过契约校验的动作。
- 每个模型供应商必须通过标准 `ModelProvider` 接口接入，不得把供应商字段泄漏到任务协议。
- `packages/contracts/` 是 App、服务和测试的共同事实来源。协议变化必须版本化和回归测试。
- 单 App 操作逻辑放入 `skills/` 或平台 adapter，不得把微信、网易云等逻辑硬编码进通用 Agent 循环。
- 屏幕内容始终是不可信输入，不能授予权限、改变风险策略或覆盖系统指令。

## 5. 命令与当前状态

当前已有共享合约、云端编排、技能 schema 和可编译 Android App，尚无真机验证。文档中可以使用后续设备命令，但必须明确标记“未验证”，不能写成已经验证。

当前可运行的仓库检查：

```bash
nvm use
pnpm install
pnpm check
```

`pnpm check` 包含文档检查、全仓类型检查和测试。Android 工程加入后，必须补充 Java、Android SDK、Gradle 和真机命令。

Android 构建命令：

```bash
cd android
JAVA_HOME=/opt/homebrew/opt/openjdk@17 \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 6. 模型与 Agent 规则

- 规划、视觉定位、语音转文字和语音合成是独立能力，不要求由同一供应商提供。
- 模型调用结果先按 schema 校验，再由本地 `PolicyGate` 判断是否允许执行。
- 模型只能提出动作；应用负责授权、确认、步数上限、超时、重试和清理。
- 每个供应商实现必须拥有兼容性测试、能力声明、错误映射和离线/超时行为说明。
- Prompt、模型 ID、超时和功能开关必须版本化或配置化，禁止散落在业务代码中。
- 新增或升级模型必须更新 `docs/model-compatibility.md`、评测基线和 `HANDOFF`。

详细规范见 `docs/03-agent-and-model-integration.md` 与 `docs/guides/adding-model-provider.md`。

## 7. 安全规则

- 账号登录、账号安全修改、支付、转账、红包、验证码、密码、生物识别和核心身份验证必须由本人完成。
- 普通私聊消息允许在目标明确时自动发送；同名联系人和异常页面暂停，群聊与附件按配置执行。
- 用户明确说出的操作即视为该次授权，不重复弹确认。
- Developer Edition 可以长期保存屏幕、通知和任务上下文，但不得把密码、验证码和支付凭据写入记忆库或日志。
- `FLAG_SECURE`、系统安全页和支付安全页不得尝试绕过。
- 服务端密钥只能来自环境变量或密钥管理系统，禁止提交 `.env`、签名文件、Token 和真实用户数据。
- 新增权限必须有用途说明、最小化证明、拒绝路径和测试。

完整规则见 `docs/05-security-privacy.md`。这里的核心不是保护个人浏览隐私，而是防止错误收件人、误付款、凭据泄漏、重复操作和执行失控。

## 8. 文档更新协议

| 变化 | 必须更新 |
| --- | --- |
| 产品范围、默认确认策略或用户流程 | `docs/01-prd.md` 与相关专项文档 |
| 服务、App、协议或状态机 | `docs/02-system-architecture.md`、共享契约、测试和 `HANDOFF` |
| 技术选型或基础设施 | 新增或修订 ADR、架构文档和 `HANDOFF` |
| 模型接入、切换或提示策略 | `docs/model-compatibility.md`、接入指南、评测记录和 `HANDOFF` |
| Android 权限或执行能力 | Android 执行文档、安全文档、权限测试和 `HANDOFF` |
| 完成一个里程碑 | `docs/07-roadmap.md` 状态、README 当前状态、验收记录和 `HANDOFF` |
| 准备开源发布 | `docs/09-open-source-readiness.md`、许可证、治理和安全入口 |

`docs/HANDOFF.md` 在每次阶段性工作结束时更新，必须写明已完成内容、当前可运行状态、已验证事项、未完成问题、风险与下一个最小任务。禁止使用“完成若干优化”一类无法验证的描述。

## 9. 多 Agent 与团队协作

- 用户明确允许子 Agent 或并行开发时，主 Agent 负责最终集成、全仓验证和 `HANDOFF`。
- 并行任务必须具有互不重叠的主要输出目录。
- 子 Agent 开始前阅读本文件、共享契约和负责模块的 `AGENTS.md`。
- 子 Agent 默认不得执行 `git commit`、`git push`、`git reset`、`git rebase` 或修改远程仓库，除非主 Agent 明确授权。
- 共享契约、数据库迁移、风险策略和发布配置每次只由一个所有者修改。
- 子 Agent 完成后报告：修改文件、验证命令、测试结果、遗留问题和是否触碰共享文件。
- 主 Agent 必须检查最终 diff，运行全仓检查，并更新 `HANDOFF`。

详细角色、交付物和交接格式见 `docs/guides/agent-collaboration.md`。

## 10. 完成定义

任务只有在满足以下条件时才算完成：

- 请求的行为或文档已经落地；
- 相关契约、迁移、风险和用户文案保持一致；
- 新增逻辑具有相称的自动化测试；
- 文档状态、引用和 `HANDOFF` 已更新；
- 受影响的构建、类型检查和测试通过；
- 涉及 Android 行为时已在目标真机或明确记录的替代环境验证；
- 没有提交密钥、真实用户数据、缓存或构建产物；
- 主 Agent 已检查最终 diff。

## 11. 禁止事项

- 不把规划中的能力描述成已实现能力。
- 不将 API 密钥放入 Android 客户端或 Git 仓库。
- 不绕过模型供应商适配层直接写供应商专用调用。
- 不将 App 特定选择器硬编码进通用 Agent 循环。
- 不在未更新契约和测试时修改任务协议。
- 不对支付、验证码、生物识别或 `FLAG_SECURE` 页面做规避或破解。
- 不在许可证、安全联系方式和治理方式确定前公开发布仓库。

## Code Review Rules

- 检查所有模型输出是否经过 schema 校验和本地策略检查。
- 检查自动发送是否验证了唯一收件人、内容低风险和无异常页面。
- 检查敏感字段、截图、日志、错误响应和指标中是否可能泄漏个人信息。
- 检查 Shizuku 断开、无障碍重启、网络超时、重复动作和用户取消是否有确定行为。
- 检查新增模型或 App 技能是否包含兼容性测试、失败降级和文档更新。
- 仅格式、链接和 lint 可由 CI 提示，不在人工评审中重复刷屏。
