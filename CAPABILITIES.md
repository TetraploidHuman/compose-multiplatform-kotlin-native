# ComposeKN 能力矩阵（相对官方 Compose Desktop）

产品：`compose-multiplatform-kotlin-native`。基线宿主见 `composekn.version`；UI 源码 **1.12.1**（发布 `1.12.1-ckn.*`）。  

状态：✅ 对齐 / ⚠️ 部分或平台差异 / ❌ 未做或诚实 no-op。

| 能力 | linuxX64 (Wayland) | mingwX64 (Win32) | 备注 |
|------|--------------------|------------------|------|
| `application` / `Window` | ✅ | ✅ | Desktop 风格 API，`ComposeNativeWindowBackend` |
| `singleWindowApplication` | ✅ | ✅ | 对齐 Desktop |
| `Window` 缺参（icon / transparent / enabled / focusable / key / visible） | ⚠️ | ✅ | 签名齐；Win `icon`/`transparent`/`focusable`/`enabled`/key/`visible` 已接线；Linux `transparent` 已接线，`focusable` 丢 Key/IME（无协议挡焦点），icon 记账 |
| `DialogModalityType` | ⚠️ | ⚠️ | Modeless / DocumentModal / ApplicationModal；无 AWT owner 时 Document≈Application（软模态） |
| `WindowExceptionHandlerFactory` | ✅ | ✅ | handle 类型为 `ComposeNativeWindowHandle`（非 AWT）；默认 stderr + rethrow |
| `rememberCursorPositionProvider` 等 | ✅ | ✅ | DesktopPopup 助手（skiko `LocalComposeScene`） |
| `pointerMoveFilter` | ✅ | ✅ | 已弃用；薄封装 `onPointerEvent` |
| `mouseScrollFilter` | ✅ | ✅ | 已弃用；无 AWT，scrollDelta→Line |
| `LocalLocalization` / `PlatformLocalization` | ✅ | ✅ | 全量 desktop l10n 表；上下文菜单文案 |
| `MenuComposable` | ✅ | ✅ | 目标注解；MenuBar/Tray/Menu 已标 |
| `WindowSize`（弃用） | ✅ | ✅ | → `DpSize`；旧 State 重载保留 |
| `Dialog(...)`（弃用） | ✅ | ✅ | → `DialogWindow`；无 Swing create/dispose |
| `DragData` / `dragData()` / `DragAndDropTransferAction` | ✅ | ✅ | 无 AWT；files/text 映射；无 image 负载 |
| `currentSystemTheme` / `isSystemInDarkTheme` | ✅ | ✅ | Win：`AppsUseLightTheme`；Linux：portal/gsettings；`COMPOSEKN_SYSTEM_THEME` 可覆盖 |
| `ComposeUiFlags.pollSystemTheme` + `ProvideSystemTheme` | ✅ | ✅ | Window/Dialog 自动提供；Linux 默认不轮询（同 Desktop） |
| Desktop `ui.res` classpath（`painterResource(String)` 等） | ❌ | ❌ | 无 ClassLoader；用 `compose.resources` |
| `WindowDecoration` | ✅ | ✅ | sealed；Win CSD `resizerThickness`→edgeMargin；`undecorated` 布尔仍兼容 |
| 多窗口 | ✅ | ✅ | Linux 共享 `wl_display` |
| 指针 / 键盘 / 滚轮 | ✅ | ✅ | |
| 窗级 `onPreviewKeyEvent` / `onKeyEvent` | ✅ | ✅ | preview → scene → onKey（对齐 Desktop） |
| `KeyShortcut` Meta（Win 键） | ✅ | ✅ | Win：`MOD_META`→`isMetaPressed`；Linux：wl Mod4 |
| 触摸 | ✅ | ✅ | |
| IME | ✅ | ✅ | Wayland text-input-v3；Win32 IME |
| 剪贴板 | ✅ | ✅ | 多 MIME（text/html/rtf/image/…） |
| DnD | ✅ | ✅ | |
| 文件对话框 | ✅ | ✅ | Linux：xdg-desktop-portal |
| 系统托盘 | ✅ | ✅ | Linux：SNI；无 SNI 时通知兜底 |
| MenuBar | ⚠️ Compose 顶栏 | ✅ | Win：HMENU；Linux：Compose 顶栏 + shortcut；icon 忽略 |
| 窗口最小化 / 最大化 / 关闭 | ✅ | ✅ | |
| 全屏 | ✅ | ✅ | |
| `setResizable` | ✅ | ✅ | |
| CSD / `WindowDraggableArea` | ✅ | ✅ | |
| `WindowPosition.Absolute` | ❌ no-op | ✅ | Wayland 标准协议无绝对坐标 |
| `alwaysOnTop` | ⚠️ 记账 | ✅ | Wayland 无通用协议 |
| `Aligned` / parent transient | ✅ | ✅ | |
| 自定义字体 `Res.font` | ✅ | ✅ | `org.jetbrains.compose.resources.Font` + `compose-kn-resources` |
| `painterResource` / `vectorResource` | ✅ | ✅ | 位图 + Android VectorDrawable `.xml`；无 SVG |
| `stringResource` locale / `pluralStringResource` | ⚠️ | ⚠️ | `values-*` overlay；plurals 为 one/other 子集（非完整 CLDR） |
| 官方 `compose.resources` 心智 | ⚠️ 子集 | ⚠️ 子集 | 同包名 / `Res.*`；无完整 CLDR / SVG |
| JVM Desktop `swing` 互操作 | ❌ | ❌ | 非 JVM |
| macOS K/N 宿主 | ❌ | — | 未提供 |
| iOS / Android / WASM | — | — | 用官方 target，非本仓库 |

更细的平台行为见 `compose-kn-linux/README.md`、`compose-kn-windows/README.md`。
