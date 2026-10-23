# 录制 App 技能

App 技能不是让用户每次手工编辑代码。推荐流程是：

1. 在一台专用手机上手动完成一次真实操作。
2. 工具持续采集屏幕截图和 Accessibility UI 树。
3. 从录制中提取页面、控件、输入和验证点。
4. 生成 `manifest.yaml` 草稿。
5. 在 Shizuku 和无障碍两种模式回放。
6. 固定测试用例并记录兼容 App 版本。

## 开始录制

确保手机已连接、解锁，目标 App 已登录测试账号。然后运行：

```bash
./scripts/capture-skill-session.sh wechat.send-message
```

每个采样周期会保存：

- `screens/NNNNN.png`：当前屏幕截图。
- `ui/NNNNN.xml`：当前 Accessibility UI 树。
- `metadata.txt`：设备、时间和采样间隔。

录制文件位于：

```text
artifacts/skill-recordings/
```

该目录已被 Git 忽略，避免误提交聊天内容或个人数据。

## 录制时怎么操作

- 从任务规定的起始页面开始。
- 每一步正常点击一次，不要连续快速点击。
- 输入完成后停顿约 1 秒，让截图和 UI 树记录到结果。
- 同名联系人、歌曲多版本等歧义场景也单独录一次。
- 完成后按 `Ctrl+C`。

## 当前首批技能

- `wechat.send-message`：微信发送普通消息。
- `netease-music.play-song`：网易云音乐搜索并播放歌曲。
- `amap.navigate`：高德地图按目的地开始导航。
- `cross.wechat-to-amap-navigate`：从微信提取地址并调用高德技能。

## 产出说明

录制不会直接生成可自动执行的技能。控制器需要：

1. 去除重复帧和无关页面。
2. 选择稳定 resource ID、文本或 contentDescription。
3. 填写输入参数和输出数据。
4. 定义成功验证和异常处理。
5. 将录制材料转成 `manifest.yaml`。
6. 在目标 App 版本执行回放测试。

完整技能规则见 [新增 App 技能](adding-app-skill.md)。
