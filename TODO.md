# compose-multiplatform-kotlin-native TODO

## 命名（已对齐）

| 角色 | 值 |
|------|-----|
| 发布者 | `cn.tetraploid` / `cn.tetraploid.compose`；插件 `cn.tetraploid*` |
| 资源实现包 | `cn.tetraploid.compose.resources`（对外门面仍为 `org.jetbrains.compose.resources`） |
| 产品短名 | `composekn`（属性 / 缓存 / 脚本 / `ComposeKN*`） |
| 产品全名 | `compose-multiplatform-kotlin-native` |

## 已完成（近期，v0.5.70）

- [x] 统一插件 + 单版本 BOM；消费方主要锁 `composekn.version`
- [x] Release CDN：`composekn-maven-*.zip` → `~/.composekn/m2/<ver>`
- [x] `runLinuxX64` / `runMingwX64` + [`MIGRATING.md`](MIGRATING.md)
- [x] `compose.resources` 对齐 + [`CAPABILITIES.md`](CAPABILITIES.md) + [`CONSUMING.md`](CONSUMING.md)
- [x] 消费冒烟（0.5.67 基线）
- [x] **发版 0.5.68**：`cn.tetraploid` 坐标重打并上传 [composekn-maven-0.5.68.zip](https://github.com/TetraploidHuman/compose-multiplatform-kotlin-native/releases/tag/v0.5.68)；consumer-smoke OK
- [x] **真消费**：`samples/lumicode` 改为独立 Maven 工程（`cn.tetraploid` / 0.5.68），接 LumiCodeNext `commonMain`
- [x] Desktop P0/P1 API 对齐 + Linux/Wine selftest；**发版 0.5.69**
- [x] GitHub Packages 补传 0.5.69（`write:packages` PAT）
- [x] `DialogModalityType` / ExceptionHandler / DesktopPopup / `pointerMoveFilter`；**发版 0.5.70**
- [x] `LocalLocalization` + `mouseScrollFilter`（无 AWT）

## 待做：相对 JVM Desktop 的 API 补齐（按优先级）

对照基线：官方 Compose Desktop / CMP **1.12.1**（`vendor/compose-core` `desktopMain`）。
权威能力表见 [`CAPABILITIES.md`](CAPABILITIES.md)。

### P0 — commonMain 共享编译 / 迁移摩擦最大

- [x] **`Window` / `DialogWindow` 缺参对齐**：`icon` / `transparent` / `enabled` / `focusable` / `onPreviewKeyEvent` / `onKeyEvent`（签名齐；Win 宿主已接线；Linux icon 记账、focusable 丢 Key/IME）
- [x] **`singleWindowApplication`** + `SingleWindowApplicationScope`
- [x] **`painterResource`**（位图 + XML vector）；locale / plurals 子集 → 见下

### P1 — API 形态不对齐，真项目会踩

- [x] **MenuBar 能力**：`mnemonic` / `shortcut` / `CheckboxItem` / `RadioButtonItem`（Win HMENU；Linux Compose 顶栏；`icon` 忽略）
- [x] **`WindowDecoration` + resizerThickness**（`undecorated` 布尔兼容保留）
- [x] **窗级快捷键路径**：preview → scene → onKey（Linux / Windows）

### P0/P1 残留（宿主语义，非签名）

- [x] `Window(visible)` 切换（Win ShowWindow；Linux 软隐藏跳过输入/绘制）
- [x] 菜单加速键标签（`Ctrl+S` 而非 `Key keyCode: N`）
- [x] Tray Checkbox/Radio 勾选态（文本前缀 `✓`；无原生 checkmark API）
- [x] `Window(icon=…)` → Win `WM_SETICON`（16+32 BGRA）；Linux 无便携协议（记账 + 一次日志）
- [x] `Window(transparent=true)` 真透明客户区（Win：WS_EX_LAYERED+UpdateLayeredWindow/软件路径；Linux：空 opaque region + clear TRANSPARENT）
- [x] `Window(focusable=…)` 原生焦点策略（Win：WS_EX_NOACTIVATE+MA_NOACTIVATE；Linux：丢 Key/IME + 一次日志）
- [x] Linux MenuBar 可见实现（Compose 顶栏 + shortcut；非 HMENU）
- [x] `compose.resources` XML vector（`painterResource` / `vectorResource`；纯 Kotlin xmldom）
- [x] `compose.resources` locale overlay（`values-*` + `COMPOSEKN_LOCALE`）
- [x] `compose.resources` plurals 子集（`Res.plurals` + one/other；非完整 CLDR）
- [x] Windows `KeyShortcut(meta=true)`（`MOD_META` → `KeyEvent.isMetaPressed`）
- [x] `WindowDecoration.resizerThickness` → Win CSD `edgeMarginDp`；Linux 记账

### 续对齐（签名 / 非 JVM）

- [x] **`DialogModalityType`** + `DialogWindow(..., modalityType)`（Modeless 不软模态；Document/Application 软阻塞其它窗）
- [x] **`WindowExceptionHandlerFactory` / `LocalWindowExceptionHandlerFactory`**（`ComposeNativeWindowHandle`；渲染/事件 catch）
- [x] **DesktopPopup**：`rememberCursorPositionProvider` / `rememberPopupPositionProviderAtPosition` / `rememberComponentRectPositionProvider`
- [x] **`modifier.pointerMoveFilter`**（弃用 API，薄封装）
- [x] **`LocalLocalization` / `PlatformLocalization`** + desktop l10n 表
- [x] **`modifier.mouseScrollFilter`**（弃用；无 AWT，Line-only）

### P2 — 平台行为诚实缺口（文档已知）

- [ ] Linux **`alwaysOnTop`**：现记账；可选 layer-shell / 厂商扩展
- [ ] Linux **`WindowPosition.Absolute`**：协议无绝对坐标（基本不可移植）
- [ ] Linux IME **Delete surrounding**（text-input-v3）
- [x] Windows README Features / Known gaps 矛盾（自定义装饰拖出）——代码已接 `drawDragDecoration`；Known gaps 已改
- [ ] Desktop `ui.res` classpath / SVG — 走 `compose.resources`（可选：Skia SVGDOM）

### P3 — 明确不做或远期

- [ ] ~~Swing / AWT 互操作~~（`SwingPanel` / `AwtWindow` / `LocalAwtWindow` / `ComposeWindow`）— 非 JVM
- [ ] `androidx.compose.ui.window.v2.*` — 等 v1 签名对齐后再跟
- [ ] macOS K/N 宿主 — 新产品线
- [ ] `InteropView` 真原生嵌入 — 现 `Any`

入口文档见 [README.md](README.md)。
