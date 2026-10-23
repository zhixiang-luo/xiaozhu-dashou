export function encodeVolcanoFrame(
  messageType: number,
  flags: number,
  payload: Buffer
): Buffer {
  const header = Buffer.from([
    0x11,
    (messageType << 4) | flags,
    messageType === 0x01 ? 0x10 : 0x00,
    0x00
  ]);
  const sequence =
    flags & 0x01 ? Buffer.alloc(4) : Buffer.alloc(0);
  const length = Buffer.alloc(4);
  length.writeUInt32BE(payload.length);
  return Buffer.concat([header, sequence, length, payload]);
}

export function parseVolcanoFrame(buffer: Buffer) {
  if (buffer.length < 4) {
    return null;
  }
  const headerSize = (buffer.readUInt8(0) & 0x0f) * 4;
  const messageType = buffer.readUInt8(1) >> 4;
  const flags = buffer.readUInt8(1) & 0x0f;
  let offset = headerSize;
  if (flags & 0x01) {
    offset += 4;
  }
  if (offset + 4 > buffer.length) {
    return null;
  }
  const payloadSize = buffer.readUInt32BE(offset);
  offset += 4;
  if (offset + payloadSize > buffer.length) {
    return null;
  }
  return {
    messageType,
    flags,
    payload: buffer.subarray(offset, offset + payloadSize)
  };
}

export function createVolcanoRequest(uid: string): Buffer {
  return Buffer.from(
    JSON.stringify({
      user: { uid: uid.slice(0, 64) },
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
        enable_ddc: true,
        enable_punc: true,
        show_utterances: true
      }
    })
  );
}

export function readVolcanoText(payload: Buffer): {
  text: string;
  definite: boolean;
} | null {
  try {
    const parsed = JSON.parse(payload.toString()) as {
      result?: {
        text?: string;
        utterances?: Array<{ definite?: boolean }>;
      };
    };
    return {
      text: parsed.result?.text?.trim() ?? "",
      definite:
        parsed.result?.utterances?.some((utterance) => utterance.definite) ??
        false
    };
  } catch {
    return null;
  }
}
