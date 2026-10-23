import { describe, expect, it, vi } from "vitest";
import { DeepSeekIntentClient } from "./client.js";

describe("DeepSeekIntentClient", () => {
  it("requests and validates one structured intent", async () => {
    const fetchImpl = vi.fn<typeof fetch>(async () =>
      new Response(
        JSON.stringify({
          choices: [
            {
              message: {
                content: JSON.stringify({
                  kind: "execute",
                  skillId: "wechat.send-message",
                  arguments: {
                    contactName: "文件传输助手",
                    message: "测试成功了"
                  }
                })
              }
            }
          ]
        }),
        { status: 200 }
      )
    );
    const client = new DeepSeekIntentClient({
      apiKey: "secret",
      baseUrl: "https://api.deepseek.com",
      model: "deepseek-chat",
      fetchImpl
    });

    const intent = await client.infer(
      "帮我到文件传输助手里面去发一句话，比如说测试成功了",
      [
        {
          id: "wechat.send-message",
          displayName: "微信发送普通消息",
          appPackages: ["com.tencent.mm"],
          inputs: {
            contactName: { type: "text", required: false },
            message: { type: "text", required: true }
          }
        }
      ],
      {
        now: "2026-10-23T05:00:00.000Z",
        timeZone: "Asia/Shanghai"
      }
    );

    expect(intent).toEqual({
      kind: "execute",
      skillId: "wechat.send-message",
      arguments: {
        contactName: "文件传输助手",
        message: "测试成功了"
      }
    });
    const request = fetchImpl.mock.calls[0];
    expect(request?.[0]).toBe("https://api.deepseek.com/chat/completions");
    const body = JSON.parse(String(request?.[1]?.body));
    expect(body).toMatchObject({
      model: "deepseek-chat",
      temperature: 0,
      response_format: { type: "json_object" }
    });
    expect(body.messages[0].content).toContain(
      "2026-10-23T05:00:00.000Z"
    );
    expect(body.messages[0].content).toContain("Asia/Shanghai");
  });

  it("rejects malformed model JSON", async () => {
    const client = new DeepSeekIntentClient({
      apiKey: "secret",
      baseUrl: "https://api.deepseek.com",
      model: "deepseek-chat",
      fetchImpl: async () =>
        new Response(
          JSON.stringify({
            choices: [{ message: { content: "not-json" } }]
          }),
          { status: 200 }
        )
    });

    await expect(client.infer("测试", [])).rejects.toThrow(
      "invalid_action"
    );
  });
});
