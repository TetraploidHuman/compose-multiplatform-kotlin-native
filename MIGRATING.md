# 从现有 Compose Multiplatform 工程接入 compose-multiplatform-kotlin-native

把 **linuxX64 / mingwX64** 桌面宿主接到已有 CMP 工程，同时保留官方 `jvm("desktop")` / Android / iOS。

基线：**0.5.69**（详见 `CONSUMING.md`）。

## 最小 diff

### 1. `gradle.properties`

```properties
composekn.version=0.5.69
```

### 2. `settings.gradle.kts`

```kotlin
pluginManagement {
    val ver = providers.gradleProperty("composekn.version").get()
    val cache = file("${System.getProperty("user.home")}/.composekn/m2/$ver")
    repositories {
        if (cache.isDirectory) maven { url = uri(cache) }
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("cn.tetraploid")) useVersion(ver)
        }
    }
}

plugins {
    // 已有插件保留
    id("cn.tetraploid") // 坐标替换 + Release 缓存
}
```

首次：

```bash
# 若尚未有 ~/.composekn/m2/<ver>
./scripts/fetch-composekn-maven.sh
```

### 3. 应用模块 `build.gradle.kts`

```kotlin
plugins {
    id("cn.tetraploid.app") // host + resources；带上 kotlin-gradle-plugin
    kotlin("multiplatform") // 不要写 version（与 app 插件 classpath 冲突）
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    // 保留原有 target：androidTarget() / ios* / jvm("desktop") / wasmJs …
    linuxX64 {
        binaries.executable { entryPoint = "main.main" } // 或你的 common/native main
    }
    mingwX64 {
        binaries.executable { entryPoint = "main.main" }
    }

    sourceSets {
        // commonMain 继续用 compose.runtime / foundation / material3 / ui
        // 无需改成 cn.tetraploid.compose 坐标（settings 会顶掉）
    }
}
```

### 4. 入口

- `commonMain` / 平台 `main` 可继续写 Desktop 风格：

```kotlin
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "App") {
        App()
    }
}
```

- **不必**手写 `registerComposeKn*Backend()`（`cn.tetraploid.app` / host 已包一层）。

### 5. 运行

```bash
./gradlew :yourApp:runLinuxX64
# Windows 主机或已交叉出的 exe：
./gradlew :yourApp:runMingwX64
```

Linux（尤其 NixOS）若直接跑 `.kexe` 缺库，优先用仓库内 `scripts/run-linux-native.sh`（`runLinuxX64` 会自动调用）。

## 与官方 Desktop 并存

| Target | 谁提供 | 说明 |
|--------|--------|------|
| `jvm("desktop")` | 官方 Compose Desktop | AWT/Skiko JVM，不变 |
| `linuxX64` / `mingwX64` | 本产品 | K/N + Wayland / Win32 |
| Android / iOS / WASM | 官方 | settings 只在 native desktop configuration 上顶平台坐标 |

建议：`commonMain` 放 UI；`desktopMain` 仍走官方；`linuxX64Main` / `mingwX64Main` 只放平台 actual（文件、prefs 等）。参考 `samples/lumicode`。

## 迁移检查清单

- [ ] 只增加 `composekn.version`，未混用「宿主 0.5.x + 手写错误 UI 坐标」
- [ ] `pluginManagement` 能解析到 `cn.tetraploid` 插件 JAR（缓存 / local / Packages）
- [ ] 已 apply `cn.tetraploid` + `cn.tetraploid.app`
- [ ] 声明了 `linuxX64` 和/或 `mingwX64` executable
- [ ] `commonMain` 依赖仍写官方 `compose.*` 名
- [ ] Linux 有 Wayland 会话；需要时用 `runLinuxX64`
- [ ] 资源：布局仍用 `composeResources/`；代码 `import org.jetbrains.compose.resources.Font`（settings 已顶 `components-resources` → `compose-kn-resources`）
- [ ] 可选：`composeKnResources { packageOfResClass("….generated.resources") }`（与官方 `packageOfResClass` 同义）

能力差异见 `CAPABILITIES.md`。

## JVM Desktop → K/N 迁移陷阱（行为差异）

签名对齐不等于行为一致。下列项在 `commonMain` 里能编译，但 **linuxX64 / mingwX64 语义与 JVM Desktop 不同**：

| API / 能力 | JVM Desktop | linuxX64 (Wayland) | mingwX64 (Win32) |
|------------|-------------|--------------------|------------------|
| `WindowPosition.Absolute` | 屏幕坐标生效 | **no-op**（协议无绝对坐标） | ✅ |
| `alwaysOnTop` | ✅ | **记账**（无通用协议；可选 layer-shell） | ✅ |
| `Window(icon=…)` | 任务栏/标题栏图标 | **记账 + 一次日志**（无便携协议） | ✅ `WM_SETICON` |
| `focusable=false` | 不抢焦点 | 丢 Key/IME；**挡不住** compositor 焦点 | ✅ `WS_EX_NOACTIVATE` |
| `MenuBar` `icon` | 可显示 | **忽略** | **忽略** |
| MenuBar 形态 | Swing / 原生 | Compose 顶栏 | 原生 HMENU |
| `plurals` / locale | 完整 CLDR | **one/other + `values-*`** | 同左 |
| Swing / `AwtWindow` | ✅ | ❌ 非 JVM | ❌ |

更细矩阵：[`CAPABILITIES.md`](CAPABILITIES.md)；Windows 行为与 Wine 限制：[`compose-kn-windows/README.md`](compose-kn-windows/README.md)。
