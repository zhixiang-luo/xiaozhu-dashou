import { z } from "zod";

const ArgumentsSchema = z.record(
  z.string(),
  z.union([z.string(), z.number(), z.boolean()])
);

export const AssistantIntentSchema = z.discriminatedUnion("kind", [
  z
    .object({
      kind: z.literal("execute"),
      skillId: z.string().min(1),
      arguments: ArgumentsSchema
    })
    .strict(),
  z
    .object({
      kind: z.literal("clarify"),
      question: z.string().trim().min(1)
    })
    .strict(),
  z
    .object({
      kind: z.literal("unsupported"),
      reason: z.string().trim().min(1)
    })
    .strict()
]);

export type AssistantIntent = z.infer<typeof AssistantIntentSchema>;

export interface SkillPromptEntry {
  id: string;
  displayName: string;
  appPackages: string[];
  inputs: Record<
    string,
    {
      type: "text" | "number" | "boolean";
      required: boolean;
      description?: string;
    }
  >;
}

export interface IntentPlanningContext {
  now: string;
  timeZone: string;
}
