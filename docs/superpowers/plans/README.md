# Personal Jarvis 实施计划

> Status: Accepted
> Owner: Architecture
> Last reviewed: 2026-10-23

Personal Jarvis V1 按可独立验收的子系统拆分。执行顺序如下：

1. [Contracts Foundation](2026-10-23-contracts-foundation.md)：共享协议、schema、测试向量。
2. [Orchestrator and Model Gateway](2026-10-23-orchestrator-model-gateway.md)：任务 API、事件流、多模型路由。
3. [Android Observer and Executor](2026-10-23-android-observer-executor.md)：语音、悬浮入口、观察、Shizuku/无障碍执行。
4. [First App Skills](2026-10-23-first-app-skills.md)：微信、网易云音乐、高德导航和跨 App 流程。

每一份计划都必须产生可独立测试的软件。共享契约变化先更新第 1 份计划和测试，再允许其他模块跟进。

## 当前状态

| Plan | Status |
| --- | --- |
| Contracts Foundation | Implemented and verified |
| Orchestrator and Model Gateway | Core, model adapter, REST routes implemented; WebSocket pending |
| Android Observer and Executor | Blocked on local Java/Android SDK |
| First App Skills | Not started |
