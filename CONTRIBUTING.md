# Contributing

感谢参与小猪搭手。代码与文档采用 MIT License，项目使用 Developer Certificate of
Origin（DCO）接受公开贡献，不要求签署 CLA。

## Before You Start

1. 阅读根目录 `AGENTS.md`。
2. 阅读 `docs/README.md`、`docs/01-prd.md` 和 `docs/HANDOFF.md`。
3. 阅读负责目录下的 `AGENTS.md`。
4. 确认任务是否改变共享契约、风险策略、模型能力或 Android 权限。

## Change Process

- 小改动可以直接提交 PR。
- 跨模块、权限、数据模型、模型供应商或风险策略变化必须先写 ADR 或规格，并获得架构负责人评审。
- 共享契约变化必须先合并契约与测试，再并行实现 Android 和云端。
- 每个 PR 只解决一个可验证问题，附带测试、文档和 `HANDOFF` 更新。
- 每个提交必须使用 `git commit -s`，提交信息末尾包含
  `Signed-off-by: Your Name <your-email@example.com>`。

## Commit Convention

使用 Conventional Commits：

```text
feat(android): add foreground app observer
feat(model): add provider capability probe
fix(orchestrator): reject stale action after cancellation
docs(adr): accept risk-based confirmation policy
test(eval): add WeChat wrong-recipient scenario
```

允许类型：`feat`、`fix`、`docs`、`test`、`refactor`、`perf`、`build`、`ci`、`chore`、`security`。

## Pull Request Checklist

- [ ] 行为和范围与 PRD/ADR 一致。
- [ ] 共享契约已版本化并更新测试。
- [ ] 新增模型通过兼容性测试和离线/超时测试。
- [ ] 新增 Android 权限有用途、拒绝路径和真机验证。
- [ ] 敏感数据、日志、指标和错误响应已检查。
- [ ] 文档和 `docs/HANDOFF.md` 已更新。
- [ ] 没有提交密钥、签名文件、真实用户数据或构建产物。

## Security

不要通过公开 Issue 报告漏洞。请使用 GitHub Private Vulnerability Reporting，
并遵守 [SECURITY.md](SECURITY.md)。
