# compose-multiplatform-kotlin-native TODO

## 命名（已对齐）

| 角色 | 值 |
|------|-----|
| 发布者 | `cn.tetraploid` / `cn.tetraploid.compose`；插件 `cn.tetraploid*` |
| 产品短名 | `composekn`（属性 / 缓存 / 脚本 / `ComposeKN*`） |
| 产品全名 | `compose-multiplatform-kotlin-native` |

## 已完成（近期，v0.5.68）

- [x] 统一插件 + 单版本 BOM；消费方主要锁 `composekn.version`
- [x] Release CDN：`composekn-maven-*.zip` → `~/.composekn/m2/<ver>`
- [x] `runLinuxX64` / `runMingwX64` + [`MIGRATING.md`](MIGRATING.md)
- [x] `compose.resources` 对齐 + [`CAPABILITIES.md`](CAPABILITIES.md) + [`CONSUMING.md`](CONSUMING.md)
- [x] 消费冒烟（0.5.67 基线）
- [x] **发版 0.5.68**：`cn.tetraploid` 坐标重打并上传 [composekn-maven-0.5.68.zip](https://github.com/TetraploidHuman/compose-multiplatform-kotlin-native/releases/tag/v0.5.68)；consumer-smoke OK
- [x] **真消费**：`samples/lumicode` 改为独立 Maven 工程（`cn.tetraploid` / 0.5.68），接 LumiCodeNext `commonMain`

## 待做：相对 JVM Desktop 的 API 补齐（按优先级）

对照基线：官方 Compose Desktop / CMP **1.12.1**（`vendor/compose-core` `desktopMain`）。
权威能力表见 [`CAPABILITIES.md`](CAPABILITIES.md)。

### P0 — commonMain 共享编译 / 迁移摩擦最大

- [x] **`Window` / `DialogWindow` 缺参对齐**：`icon` / `transparent` / `enabled` / `focusable` / `onPreviewKeyEvent` / `onKeyEvent`（签名齐；icon/transparent/focusable 宿主多为记账，enabled 已接线）
- [x] **`singleWindowApplication`** + `SingleWindowApplicationScope`
- [x] **`painterResource`**（位图）；locale / plurals 仍缺 → 见下

### P1 — API 形态不对齐，真项目会踩

- [x] **MenuBar 能力**：`mnemonic` / `shortcut` / `CheckboxItem` / `RadioButtonItem`（Win HMENU + 快捷键；`icon` 忽略；Linux 显示仍 no-op）
- [x] **`WindowDecoration` + resizerThickness**（`undecorated` 布尔兼容保留）
- [x] **窗级快捷键路径**：preview → scene → onKey（Linux / Windows）

### P0/P1 残留（宿主语义，非签名）

- [ ] `Window(icon=…)` → 原生任务栏/标题栏图标（Win `WM_SETICON`；Linux desktop entry）
- [ ] `Window(transparent=true)` 真透明客户区
- [ ] `Window(focusable=…)` 原生焦点策略
- [ ] Linux MenuBar 可见实现（或文档明确仅 Win）
- [ ] `compose.resources` locale / plurals / XML vector

### P2 — 平台行为诚实缺口（文档已知）

- [ ] Linux **`alwaysOnTop`**：现记账；可选 layer-shell / 厂商扩展
- [ ] Linux **`WindowPosition.Absolute`**：协议无绝对坐标（基本不可移植）
- [ ] Linux IME **Delete surrounding**（text-input-v3）
- [ ] Windows README Features / Known gaps 矛盾（自定义装饰拖出）——先核实测再改文档或代码

### P3 — 明确不做或远期

- [ ] ~~Swing / AWT 互操作~~（`SwingPanel` / `AwtWindow` / `LocalAwtWindow` / `ComposeWindow`）— 非 JVM
- [ ] `androidx.compose.ui.window.v2.*` — 等 v1 签名对齐后再跟
- [ ] macOS K/N 宿主 — 新产品线
- [ ] `InteropView` 真原生嵌入 — 现 `Any`

入口文档见 [README.md](README.md)。
