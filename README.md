# Ethereal

> 一个基于 **MCP 1.8.8** 的 Minecraft 客户端框架，纯手写 UI、无 Mixin、无第三方 GUI 库。

![Minecraft](https://img.shields.io/badge/Minecraft-1.8.8-brightgreen)
![Java](https://img.shields.io/badge/Java-8-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 简介

Ethereal 是一个从零构建的 Minecraft 1.8.8 客户端，目标是在**不依赖 Mixin、不使用第三方 GUI 库**的前提下，用最原生的方式实现一整套客户端功能：模块系统、事件总线、手搓 UI、HUD、动画、账号登录等。

项目使用 **MCP（Mod Coder Pack）1.8.8** 反编译源码作为开发环境，需要改原版行为时**直接修改 MCP 源码**（如 `ItemRenderer.java`、`GuiIngame.java`），而不是注入 Mixin。代价是重新反编译会丢失改动，但换来的是**零运行时注入、零兼容性风险**。

---

## 开发者

@kojkl deepseek

---

## 功能特性

### 战斗
- **KillAura** — 单目标 / 切换模式，9 种排序策略，AutoBlock，Packet / Normal / None 三种转头模式
- **Target** — 全局目标管理，过滤 + 排序 + 切换冷却
- **ThrowableAura** — 投掷物自动瞄准（蛋 / 雪球），预判 + 重力补偿，静默切槽

### 移动
- **Scaffold** — 自动搭路，支持跑跳搭（AutoJump）、Tower（按空格往上搭）、背包自动切块
- **Sprint** — 自动疾跑
- **Noslow** — 取消使用物品时的减速

### 玩家
- **FullBright** — 全亮
- **Rotations** — 静默转头渲染层，本地头部跟随

### 渲染 / HUD
- **HUD** — HUD 总控模块，统一管理所有 HUD 元素的开关、位置、主题色
- **Watermark** — 客户端水印，支持 **Classic**（左上角）/ **Modern**（顶部居中状态栏）两种模式
- **Modern Watermark** — 状态栏式水印，按优先级动态切换内容：
  - `NOTI` — 通知（最多 2 条，加高显示）
  - `TARGET` — 目标信息（头像 + 名字 + 血量 + 伤害残留）
  - `EATING` — 进食 / 喝药进度
  - `SCAFFOLD` — 搭路方块数 + 进度条
  - `IDLE` — Logo + 客户端名 + FPS + 服务器 IP + 玩家名
- **ArrayList** — 模块列表，按显示宽度排序，滑入动画
- **TargetHud** — 目标信息面板，支持伤害残留、血量颜色插值、渐入渐出
- **Scoreboard** — 自定义计分板（屏蔽原版 sidebar）
- **Username** — 用户名 + 服务器显示

### 通知系统
- 堆叠 + 淡入淡出 + 缓动
- 4 种类型：`SUCCESS` / `ERROR` / `WARN` / `INFO`，每种自带图标（对勾 / 叉 / 叹号 / i）
- **Classic 模式**：右下角竖版通知
- **Modern 模式**：嵌入顶部状态栏

### 账号
- **微软正版登录** — OAuth 设备代码流（`MinecraftAuth` + `lenni0451.commons`）
- 独立的登录窗口（`GuiMicrosoftLogin`），显示设备代码 + 一键打开浏览器 + 复制代码

### UI
- **ClickGUI** — 三栏式界面（Tab + 模块列表 + 设置）
  - 开 / 关动画、Tab 滑块、状态点颜色过渡
  - 右侧设置区**滚轮滑动** + scissor 裁剪 + 滚动条
  - 动态高度，支持展开式 Dropdown

### 系统
- **ConfigManager** — Gson 序列化，保存模块状态 / 键位 / 所有参数值 / HUD 位置
- **AutoSaveManager** — 每 60 秒自动保存
- **CommandManager** — 命令系统（`.bind` / `.toggle` / `.modules` / `.config` / `.login`）

---

## 技术栈

| 类别 | 选型 | 说明 |
|---|---|---|
| 游戏版本 | Minecraft 1.8.8 | MCP 反编译源码 |
| 语言 | Java 8 | 不使用高版本 API |
| 依赖管理 | **手动** | Jar 放 `lib/` 目录，IDE 里手动加 Build Path |
| 字节码注入 | **无** | 需要改原版时直接改 MCP 源码 |
| GUI | **手搓** | 全部用 `Tessellator` + `RenderUtil` |
| 序列化 | Gson 2.10.1+ | 旧版没有 `JsonParser.parseString` |
| 登录 | MinecraftAuth | 需要两个 lenni0451 依赖 |

### 依赖说明

项目依赖以下几个 Jar，全部放在 `lib/` 目录：

- `MinecraftAuth` — 微软登录 OAuth
- `lenni0451 httpclient-1.9.0.jar`
- `lenni0451 gson-1.9.2.jar` — **注意与 Google Gson 是两个不同的包**
- `Google gson-2.10.1.jar` — 必须 2.10.1+，旧版缺 `JsonParser.parseString`

> ⚠️ **不要把 `-sources.jar` / `-javadoc.jar` 加进 classpath**，它们没有 `.class` 文件。

---

## 目录结构
