# DeepSeek 意图规划器设计

> 状态：已确认
> 日期：2026-10-23

## 目标

把自然语言转换成受约束的技能调用。首个可用场景是向微信“文件传输助手”发送任意
普通文本。DeepSeek 只做语义拆解，不直接生成点击坐标、Shell 命令或未注册动作。

## 数据流

```text
实时语音文字
  -> DeepSeek chat/completions
  -> AssistantIntent JSON
  -> zod 结构校验
  -> SkillCatalog 存在性与参数校验
  -> 本地安全校验
  -> 确定性 AssistantAction 序列
  -> Android PolicyGate 与执行器
```

## 意图协议

```json
{
  "kind": "execute",
  "skillId": "wechat.send-message",
  "arguments": {
    "contactName": "文件传输助手",
    "message": "测试成功了"
  }
}
```

也允许：

```json
{"kind":"clarify","question":"你想发给谁？"}
```

```json
{"kind":"unsupported","reason":"当前没有可执行这个请求的技能。"}
```

未知字段、未知技能、缺少必需参数和非法参数类型必须拒绝。

## 首批能力

- `wechat.send-message`：只接受已校准的“文件传输助手”。
- 消息内容由模型抽取，进入 `input_text` 动作。
- 其他联系人或群聊返回澄清提示，不冒险盲点。
- 其他已支持 App 仍由本地开发版规划器处理。

## 模型适配

- 默认端点：`https://api.deepseek.com/chat/completions`
- 默认模型：`deepseek-chat`
- `temperature: 0`
- `response_format: {"type":"json_object"}`
- 超时：10 秒
- 密钥：`DEEPSEEK_API_KEY`，只存在于服务端环境变量
- 适配器输出只允许 Intent Schema，不返回供应商原始响应

## 降级

- Key 未配置、DeepSeek 超时、限流或返回非法 JSON 时，回退 `DevTaskPlanner`。
- 回退仍不支持的自然语言返回明确澄清，不假装成功。
- 日志只记录错误类别和耗时，不记录用户正文、消息参数或 Key。

## 安全

- 模型不能直接产生 Android 点击动作。
- 文件传输助手名称必须归一化后完全匹配。
- 消息正文为空、超长或明显包含验证码、密码、支付凭据时停止自动发送。
- 所有生成动作继续经过 Android `PolicyGate`。

## 验收

- 用户原话“帮我到文件传输助手里面去发一句话，比如说‘测试成功了’”产生四步微信动作，
  其中消息正文是“测试成功了”。
- DeepSeek 返回非法技能或非法 JSON 时不执行任何动作。
- 超时后基础“打开设置”仍可执行。
- 真实 DeepSeek 请求成功一次，且日志和测试快照不含 Key。
