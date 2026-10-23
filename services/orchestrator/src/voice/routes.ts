import type { FastifyInstance } from "fastify";
import WebSocket from "ws";
import {
  VolcanoVoiceSession,
  type VolcanoSessionOptions
} from "./volcano-session.js";

export interface VoiceRouteOptions extends VolcanoSessionOptions {
  apiKey: string;
}

export function registerVoiceRoutes(
  app: FastifyInstance,
  options: VoiceRouteOptions
) {
  app.register(async (scope) => {
    scope.get(
      "/v1/voice/stream",
      { websocket: true },
      (socket, request) => {
        if (!options.apiKey.trim()) {
          setTimeout(() => {
            socket.send(
              JSON.stringify({
                type: "error",
                code: "VOLCANO_NOT_CONFIGURED",
                message: "服务端尚未配置火山实时语音密钥。"
              })
            );
            setTimeout(
              () => socket.close(1011, "volcano_not_configured"),
              50
            );
          }, 50);
          return;
        }

        const installationId =
          typeof request.headers["x-device-id"] === "string"
            ? request.headers["x-device-id"]
            : "xiaozhu-dashou";
        const session = new VolcanoVoiceSession(
          socket as unknown as WebSocket,
          installationId,
          options
        );
        socket.on("message", (data, isBinary) => {
          if (isBinary) {
            session.pushAudio(Buffer.from(data as Buffer));
            return;
          }
          try {
            const message = JSON.parse(data.toString()) as {
              type?: string;
            };
            if (message.type === "stop") {
              session.stop();
            }
          } catch {
            // Ignore malformed client control messages.
          }
        });
        socket.on("close", () => session.stop());
        socket.on("error", () => session.stop());
      }
    );
  });
}
