import type { AssistantAction } from "@personal-jarvis/contracts";
import type { AssistantIntent } from "../model/intent.js";
import type { TaskRecord } from "../tasks/coordinator.js";
import {
  type SkillCatalog,
  SkillValidationError
} from "./catalog.js";

type EnvelopeField = "actionId" | "taskId" | "taskGeneration";
type DistributiveOmit<T, K extends PropertyKey> = T extends unknown
  ? Omit<T, K>
  : never;
type ActionDraft = DistributiveOmit<AssistantAction, EnvelopeField>;

export interface IntentTranslationOptions {
  allowWechatAutoSend: boolean;
}

export async function translateIntent(
  task: TaskRecord,
  intent: AssistantIntent,
  catalog: SkillCatalog,
  options: IntentTranslationOptions = {
    allowWechatAutoSend: false
  }
): Promise<AssistantAction[]> {
  if (intent.kind === "clarify") {
    return [askUser(task, intent.question)];
  }
  if (intent.kind === "unsupported") {
    return [askUser(task, intent.reason)];
  }

  let argumentsValue: Record<string, string | number | boolean>;
  try {
    argumentsValue = catalog.validateArguments(
      intent.skillId,
      intent.arguments
    );
  } catch (error) {
    return [
      askUser(
        task,
        error instanceof SkillValidationError
          ? "这个技能缺少必要信息，请换一种更明确的说法。"
          : "当前不能安全执行这个请求。"
      )
    ];
  }

  if (intent.skillId !== "wechat.send-message") {
    return [askUser(task, "这个技能还没有接入开发版手机执行器。")];
  }

  const contactName = stringArgument(argumentsValue.contactName);
  const message = stringArgument(argumentsValue.message).trim();
  if (!isFileTransferAssistant(contactName)) {
    return [
      askUser(
        task,
        "目前微信只校准了“文件传输助手”。发给其他联系人的流程还没有验证。"
      )
    ];
  }
  if (!message) {
    return [askUser(task, "你想发送什么内容？")];
  }
  if (message.length > 2_000) {
    return [askUser(task, "这条消息太长，请缩短到 2000 字以内。")];
  }
  if (containsSensitiveContent(message)) {
    return [
      askUser(
        task,
        "这条消息可能包含验证码、密码或支付信息，需要你本人发送。"
      )
    ];
  }
  if (!options.allowWechatAutoSend) {
    return [
      askUser(
        task,
        "微信自动发送默认关闭。请由开发者在本地明确开启后再执行。"
      )
    ];
  }

  return [
    action(task, {
      type: "restart_app",
      expectedState: "wechat.home",
      expectedPackage: "com.tencent.mm",
      timeoutMs: 10_000,
      packageName: "com.tencent.mm"
    }),
    action(task, {
      type: "tap",
      expectedState: "wechat.file_assistant_visible",
      expectedPackage: "com.tencent.mm",
      timeoutMs: 5_000,
      target: { x: 0.32, y: 0.26 }
    }),
    action(task, {
      type: "tap",
      expectedState: "wechat.chat_input_focused",
      expectedPackage: "com.tencent.mm",
      timeoutMs: 5_000,
      target: { x: 0.5, y: 0.93 }
    }),
    action(task, {
      type: "input_text",
      expectedState: "wechat.message_drafted",
      expectedPackage: "com.tencent.mm",
      timeoutMs: 5_000,
      text: message
    }),
    action(task, {
      type: "tap",
      expectedState: "wechat.message_sent",
      expectedPackage: "com.tencent.mm",
      timeoutMs: 5_000,
      target: { x: 0.91, y: 0.59 }
    })
  ];
}

function stringArgument(value: string | number | boolean | undefined): string {
  return typeof value === "string" ? value : "";
}

function isFileTransferAssistant(value: string): boolean {
  const normalized = value.replace(/[\s，。,.、"'“”‘’]/g, "");
  return normalized === "文件传输助手" || normalized === "文件助手";
}

function containsSensitiveContent(value: string): boolean {
  return /(验证码|支付密码|银行卡|转账|红包|身份证|短信密码)/i.test(value);
}

function askUser(task: TaskRecord, question: string): AssistantAction {
  return action(task, {
    type: "ask_user",
    expectedState: "user_input",
    timeoutMs: 30_000,
    question
  });
}

function action(task: TaskRecord, fields: ActionDraft): AssistantAction {
  return {
    ...fields,
    actionId: crypto.randomUUID(),
    taskId: task.id,
    taskGeneration: task.generation
  } as AssistantAction;
}
