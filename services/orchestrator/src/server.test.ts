import { expect, it } from "vitest";
import { buildServer } from "./server.js";

it("reports health", async () => {
  const app = buildServer();
  const response = await app.inject({ method: "GET", url: "/health" });

  expect(response.statusCode).toBe(200);
  expect(response.json()).toEqual({ status: "ok" });

  await app.close();
});
