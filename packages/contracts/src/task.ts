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

const ContextTimeSchema = z.string().min(1).max(64);

export const TaskContextSchema = z
  .object({
    currentTime: ContextTimeSchema,
    timeZone: z.string().min(1).max(64),
    currentPackage: z.string().min(1).max(255).optional(),
    screenSummary: z.string().min(1).max(500).optional(),
    upcomingEvents: z
      .array(
        z
          .object({
            title: z.string().min(1).max(200),
            startTime: ContextTimeSchema,
            endTime: ContextTimeSchema,
            timeZone: z.string().min(1).max(64)
          })
          .strict()
      )
      .max(20),
    recentTasks: z
      .array(
        z
          .object({
            summary: z.string().min(1).max(1_000),
            occurredAt: ContextTimeSchema
          })
          .strict()
      )
      .max(20),
    relevantMemory: z
      .array(
        z
          .object({
            kind: z.string().min(1).max(50),
            summary: z.string().min(1).max(1_000),
            occurredAt: ContextTimeSchema
          })
          .strict()
      )
      .max(10),
    contacts: z
      .array(
        z
          .object({
            alias: z.string().min(1).max(100),
            canonicalName: z.string().min(1).max(100)
          })
          .strict()
      )
      .max(20),
    preferences: z
      .array(
        z
          .object({
            key: z.string().min(1).max(100),
            value: z.string().min(1).max(500)
          })
          .strict()
      )
      .max(20)
  })
  .strict();

export type TaskContext = z.infer<typeof TaskContextSchema>;
