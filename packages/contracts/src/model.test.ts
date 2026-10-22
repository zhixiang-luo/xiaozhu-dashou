import { expect, it } from "vitest";
import { ModelRoleSchema } from "./model.js";
import { TaskEventSchema } from "./task-event.js";

it("rejects a cancelled task event without generation", () => {
  expect(() =>
    TaskEventSchema.parse({
      eventId: "e1",
      taskId: "t1",
      type: "cancelled"
    })
  ).toThrow();
});

it("accepts all four provider roles", () => {
  expect(
    ModelRoleSchema.array().parse([
      "planner",
      "vision_grounder",
      "speech_to_text",
      "text_to_speech"
    ])
  ).toHaveLength(4);
});
