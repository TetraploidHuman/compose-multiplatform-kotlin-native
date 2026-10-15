# LumiCode × ComposeKN（Maven 消费）

把隔壁 [LumiCodeNext](https://github.com/TetraploidHuman/LumiCode) 的 `commonMain` UI
接到 **已发布的** ComposeKN（`cn.tetraploid` / `composekn.version`），验证 Linux（Wayland）
与 Windows（mingwX64）。本目录是**独立 Gradle 工程**（与 `consumer-smoke` 同模式），
不再挂在仓库 composite 里。

## 布局

```
~/compose-multiplatform-kotlin-native/   # 本仓库（提供 gradlew / 脚本）
  samples/lumicode/                      # 本工程
~/LumiCodeNext/                          # sibling
~/.composekn/m2/0.5.71/                  # Release zip 缓存（或 -Pcomposekn.maven.local=…）
```

`build.gradle.kts` 引用：

`../../../LumiCodeNext/composeApp/src/commonMain/kotlin`

平台 actual 在 `src/linuxX64Main` / `mingwX64Main` / `knShared`。

## 首次准备 Maven

```bash
# 推荐：Release zip → 默认缓存
./scripts/fetch-composekn-maven.sh
# 或指向本机刚 publish 的目录：
#   -Pcomposekn.maven.local=$PWD/build/maven-repo
```

## 构建

在本目录用仓库根的 wrapper：

```bash
cd samples/lumicode

# Linux .kexe（NixOS：外层包 nix-shell）
../../gradlew linkReleaseExecutableLinuxX64 --no-daemon

../../scripts/run-linux-native.sh \
  build/bin/linuxX64/releaseExecutable/lumicode.kexe

# 或插件任务
../../gradlew runLinuxX64 --no-daemon
```

Windows（需先 fetch MinGW Skia 预编译包）：

```bash
./scripts/fetch-skia-mingw.sh
./samples/lumicode/build-windows.sh
```

## 状态

- ✅ 消费 `cn.tetraploid` / **0.5.71**（非 monorepo `project()`）
- ✅ linuxX64 / mingwX64 可链
- ✅ 自定义字体（`compose-kn-resources` → `Res.font.*`）
- ✅ `LocalPrefs`：`~/.config/lumicode/settings` / `%APPDATA%/lumicode/settings`
- ⚠️ 工作区 / DSH / PTY：KN 侧暂 no-op（`installPlatformBackends`）
