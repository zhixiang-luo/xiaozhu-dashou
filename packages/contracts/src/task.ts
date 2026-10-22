import { z } from "zod";

export const TaskStateSchema = z.enum([
  "queued",
  "observing",
  "planning",
  "acting",
  "verifying",
  "awaiting_confirmation",
  "completed",
  "failed",
  "cancelled"
]);

export type TaskState = z.infer<typeof TaskStateSchema>;
