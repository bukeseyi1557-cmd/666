# 小伙伴桌宠 2.0（清晰形象 + 桌面对话版）

本升级版解决了两个核心问题：

1. **角色发糊**：重新整理了本地角色图集 `pet_atlas.png`，使用更清晰的透明背景图集显示桌宠。
2. **交互单一**：新增了桌面对话悬浮窗、长按菜单、快捷短语、AI/本地模式切换、动作演示等功能。

## 新交互方式

- **单击桌宠**：随机卖萌、摸摸头、惊讶、跳一下等反应。
- **双击桌宠**：打开桌面对话悬浮窗，可直接在桌面输入消息聊天。
- **长按桌宠**：打开功能菜单（桌面对话 / 完整聊天 / 动作演示 / 休息唤醒 / 隐藏 / 模式切换）。
- **拖动桌宠**：移动位置，松手后自动吸附到屏幕左右边缘。

## 桌面对话支持

- 本地免费模式：无需联网，规则式回复。
- 独立 AI 模式：读取 `AiSettingsActivity` 中配置的兼容 OpenAI Chat Completions 的接口。
- 快捷短语：你好、夸夸我、安慰我、讲个笑话、晚安、摸摸头。

## 主要修改文件

- `app/src/main/java/com/example/aipet/PetView.kt`
- `app/src/main/java/com/example/aipet/PetService.kt`
- `app/src/main/java/com/example/aipet/PetChatEngine.kt`（新增）
- `app/src/main/java/com/example/aipet/ChatActivity.kt`
- `app/src/main/java/com/example/aipet/MainActivity.kt`
- `app/src/main/res/drawable-nodpi/pet_atlas.png`

## 升级方式

将本项目上传到你已有的 GitHub 仓库中覆盖同路径文件，然后让 GitHub Actions 重新构建 APK。
