import { randomUUID } from "node:crypto";
import WebSocket from "ws";
import {
  createVolcanoRequest,
  encodeVolcanoFrame,
  parseVolcanoFrame,
  readVolcanoText
} from "./protocol.js";

const DEFAULT_UPSTREAM_URL =
  "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel";
const AUDIO_CHUNK_BYTES = 6_400;
const FINALIZE_DELAY_MS = 1_000;
const FINAL_TIMEOUT_MS = 3_000;

export interface VolcanoSessionOptions {
  apiKey: string;
  resourceId: string;
  upstreamUrl?: string;
  maxStreamBytes?: number;
  maxStreamMs?: number;
}

type ClientEvent =
  | { type: "ready" }
  | { type: "partial"; text: string }
  | { type: "final"; text: string }
  | { type: "error"; code: string; message: string };

export class VolcanoVoiceSession {
  private readonly upstream: WebSocket;
  private readonly audioQueue: Buffer[] = [];
  private audioBuffer = Buffer.alloc(0);
  private ready = false;
  private stopping = false;
  private finished = false;
  private latestText = "";
  private receivedAudioBytes = 0;
  private finalizeTimer: ReturnType<typeof setTimeout> | null = null;
  private finalTimeout: ReturnType<typeof setTimeout> | null = null;
  private readonly maxSessionTimer: ReturnType<typeof setTimeout>;

  constructor(
    private readonly client: WebSocket,
    installationId: string,
    private readonly options: VolcanoSessionOptions
  ) {
    this.upstream = new WebSocket(
      options.upstreamUrl ?? DEFAULT_UPSTREAM_URL,
      {
        headers: {
          "X-Api-Key": options.apiKey,
          "X-Api-Resource-Id": options.resourceId,
          "X-Api-Connect-Id": randomUUID()
        }
      }
    );
    this.maxSessionTimer = setTimeout(
      () => this.abort("VOICE_STREAM_TOO_LONG", "单次语音时间过长，请缩短后重试。"),
      options.maxStreamMs ?? 30_000
    );
    this.maxSessionTimer.unref();
    this.upstream.on("open", () => this.handleUpstreamOpen(installationId));
    this.upstream.on("message", (data) =>
      this.handleUpstreamMessage(Buffer.from(data as Buffer))
    );
    this.upstream.on("error", () => {
      if (this.stopping && !this.latestText.trim()) {
        this.finish();
        return;
      }
      this.send({
        type: "error",
        code: "VOLCANO_CONNECTION_FAILED",
        message: "火山实时语音连接失败，请重试。"
      });
      this.close();
    });
    this.upstream.on("close", () => {
      if (this.finished) {
        return;
      }
      if (this.stopping) {
        this.finish();
        return;
      }
      this.send({
        type: "error",
        code: "VOLCANO_CLOSED",
        message: "火山实时语音连接提前断开，请重试。"
      });
      this.close();
    });
  }

  pushAudio(chunk: Buffer) {
    this.receivedAudioBytes += chunk.byteLength;
    if (this.receivedAudioBytes > (this.options.maxStreamBytes ?? 960_000)) {
      this.abort("VOICE_AUDIO_TOO_LARGE", "单次语音时间过长，请缩短后重试。");
      return;
    }
    if (!this.ready) {
      this.audioQueue.push(chunk);
      return;
    }
    this.appendAudio(chunk);
  }

  stop() {
    if (this.stopping || this.finished) {
      return;
    }
    this.stopping = true;
    this.sendFinalIfReady();
    this.finalTimeout = setTimeout(() => this.finish(), FINAL_TIMEOUT_MS);
  }

  private sendFinalIfReady() {
    if (this.upstream.readyState !== WebSocket.OPEN) {
      return;
    }
    const finalPayload = Buffer.concat([
      this.audioBuffer,
      Buffer.alloc(AUDIO_CHUNK_BYTES)
    ]);
    this.audioBuffer = Buffer.alloc(0);
    this.upstream.send(
      encodeVolcanoFrame(0x02, 0x02, finalPayload)
    );
  }

  close() {
    if (this.finished) {
      return;
    }
    this.finished = true;
    clearTimeout(this.maxSessionTimer);
    if (this.finalizeTimer) {
      clearTimeout(this.finalizeTimer);
    }
    if (this.finalTimeout) {
      clearTimeout(this.finalTimeout);
    }
    if (
      this.upstream.readyState === WebSocket.OPEN ||
      this.upstream.readyState === WebSocket.CONNECTING
    ) {
      this.upstream.close();
    }
    if (this.client.readyState === WebSocket.OPEN) {
      this.client.close();
    }
  }

  private handleUpstreamOpen(installationId: string) {
    this.ready = true;
    this.upstream.send(
      encodeVolcanoFrame(
        0x01,
        0x00,
        createVolcanoRequest(installationId)
      )
    );
    this.send({ type: "ready" });
    for (const chunk of this.audioQueue.splice(0)) {
      this.appendAudio(chunk, true);
    }
    if (this.stopping) {
      this.sendFinalIfReady();
    }
  }

  private appendAudio(chunk: Buffer, allowStopping = false) {
    if ((!allowStopping && this.stopping) || this.finished) {
      return;
    }
    this.audioBuffer = Buffer.concat([this.audioBuffer, chunk]);
    while (this.audioBuffer.length >= AUDIO_CHUNK_BYTES) {
      const packet = this.audioBuffer.subarray(0, AUDIO_CHUNK_BYTES);
      this.audioBuffer = this.audioBuffer.subarray(AUDIO_CHUNK_BYTES);
      this.upstream.send(encodeVolcanoFrame(0x02, 0x00, packet));
    }
  }

  private handleUpstreamMessage(buffer: Buffer) {
    const frame = parseVolcanoFrame(buffer);
    if (!frame) {
      return;
    }
    if (frame.messageType === 0x0f) {
      if (this.stopping && !this.latestText.trim()) {
        this.finish();
        return;
      }
      this.send({
        type: "error",
        code: "VOLCANO_ASR_ERROR",
        message: "火山实时语音识别失败，请重试。"
      });
      this.close();
      return;
    }
    if (frame.messageType !== 0x09) {
      return;
    }
    const result = readVolcanoText(frame.payload);
    if (!result) {
      return;
    }
    if (result.text && result.text !== this.latestText) {
      this.latestText = result.text;
      this.send({ type: "partial", text: result.text });
    }
    if (this.stopping) {
      if (this.finalizeTimer) {
        clearTimeout(this.finalizeTimer);
      }
      if (result.definite) {
        this.finish();
      } else {
        this.finalizeTimer = setTimeout(
          () => this.finish(),
          FINALIZE_DELAY_MS
        );
      }
    }
  }

  private finish() {
    if (this.finished) {
      return;
    }
    clearTimeout(this.maxSessionTimer);
    if (this.finalizeTimer) {
      clearTimeout(this.finalizeTimer);
    }
    if (this.finalTimeout) {
      clearTimeout(this.finalTimeout);
    }
    this.send({ type: "final", text: this.latestText });
    this.close();
  }

  private abort(code: string, message: string) {
    if (this.finished) {
      return;
    }
    this.send({ type: "error", code, message });
    this.close();
  }

  private send(event: ClientEvent) {
    if (this.client.readyState === WebSocket.OPEN) {
      this.client.send(JSON.stringify(event));
    }
  }
}
