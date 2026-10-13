# Tetraploid 能力矩阵（相对官方 Compose Desktop）

基线宿主 **0.5.68**；UI 源码 **1.12.1**（发布 `1.12.1-tp.*`）。  
状态：✅ 对齐 / ⚠️ 部分或平台差异 / ❌ 未做或诚实 no-op。

| 能力 | linuxX64 (Wayland) | mingwX64 (Win32) | 备注 |
|------|--------------------|------------------|------|
| `application` / `Window` | ✅ | ✅ | Desktop 风格 API，`ComposeNativeWindowBackend` |
| 多窗口 | ✅ | ✅ | Linux 共享 `wl_display` |
| 指针 / 键盘 / 滚轮 | ✅ | ✅ | |
| 触摸 | ✅ | ✅ | |
| IME | ✅ | ✅ | Wayland text-input-v3；Win32 IME |
| 剪贴板 | ✅ | ✅ | 多 MIME（text/html/rtf/image/…） |
| DnD | ✅ | ✅ | |
| 文件对话框 | ✅ | ✅ | Linux：xdg-desktop-portal |
| 系统托盘 | ✅ | ✅ | Linux：SNI；无 SNI 时通知兜底 |
| 窗口最小化 / 最大化 / 关闭 | ✅ | ✅ | |
| 全屏 | ✅ | ✅ | |
| `setResizable` | ✅ | ✅ | |
| CSD / `WindowDraggableArea` | ✅ | ✅ | |
| `WindowPosition.Absolute` | ❌ no-op | ✅ | Wayland 标准协议无绝对坐标 |
| `alwaysOnTop` | ⚠️ 记账 | ✅ | Wayland 无通用协议 |
| `Aligned` / parent transient | ✅ | ✅ | |
| 自定义字体 `Res.font` | ✅ | ✅ | `org.jetbrains.compose.resources.Font` + `compose-kn-resources` |
| 官方 `compose.resources` 心智 | ⚠️ 子集 | ⚠️ 子集 | 同包名 / `Res.*`；无 locale/plurals/painter 全套 |
| JVM Desktop `swing` 互操作 | ❌ | ❌ | 非 JVM |
| macOS K/N 宿主 | ❌ | — | 未提供 |
| iOS / Android / WASM | — | — | 用官方 target，非本仓库 |

更细的平台行为见 `compose-kn-linux/README.md`、`compose-kn-windows/README.md`。
