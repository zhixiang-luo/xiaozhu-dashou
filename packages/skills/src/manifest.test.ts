import { readFile } from "node:fs/promises";
import { describe, expect, it } from "vitest";
import { parseSkillManifest } from "./manifest.js";

const validWeChatManifest = {
  id: "wechat.send-message",
  version: "0.1.0",
  appPackages: ["com.tencent.mm"],
  prerequisites: ["wechat.foreground"],
  capabilities: ["accessibility", "shizuku"],
  risk: {
    autoSend: "unique_recipient"
  },
  verification: ["sent_message_visible"],
  steps: ["resolve_target", "open_chat", "input_message", "send", "verify"]
};

describe("Skill manifest", () => {
  it("accepts a valid WeChat message skill", () => {
    expect(parseSkillManifest(validWeChatManifest).id).toBe(
      "wechat.send-message"
    );
  });

  it("rejects a skill without verification", () => {
    const { verification: _verification, ...invalid } = validWeChatManifest;
    expect(() => parseSkillManifest(invalid)).toThrow();
  });

  it("rejects an invalid Android package", () => {
    expect(() =>
      parseSkillManifest({
        ...validWeChatManifest,
        appPackages: ["not a package"]
      })
    ).toThrow();
  });

  it("ships a JSON schema with verification required", async () => {
    const schema = JSON.parse(
      await readFile("../../skills/schema/skill-manifest.schema.json", "utf8")
    ) as { required?: string[] };

    expect(schema.required).toContain("verification");
  });
});
