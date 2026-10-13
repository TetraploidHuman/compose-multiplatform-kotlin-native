@file:Suppress("PackageDirectoryMismatch")

/**
 * 官方 `org.jetbrains.compose.resources` 包名对齐层。
 *
 * 官方 components-resources 无 linuxX64/mingwX64；settings 插件会把
 * `org.jetbrains.compose.components:components-resources` 顶到本模块。
 * 消费方应优先 `import org.jetbrains.compose.resources.*`（与官方 CMP 一致）。
 */
package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import cn.tetraploid.compose.resources.DrawableResource as ComposeKnDrawableResource
import cn.tetraploid.compose.resources.Font as ComposeKnFont
import cn.tetraploid.compose.resources.FontFromFile as ComposeKnFontFromFile
import cn.tetraploid.compose.resources.FontResource as ComposeKnFontResource
import cn.tetraploid.compose.resources.PluralStringResource as ComposeKnPluralStringResource
import cn.tetraploid.compose.resources.ResourceLocale as ComposeKnResourceLocale
import cn.tetraploid.compose.resources.StringResource as ComposeKnStringResource
import cn.tetraploid.compose.resources.currentResourceLocale as composeKnCurrentResourceLocale
import cn.tetraploid.compose.resources.decodeToImageVector as composeKnDecodeToImageVector
import cn.tetraploid.compose.resources.drawableResource as composeKnDrawableResource
import cn.tetraploid.compose.resources.fontFamilyOf as composeKnFontFamilyOf
import cn.tetraploid.compose.resources.fontResource as composeKnFontResource
import cn.tetraploid.compose.resources.loadDrawableBytes as composeKnLoadDrawableBytes
import cn.tetraploid.compose.resources.painterResource as composeKnPainterResource
import cn.tetraploid.compose.resources.pluralStringResource as composeKnPluralStringResourceFactory
import cn.tetraploid.compose.resources.resourceLocaleOverride as composeKnResourceLocaleOverride
import cn.tetraploid.compose.resources.stringResource as composeKnStringResourceFactory
import cn.tetraploid.compose.resources.vectorResource as composeKnVectorResource

typealias FontResource = ComposeKnFontResource
typealias StringResource = ComposeKnStringResource
typealias PluralStringResource = ComposeKnPluralStringResource
typealias DrawableResource = ComposeKnDrawableResource
typealias ResourceLocale = ComposeKnResourceLocale

fun fontResource(
    identity: String,
    relativePath: String,
    developmentPath: String? = null,
): FontResource = composeKnFontResource(identity, relativePath, developmentPath)

fun stringResource(
    identity: String,
    relativePath: String,
    defaultValue: String,
    developmentPath: String? = null,
): StringResource = composeKnStringResourceFactory(identity, relativePath, defaultValue, developmentPath)

/** 读取字符串（locale overlay → defaultValue）。 */
fun stringResource(resource: StringResource): String =
    composeKnStringResourceFactory(resource)

fun pluralStringResource(
    identity: String,
    relativePath: String,
    items: Map<String, String>,
    developmentPath: String? = null,
): PluralStringResource =
    composeKnPluralStringResourceFactory(identity, relativePath, items, developmentPath)

/** 按数量选复数文案（locale overlay → 默认 items）。 */
fun pluralStringResource(resource: PluralStringResource, quantity: Int): String =
    composeKnPluralStringResourceFactory(resource, quantity)

fun currentResourceLocale(): ResourceLocale = composeKnCurrentResourceLocale()

var resourceLocaleOverride: ResourceLocale?
    get() = composeKnResourceLocaleOverride
    set(value) {
        composeKnResourceLocaleOverride = value
    }

fun drawableResource(
    identity: String,
    relativePath: String,
    developmentPath: String? = null,
): DrawableResource = composeKnDrawableResource(identity, relativePath, developmentPath)

fun loadDrawableBytes(resource: DrawableResource): ByteArray? =
    composeKnLoadDrawableBytes(resource)

/** 对齐官方 `painterResource(DrawableResource)`（位图 + Android VectorDrawable `.xml`）。 */
@Composable
fun painterResource(resource: DrawableResource): Painter =
    composeKnPainterResource(resource)

/** 对齐官方 `vectorResource(DrawableResource)`。 */
@Composable
fun vectorResource(resource: DrawableResource): ImageVector =
    composeKnVectorResource(resource)

/** 把 VectorDrawable XML 字节解析为 [ImageVector]。 */
fun ByteArray.decodeToImageVector(density: Density): ImageVector =
    composeKnDecodeToImageVector(density)

fun Font(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): Font = ComposeKnFont(resource, weight, style)

fun Font(
    identity: String,
    bytes: ByteArray,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): Font = ComposeKnFont(identity, bytes, weight, style)

fun FontFromFile(
    path: String,
    identity: String = path.substringAfterLast('/').substringAfterLast('\\')
        .substringBeforeLast('.'),
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): Font = ComposeKnFontFromFile(path, identity, weight, style)

fun fontFamilyOf(vararg fonts: Font) = composeKnFontFamilyOf(*fonts)
