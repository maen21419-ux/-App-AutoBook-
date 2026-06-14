# AutoBook - 自动记账助手

自动识别支付宝、微信支付、银行卡消费通知，一键记账。

## 功能

- **短信监听** - 自动识别银行消费短信，提取金额、商户
- **通知栏监听** - 监听支付宝/微信支付通知，实时记录
- **AI 分类** - 通过 DeepSeek API 自动归类消费
- **数据管理** - 支持导出 CSV、按类别/时间查询

## 技术栈

- Kotlin + Jetpack Compose (Material 3)
- Room Database
- Hilt DI
- OkHttp + Gson
- NotificationListenerService
- Coroutines + Flow

## 系统要求

- Android 8.0+ (API 26+)
- 需要授予：短信权限、通知监听权限

## 构建

1. 在 `local.properties` 中配置 Android SDK 路径：
   ```
   sdk.dir=/path/to/Android/sdk
   ```
2. 使用 Android Studio 打开项目，或命令行构建：
   ```bash
   ./gradlew assembleDebug
   ```

## 使用

1. 设置 DeepSeek API Key（可选，启用 AI 分类）
2. 开启短信监听 / 通知栏监听
3. 在系统设置中授予相应权限
4. 消费后自动记账，无需手动操作

## License

MIT
