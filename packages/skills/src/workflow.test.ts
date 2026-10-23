import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
import { describe, expect, it } from "vitest";
import { parseSkillYaml } from "./manifest.js";

const stepManifest = `
id: wechat.send-message
version: 0.1.0
displayName: 微信发送消息
appPackages: [com.tencent.mm]
supportedVersions: ["8.0.79"]
prerequisites: [wechat.foreground]
capabilities: [accessibility, shizuku]
risk:
  sideEffect: external_communication
  autoSend: unique_recipient
  handoffOn: [payment, otp, account_security]
inputs:
  contactName: { type: text, required: false }
  message: { type: text, required: true }
verification: [sent_message_visible]
steps:
  - id: open
    action: open_app
    package: com.tencent.mm
  - id: send
    action: send
    target: { text: 发送 }
`;

describe("Workflow skill manifests", () => {
  it("parses structured workflow steps", () => {
    const parsed = parseSkillYaml(stepManifest);
    expect(parsed.steps[1]).toMatchObject({
      id: "send",
      action: "send",
      target: { text: "发送" }
    });
  });

  it("rejects unknown step actions", () => {
    expect(() =>
      parseSkillYaml(
        stepManifest.replace("action: send", "action: execute_anything")
      )
    ).toThrow();
  });

  it("validates every shipped manifest", async () => {
    const skillRoot = path.resolve("../../skills");
    const manifestPaths: string[] = [];

    async function visit(directory: string) {
      for (const entry of await readdir(directory, { withFileTypes: true })) {
        const absolute = path.join(directory, entry.name);
        if (entry.isDirectory()) {
          await visit(absolute);
        } else if (entry.name === "manifest.yaml") {
          manifestPaths.push(absolute);
        }
      }
    }

    await visit(skillRoot);
    expect(manifestPaths.length).toBeGreaterThanOrEqual(3);

    for (const manifestPath of manifestPaths) {
      parseSkillYaml(await readFile(manifestPath, "utf8"));
    }
  });
});
