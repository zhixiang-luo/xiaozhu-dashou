# 小猪搭手当前交接

> Last updated: 2026-10-23

## Current State

项目已正式命名为小猪搭手，定位为开发者专用 Android Agent 设备。Redmi K60 已完成
安装、无障碍观察、Shizuku UserService、火山实时语音、DeepSeek 意图拆解和微信文件
传输助手技能校准；当前进入公开 v0.1 发布准备。

## Completed

- 明确 Android 优先、云端编排、本机执行的产品技术方向。
- 明确 Shizuku 优先、无障碍降级的执行策略。
- 明确低风险消息自动发送、支付/敏感信息人工处理的风险策略。
- 建立产品、PRD、架构、Agent、模型、安全、测试和路线图文档。
- 建立根目录与模块级 `AGENTS.md`。
- 建立 ADR、模型接入指南、App 技能指南和 GitHub 协作模板。
- 建立角色化 Agent 任务书、CODEOWNERS 示例和文档 CI。
- 将项目从 Mobile Agent 重命名为 Personal Jarvis，后续正式定名小猪搭手，并加入 Developer Appliance Mode。
- 实时语音使用火山大模型双向流式 ASR。
- 意图拆解使用 DeepSeek `deepseek-chat`，并保留开发版规则规划器降级。
- 建立 pnpm workspace、共享合约、云端编排服务和统一 CI 检查。
- 增加 WebSocket 任务事件、确认接口和 Android CI。
- 建立技能 manifest 的 JSON Schema 与 Zod 校验包。
- 建立 Compose App 骨架并生成 Debug APK。
- 接入 AccessibilityService、页面风险分类、取消代次门禁和无障碍执行驱动。
- 接入 Shizuku Provider、AIDL UserService 和 Shizuku/无障碍自动回退。
- 在 Redmi K60 / Android 15 / HyperOS OS3.0 上完成安装与权限验收。
- 修复 HyperOS 下 Shizuku UserService 缺少进程名导致的启动崩溃。
- 将 App 可见按钮和状态切换为中文，并在真机验证页面读取显示中文结果。
- 建立中文悬浮球：短按回到主界面，长按启动系统语音识别并显示临时转写结果。
- 接入 `TaskPlannerClient` 和开发版任务规划器，支持把“打开设置/微信/网易云/高德”转换为本机动作。
- 建立含输入、步骤、风险、验证和跨技能调用的 YAML 技能格式，并添加 4 个首批技能草稿。
- 建立 ADB 截图/UI 树技能录制脚本和录制指南。
- 确定“暖科技任务猪”角色方向，明确产品名“小猪搭手”、人格名“猪哥”和“说一句，帮你做。”品牌主张。
- 建立主标记、App 图标、Android 自适应/单色图标、横向品牌组合和六态交互总览的 SVG 源文件与 PNG 预览。
- 记录“猪哥猪哥”唤醒流程、悬浮球状态、声音人格、统一话术和 TTS 候选盲测要求。
- 建立猪哥 IP v0.1：约 3.2 头身全身正面、四视图、八种基础表情和六种手部手势。
- 记录跨产品 IP 架构、产品换装规则、3D 生产前置条件、文件版本规范和原创产权清单。
- 在 Figma 中仅使用声账原始标记、猪哥头部标记和 V0.1 全身正面三张身份参考，手工重排 `v0.2` 精修候选。
- 停止使用未达到预期的 Figma 版本，建立 `v0.3` 本地高保真产品界面；角色、图标与表情直接复用既有资产，没有再次生成角色。

## Verified

- 共享合约：6 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- 云端编排：31 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- 技能 schema：9 项 Vitest 测试通过，TypeScript 严格类型检查通过。
- Android：32 项 JVM 单元测试通过，`assembleDebug` 成功。
- `node scripts/check-docs.mjs` 通过：相对链接可解析、代码块闭合、无空文档和尾随空格。
- GitHub Actions 和 Issue 模板的 3 个 YAML 文件解析通过。
- 未发现常见密钥格式内容。
- Debug APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`。
- 真机屏幕读取返回 `com.personaljarvis: NORMAL, 4 nodes`。
- Shizuku 服务端以 shell UID 运行，小猪搭手 UserService 进程 `com.personaljarvis:jarvis-actions` 已启动。
- 中文界面验证通过：当前应用、页面类型和控件数量的结果均为中文。
- 悬浮球前台服务已运行，系统窗口层确认 168×168 的悬浮控件存在。
- 短按悬浮球已在真机验证可以打开小猪搭手；长按手势能启动语音会话且进程不崩溃。
- 录音状态增加“正在听…”展开、触感、脉冲和实时部分文字；按住等待期间不会提前提示未听清。
- 本地任务服务已验证为“打开设置”返回 `open_app / com.android.settings` 动作。
- `adb reverse tcp:4100 tcp:4100` 已建立，手机通过回环地址访问任务服务。
- 设备所有者完成端到端真机验收：语音说“打开设置”后，系统设置 App 被自动打开。
- 设备所有者已完成新一轮真机体验测试，确认录音状态反馈与中文转写效果可接受。
- 品牌 SVG 通过 `xmllint --noout design/brand/*.svg` 结构检查，`tokens.json` 通过 `jq empty` 校验。
- 技能录制脚本通过 2 帧截图/UI 树冒烟测试；4 个技能清单均通过结构校验。
- PNG 预览通过 macOS Quick Look 渲染：App/自适应/单色/主标记为 1024×1024，横向组合为 1600×400，六态总览为 1800×980。
- IP v0.1 的全身正面对应 800×1200，四视图 2400×1500，表情 1800×820，手势 1800×760。
- IP v0.2 精修候选导出为 3840×2160 PNG；角色主体直接沿用参考图，未进行生成式重绘。
- IP v0.3 页面在 1600×1000 视口完成截图，主视觉导出为 3200×2000 PNG；角色、图标和表情资源均返回 200 并成功渲染。
- “暖科技任务猪”图标、角色动画、常驻唤醒和正式 TTS 尚未接入 Android App 或目标真机验证。
- 猪哥 IP v0.1 仍是概念评审稿，尚未完成 3D 建模、绑定、动作或法律确权。
- 真实火山实时 ASR 已用 6.8 秒中文 PCM 验证，返回 16 次 partial 和完整 final。
- 真实 DeepSeek API 已把“帮我到文件传输助手里面去发一句话，比如说测试成功了”
  拆解为文件传输助手和消息正文“测试成功了”。
- 微信发送动作已加入受控重启、前台 App 包名校验和默认关闭的显式开发开关。
- 微信文件传输助手五步技能和中文剪贴板输入已实现；仍需完成最终整链真机回归。

## Open Decisions

- Android 包名。
- 云端服务部署主机与域名。
- 默认 TTS 声线仍需完成三种候选盲测。
- 本地唤醒词引擎和误唤醒基线尚未确定。
- 猪哥全身比例采用约 3.2 头身还是回调到约 2.8 头身。
- 四指手长度、任务马甲遮盖比例和尾巴信号灯结构需要下一轮评审。
- “小猪搭手”“猪哥”和主标记尚未完成商标近似检索。

## Open Risks

- 当前已连接专用 Redmi K60，真机基础权限和开发版执行链路已验证。
- HyperOS 后台限制和 Shizuku 重启失效可能导致执行不稳定。
- 微信界面、输入控件和搜索结果会随版本变化。
- 消息自动发送必须实现严格的唯一收件人和敏感内容判断。
- 新品牌资产尚未经过目标真机桌面、深色主题、通知栏和小尺寸验证。
- “猪哥猪哥”包含重复音节，需要真实噪声环境下的误唤醒测试。
- 角色 IP 尚未建立完整创作档案、外部贡献者授权和著作权登记。
- 开源前需要完成许可证、安全披露和个人数据清理。

## Next Smallest Task

完成公开 v0.1 发布前的密钥与隐私审计、GitHub CI 验证、Private Vulnerability
Reporting 配置和 Release 发布；随后用真机回归自然语言微信消息整链。
