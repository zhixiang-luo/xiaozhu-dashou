import { z } from "zod";
import { TaskStateSchema } from "./task.js";

export const TaskEventSchema = z.object({
  eventId: z.string().min(1),
  taskId: z.string().min(1),
  taskGeneration: z.number().int().nonnegative(),
  type: z.enum([
    "state",
    "observation",
    "action",
    "confirmation",
    "result"
  ]),
  state: TaskStateSchema.optional()
});

export type TaskEvent = z.infer<typeof TaskEventSchema>;
