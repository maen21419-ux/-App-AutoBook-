# AutoBook - 自动记账助手 📒

[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

**AutoBook** 是一个全自动记账应用，能够监听短信和通知栏，自动识别支付宝、微信支付、银行卡消费记录，让你每笔开销都自动记录，告别手动记账。

## ✨ 功能

| 功能 | 说明 |
|------|------|
| 📩 **短信监听** | 自动识别银行消费短信（支持 招商/工商/建设/中国/浦发/中信/交通 等主流银行），提取金额和商户 |
| 🔔 **通知栏监听** | 监听支付宝、微信支付通知，实时自动记账（无需手动触发） |
| 🤖 **AI 智能分类** | 接入 LLM API，自动将消费归类（餐饮/购物/交通/娱乐…） |
| 📊 **消费统计** | 今日/本月消费汇总，按类别饼图分析 |
| 📋 **记录列表** | 分页查询、关键词搜索、按类别筛选 |
| 📤 **导出 CSV** | 一键导出 Excel 兼容的交易记录文件 |
| 🔒 **隐私安全** | API Key 使用 Android Keystore + AES-256 加密存储，不上传服务器 |

## 📱 下载

> [⬇️ 下载最新 APK](https://github.com/maen21419-ux/-App-AutoBook-/releases/latest)

或前往 [Releases](https://github.com/maen21419-ux/-App-AutoBook-/releases) 页面选择历史版本。

## 🚀 快速开始

### 1. 安装
下载 APK → 安装 → 授予以下权限：
- **短信权限** — 读取银行消费短信
- **通知监听权限** — 读取支付宝/微信支付通知（设置 → 特殊权限 → 通知使用权）

### 2. 配置（可选）
- 设置 **LLM API Key** 以启用 AI 自动分类
- 支持 OpenAI 兼容接口，可自行配置 API 地址和模型

### 3. 开始使用
支付后无需任何操作，消费记录自动出现在首页！

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository Pattern |
| 数据库 | Room |
| 依赖注入 | Hilt |
| 网络 | OkHttp + Gson |
| 异步 | Coroutines + Flow |
| 加密 | Android Keystore + EncryptedSharedPreferences |

## 🔨 本地构建

```bash
# 1. 配置 SDK 路径（在 local.properties）
sdk.dir=/path/to/Android/sdk

# 2. 构建调试版
./gradlew assembleDebug

# 3. 构建发布版（需要签名）
./gradlew assembleRelease
```

用 Android Studio 打开项目即可一键运行。

## 📖 项目结构

```
app/src/main/java/com/autobook/app/
├── data/           # 数据层（Room, Model, Repository）
├── di/             # Hilt 依赖注入
├── domain/         # 业务逻辑（解析器、分类器、API Client）
├── service/        # 后台服务（短信接收、通知监听）
└── ui/             # 界面（首页、设置、统计、列表）
```

## 📄 License

MIT © AutoBook
