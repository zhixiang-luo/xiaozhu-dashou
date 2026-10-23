import Fastify from "fastify";
import fastifyWebsocket from "@fastify/websocket";
import { z } from "zod";
import { TaskCoordinator } from "./tasks/coordinator.js";
import {
  DevTaskPlanner,
  type TaskPlanner
} from "./tasks/planner.js";

interface BuildServerOptions {
  deviceToken?: string;
  coordinator?: TaskCoordinator;
  planner?: TaskPlanner;
}

const CreateTaskBodySchema = z.object({
  instruction: z.string().trim().min(1).max(4_000)
});

const ConfirmationBodySchema = z.object({
  actionId: z.string().min(1),
  decision: z.enum(["approve", "reject"])
});

export function buildServer(options: BuildServerOptions = {}) {
  const app = Fastify({ logger: false });
  const coordinator = options.coordinator ?? new TaskCoordinator();
  const planner = options.planner ?? new DevTaskPlanner();
  const deviceToken = options.deviceToken ?? "dev-token";

  app.register(fastifyWebsocket);
  app.get("/health", async () => ({ status: "ok" }));

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
      actions: planner.plan(task)
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
