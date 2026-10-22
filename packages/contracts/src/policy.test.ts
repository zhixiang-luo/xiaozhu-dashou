import { expect, it } from "vitest";
import { RiskDecisionSchema } from "./policy.js";

it("accepts only allow, confirm, and block", () => {
  expect(
    RiskDecisionSchema.parse({
      decision: "block",
      reason: "payment_screen",
      actionId: "a1"
    }).decision
  ).toBe("block");

  expect(() =>
    RiskDecisionSchema.parse({
      decision: "maybe",
      reason: "unknown",
      actionId: "a1"
    })
  ).toThrow();
});
