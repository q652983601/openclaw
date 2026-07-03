# 🦞 OpenClaw Android 简体中文本地化版

<p align="center">
    <picture>
        <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/openclaw/openclaw/main/docs/assets/openclaw-logo-text-dark.svg">
        <img src="https://raw.githubusercontent.com/openclaw/openclaw/main/docs/assets/openclaw-logo-text.svg" alt="OpenClaw" width="500">
    </picture>
</p>

<p align="center">
  <strong>OPENCLAW 安卓汉化版</strong>
</p>

<p align="center">
  <a href="https://github.com/q652983601/openclaw/actions"><img src="https://img.shields.io/badge/CI-passing-brightgreen?style=for-the-badge" alt="CI status"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="MIT License"></a>
  <a href="https://github.com/q652983601/openclaw/tree/android-zh-localization/apps/android"><img src="https://img.shields.io/badge/Android-汉化版-orange?style=for-the-badge" alt="Android 汉化版"></a>
</p>

这是 [OpenClaw](https://github.com/openclaw/openclaw) 的一个 fork，专门为 Android 端提供**简体中文本地化**。

OpenClaw 是一个可运行在自有设备上的个人 AI 助手，支持多种聊天频道、语音唤醒、Canvas 绘图等功能。本分支把 Android 应用的用户界面全面中文化，方便中文用户使用。

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/q652983601/openclaw.git
cd openclaw
git checkout android-zh-localization
```

### 2. 编译 Android 应用

```bash
cd apps/android
export JAVA_HOME=/opt/homebrew/opt/openjdk@21    # macOS 示例
export ANDROID_HOME=/Users/wilsonlu/Library/Android/sdk

./gradlew :app:assembleThirdPartyDebug
```

### 3. 安装到手机

```bash
# 连接 Android 设备或启动模拟器
./gradlew :app:installThirdPartyDebug
adb shell am start -n ai.openclaw.app/.MainActivity
```

## 汉化范围

- ✅ 总览页（Overview）：底部导航、状态卡片、设置入口
- ✅ 设置页（Settings）：Gateway、频道、节点与设备、审批、用量、技能、Dreaming、外观、关于、健康等
- ✅ 引导页（Onboarding）：欢迎、Gateway 配置、恢复、权限说明
- ✅ 语音页（Voice）：麦克风状态、扬声器控制、唤醒错误提示
- ✅ 聊天页（Chat）：输入框、消息列表、上下文用量条
- ✅ 会话页（Sessions）：会话列表、空态提示
- ✅ 系统通知 / Toast / Snackbar

## 保留英文的部分

- 品牌名 `OpenClaw`
- 协议/产品术语：`Gateway`、`Canvas`、`Cron`、`Provider`、`Agent`、`Node`、`Channel`、`TLS`
- 少量非 UI 逻辑函数中的状态摘要，避免破坏单元测试

## 文档

- 英文说明：[README_EN.md](README_EN.md)
- 英文本地化说明：[docs/ANDROID_LOCALIZATION.md](docs/ANDROID_LOCALIZATION.md)
- 中文本地化说明：[docs/ANDROID_LOCALIZATION_ZH.md](docs/ANDROID_LOCALIZATION_ZH.md)
- 上游官方文档：[https://docs.openclaw.ai](https://docs.openclaw.ai)

## 许可证

与上游项目一致，采用 [MIT License](LICENSE)。
