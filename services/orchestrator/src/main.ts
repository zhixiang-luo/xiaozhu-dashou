import { existsSync } from "node:fs";
import { loadEnvFile } from "node:process";
import { fileURLToPath } from "node:url";
import { buildServer } from "./server.js";

const localEnvironment = fileURLToPath(
  new URL("../../../.env", import.meta.url)
);
if (existsSync(localEnvironment)) {
  loadEnvFile(localEnvironment);
}

const app = buildServer();
const port = Number(process.env.PORT ?? 4100);
const host = process.env.HOST ?? "127.0.0.1";

await app.listen({ host, port });
