# 验收记录

> Status: Draft
> Owner: QA
> Last reviewed: 2026-10-23

当前没有可运行的 Android App，因此尚无真机验收结果。

## 记录模板

每个版本或目标 App 更新后新增一条记录：

```md
## YYYY-MM-DD / <app-or-build-version>

- Tester:
- Commit:
- Device:
- Android / HyperOS:
- Shizuku:
- WeChat:
- NetEase Cloud Music:
- Driver mode:
- Test account:

### Passed

### Failed

### Manual interventions

### Security observations

### Release decision
```

## 必须记录

- 测试的 Git commit；
- 设备型号、系统版本和 Shizuku 版本；
- 微信、网易云音乐和本 App 版本；
- 每条失败任务的输入、起始页面和错误码；
- 人工接管次数与原因；
- 任何错误收件人、重复发送、敏感信息暴露或迟到动作；
- 是否允许进入下一里程碑。

禁止用真实联系人、真实支付或生产账号填充记录。
