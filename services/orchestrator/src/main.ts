import { buildServer } from "./server.js";

const app = buildServer();
const port = Number(process.env.PORT ?? 4100);

await app.listen({ host: "0.0.0.0", port });
