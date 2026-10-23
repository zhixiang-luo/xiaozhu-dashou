import { z } from "zod";

const ActionEnvelope = z.object({
  actionId: z.string().min(1),
  taskId: z.string().min(1),
  taskGeneration: z.number().int().nonnegative(),
  expectedState: z.string().min(1),
  expectedPackage: z.string().min(1).optional(),
  timeoutMs: z.number().int().positive().max(30_000)
});

export const ActionSchema = z.discriminatedUnion("type", [
  ActionEnvelope.extend({
    type: z.literal("observe")
  }),
  ActionEnvelope.extend({
    type: z.literal("open_app"),
    packageName: z.string().min(1)
  }),
  ActionEnvelope.extend({
    type: z.literal("restart_app"),
    packageName: z.string().min(1)
  }),
  ActionEnvelope.extend({
    type: z.literal("tap"),
    target: z.union([
      z.object({ nodeId: z.string().min(1) }),
      z.object({ x: z.number().min(0).max(1), y: z.number().min(0).max(1) })
    ])
  }),
  ActionEnvelope.extend({
    type: z.literal("swipe"),
    direction: z.enum(["up", "down", "left", "right"]),
    distance: z.number().min(0).max(1)
  }),
  ActionEnvelope.extend({
    type: z.literal("input_text"),
    text: z.string()
  }),
  ActionEnvelope.extend({
    type: z.literal("key"),
    key: z.string().min(1)
  }),
  ActionEnvelope.extend({
    type: z.literal("media_control"),
    command: z.enum(["play", "pause", "next", "previous"])
  }),
  ActionEnvelope.extend({
    type: z.literal("calendar_create_event"),
    title: z.string().trim().min(1).max(200),
    startTimeMillis: z.number().int().positive(),
    endTimeMillis: z.number().int().positive(),
    timeZone: z.string().min(1).max(64),
    description: z.string().max(4_000).optional()
  }),
  ActionEnvelope.extend({
    type: z.literal("calendar_list_events"),
    rangeStartMillis: z.number().int().positive(),
    rangeEndMillis: z.number().int().positive(),
    titleQuery: z.string().trim().min(1).max(200).optional(),
    timeZone: z.string().min(1).max(64)
  }),
  ActionEnvelope.extend({
    type: z.literal("calendar_update_event"),
    rangeStartMillis: z.number().int().positive(),
    rangeEndMillis: z.number().int().positive(),
    titleQuery: z.string().trim().min(1).max(200),
    newTitle: z.string().trim().min(1).max(200).optional(),
    newStartTimeMillis: z.number().int().positive().optional(),
    newDurationMinutes: z.number().int().min(1).max(1_440).optional(),
    timeZone: z.string().min(1).max(64)
  }),
  ActionEnvelope.extend({
    type: z.literal("calendar_delete_event"),
    rangeStartMillis: z.number().int().positive(),
    rangeEndMillis: z.number().int().positive(),
    titleQuery: z.string().trim().min(1).max(200),
    timeZone: z.string().min(1).max(64)
  }),
  ActionEnvelope.extend({
    type: z.literal("ask_user"),
    question: z.string().min(1)
  }),
  ActionEnvelope.extend({
    type: z.literal("finish"),
    summary: z.string().min(1)
  })
]);

export type AssistantAction = z.infer<typeof ActionSchema>;
