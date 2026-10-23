# App Skills

这里存放微信、系统日历、网易云音乐和高德等 App/系统能力的可版本化技能。格式和验收规范见：

- [新增 App 技能](../docs/guides/adding-app-skill.md)
- [Android 执行层](../docs/04-android-execution.md)
- [安全与隐私](../docs/05-security-privacy.md)

技能不是对第三方 App 的私有 API 声明。每个技能必须使用用户授权的界面、官方 Deep Link 或公开能力，并遵守应用条款与平台安全边界。

机器可读 schema：

- [JSON Schema](schema/skill-manifest.schema.json)
- `@personal-jarvis/skills`：Zod 校验器和 TypeScript 类型
