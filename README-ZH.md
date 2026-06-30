# OpenClaw Android 汉化版

> 本仓库是 `openclaw/openclaw` 的社区汉化分支，仅针对 Android App 进行中文本地化。

## 汉化内容

| 文件 | 说明 |
|------|------|
| `apps/android/app/src/main/res/values/strings.xml` | 英文源字符串（157 个翻译键） |
| `apps/android/app/src/main/res/values-zh-rCN/strings.xml` | 简体中文翻译 |
| `kotlin-modifications.md` | Kotlin 代码修改指南（104 处替换规则） |

## 使用方式

### 方式一：GitHub Actions 自动编译（推荐）

1. 将本分支的修改合并到 fork 的 `android-zh-localization` 分支
2. 进入 GitHub → Actions → Build Android APK (ZH)
3. 点击 **Run workflow** 手动触发
4. 等待约 5-10 分钟，下载生成的 APK

### 方式二：本地 Android Studio 编译

1. 打开 Android Studio
2. 打开 `apps/android` 目录
3. 应用 `kotlin-modifications.md` 中的替换规则
4. 点击 Build → Build APK

## 同步官方更新

```bash
# 添加官方 upstream
git remote add upstream https://github.com/openclaw/openclaw.git

# 拉取官方更新
git fetch upstream

# 合并到汉化分支
git checkout android-zh-localization
git rebase upstream/main

# 如有冲突，重新翻译新增字符串，然后推送
git push origin android-zh-localization --force-with-lease
```

## 已知限制

- 当前汉化仅覆盖 UI 字符串（strings.xml 方式），后端 Gateway 的错误消息仍为英文
- 语音唤醒关键词（assistant.xml）保持英文，以兼容 Google Assistant

## 翻译规范

- 品牌名 **OpenClaw** 保留英文
- 技术术语 **Gateway、TLS、SHA-256、ws://、wss://** 保留英文
- 符合 Android 中文 UI 习惯，简洁直接
