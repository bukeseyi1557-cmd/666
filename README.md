# 小伙伴桌宠（Android）

一个原生 Kotlin 安卓桌面宠物 MVP：桌面悬浮、拖动、点击进入聊天、免费本地回复、打开 ChatGPT 网页，以及可配置的 OpenAI Chat Completions 兼容接口。

## 功能

- **桌面悬浮宠物**：使用 Canvas 直接绘制二次元小人，不依赖图片资源或素材商店。
- **拖动与点击**：拖动角色可移动位置；点击角色打开聊天界面。
- **免费本地模式**：简单规则回复，不调用网络、不需要 API Key。
- **ChatGPT 网页**：打开 `https://chatgpt.com/`，使用浏览器或已关联的应用登录。
- **独立 AI 接口**：支持 OpenAI Chat Completions 兼容格式，填写完整 HTTPS endpoint、模型名和 API Key。
- **密钥保护**：API Key 使用 Android Keystore 的 AES-GCM 加密后存放在本机偏好设置中。

## 不安装 Android Studio：使用 GitHub 自动构建 APK

项目已包含 `.github/workflows/build-apk.yml`，可以让 GitHub 的云端构建环境自动编译 APK。

1. 登录 GitHub，创建一个新的 **Public** 或 **Private** 仓库。
2. 解压本压缩包，进入 `AndroidAIPet` 文件夹，将里面的**所有文件和文件夹**上传到仓库根目录（包括 `.github` 文件夹；不要把整个 `AndroidAIPet` 文件夹再套一层上传）。
3. 上传完成后，打开仓库的 **Actions** 页面，选择 **Build Android APK**。如果没有自动运行，点 **Run workflow**。
4. 等待任务完成，打开成功的那次运行记录，在 **Artifacts** 区域下载 `AndroidAIPet-debug-apk`。
5. 解压下载的产物，得到 `app-debug.apk`，将它传到安卓手机安装。首次安装时，按手机提示允许从当前来源安装应用。

这是 **debug 测试 APK**，不是发布商店用的正式签名版本。GitHub 会在云端下载 Gradle 和 Android SDK，因此需要 GitHub Actions 可用且云端网络正常。若构建失败，请打开失败的任务，把日志末尾的错误发给开发者排查。

## 在 Android Studio 中运行

1. 安装较新的 Android Studio，并使用其内置 JDK 17。
2. 解压项目，在 Android Studio 选择 **Open**，打开 `AndroidAIPet` 文件夹。
3. 等待 Gradle 同步。项目使用 Android Gradle Plugin 8.7.3、Gradle 8.9、compileSdk 35、minSdk 26、targetSdk 33。
4. 用 USB 调试或 Android Studio 的设备管理器连接安卓手机，然后点击 Run 安装。
5. 打开“小伙伴桌宠”，点击“开启桌面宠物”，在系统设置里允许“显示在其他应用上层”。
6. 返回应用，宠物出现后可拖动；点击宠物即可打开聊天。

> 当前交付为 Android Studio 源码项目，不是已签名 APK。构建 APK 需要在安装了 Android SDK 的 Android Studio 环境中完成。

## 独立 AI 设置

1. 在聊天界面点击“AI 设置”。
2. 填写服务商提供的完整 HTTPS 接口地址，例如 `https://api.openai.com/v1/chat/completions`。
3. 填写该服务商支持的模型名与 API Key，保存后返回聊天界面，点击“独立 AI”。
4. 之后发送的消息会发送到所配置的服务商。请先确认服务商的接口格式、模型权限、网络与计费规则。

**重要：** ChatGPT Plus 订阅不等于 API 额度。使用 OpenAI API 或其他服务商的 API，可能需要另行开通和付费。不要把 API Key 分享给他人。项目只接受 HTTPS 接口地址，以避免通过明文 HTTP 传输密钥。

## 当前版本的限制

- 本地免费模式是规则式陪聊，不是真正的语言模型。
- ChatGPT 网页模式会切换到浏览器/已关联应用；它不会把本应用中的聊天记录自动同步过去。
- 独立 AI 模式目前按 OpenAI Chat Completions 兼容格式发送请求；不同服务商的鉴权或响应格式若不兼容，可能需要修改 `ChatActivity.kt`。
- Android 厂商的后台管理策略不同。部分手机可能需要在系统设置中允许后台运行或将应用加入电池优化白名单。
- 当前服务使用常驻通知来维持悬浮宠物运行；在系统设置中强行停止应用会结束桌宠。

## 项目结构

```text
AndroidAIPet/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/aipet/
        │   ├── MainActivity.kt
        │   ├── PetService.kt
        │   ├── PetView.kt
        │   ├── ChatActivity.kt
        │   ├── AiSettingsActivity.kt
        │   └── ApiKeyStore.kt
        └── res/values/styles.xml
```

## Gradle 启动说明

项目包含 `gradlew` / `gradlew.bat`。首次在命令行构建时会从 Gradle 官方分发地址下载 Gradle 8.9，因此首次运行需要网络。也可以直接通过 Android Studio 的 Gradle 同步和 Run 按钮构建。
