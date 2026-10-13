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
import com.tetraploid.resources.DrawableResource as TetraploidDrawableResource
import com.tetraploid.resources.Font as TetraploidFont
import com.tetraploid.resources.FontFromFile as TetraploidFontFromFile
import com.tetraploid.resources.FontResource as TetraploidFontResource
import com.tetraploid.resources.StringResource as TetraploidStringResource
import com.tetraploid.resources.drawableResource as tetraploidDrawableResource
import com.tetraploid.resources.fontFamilyOf as tetraploidFontFamilyOf
import com.tetraploid.resources.fontResource as tetraploidFontResource
import com.tetraploid.resources.loadDrawableBytes as tetraploidLoadDrawableBytes
import com.tetraploid.resources.stringResource as tetraploidStringResourceFactory

typealias FontResource = TetraploidFontResource
typealias StringResource = TetraploidStringResource
typealias DrawableResource = TetraploidDrawableResource

fun fontResource(
    identity: String,
    relativePath: String,
    developmentPath: String? = null,
): FontResource = tetraploidFontResource(identity, relativePath, developmentPath)

fun stringResource(
    identity: String,
    relativePath: String,
    defaultValue: String,
    developmentPath: String? = null,
): StringResource = tetraploidStringResourceFactory(identity, relativePath, defaultValue, developmentPath)

/** 读取字符串（当前 = 生成期 defaultValue）。 */
fun stringResource(resource: StringResource): String =
    tetraploidStringResourceFactory(resource)

fun drawableResource(
    identity: String,
    relativePath: String,
    developmentPath: String? = null,
): DrawableResource = tetraploidDrawableResource(identity, relativePath, developmentPath)

fun loadDrawableBytes(resource: DrawableResource): ByteArray? =
    tetraploidLoadDrawableBytes(resource)

fun Font(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): Font = TetraploidFont(resource, weight, style)

fun Font(
    identity: String,
    bytes: ByteArray,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): Font = TetraploidFont(identity, bytes, weight, style)

fun FontFromFile(
    path: String,
    identity: String = path.substringAfterLast('/').substringAfterLast('\\')
        .substringBeforeLast('.'),
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): Font = TetraploidFontFromFile(path, identity, weight, style)

fun fontFamilyOf(vararg fonts: Font) = tetraploidFontFamilyOf(*fonts)
