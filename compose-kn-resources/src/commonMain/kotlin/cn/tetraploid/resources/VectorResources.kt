package cn.tetraploid.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.vector.decodeToImageVector as vectorBytesToImageVector

/**
 * 把 Android VectorDrawable XML 字节解析为 [ImageVector]。
 */
fun ByteArray.decodeToImageVector(density: Density): ImageVector =
    vectorBytesToImageVector(density)

/**
 * 对齐官方 `vectorResource(DrawableResource)`。
 */
@Composable
fun vectorResource(resource: DrawableResource): ImageVector {
    val density = LocalDensity.current
    return remember(resource.identity, resource.relativePath, resource.developmentPath, density) {
        val bytes = loadDrawableBytes(resource)
            ?: error(
                "Drawable not found: ${resource.identity} " +
                    "(${resource.relativePath}; dev=${resource.developmentPath})",
            )
        bytes.decodeToImageVector(density)
    }
}
