package org.jetbrains.compose.resources.vector

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.vector.xmldom.toXmlElement

/** VectorDrawable XML 字节 → [ImageVector]（posix 纯 Kotlin xmldom）。 */
fun ByteArray.decodeToImageVector(density: Density): ImageVector =
    toXmlElement().toImageVector(density)
