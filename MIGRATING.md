# 从现有 Compose Multiplatform 工程接入 ComposeKN

把 **linuxX64 / mingwX64** 桌面宿主接到已有 CMP 工程，同时保留官方 `jvm("desktop")` / Android / iOS。

基线：**0.5.67**（详见 `CONSUMING.md`）。

## 最小 diff

### 1. `gradle.properties`

```properties
composekn.version=0.5.67
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
            if (requested.id.id.startsWith("com.composekn")) useVersion(ver)
        }
    }
}

plugins {
    // 已有插件保留
    id("com.composekn") // 坐标替换 + Release 缓存
}
```

首次：

```bash
# 若尚未有 ~/.composekn/m2/0.5.67
curl -fsSL … 或使用 ComposeKN 仓库的:
./scripts/fetch-composekn-maven.sh
```

### 3. 应用模块 `build.gradle.kts`

```kotlin
plugins {
    id("com.composekn.app") // host + resources；带上 kotlin-gradle-plugin
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
        // 无需改成 com.composekn.compose 坐标（settings 会顶掉）
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

- **不必**手写 `registerComposeKn*Backend()`（`com.composekn.app` / host 已包一层）。

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
| `linuxX64` / `mingwX64` | ComposeKN | K/N + Wayland / Win32 |
| Android / iOS / WASM | 官方 | settings 只在 native desktop configuration 上顶平台坐标 |

建议：`commonMain` 放 UI；`desktopMain` 仍走官方；`linuxX64Main` / `mingwX64Main` 只放平台 actual（文件、prefs 等）。参考 `samples/lumicode`。

## 迁移检查清单

- [ ] 只增加 `composekn.version`，未混用「宿主 0.5.x + 手写错误 UI 坐标」
- [ ] `pluginManagement` 能解析到 `com.composekn` 插件 JAR（缓存 / local / Packages）
- [ ] 已 apply `com.composekn` + `com.composekn.app`
- [ ] 声明了 `linuxX64` 和/或 `mingwX64` executable
- [ ] `commonMain` 依赖仍写官方 `compose.*` 名
- [ ] Linux 有 Wayland 会话；需要时用 `runLinuxX64`
- [ ] 资源：布局仍用 `composeResources/`；代码 `import org.jetbrains.compose.resources.Font`（settings 已顶 `components-resources` → `compose-kn-resources`）
- [ ] 可选：`composeKnResources { packageOfResClass("….generated.resources") }`（与官方 `packageOfResClass` 同义）

能力差异见 `CAPABILITIES.md`。
