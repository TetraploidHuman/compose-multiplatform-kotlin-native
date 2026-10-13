package cn.tetraploid.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * 实现包侧入口；消费方请用 `org.jetbrains.compose.resources.painterResource`
 *（门面含真实 `@Composable` 体，避免 K/N 跨包转发断链）。
 */
@Composable
fun painterResource(resource: DrawableResource): Painter =
    org.jetbrains.compose.resources.painterResource(resource)
