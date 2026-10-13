package cn.tetraploid.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.vector.decodeToImageVector as vectorBytesToImageVector

/**
 * 实现包侧入口；消费方请用 `org.jetbrains.compose.resources.vectorResource`。
 */
@Composable
fun vectorResource(resource: DrawableResource): ImageVector =
    org.jetbrains.compose.resources.vectorResource(resource)

/** 把 Android VectorDrawable XML 字节解析为 [ImageVector]。 */
fun ByteArray.decodeToImageVector(density: Density): ImageVector =
    vectorBytesToImageVector(density)
