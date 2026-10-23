import type { AssistantAction } from "@personal-jarvis/contracts";
import type { AssistantIntent } from "../model/intent.js";
import type { SkillPromptEntry } from "../model/intent.js";
import type { SkillCatalog } from "../skills/catalog.js";
import {
  translateIntent,
  type IntentTranslationOptions
} from "../skills/translate-intent.js";
import type { TaskRecord } from "./coordinator.js";
import { DevTaskPlanner, type TaskPlanner } from "./planner.js";

export interface IntentClient {
  infer(
    instruction: string,
    catalog: SkillPromptEntry[]
  ): Promise<AssistantIntent>;
}

export class DeepSeekTaskPlanner implements TaskPlanner {
  private readonly fallback: TaskPlanner;

  constructor(
    private readonly client: IntentClient,
    private readonly catalog: SkillCatalog,
    fallback?: TaskPlanner,
    private readonly options: IntentTranslationOptions = {
      allowWechatAutoSend: false
    }
  ) {
    this.fallback = fallback ?? new DevTaskPlanner({
      allowWechatAutoSend: options.allowWechatAutoSend
    });
  }

  async plan(task: TaskRecord): Promise<AssistantAction[]> {
    try {
      const intent = await this.client.infer(
        task.instruction,
        this.catalog.promptEntries()
      );
      if (intent.kind === "unsupported") {
        return this.fallback.plan(task);
      }
      return await translateIntent(
        task,
        intent,
        this.catalog,
        this.options
      );
    } catch {
      return this.fallback.plan(task);
    }
  }
}
