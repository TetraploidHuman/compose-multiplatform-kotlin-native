package org.jetbrains.compose.resources

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asSkiaColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Data
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.svg.SVGPreserveAspectRatio
import org.jetbrains.skia.svg.SVGPreserveAspectRatioAlign

/**
 * Decode SVG bytes into a [Painter] (Desktop `decodeToSvgPainter` / `loadSvgPainter` 对齐；无 AWT)。
 */
fun ByteArray.decodeToSvgPainter(density: Density): Painter {
    val data = Data.makeFromBytes(this)
    return SvgPainter(SVGDOM(data), density)
}

private class SvgPainter(
    private val dom: SVGDOM,
    private val density: Density,
) : Painter() {
    private val root = dom.root

    private val defaultSizePx: Size = run {
        val width = root?.width?.withUnit(SVGLengthUnit.PX)?.value ?: 0f
        val height = root?.height?.withUnit(SVGLengthUnit.PX)?.value ?: 0f
        if (width == 0f && height == 0f) {
            Size.Unspecified
        } else {
            Size(width, height)
        }
    }

    init {
        if (root?.viewBox == null && defaultSizePx.isSpecified) {
            root?.viewBox = Rect.makeXYWH(0f, 0f, defaultSizePx.width, defaultSizePx.height)
        }
    }

    override val intrinsicSize: Size
        get() = if (defaultSizePx.isSpecified) {
            defaultSizePx * density.density
        } else {
            Size.Unspecified
        }

    private var alpha: Float = 1.0f
    private var colorFilter: ColorFilter? = null

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            val skia = canvas.skiaCanvas
            val saveCount = skia.save()
            val paint = Paint()
            try {
                paint.setAlphaf(alpha)
                paint.colorFilter = colorFilter?.asSkiaColorFilter()
                skia.saveLayer(null, paint)
                root?.width = SVGLength(size.width, SVGLengthUnit.PX)
                root?.height = SVGLength(size.height, SVGLengthUnit.PX)
                root?.preserveAspectRatio = SVGPreserveAspectRatio(SVGPreserveAspectRatioAlign.NONE)
                dom.render(skia)
            } finally {
                paint.close()
                skia.restoreToCount(saveCount)
            }
        }
    }
}
