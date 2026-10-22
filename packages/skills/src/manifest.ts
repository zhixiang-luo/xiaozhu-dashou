import { z } from "zod";

const PackageNameSchema = z
  .string()
  .regex(
    /^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/,
    "invalid Android package name"
  );

export const SkillManifestSchema = z
  .object({
    id: z
      .string()
      .regex(/^[a-z0-9]+(?:[.-][a-z0-9]+)*$/, "invalid skill id"),
    version: z
      .string()
      .regex(/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/, "invalid semantic version"),
    appPackages: z.array(PackageNameSchema).min(1),
    prerequisites: z.array(z.string().min(1)),
    capabilities: z
      .array(
        z.enum([
          "accessibility",
          "shizuku",
          "media_session",
          "deep_link"
        ])
      )
      .min(1),
    risk: z
      .object({
        autoSend: z.enum(["unique_recipient", "confirm", "never"])
      })
      .strict(),
    verification: z.array(z.string().min(1)).min(1),
    steps: z.array(z.string().min(1)).min(1)
  })
  .strict();

export type SkillManifest = z.infer<typeof SkillManifestSchema>;

export function parseSkillManifest(input: unknown): SkillManifest {
  return SkillManifestSchema.parse(input);
}
