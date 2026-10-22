# @personal-jarvis/skills

平台无关的 App Skill manifest 校验包。Android、Orchestrator 和评测工具使用同一 schema，避免不同团队对技能格式产生不同解释。

```ts
import { parseSkillManifest } from "@personal-jarvis/skills";

const manifest = parseSkillManifest(candidate);
```

JSON Schema 位于 `skills/schema/skill-manifest.schema.json`。
