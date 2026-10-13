# ComposeKN 能力矩阵（相对官方 Compose Desktop）

产品：`compose-multiplatform-kotlin-native`。基线宿主见 `composekn.version`；UI 源码 **1.12.1**（发布 `1.12.1-ckn.*`）。  

状态：✅ 对齐 / ⚠️ 部分或平台差异 / ❌ 未做或诚实 no-op。

| 能力 | linuxX64 (Wayland) | mingwX64 (Win32) | 备注 |
|------|--------------------|------------------|------|
| `application` / `Window` | ✅ | ✅ | Desktop 风格 API，`ComposeNativeWindowBackend` |
| `singleWindowApplication` | ✅ | ✅ | 对齐 Desktop |
| `Window` 缺参（icon / transparent / enabled / focusable / key / visible） | ⚠️ | ⚠️ | 签名齐；`enabled`+窗级 key+`visible` 已接线（Win 真 hide；Linux 软隐藏）；icon/transparent/focusable 多为记账 |
| `WindowDecoration` | ✅ | ✅ | sealed；`undecorated` 布尔仍兼容 |
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
| MenuBar | ⚠️ no-op | ✅ | Win：Item/Checkbox/Radio + mnemonic/shortcut；Linux 显示仍 no-op |
| 窗口最小化 / 最大化 / 关闭 | ✅ | ✅ | |
| 全屏 | ✅ | ✅ | |
| `setResizable` | ✅ | ✅ | |
| CSD / `WindowDraggableArea` | ✅ | ✅ | |
| `WindowPosition.Absolute` | ❌ no-op | ✅ | Wayland 标准协议无绝对坐标 |
| `alwaysOnTop` | ⚠️ 记账 | ✅ | Wayland 无通用协议 |
| `Aligned` / parent transient | ✅ | ✅ | |
| 自定义字体 `Res.font` | ✅ | ✅ | `org.jetbrains.compose.resources.Font` + `compose-kn-resources` |
| `painterResource` | ✅ | ✅ | 位图（png/jpeg/webp…）；无 XML vector / locale / plurals |
| 官方 `compose.resources` 心智 | ⚠️ 子集 | ⚠️ 子集 | 同包名 / `Res.*`；仍无 locale/plurals |
| JVM Desktop `swing` 互操作 | ❌ | ❌ | 非 JVM |
| macOS K/N 宿主 | ❌ | — | 未提供 |
| iOS / Android / WASM | — | — | 用官方 target，非本仓库 |

更细的平台行为见 `compose-kn-linux/README.md`、`compose-kn-windows/README.md`。
