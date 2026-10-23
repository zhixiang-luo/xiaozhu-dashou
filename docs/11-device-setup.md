# 专用 Android 设备配置

> Status: Draft
> Owner: Android
> Last reviewed: 2026-10-23

本文用于配置小猪搭手 Developer Edition 的独立测试手机。APK 已可构建，本清单是安装和真机验收的前置环境。

## 设备原则

- 使用独立的小米/Redmi 手机和测试账号。
- 不在设备上登录主要银行、支付、生产社交或公司账号。
- 保留锁屏、系统更新和设备恢复能力。
- 不通过 Root、录制或自动化规避支付、验证码、生物识别和 `FLAG_SECURE`。
- 出问题时可恢复出厂，不影响个人主力设备。

## 开发机准备

需要：

- JDK 17
- Android SDK Command-Line Tools
- Platform Tools（包含 `adb`）
- Android API 30 或更高平台
- Node.js 22.6+
- pnpm 11.25

当前开发机已经安装 JDK 17、Android SDK、Build Tools 和 Gradle Wrapper。换到其他机器时先验证：

```bash
java -version
adb version
sdkmanager --version
node --version
pnpm --version
```

## 手机基础配置

1. 准备独立设备并升级到目标 HyperOS 版本。
2. 开启开发者选项和 USB 调试。
3. 通过 USB 连接开发机，确认 `adb devices` 显示 `device`。
4. 在 HyperOS 中允许 USB 安装和调试。
5. 关闭针对小猪搭手的省电限制和后台自动清理。
6. 保持设备插电并连接稳定网络，用于长任务测试。

## App 权限

安装 App 后按需开启：

- 麦克风：语音指令和常驻唤醒词。
- 悬浮窗：跨 App 的 Jarvis 入口和状态卡片。
- 无障碍：读取前台 UI 树和执行降级手势。
- 通知使用权：读取通知和媒体会话。
- 屏幕录制：无障碍截图不可用时的屏幕观察。
- 前台服务：保持任务和悬浮入口运行。
- 默认助手：长按 Home/电源键进入小猪搭手。

每项权限在 App 内展示用途、当前状态、拒绝后的功能和对系统设置的跳转。

## Shizuku

1. 安装并启动 Shizuku。
2. 在开发者选项中开启无线调试。
3. 按 Shizuku 指引完成配对和服务启动。
4. 在小猪搭手内确认 Shizuku 状态为可用。
5. 重启手机后重新检查 Shizuku；不可用时验证无障碍降级。

App 不得把 Shizuku 用户文本拼接成 shell 命令。所有点击、滑动、按键和文本输入必须来自共享 schema 校验后的结构化动作。

## Device Owner

Device Owner 可在专用设备上减少重复权限配置，但必须等 Android 管理模块和测试实现后再启用。启用前必须确认：

- 设备已恢复出厂且没有绑定需要保留的个人账号；
- 管理能力不绕过支付、验证码和生物识别；
- 有明确卸载和恢复出厂方案；
- 管理模块有独立 ADR、测试和设备恢复文档。

当前阶段不使用 Device Owner。

## 首次验收

完成环境配置后依次验证：

1. App 安装、启动和崩溃恢复。
2. 权限拒绝、授权和系统撤回。
3. 悬浮球在微信、网易云、高德和系统桌面上的显示。
4. 无障碍读取前台 App。
5. Shizuku 可用时的结构化点击和输入。
6. 断开 Shizuku 后的无障碍降级。
7. 用户取消后没有迟到动作。
8. 进入支付、验证码和账号安全页时立即暂停。

验收结果写入 `docs/10-acceptance-record.md`，包含设备型号、系统版本、App commit、Shizuku 版本和各 App 版本。
