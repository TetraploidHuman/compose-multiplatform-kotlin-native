# compose-multiplatform-kotlin-native

**ComposeKN**：在 Kotlin/Native（`linuxX64` / `mingwX64`）上跑 Compose Multiplatform——Linux 走 Wayland，Windows 走 Win32。

| 角色 | 标识 |
|------|------|
| 产品全名 | `compose-multiplatform-kotlin-native` |
| 产品短名 | `composekn` / ComposeKN |
| 发布者 | [`cn.tetraploid`](https://github.com/TetraploidHuman/compose-multiplatform-kotlin-native) |

当前版本见 `gradle.properties` 的 `composekn.version`。

## 文档

| 文档 | 内容 |
|------|------|
| [CONSUMING.md](CONSUMING.md) | 怎么当依赖用（推荐入口） |
| [MIGRATING.md](MIGRATING.md) | 从现有 CMP 工程接入 |
| [CAPABILITIES.md](CAPABILITIES.md) | 相对官方 Desktop 的能力矩阵 |
| [RUNNING.md](RUNNING.md) | NixOS / Wayland 运行注意 |
| [SYNCING.md](SYNCING.md) | vendoring 与上游同步 |
| [TODO.md](TODO.md) | 近期待办 |

模块说明：[`compose-kn-linux`](compose-kn-linux/README.md)、[`compose-kn-windows`](compose-kn-windows/README.md)、[`compose-kn-resources`](compose-kn-resources/README.md)。

历史会话交接日志已归档到 [`docs/archive/HANDOVER.md`](docs/archive/HANDOVER.md)（过时坐标勿直接照抄）。

## 快速消费

```properties
# gradle.properties
composekn.version=0.5.72
```

```kotlin
// settings.gradle.kts
plugins { id("cn.tetraploid") }

// 模块
plugins { id("cn.tetraploid.app") }
```

详情与坐标表见 [CONSUMING.md](CONSUMING.md)。

## 本仓库开发

```bash
nix-shell ./shell.nix --run './scripts/link-wayland-demo.sh'
./scripts/run-linux-native.sh
```
