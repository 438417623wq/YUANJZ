# 小秋记账 / XiaoQiu Accounting

## 中文介绍

小秋记账是一款本地化日常记账 Android App，包名为 `com.qiuqiuqiu`。应用面向个人日常使用，支持记录支出、收入和转账，数据默认保存在本机 SQLite 数据库中，图片票据和自定义音效也会复制保存到 App 私有目录，方便完整备份与恢复。

### 主要功能

- 日常记账：支持支出、收入、转账记录。
- 分类记账：内置餐饮、购物、交通、住房、工资、奖金等常用分类。
- 图片票据：添加图片时会复制进 App 本地目录，避免原相册文件变化导致丢失。
- 明细筛选：支持按月、按日、今天切换，并可按全部、支出、收入、转账筛选。
- 图表统计：支持月、年、总统计，包含概览、环形占比图、趋势柱状图和分类排行。
- 本地账户：支持现金、微信、支付宝、银行卡、信用卡等账户余额管理。
- 完整备份：可导出和导入完整备份，包含数据库、图片、录音音效和本地设置。
- 音效设置：支持多种点击音效、动物风格音效、录音自定义按键音效。
- 按键震动：可开启或关闭点击震动反馈。
- 宠物陪伴：底部宠物板块支持领养 3D 玩偶风宠物，通过记账获得经验和金币，可喂食、玩耍、抚摸和购买装饰。

### 技术说明

- 开发语言：Java
- 平台：Android
- 数据存储：SQLite + SharedPreferences
- UI：原生 Android View 动态构建
- 图表和宠物：Canvas 自绘
- 备份格式：ZIP

### 构建方式

```bash
./gradlew assembleDebug
```

Windows 环境：

```bat
gradlew.bat assembleDebug
```

构建后的调试 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## English Description

XiaoQiu Accounting is an offline-first Android bookkeeping app for daily personal finance tracking. Its package name is `com.qiuqiuqiu`. The app stores accounting data locally in SQLite, while receipt images and custom click sounds are copied into the app-private directory so they can be included in full backup and restore.

### Key Features

- Daily bookkeeping: record expenses, income, and transfers.
- Category-based entries: built-in daily categories such as food, shopping, transport, housing, salary, bonus, and more.
- Receipt images: selected images are copied into the app storage to reduce the risk of missing files.
- Transaction details: filter by month, day, today, and by all, expense, income, or transfer.
- Statistics dashboard: monthly, yearly, and all-time statistics with overview metrics, donut chart, trend bars, and category ranking.
- Local accounts: manage balances for cash, WeChat, Alipay, bank cards, credit cards, and custom accounts.
- Full backup: export and import ZIP backups containing the database, receipt images, recorded sounds, and local settings.
- Sound settings: choose built-in click sounds, animal-style sounds, or record a custom click sound.
- Haptic feedback: enable or disable button vibration.
- Pet companion: adopt a 3D doll-style pet, earn experience and coins from bookkeeping, feed it, play with it, pet it, and buy decorations.

### Technical Details

- Language: Java
- Platform: Android
- Storage: SQLite + SharedPreferences
- UI: Native Android Views built programmatically
- Charts and pet rendering: Canvas drawing
- Backup format: ZIP

### Build

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
