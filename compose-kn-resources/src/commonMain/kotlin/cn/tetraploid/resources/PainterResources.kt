package cn.tetraploid.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter

/**
 * 从 [DrawableResource] 加载 [Painter]（对齐官方 `painterResource` 心智）。
 *
 * 当前支持 Skia 可解码的位图（png/jpeg/webp/bmp 等）。矢量 XML 未实现。
 */
@Composable
fun painterResource(resource: DrawableResource): Painter {
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
