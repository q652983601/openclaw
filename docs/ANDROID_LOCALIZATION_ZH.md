# OpenClaw Android 简体中文本地化

本分支（`android-zh-localization`）为 OpenClaw Android 应用提供**完整的简体中文（zh-CN）本地化**。

## 覆盖范围

以下界面的用户可见英文已全部外置到 `apps/android/app/src/main/res/values/strings.xml`，并在 `values-zh-rCN/strings.xml` 中提供中文翻译：

- **总览 / Shell** — 底部导航、状态芯片、指标卡片、设置主页
- **设置** — Gateway、频道、节点与设备、审批、用量、技能、Dreaming、外观、关于、健康、Canvas、通知、手机上下文
- **引导页** — 欢迎、Gateway 设置、恢复、权限
- **语音** — 语音球、麦克风状态、唤醒错误、扬声器控制
- **聊天** — 输入框、消息列表、Markdown 渲染、上下文用量条
- **会话** — 会话列表、空态、运行状态
- **ViewModel / Manager** — `MainViewModel`、`NodeRuntime`、`MicCaptureManager`、`VoiceWakeManager` 的 Toast / Snackbar / 通知文案

品牌与协议名词（如 `OpenClaw`、`Gateway`、`Canvas`、`Cron`、`Provider`、`Agent`、`Node`、`Channel`、`TLS`）保留英文。

## 构建

```bash
cd apps/android
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/Users/wilsonlu/Library/Android/sdk
./gradlew :app:assembleThirdPartyDebug
```

## 测试

```bash
./gradlew :app:testThirdPartyDebugUnitTest
```

## 安装

```bash
# 连接 Android 设备或启动模拟器
./gradlew :app:installThirdPartyDebug
adb shell am start -n ai.openclaw.app/.MainActivity
```

## 关键设计决策

1. **字符串外置优先**：所有硬编码用户可见字符串统一移入 `strings.xml`。
2. **双语言包**：默认 `values/strings.xml` 保留英文；`values-zh-rCN/strings.xml` 存放中文翻译。
3. **Compose 安全**：`stringResource(...)` 仅在 `@Composable` 函数中使用。非 Composable 的摘要帮助函数保持英文硬编码，以免破坏现有 JVM 单元测试。
4. **AAPT 安全**：XML 中的 `&` 转义为 `&amp;`；单引号通过避免使用或整体用双引号包裹来处理。
5. **Locale 配置**：`res/xml/locale_config.xml` 声明 `zh-Hans-CN`，让系统能正确广播支持的语言环境。

## 已知仍保留英文的地方

以下英文属于有意保留：

- 品牌与协议名词（`OpenClaw`、`Gateway`、`Canvas` 等）
- 少量返回状态摘要的非 Composable 帮助函数（如 `voiceStatusLabel`、`contextMeterLabel`、`androidDistributionChannel`），避免破坏现有单元测试。
- 产品口号 `"Exfoliate! Exfoliate!"` 等待产品决策。

## 贡献方式

如果你想扩展本本地化（例如增加繁体中文 `zh-rTW` 或其他语言）：

1. 复制 `apps/android/app/src/main/res/values-zh-rCN/strings.xml` 到新的 `values-*/strings.xml`。
2. 保持 key 不变，仅翻译字符串值。
3. 将新 locale 添加到 `res/xml/locale_config.xml`。
4. 运行 `./gradlew :app:assembleThirdPartyDebug` 和 `./gradlew :app:testThirdPartyDebugUnitTest`。

## 许可证

本本地化工作与上游 OpenClaw 项目使用相同许可证（MIT）。详见 [LICENSE](../LICENSE)。
