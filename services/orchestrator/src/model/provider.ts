import type {
  AssistantAction,
  Observation,
  ProviderCapabilities
} from "@personal-jarvis/contracts";
import type { ModelRoute } from "./router.js";

export interface PlanRequest {
  taskId: string;
  taskGeneration: number;
  role: "planner" | "vision_grounder";
  instruction: string;
  model: ModelRoute["model"];
  observation?: Observation;
}

export interface ModelProvider {
  readonly id: string;
  capabilities(): Promise<ProviderCapabilities>;
  plan(request: PlanRequest): Promise<AssistantAction[]>;
}

export class ProviderError extends Error {
  constructor(
    readonly code:
      | "provider_unavailable"
      | "provider_rate_limited"
      | "provider_error"
      | "invalid_action",
    message: string
  ) {
    super(`${code}: ${message}`);
    this.name = "ProviderError";
  }
}
