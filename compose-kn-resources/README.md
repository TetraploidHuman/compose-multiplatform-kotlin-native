# compose-kn-resources

Kotlin/Native（**linuxX64** / **mingwX64**）资源子集，消费心智对齐官方 `compose.resources`：

- `import org.jetbrains.compose.resources.Font`（推荐；实现包为 `cn.tetraploid.compose.resources`，勿直接依赖）
- `Res.font.*` / `Font(Res.font.xxx)`
- `Res.string.*` / `stringResource(...)`
- `Res.drawable.*` / `loadDrawableBytes(...)` / `painterResource(...)`（位图 + Android VectorDrawable `.xml`）
- `vectorResource(...)` / `ByteArray.decodeToImageVector(density)`

> 官方 `components-resources` **没有** linuxX64/mingwX64。`cn.tetraploid` settings 插件会把该坐标顶到本模块。

## 布局

```
src/commonMain/composeResources/
  font/*.ttf|otf
  drawable/*              # 位图或 Android VectorDrawable .xml
  values/strings.xml      # 默认文案 + 可选 <plurals>
  values-zh/strings.xml   # locale overlay（运行时）
  values-zh-rCN/…
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
stringResource(Res.string.app_name)           // values-{lang}[-rREGION] → values
pluralStringResource(Res.plurals.items, 3)    // 整数 CLDR + %d
loadDrawableBytes(Res.drawable.logo)
painterResource(Res.drawable.logo)            // png/jpeg/webp/bmp…
painterResource(Res.drawable.ic_foo)          // Android VectorDrawable .xml
vectorResource(Res.drawable.ic_foo)

// 强制 locale（测试 / 覆盖系统）：
resourceLocaleOverride = ResourceLocale("zh", "CN")
// 或环境变量 COMPOSEKN_LOCALE=zh-CN
```

Locale 解析：`COMPOSEKN_LOCALE` → `LC_*`/`LANG` → Win `GetUserDefaultLocaleName`。  
复数 quantity：东亚语系一律 `other`；其它语言 `one`/`other`（完整 CLDR 未移植）。

## 坐标

`cn.tetraploid:compose-kn-resources`（见 [CONSUMING.md](../CONSUMING.md)）
