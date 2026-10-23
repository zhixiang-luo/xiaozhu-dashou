# 开源准备

> Status: Ready for v0.1.0 public review
> Owner: Maintainers
> Last reviewed: 2026-10-23

## 当前结论

小猪搭手采用 MIT License 公开代码和文档，使用 DCO 接受贡献。维护者为
`@zhixiang-luo`，项目名称、Logo 和猪哥角色保留为官方品牌。仓库公开发布前仍需完成
密钥扫描、安全扫描和 GitHub Private Vulnerability Reporting 配置。

## 发布前必须决定

1. 许可证：MIT License。
2. 贡献模式：DCO，不要求 CLA。
3. 治理：个人维护者负责架构、安全和发布，社区通过 Issue/PR 参与。
4. 安全披露：GitHub Private Vulnerability Reporting。
5. 品牌：官方名称、Logo 和猪哥角色使用边界见 `TRADEMARK.md`。
6. 支持范围：Developer Edition 固定设备优先，接受社区兼容性报告。

## 技术发布检查

- [ ] 所有 API Key、Token、签名文件和个人配置已移除。
- [ ] 固定测试语料和截图不含真实联系人、聊天、手机号或订单。
- [ ] 示例模型配置使用 `.env.example`，不包含真实供应商账户信息。
- [ ] README 的安装、构建、测试和已知限制与当前代码一致。
- [ ] 依赖许可证与目标许可证兼容。
- [ ] 依赖锁定并完成漏洞扫描。
- [ ] Android 权限、Shizuku 风险和数据流向有明确说明。
- [ ] 风险策略与支付/验证码边界在 README 和产品文档中一致。
- [ ] CI 在干净环境可重复运行。
- [ ] `AGENTS.md`、ADR、HANDOFF 和贡献指南已更新。

## 社区文档最低集合

- `README.md`
- `CONTRIBUTING.md`
- `SECURITY.md`
- `CODE_OF_CONDUCT.md`
- `LICENSE`
- Issue 和 PR 模板
- 架构、模型接入、App 技能和安全文档

## 发布策略

- 首个公开版本使用明确版本号和 Changelog。
- 发布 APK 与 GitHub Release 使用可验证的签名和校验值。
- 不把个人测试账号、内网地址或开发服务器密钥带入发布包。
- 公开后模型 adapter 采用独立目录和所有者，新增供应商不得修改通用 Agent 协议。
