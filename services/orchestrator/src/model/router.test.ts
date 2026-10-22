import { expect, it } from "vitest";
import { selectModel } from "./router.js";

it("uses the cheap model for wake intent", () => {
  expect(
    selectModel({ role: "planner", attempt: 0, taskType: "intent" })
  ).toEqual({
    providerId: "openai",
    model: "gpt-6-luna",
    reasoningEffort: "none"
  });
});

it("uses recovery after two failures", () => {
  expect(
    selectModel({ role: "planner", attempt: 2, taskType: "ui" }).model
  ).toBe("gpt-6-astra");
});
