package cn.tetraploid.compose.resources

/**
 * 字符串资源句柄（对齐官方 `StringResource` 形态）。
 * 默认文案来自 `composeResources/values/strings.xml`；运行时按 locale 覆盖。
 */
class StringResource internal constructor(
    val identity: String,
    val relativePath: String,
    val developmentPath: String? = null,
    /** 生成期默认文案（`values/`）；运行时优先 [StringCatalog] overlay。 */
    val defaultValue: String,
) {
    override fun toString(): String = "StringResource($identity)"
}

fun stringResource(
    identity: String,
    relativePath: String,
    defaultValue: String,
    developmentPath: String? = null,
): StringResource = StringResource(identity, relativePath, developmentPath, defaultValue)

/**
 * 读取字符串：`values-{lang}-r{REGION}` → `values-{lang}` → [StringResource.defaultValue]。
 */
fun stringResource(resource: StringResource): String =
    StringCatalog.string(resource.identity, currentResourceLocale())
        ?: resource.defaultValue

/**
 * 复数资源句柄（对齐官方 `PluralStringResource`）。
 * [items] 的 key 为 quantity：`zero` / `one` / `two` / `few` / `many` / `other`。
 */
class PluralStringResource internal constructor(
    val identity: String,
    val relativePath: String,
    val developmentPath: String? = null,
    val items: Map<String, String>,
) {
    override fun toString(): String = "PluralStringResource($identity)"
}

fun pluralStringResource(
    identity: String,
    relativePath: String,
    items: Map<String, String>,
    developmentPath: String? = null,
): PluralStringResource =
    PluralStringResource(identity, relativePath, developmentPath, items)

/**
 * 按 [quantity] 选复数文案，并把 `%d` / `%s` 替换为数量。
 */
fun pluralStringResource(resource: PluralStringResource, quantity: Int): String {
    val locale = currentResourceLocale()
    val items = StringCatalog.plurals(resource.identity, locale) ?: resource.items
    val key = selectPluralQuantity(locale.language, quantity)
    val template = items[key]
        ?: items["other"]
        ?: items.values.firstOrNull()
        ?: ""
    return formatQuantity(template, quantity)
}

/**
 * CLDR 基数复数（整数）。[language] 是 ISO 639；小数操作数用不到，因为数量是 [Int]。
 * 缺的 quantity 由调用方回退到 `other`。
 */
internal fun selectPluralQuantity(language: String, count: Int): String =
    selectCardinalPlural(language, count)

internal fun formatQuantity(template: String, quantity: Int): String {
    val q = quantity.toString()
    return template
        .replace("%d", q)
        .replace("%s", q)
        .replace("%1\$d", q)
        .replace("%1\$s", q)
}

/**
 * Drawable / 图片资源句柄（对齐官方 `DrawableResource` 形态）。
 * 文件在 `composeResources/drawable/`；运行时查找同 [FontResource]。
 */
class DrawableResource internal constructor(
    val identity: String,
    val relativePath: String,
    val developmentPath: String? = null,
) {
    override fun toString(): String = "DrawableResource($identity → $relativePath)"
}

fun drawableResource(
    identity: String,
    relativePath: String,
    developmentPath: String? = null,
): DrawableResource = DrawableResource(identity, relativePath, developmentPath)

/** 读 drawable 字节（png/webp/svg/xml 等）；查找顺序同字体。 */
fun loadDrawableBytes(resource: DrawableResource): ByteArray? =
    ResourceLoads.loadBytes(
        FontResource(resource.identity, resource.relativePath, resource.developmentPath),
    )
