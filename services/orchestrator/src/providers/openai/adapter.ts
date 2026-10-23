import { ActionSchema } from "@personal-jarvis/contracts";
import type { ProviderCapabilities } from "@personal-jarvis/contracts";
import type {
  ModelProvider,
  PlanRequest
} from "../../model/provider.js";
import { ProviderError } from "../../model/provider.js";

type FetchLike = typeof fetch;

interface OpenAIProviderOptions {
  apiKey: string;
  baseUrl: string;
  fetchImpl: FetchLike;
  timeoutMs?: number;
}

interface ResponsesPayload {
  output_text?: unknown;
}

export class OpenAIProvider implements ModelProvider {
  readonly id = "openai";

  constructor(private readonly options: OpenAIProviderOptions) {}

  async capabilities(): Promise<ProviderCapabilities> {
    return {
      roles: ["planner", "vision_grounder"],
      supportsToolCalling: true,
      supportsStructuredOutput: true,
      supportsImages: true,
      contextWindow: 1_050_000,
      regions: []
    };
  }

  async plan(request: PlanRequest) {
    let response: Response;

    try {
      response = await this.options.fetchImpl(
        new URL("responses", ensureTrailingSlash(this.options.baseUrl)),
        {
          method: "POST",
          headers: {
            authorization: `Bearer ${this.options.apiKey}`,
            "content-type": "application/json"
          },
          body: JSON.stringify({
            model: request.model,
            instructions:
              "Return one or more 小猪搭手 actions as a JSON array. Use only the provided action schema.",
            input: [
              {
                role: "user",
                content: request.instruction
              }
            ],
            text: {
              format: {
                type: "json_object"
              }
            }
          }),
          signal: AbortSignal.timeout(this.options.timeoutMs ?? 25_000)
        }
      );
    } catch (error) {
      throw new ProviderError(
        "provider_unavailable",
        error instanceof Error ? error.message : "network failure"
      );
    }

    if (response.status === 429) {
      throw new ProviderError("provider_rate_limited", "rate limited");
    }

    if (!response.ok) {
      throw new ProviderError(
        "provider_error",
        `HTTP ${response.status}`
      );
    }

    let payload: ResponsesPayload;
    try {
      payload = (await response.json()) as ResponsesPayload;
    } catch {
      throw new ProviderError("invalid_action", "response was not JSON");
    }

    const output = payload.output_text;
    if (typeof output !== "string") {
      throw new ProviderError("invalid_action", "missing output_text");
    }

    let decoded: unknown;
    try {
      decoded = JSON.parse(output);
    } catch {
      throw new ProviderError("invalid_action", "output_text was not JSON");
    }

    const actions = Array.isArray(decoded) ? decoded : [decoded];
    try {
      return ActionSchema.array().parse(actions);
    } catch {
      throw new ProviderError("invalid_action", "unsupported action output");
    }
  }
}

function ensureTrailingSlash(value: string): string {
  return value.endsWith("/") ? value : `${value}/`;
}
