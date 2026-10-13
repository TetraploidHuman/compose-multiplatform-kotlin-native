# compose-kn-resources

Kotlin/Native（**linuxX64** / **mingwX64**）资源子集，消费心智对齐官方 `compose.resources`：

- `import org.jetbrains.compose.resources.Font`（推荐，勿再写 Kotlin 包 `cn.tetraploid.resources`）
- `Res.font.*` / `Font(Res.font.xxx)`
- `Res.string.*` / `stringResource(...)`
- `Res.drawable.*` / `loadDrawableBytes(...)`

> 官方 `components-resources` **没有** linuxX64/mingwX64。`cn.tetraploid` settings 插件会把该坐标顶到本模块。

## 布局

```
src/commonMain/composeResources/
  font/*.ttf|otf
  drawable/*
  values/strings.xml      # <string name="app_name">…</string>
```

## Gradle

```kotlin
plugins {
    id("cn.tetraploid.app") // 已含 resources；也可单独 id("cn.tetraploid.resources")
}

composeKnResources {
    // 官方同义：packageOfResClass；也可用 packageName.set("…")
    packageOfResClass("com.example.app.generated.resources")
    extraFontDirs.add("/path/to/other/font/dir")
}
```

未设置包名时默认 `{group}.{module}.generated.resources`。

链接后 copy 到 `<exeDir>/composeResources/{font,drawable,values}/`。

## 用法

```kotlin
import org.jetbrains.compose.resources.Font
import com.example.app.generated.resources.Res

Font(Res.font.noto_sans_regular)
stringResource(Res.string.app_name)
loadDrawableBytes(Res.drawable.logo)
```

## 坐标

`cn.tetraploid:compose-kn-resources`（见 [CONSUMING.md](../CONSUMING.md)）
