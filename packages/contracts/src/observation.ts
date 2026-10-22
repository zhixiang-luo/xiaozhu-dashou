import { z } from "zod";

export const UiNodeSchema = z.object({
  nodeId: z.string().min(1),
  role: z.string().min(1),
  text: z.string().optional(),
  contentDescription: z.string().optional(),
  bounds: z.object({
    left: z.number(),
    top: z.number(),
    right: z.number(),
    bottom: z.number()
  }),
  clickable: z.boolean(),
  editable: z.boolean()
});

export const ObservationSchema = z.object({
  observationId: z.string().min(1),
  taskId: z.string().min(1),
  foregroundPackage: z.string().min(1),
  pageTitle: z.string().optional(),
  screenHash: z.string().min(1),
  nodes: z.array(UiNodeSchema),
  screenshotRef: z.string().optional()
});

export type UiNode = z.infer<typeof UiNodeSchema>;
export type Observation = z.infer<typeof ObservationSchema>;
