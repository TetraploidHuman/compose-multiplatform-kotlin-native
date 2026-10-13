@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import cn.tetraploid.compose.resources.DrawableResource
import cn.tetraploid.compose.resources.loadDrawableBytes
import org.jetbrains.compose.resources.vector.decodeToImageVector as vectorBytesToImageVector

/**
 * 门面侧真实实现（勿再薄转发到另一包的 `@Composable`：K/N IR 会断链）。
 */

/** 对齐官方 `painterResource(DrawableResource)`（位图 + Android VectorDrawable `.xml`）。 */
@Composable
fun painterResource(resource: DrawableResource): Painter {
    val path = resource.relativePath.lowercase()
    if (path.endsWith(".xml")) {
        val image = vectorResource(resource)
        return rememberVectorPainter(image)
    }
    val image = remember(resource.identity, resource.relativePath, resource.developmentPath) {
        val bytes = loadDrawableBytes(resource)
            ?: error(
                "Drawable not found: ${resource.identity} " +
                    "(${resource.relativePath}; dev=${resource.developmentPath})",
            )
        bytes.decodeToImageBitmap()
    }
    return remember(image) { BitmapPainter(image) }
}

/** 对齐官方 `vectorResource(DrawableResource)`。 */
@Composable
fun vectorResource(resource: DrawableResource): ImageVector {
    val density = LocalDensity.current
    return remember(resource.identity, resource.relativePath, resource.developmentPath, density) {
        val bytes = loadDrawableBytes(resource)
            ?: error(
                "Drawable not found: ${resource.identity} " +
                    "(${resource.relativePath}; dev=${resource.developmentPath})",
            )
        bytes.vectorBytesToImageVector(density)
    }
}

/** 把 VectorDrawable XML 字节解析为 [ImageVector]。 */
fun ByteArray.decodeToImageVector(density: Density): ImageVector =
    vectorBytesToImageVector(density)
