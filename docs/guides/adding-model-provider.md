# 新增模型供应商

> Status: Draft
> Owner: Model Platform
> Last reviewed: 2026-10-23

本指南让新团队接入模型时不修改 Android、通用 Agent 循环或其他供应商。

## 1. 定义能力

先填写接入提案，说明：

- `providerId` 与维护团队；
- 实现角色：planner、vision_grounder、speech_to_text、text_to_speech；
- 是否支持工具调用、结构化输出、图片和坐标；
- 支持区域、上下文、图片尺寸、限流和价格来源；
- 数据处理与保留政策；
- 计划使用的模型 ID 和配置方式。

不确定的能力必须标为不支持或 Experimental，不能假设。

## 2. 创建隔离目录

计划目录：

```text
services/orchestrator/src/providers/<provider-id>/
├── adapter.ts
├── config.ts
├── errors.ts
├── README.md
├── fixtures/
└── adapter.test.ts
```

- adapter 只依赖 `packages/contracts` 和 `ModelProvider` 公共接口。
- 不得 import 其他供应商的 adapter。
- 不得在 App 或 skill 中出现供应商模型名和响应格式。

## 3. 实现接口

必须实现：

- 能力探测；
- 请求 schema 到供应商格式的转换；
- 响应到标准动作或计划结果的转换；
- 超时、限流、鉴权、网络和内容错误的稳定映射；
- 取消传播；
- 日志脱敏；
- 对未知动作和非法 JSON 的安全拒绝。

可选能力缺失时明确返回不支持，不伪造结果。

## 4. 配置

- API Key、base URL 和模型 ID 通过服务端环境变量或密钥服务提供。
- 提交 `.env.example` 和配置文档，不提交真实值。
- 为每项能力设置独立超时、重试和成本上限。
- Prompt 版本与模型能力版本分开记录。

## 5. 测试

必须通过共享 provider suite：

- 正常文本和中文指令；
- 工具调用和结构化 JSON；
- 非法 JSON、未知工具和空响应；
- 401/403/429/5xx/网络超时；
- 取消与 late response；
- 图片输入与非图片降级；
- 敏感数据不得进入日志和错误消息。

视觉模型还需通过屏幕分辨率、相似按钮、中文 OCR、遮挡和归一化坐标测试。

## 6. 评测与默认路由

1. 在 `docs/model-compatibility.md` 添加真实条目。
2. 运行固定评测集，保存版本、日期、成功率和成本。
3. 先以 `Experimental` 手动开启。
4. 由模型平台和 QA 评审后才可加入默认路由。
5. 更新 `HANDOFF` 和发布说明。

## 7. Review Checklist

- [ ] 供应商实现完全隔离。
- [ ] 使用共享契约和错误模型。
- [ ] 密钥不进入客户端、仓库或日志。
- [ ] 超时、取消、限流和降级已验证。
- [ ] 兼容性矩阵和评测记录已更新。
- [ ] 没有改变既有供应商行为。
