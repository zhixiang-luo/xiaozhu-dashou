import { z } from "zod";

export const ModelRoleSchema = z.enum([
  "planner",
  "vision_grounder",
  "speech_to_text",
  "text_to_speech"
]);

export const ProviderCapabilitiesSchema = z.object({
  roles: z.array(ModelRoleSchema),
  supportsToolCalling: z.boolean(),
  supportsStructuredOutput: z.boolean(),
  supportsImages: z.boolean(),
  contextWindow: z.number().int().positive(),
  regions: z.array(z.string())
});

export type ModelRole = z.infer<typeof ModelRoleSchema>;
export type ProviderCapabilities = z.infer<typeof ProviderCapabilitiesSchema>;
