import { expect, it } from "vitest";
import { buildServer } from "../server.js";

it("rejects an anonymous realtime voice handshake", async () => {
  const app = buildServer({
    deviceToken: "secret",
    voice: {
      apiKey: "",
      resourceId: "volc.seedasr.sauc.duration"
    }
  });
  await app.ready();

  const response = await app.inject({
    method: "GET",
    url: "/v1/voice/stream",
    headers: {
      connection: "Upgrade",
      upgrade: "websocket",
      "sec-websocket-key": "dGhlIHNhbXBsZSBub25jZQ==",
      "sec-websocket-version": "13"
    }
  });

  expect(response.statusCode).toBe(401);
  await app.close();
});

it("reports missing Volcano configuration without opening an upstream stream", async () => {
  const app = buildServer({
    deviceToken: "secret",
    voice: {
      apiKey: "",
      resourceId: "volc.seedasr.sauc.duration"
    }
  });
  await app.ready();

  const socket = await app.injectWS("/v1/voice/stream", {
    headers: { authorization: "Bearer secret" }
  });
  const message = new Promise<string>((resolve) => {
    socket.once("message", (data: { toString(): string }) =>
      resolve(data.toString())
    );
  });

  expect(JSON.parse(await message)).toMatchObject({
    type: "error",
    code: "VOLCANO_NOT_CONFIGURED"
  });
  socket.terminate();
  await app.close();
});
