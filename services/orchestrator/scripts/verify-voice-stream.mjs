import { readFile } from "node:fs/promises";
import { setTimeout as delay } from "node:timers/promises";
import WebSocket from "ws";

const pcmPath = process.argv[2];
if (!pcmPath) {
  throw new Error("usage: verify-voice-stream.mjs <pcm16le-file>");
}

const pcm = await readFile(pcmPath);
const socket = new WebSocket("ws://127.0.0.1:4100/v1/voice/stream", {
  headers: {
    authorization: `Bearer ${process.env.DEVICE_TOKEN ?? "dev-token"}`,
    "x-device-id": "xiaozhu-verifier"
  }
});

const partials = [];
let streaming = false;
const timeout = setTimeout(() => {
  console.error("voice stream timed out");
  socket.terminate();
  process.exitCode = 1;
}, 40_000);

socket.on("message", async (data) => {
  const message = JSON.parse(data.toString());
  if (message.type === "ready" && !streaming) {
    streaming = true;
    for (let offset = 0; offset < pcm.length; offset += 3_200) {
      socket.send(pcm.subarray(offset, offset + 3_200));
      await delay(100);
    }
    socket.send(JSON.stringify({ type: "stop" }));
    return;
  }
  if (message.type === "partial") {
    partials.push(message.text);
    return;
  }
  if (message.type === "error") {
    clearTimeout(timeout);
    console.error(`voice stream error: ${message.code}: ${message.message}`);
    socket.close();
    process.exitCode = 1;
    return;
  }
  if (message.type === "final") {
    clearTimeout(timeout);
    console.log(
      JSON.stringify({
        partialCount: partials.length,
        finalText: message.text
      })
    );
    socket.close();
  }
});

socket.on("error", (error) => {
  clearTimeout(timeout);
  console.error(`voice websocket failed: ${error.message}`);
  process.exitCode = 1;
});
