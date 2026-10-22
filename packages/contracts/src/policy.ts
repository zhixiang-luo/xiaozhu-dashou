import { z } from "zod";

export const RiskDecisionSchema = z.object({
  actionId: z.string().min(1),
  decision: z.enum(["allow", "confirm", "block"]),
  reason: z.string().min(1)
});

export type RiskDecision = z.infer<typeof RiskDecisionSchema>;
