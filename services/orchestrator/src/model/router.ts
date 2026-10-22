export interface ModelRoute {
  providerId: "openai";
  model: "gpt-6-luna" | "gpt-6.1-sol" | "gpt-6-astra";
  reasoningEffort: "none" | "medium" | "high";
}

export function selectModel(input: {
  role: "planner" | "vision_grounder";
  attempt: number;
  taskType: "intent" | "ui";
}): ModelRoute {
  if (input.taskType === "intent") {
    return {
      providerId: "openai",
      model: "gpt-6-luna",
      reasoningEffort: "none"
    };
  }

  if (input.attempt >= 2) {
    return {
      providerId: "openai",
      model: "gpt-6-astra",
      reasoningEffort: "high"
    };
  }

  return {
    providerId: "openai",
    model: "gpt-6.1-sol",
    reasoningEffort: "medium"
  };
}
