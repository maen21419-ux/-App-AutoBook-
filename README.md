# AutoBook - 自动记账助手 📒

[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

**AutoBook** 是一个全自动记账应用。核心基于通知栏监听，自动捕获支付宝和微信支付通知，每笔支出无需手动操作即可记录。银行短信监听作为补充，覆盖开通了短信提醒的银行卡。

## ✨ 功能

| 功能 | 说明 |
|------|------|
| 🔔 **通知栏监听** | 监听支付宝、微信支付通知，覆盖所有移动支付（核心功能） |
| 📩 **短信监听** | 识别银行消费短信（支持 招商/工商/建设/中国/浦发/中信/交通 等），补充覆盖银行卡交易 |
| 🤖 **AI 智能分类** | 接入 LLM API，自动将消费归类（餐饮/购物/交通/娱乐…） |
| 📊 **消费统计** | 今日/本月消费汇总，按类别饼图分析 |
| 📋 **记录列表** | 分页查询、关键词搜索、按类别筛选 |
| 📤 **导出 CSV** | 一键导出 Excel 兼容的交易记录文件 |
| 🔒 **隐私安全** | API Key 使用 Android Keystore + AES-256 加密存储，不上传服务器 |

<div align="center">
  <img width="32%" alt="首页" src="https://github.com/user-attachments/assets/3e088907-4572-4ca0-bf38-a2ee79b44663" />
  <img width="32%" alt="统计" src="https://github.com/user-attachments/assets/b58dc3eb-8372-4a11-92e8-e0abf3651728" />
  <img width="32%" alt="设置" src="https://github.com/user-attachments/assets/8442e339-66f3-47e3-96c4-bcbb7ef467b4" />
</div>



## 📱 下载

> [⬇️ 下载最新 APK](https://github.com/maen21419-ux/-App-AutoBook-/releases/latest)

或前往 [Releases](https://github.com/maen21419-ux/-App-AutoBook-/releases) 页面选择历史版本。

## 🚀 快速开始

### 1. 安装
下载 APK → 安装 → 授予以下权限：
- **通知监听权限** — 监听支付宝/微信支付通知，覆盖绝大多数消费（设置 → 特殊权限 → 通知使用权）
- **短信权限** — 补充覆盖开通短信提醒的银行卡交易

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

## 📋 二期规划

### 新平台支持
- [ ] 主流银行 App 通知监听（招商银行、工商银行、建设银行等）
- [ ] 银联云闪付解析完善 & 真机测试
- [ ] 更多银行短信模板覆盖

### 功能增强
- [ ] LLM API 配置面板 — 支持自定义 API 地址、模型名称（不再硬编码 DeepSeek）
- [ ] 月度预算设置 & 超支提醒
- [ ] 数据备份/恢复（JSON 导出导入）
- [ ] 清空数据增加确认对话框
- [ ] 定期推送消费周报/月报

### 体验优化
- [ ] 替换应用图标（当前为系统占位图标）
- [ ] 发布签名 + Google Play / 酷安 上架
- [ ] 交易列表按日期分组（今天/昨天/本周/更早）
- [ ] 通知栏记账结果点击跳转详情页

### 技术改进
- [ ] settings.gradle.kts 仓库镜像提供国际用户选项
- [ ] 单元测试覆盖核心解析器
- [ ] CI/CD 自动构建 Release APK

## 📄 License

MIT © AutoBook
