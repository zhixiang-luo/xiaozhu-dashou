# 验收记录

> Status: Draft
> Owner: QA
> Last reviewed: 2026-10-23

## 2026-10-23 / build 0.1.0

- Tester: controller agent
- Commit: workspace before final integration commit
- Device: not connected
- Android / HyperOS: unavailable
- Shizuku: not tested
- WeChat: not tested
- NetEase Cloud Music: not tested
- Driver mode: not implemented

### Passed

- `:app:testDebugUnitTest`
- `:app:assembleDebug`
- Debug APK produced at `android/app/build/outputs/apk/debug/app-debug.apk`.

### Failed

- No attached ADB device; installation and runtime checks were not possible.

### Release decision

Not eligible for device testing until a dedicated Redmi/HyperOS phone is connected.

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
