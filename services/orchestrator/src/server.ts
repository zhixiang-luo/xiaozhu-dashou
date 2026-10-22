import Fastify from "fastify";
import { z } from "zod";
import { TaskCoordinator } from "./tasks/coordinator.js";

interface BuildServerOptions {
  deviceToken?: string;
  coordinator?: TaskCoordinator;
}

const CreateTaskBodySchema = z.object({
  instruction: z.string().trim().min(1).max(4_000)
});

export function buildServer(options: BuildServerOptions = {}) {
  const app = Fastify({ logger: false });
  const coordinator = options.coordinator ?? new TaskCoordinator();
  const deviceToken = options.deviceToken ?? "dev-token";

  app.get("/health", async () => ({ status: "ok" }));

  app.addHook("onRequest", async (request, reply) => {
    if (!request.url.startsWith("/v1/")) {
      return;
    }

    if (request.headers.authorization !== `Bearer ${deviceToken}`) {
      await reply.code(401).send({ error: "unauthorized" });
    }
  });

  app.post("/v1/tasks", async (request, reply) => {
    const parsed = CreateTaskBodySchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: "invalid_task" });
    }

    const task = coordinator.create(parsed.data);
    return reply.code(201).send(task);
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

  return app;
}
