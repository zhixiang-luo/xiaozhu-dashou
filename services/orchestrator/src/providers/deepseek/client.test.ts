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
        timeZone: "Asia/Shanghai",
        appContext: {
          currentTime: "2026-10-23T05:00:00.000Z",
          timeZone: "Asia/Shanghai",
          currentConversation: {
            title: "文件传输助手",
            appPackage: "com.tencent.mm",
            capturedAt: "2026-10-23T05:00:00.000Z",
            recentMessages: [
              {
                direction: "incoming",
                text: "明天记得把文件发我"
              }
            ]
          },
          upcomingEvents: [
            {
              title: "和猪哥开会",
              startTime: "2026-10-24T15:00:00+08:00",
              endTime: "2026-10-24T16:00:00+08:00",
              timeZone: "Asia/Shanghai"
            }
          ],
          recentTasks: [],
          relevantMemory: [],
          contacts: [],
          preferences: []
        }
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
    expect(body.messages[0].content).toContain("和猪哥开会");
    expect(body.messages[0].content).toContain(
      "titleQuery 使用上下文里的实际标题"
    );
    expect(body.messages[0].content).toContain(
      "生成最终回复正文"
    );
    expect(body.messages[0].content).toContain("明天记得把文件发我");
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
