# 消费 compose-multiplatform-kotlin-native

目标：**一个版本号 + `com.tetraploid` + `com.tetraploid.app`**，尽量不再 clone / includeBuild。

只需锁 **`composekn.version`**；UI 自动为 `1.12.1-ckn.<ver>`。

### 版本对照（官方 CMP ↔ 本产品）

| 角色 | 官方 / 上游 | 0.5.68 |
|------|-------------|--------|
| 产品版本 | — | **0.5.68**（宿主 / Skiko / 插件 / BOM） |
| Kotlin | 2.4.0（与本仓库对齐） | **2.4.0** |
| Compose UI 源码基线 | Multiplatform **1.12.1** | 发布坐标 **`1.12.1-ckn.0.5.68`** |
| Compose Gradle 插件 / `compose_deps` | 1.11.1 | 同左（仅未顶掉的 runtime 等） |
| `compose.resources` | `org.jetbrains.compose.resources` + `components-resources` | **同包名**；K/N 桌面由 `compose-kn-resources` 提供（settings 自动顶替） |

发新版时改 `gradle.properties` 的 `composekn.version`，并同步本表一行。

### 仓库怎么拿（按优先级）

1. **Release Maven zip（推荐，稳定 CDN）**  
   发布资产：`composekn-maven-<ver>.zip`  
   - 自动：settings 插件默认拉到 `~/.composekn/m2/<ver>/`（`composekn.maven.auto=true`）  
   - 手动：`./scripts/fetch-composekn-maven.sh` → 同上缓存目录  

2. **显式本地目录**：`composekn.maven.local=/path/to/m2`

3. **GitHub Packages**（需 Billing；**显式打开**）  
   `composekn.settings.useGithubPackages=true` + `gpr.token`

4. **本机 publish**：`./scripts/publish-composekn-packages.sh` → `build/maven-repo`

5. **Maven Central**（维护者可选）：`-Pcomposekn.publish.central=true` + Central 凭证与 signing；消费方 Central 就绪后可去掉 zip/缓存步骤。

---

## A. 推荐：单版本 + 统一插件

### 1. `gradle.properties`

```properties
composekn.version=0.5.68
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
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.tetraploid")) useVersion(ver)
        }
    }
}

plugins {
    id("com.tetraploid") // = settings：坐标替换 + Release 缓存
}
```

首次若缓存为空，先跑一次：

```bash
./scripts/fetch-composekn-maven.sh   # → ~/.composekn/m2/0.5.68
```

（之后 `com.tetraploid` 也会尝试自动补全同一目录，供依赖解析使用。）

### 3. 模块

```kotlin
plugins {
    id("com.tetraploid.app") // = host + resources；会带上 kotlin-gradle-plugin
    kotlin("multiplatform") // 不要写 version（已由 app 插件 classpath 提供 2.4.0）
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
}

kotlin {
    linuxX64 { binaries.executable { entryPoint = "main.main" } }
    mingwX64 { binaries.executable { entryPoint = "main.main" } }
}
```

`main` **不必**再写 `registerComposeKn*Backend()` / `init*MainThread()`。

运行：

```bash
./gradlew :app:runLinuxX64
./gradlew :app:runMingwX64
```

从已有 CMP 工程迁移见 [`MIGRATING.md`](MIGRATING.md)；能力差异见 [`CAPABILITIES.md`](CAPABILITIES.md)。

可选 BOM（约束宿主 + UI 发布坐标）：

```kotlin
dependencies {
    implementation(platform("com.tetraploid:compose-kn-bom:0.5.68"))
}
```

旧 ID 仍可用：`com.tetraploid.settings` / `host` / `resources`。

---

## B. 开发期：clone + includeBuild

与根 `settings.gradle.kts` 相同：顶掉 skiko + compose-core 源码。  
此时加 `-Pcomposekn.settings.skipSubstitution=true`（若仍 apply settings 插件）。

---

## 坐标一览

| 坐标 | 内容 |
|------|------|
| `com.tetraploid:compose-kn-bom` | 约束 linux / windows / resources / skiko / UI |
| `com.tetraploid:compose-kn-linux` | Wayland 宿主 |
| `com.tetraploid:compose-kn-windows` | Win32 宿主 |
| `com.tetraploid:compose-kn-resources` | Res.font / string / drawable |
| `com.tetraploid:skiko` | KN Skiko |
| `com.tetraploid:composekn-catalog` | version catalog |
| `com.tetraploid` / `com.tetraploid.app` | 推荐插件入口 |
| `com.tetraploid.compose:ui` 等 | UI **1.12.1-ckn.*** |

---

## 发布（维护者）

```bash
GITHUB_TOKEN=… GITHUB_ACTOR=… \
  ./scripts/publish-composekn-packages.sh --github --skiko --compose-ui --plugins --release

# 消费冒烟
./scripts/smoke-composekn-consumer.sh

# 可选：Maven Central（需账号 + GPG signing）
# -Pcomposekn.publish.central=true \
# -Pcomposekn.publish.central.user=… \
# -Pcomposekn.publish.central.password=…
```

CI：tag `v*` → publish workflow（含 merge 断言 + consumer-smoke）。
