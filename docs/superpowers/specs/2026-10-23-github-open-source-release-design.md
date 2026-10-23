# GitHub 公开发布设计

> 状态：已确认
> 日期：2026-10-23

## 目标

在 `github.com/zhixiang-luo/xiaozhu-dashou` 建立首个公开仓库，让访问者能在几分钟内
理解产品、看到真机能力、完成本地运行，并清楚知道安全和品牌边界。

## 许可证与贡献

- 代码和文档：MIT License。
- “小猪搭手”“猪哥”、Logo 和角色视觉不授权为第三方产品身份。
- 贡献采用 DCO，提交必须包含 `Signed-off-by`。
- 个人维护者拥有架构、安全策略和发布权限，社区通过 Issue/PR 参与。

## 公开首页

README 必须包含：

- 一句话定位、品牌图、状态徽章和真机演示入口；
- 为什么做、当前能做什么、明确不能做什么；
- 实时语音和意图规划的架构图；
- 5 分钟快速开始；
- DeepSeek、火山和 Android 配置；
- 风险门禁、权限与隐私边界；
- 技能扩展方法、路线图、贡献和许可证。

## 社区基础设施

- `LICENSE`
- `TRADEMARK.md`
- `CODE_OF_CONDUCT.md`
- `SUPPORT.md`
- DCO 说明和 PR 模板
- Issue Forms：Bug、功能建议、兼容性报告
- CODEOWNERS、Dependabot、CI 和 Release 检查清单

## 发布前门槛

- 工作区测试、类型检查、Android 构建和文档检查全部通过。
- Git 历史与工作区无 API Key、Token、`.env`、签名文件。
- 不包含真实聊天截图、录音、联系人、手机号或设备私密文件。
- GitHub Actions 在干净环境通过。
- Private Vulnerability Reporting 开启。
- 仓库可见性在最后一步从私有或本地状态改为公开。

## 发布步骤

1. 完成代码与文档提交。
2. 通过安全扫描和密钥审计。
3. 创建公开仓库 `zhixiang-luo/xiaozhu-dashou`。
4. 推送 `main` 分支并等待 CI。
5. 创建首个 `v0.1.0` Release 和变更说明。
