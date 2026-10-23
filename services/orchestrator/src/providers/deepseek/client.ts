import {
  AssistantIntentSchema,
  type AssistantIntent,
  type IntentPlanningContext,
  type SkillPromptEntry
} from "../../model/intent.js";
import { ProviderError } from "../../model/provider.js";

type FetchLike = typeof fetch;

interface DeepSeekClientOptions {
  apiKey: string;
  baseUrl: string;
  model: string;
  fetchImpl?: FetchLike | undefined;
  timeoutMs?: number | undefined;
}

interface DeepSeekPayload {
  choices?: Array<{
    message?: {
      content?: unknown;
    };
  }>;
}

export class DeepSeekIntentClient {
  private readonly fetchImpl: FetchLike;

  constructor(private readonly options: DeepSeekClientOptions) {
    this.fetchImpl = options.fetchImpl ?? fetch;
  }

  async infer(
    instruction: string,
    catalog: SkillPromptEntry[],
    context?: IntentPlanningContext
  ): Promise<AssistantIntent> {
    let response: Response;
    try {
      response = await this.fetchImpl(
        `${this.options.baseUrl.replace(/\/+$/, "")}/chat/completions`,
        {
          method: "POST",
          headers: {
            authorization: `Bearer ${this.options.apiKey}`,
            "content-type": "application/json"
          },
          body: JSON.stringify({
            model: this.options.model,
            temperature: 0,
            response_format: { type: "json_object" },
            messages: [
              {
                role: "system",
                content: [
                  "你是小猪搭手的意图规划器。",
                  "只能从给定技能目录选择，不得创造技能或动作。",
                  "输出一个 JSON 对象，kind 只能是 execute、clarify、unsupported。",
                  "execute 必须包含 skillId 和 arguments。",
                  "缺少必要信息或存在歧义时使用 clarify。",
                  context
                    ? `当前时间：${context.now}\n当前时区：${context.timeZone}`
                    : "当前时间未知，涉及相对日期时必须使用 clarify。",
                  "calendar.create-event 的 startTime 必须是带时区偏移的 ISO 8601。",
                  "技能目录：",
                  JSON.stringify(catalog)
                ].join("\n")
              },
              { role: "user", content: instruction }
            ]
          }),
          signal: AbortSignal.timeout(this.options.timeoutMs ?? 10_000)
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
      throw new ProviderError("provider_error", `HTTP ${response.status}`);
    }

    let payload: DeepSeekPayload;
    try {
      payload = (await response.json()) as DeepSeekPayload;
    } catch {
      throw new ProviderError("invalid_action", "response was not JSON");
    }

    const content = payload.choices?.[0]?.message?.content;
    if (typeof content !== "string") {
      throw new ProviderError("invalid_action", "missing message content");
    }

    let decoded: unknown;
    try {
      decoded = JSON.parse(stripCodeFence(content));
    } catch {
      throw new ProviderError("invalid_action", "message content was not JSON");
    }

    try {
      return AssistantIntentSchema.parse(decoded);
    } catch {
      throw new ProviderError("invalid_action", "unsupported intent output");
    }
  }
}

function stripCodeFence(value: string): string {
  return value
    .trim()
    .replace(/^```(?:json)?\s*/i, "")
    .replace(/\s*```$/, "");
}
