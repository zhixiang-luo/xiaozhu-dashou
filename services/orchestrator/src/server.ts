import Fastify from "fastify";
import fastifyWebsocket from "@fastify/websocket";
import { z } from "zod";
import { TaskContextSchema } from "@personal-jarvis/contracts";
import { TaskCoordinator } from "./tasks/coordinator.js";
import {
  DevTaskPlanner,
  type TaskPlanner
} from "./tasks/planner.js";
import { DeepSeekTaskPlanner } from "./tasks/deepseek-planner.js";
import { DeepSeekIntentClient } from "./providers/deepseek/client.js";
import {
  loadDefaultSkillCatalog,
  type SkillCatalog
} from "./skills/catalog.js";
import { registerVoiceRoutes } from "./voice/routes.js";

interface BuildServerOptions {
  deviceToken?: string;
  allowWechatAutoSend?: boolean;
  coordinator?: TaskCoordinator;
  planner?: TaskPlanner;
  skillCatalog?: SkillCatalog;
  deepSeek?: {
    apiKey: string;
    baseUrl?: string;
    model?: string;
    timeoutMs?: number;
    fetchImpl?: typeof fetch;
  };
  voice?: {
    apiKey: string;
    resourceId: string;
    upstreamUrl?: string;
    maxStreamBytes?: number;
    maxStreamMs?: number;
  };
}

const CreateTaskBodySchema = z.object({
  instruction: z.string().trim().min(1).max(4_000),
  context: TaskContextSchema.optional()
});

const ConfirmationBodySchema = z.object({
  actionId: z.string().min(1),
  decision: z.enum(["approve", "reject"])
});

export function buildServer(options: BuildServerOptions = {}) {
  const app = Fastify({ logger: false });
  const coordinator = options.coordinator ?? new TaskCoordinator();
  const skillCatalog =
    options.skillCatalog ?? loadDefaultSkillCatalog();
  const deepSeekApiKey =
    options.deepSeek?.apiKey ?? process.env.DEEPSEEK_API_KEY ?? "";
  const plannerOptions = {
    allowWechatAutoSend:
      options.allowWechatAutoSend ??
      process.env.ALLOW_WECHAT_AUTOSEND === "true",
    timeZone: process.env.ASSISTANT_TIME_ZONE ?? "Asia/Shanghai"
  };
  const planner =
    options.planner ??
    (deepSeekApiKey
      ? new DeepSeekTaskPlanner(
          new DeepSeekIntentClient({
            apiKey: deepSeekApiKey,
            baseUrl:
              options.deepSeek?.baseUrl ??
              process.env.DEEPSEEK_BASE_URL ??
              "https://api.deepseek.com",
            model:
              options.deepSeek?.model ??
              process.env.DEEPSEEK_MODEL ??
              "deepseek-chat",
            timeoutMs: options.deepSeek?.timeoutMs,
            fetchImpl: options.deepSeek?.fetchImpl
          }),
          skillCatalog,
          undefined,
          plannerOptions
        )
      : new DevTaskPlanner(plannerOptions));
  const deviceToken =
    options.deviceToken ?? process.env.DEVICE_TOKEN ?? "dev-token";

  app.register(fastifyWebsocket, {
    options: { maxPayload: 64 * 1024 }
  });
  app.get("/health", async () => ({ status: "ok" }));
  const voiceOptions = {
    apiKey: options.voice?.apiKey ?? process.env.VOLCENGINE_ASR_API_KEY ?? "",
    resourceId:
      options.voice?.resourceId ??
      process.env.VOLCENGINE_ASR_RESOURCE_ID ??
      "volc.seedasr.sauc.duration"
  };
  registerVoiceRoutes(app, {
    ...voiceOptions,
    ...(options.voice?.upstreamUrl
      ? { upstreamUrl: options.voice.upstreamUrl }
      : {}),
    ...(options.voice?.maxStreamBytes
      ? { maxStreamBytes: options.voice.maxStreamBytes }
      : {}),
    ...(options.voice?.maxStreamMs
      ? { maxStreamMs: options.voice.maxStreamMs }
      : {})
  });

  app.addHook("onRequest", async (request, reply) => {
    if (!request.url.startsWith("/v1/")) {
      return;
    }

    if (request.headers.authorization !== `Bearer ${deviceToken}`) {
      return reply.code(401).send({ error: "unauthorized" });
    }
  });

  app.post("/v1/tasks", async (request, reply) => {
    const parsed = CreateTaskBodySchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid_task" });
    }

    const task = coordinator.create(parsed.data);
    return reply.code(201).send({
      task,
      actions: await planner.plan(task)
    });
  });

  app.post<{ Params: { id: string } }>(
    "/v1/tasks/:id/cancel",
    async (request, reply) => {
      try {
        return coordinator.cancel(request.params.id);
      } catch {
        return reply.code(404).send({ error: "task_not_found" });
      }
    }
  );

  app.register(async (scope) => {
    scope.get<{ Params: { id: string } }>(
      "/v1/tasks/:id/events",
      { websocket: true },
      (socket, request) => {
        let unsubscribe: (() => void) | undefined;

        try {
          unsubscribe = coordinator.subscribe(request.params.id, (event) => {
            socket.send(JSON.stringify(event));
          });
        } catch {
          socket.close(1008, "task_not_found");
          return;
        }

        socket.on("close", () => unsubscribe?.());
      }
    );
  });

  app.post<{ Params: { id: string } }>(
    "/v1/tasks/:id/confirmation",
    async (request, reply) => {
      try {
        coordinator.get(request.params.id);
      } catch {
        return reply.code(404).send({ error: "task_not_found" });
      }

      const parsed = ConfirmationBodySchema.safeParse(request.body);
      if (!parsed.success) {
        return reply.code(400).send({ error: "invalid_confirmation" });
      }

      try {
        return coordinator.confirm(
          request.params.id,
          parsed.data.actionId,
          parsed.data.decision
        );
      } catch {
        return reply.code(409).send({ error: "confirmation_not_pending" });
      }
    }
  );

  return app;
}
