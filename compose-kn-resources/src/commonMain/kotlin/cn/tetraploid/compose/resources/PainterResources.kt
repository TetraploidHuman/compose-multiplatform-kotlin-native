package cn.tetraploid.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

/**
 * 从 [DrawableResource] 加载 [Painter]（对齐官方 `painterResource`）。
 *
 * - `.xml` → Android VectorDrawable → [rememberVectorPainter]
 * - 其它 → Skia 位图解码（png/jpeg/webp/bmp…）
 */
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
