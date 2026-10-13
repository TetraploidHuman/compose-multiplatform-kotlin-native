@file:Suppress("PackageDirectoryMismatch")

/**
 * 官方 `org.jetbrains.compose.resources` 包名对齐层。
 *
 * 官方 components-resources 无 linuxX64/mingwX64；settings 插件会把
 * `org.jetbrains.compose.components:components-resources` 顶到本模块。
 * 消费方应优先 `import org.jetbrains.compose.resources.*`（与官方 CMP 一致）。
 */
package org.jetbrains.compose.resources

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.composekn.resources.DrawableResource as ComposeKnDrawableResource
import com.composekn.resources.Font as ComposeKnFont
import com.composekn.resources.FontFromFile as ComposeKnFontFromFile
import com.composekn.resources.FontResource as ComposeKnFontResource
import com.composekn.resources.StringResource as ComposeKnStringResource
import com.composekn.resources.drawableResource as composeKnDrawableResource
import com.composekn.resources.fontFamilyOf as composeKnFontFamilyOf
import com.composekn.resources.fontResource as composeKnFontResource
import com.composekn.resources.loadDrawableBytes as composeKnLoadDrawableBytes
import com.composekn.resources.stringResource as composeKnStringResourceFactory

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
