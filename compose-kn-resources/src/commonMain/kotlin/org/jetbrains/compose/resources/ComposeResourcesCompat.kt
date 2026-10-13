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
import cn.tetraploid.resources.DrawableResource as ComposeKnDrawableResource
import cn.tetraploid.resources.Font as ComposeKnFont
import cn.tetraploid.resources.FontFromFile as ComposeKnFontFromFile
import cn.tetraploid.resources.FontResource as ComposeKnFontResource
import cn.tetraploid.resources.StringResource as ComposeKnStringResource
import cn.tetraploid.resources.decodeToImageVector as composeKnDecodeToImageVector
import cn.tetraploid.resources.drawableResource as composeKnDrawableResource
import cn.tetraploid.resources.fontFamilyOf as composeKnFontFamilyOf
import cn.tetraploid.resources.fontResource as composeKnFontResource
import cn.tetraploid.resources.loadDrawableBytes as composeKnLoadDrawableBytes
import cn.tetraploid.resources.painterResource as composeKnPainterResource
import cn.tetraploid.resources.stringResource as composeKnStringResourceFactory
import cn.tetraploid.resources.vectorResource as composeKnVectorResource

typealias FontResource = ComposeKnFontResource
typealias StringResource = ComposeKnStringResource
typealias DrawableResource = ComposeKnDrawableResource

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

/** 读取字符串（当前 = 生成期 defaultValue）。 */
fun stringResource(resource: StringResource): String =
    composeKnStringResourceFactory(resource)

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
