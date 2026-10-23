import { describe, expect, it } from "vitest";
import {
  createVolcanoRequest,
  encodeVolcanoFrame,
  parseVolcanoFrame
} from "./protocol.js";

describe("Volcano streaming protocol", () => {
  it("encodes an audio-only final frame", () => {
    expect(encodeVolcanoFrame(0x02, 0x02, Buffer.from([1, 2]))).toEqual(
      Buffer.from([0x11, 0x22, 0x00, 0x00, 0, 0, 0, 2, 1, 2])
    );
  });

  it("parses the server payload from a binary frame", () => {
    const frame = encodeVolcanoFrame(
      0x09,
      0x01,
      Buffer.from('{"result":{"text":"你好"}}')
    );

    expect(parseVolcanoFrame(frame)).toMatchObject({
      messageType: 0x09,
      flags: 0x01,
      payload: Buffer.from('{"result":{"text":"你好"}}')
    });
  });

  it("rejects malformed frames", () => {
    expect(parseVolcanoFrame(Buffer.from([1]))).toBeNull();
  });

  it("creates a 16 kHz mono PCM request", () => {
    const payload = JSON.parse(createVolcanoRequest("device-1").toString());

    expect(payload).toMatchObject({
      user: { uid: "device-1" },
      audio: {
        format: "pcm",
        codec: "raw",
        rate: 16_000,
        bits: 16,
        channel: 1
      },
      request: {
        model_name: "bigmodel",
        enable_itn: true,
        enable_punc: true,
        show_utterances: true
      }
    });
  });
});
