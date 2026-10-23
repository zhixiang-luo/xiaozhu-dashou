import { z } from "zod";
import { parse as parseYaml } from "yaml";

const PackageNameSchema = z
  .string()
  .regex(
    /^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/,
    "invalid Android package name"
  );

const SkillInputSchema = z.object({
  type: z.enum(["text", "number", "boolean"]),
  required: z.boolean(),
  description: z.string().min(1).optional()
}).strict();

const TargetSchema = z
  .object({
    text: z.string().min(1).optional(),
    contentDescription: z.string().min(1).optional(),
    resourceId: z.string().min(1).optional(),
    role: z.string().min(1).optional(),
    fallbackPoint: z
      .object({
        x: z.number().min(0).max(1),
        y: z.number().min(0).max(1)
      })
      .strict()
      .optional()
  })
  .strict()
  .refine(
    (value) =>
      Boolean(
        value.text ||
          value.contentDescription ||
          value.resourceId ||
          value.role
      ),
    "target needs at least one selector"
  );

const StepEnvelope = z.object({
  id: z.string().min(1)
});

const SkillStepSchema = z.discriminatedUnion("action", [
  StepEnvelope.extend({
    action: z.literal("open_app"),
    package: PackageNameSchema
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("restart_app"),
    package: PackageNameSchema
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("require_foreground"),
    package: PackageNameSchema
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("tap"),
    target: TargetSchema,
    unique: z.boolean().default(true)
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("input_text"),
    target: TargetSchema,
    inputKey: z.string().min(1)
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("send"),
    target: TargetSchema
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("extract_text"),
    target: TargetSchema,
    outputKey: z.string().min(1)
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("verify"),
    predicate: z.string().min(1)
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("media_control"),
    command: z.enum(["play", "pause", "next", "previous"])
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("invoke_skill"),
    skill: z.string().min(1),
    inputs: z.record(z.string(), z.string()).default({})
  }).strict(),
  StepEnvelope.extend({
    action: z.literal("wait"),
    durationMs: z.number().int().positive().max(30_000)
  }).strict()
]);

export const SkillManifestSchema = z
  .object({
    id: z
      .string()
      .regex(/^[a-z0-9]+(?:[.-][a-z0-9]+)*$/, "invalid skill id"),
    version: z
      .string()
      .regex(/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/, "invalid semantic version"),
    displayName: z.string().min(1).optional(),
    appPackages: z.array(PackageNameSchema).min(1),
    supportedVersions: z.array(z.string().min(1)).default([]),
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
        sideEffect: z
          .enum(["none", "read", "external_communication", "payment"])
          .default("none"),
        autoSend: z
          .enum(["unique_recipient", "confirm", "never"])
          .optional(),
        handoffOn: z.array(z.string().min(1)).default([])
      })
      .strict(),
    inputs: z.record(z.string(), SkillInputSchema).default({}),
    verification: z.array(z.string().min(1)).min(1),
    steps: z
      .array(z.union([z.string().min(1), SkillStepSchema]))
      .min(1)
  })
  .strict();

export type SkillManifest = z.infer<typeof SkillManifestSchema>;
export type SkillStep = z.infer<typeof SkillStepSchema>;

export function parseSkillManifest(input: unknown): SkillManifest {
  return SkillManifestSchema.parse(input);
}

export function parseSkillYaml(input: string): SkillManifest {
  return parseSkillManifest(parseYaml(input));
}
