@file:OptIn(
    androidx.compose.ui.InternalComposeUiApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package cn.tetraploid.windows

import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformDragAndDropSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import org.jetbrains.skiko.Win32Message
import org.jetbrains.skiko.Win32Window
import org.jetbrains.skiko.win32Log

/**
 * Windows OLE 拖放**发出侧**，对齐上游 `AwtDragAndDropManager`：
 *
 * 1. `isRequestDragAndDropTransferRequired = true` —— foundation 的
 *    `Modifier.dragAndDropSource` 才会挂启动手势；
 * 2. `requestDragAndDropTransfer` → 源节点的 `startDragAndDropTransfer` →
 *    排入 [pendingOutgoing]；[flushPendingOutgoingDrags] 在指针派发结束后再
 *    `DoDragDrop`（避免在 Compose `handleEvent` 栈里跑 OLE 嵌套泵，IME 残留
 *    Escape / 空 keyState 会立刻 CANCEL）；
 * 3. [drawDragDecoration] 光栅化为 BGRA，经 `IDragSourceHelper::InitializeFromBitmap`
 *    作为自定义拖影（热点取装饰中心）。
 */
internal class WindowsDragAndDropManager(
    private val windowProvider: () -> Win32Window?,
) : PlatformDragAndDropManager {

    override val isRequestDragAndDropTransferRequired: Boolean
        get() = true

    override fun requestDragAndDropTransfer(source: PlatformDragAndDropSource, offset: Offset) {
        var isTransferStarted = false
        val startTransferScope =
            object : PlatformDragAndDropSource.StartTransferScope {
                override fun startDragAndDropTransfer(
                    transferData: DragAndDropTransferData,
                    decorationSize: Size,
                    drawDragDecoration: DrawScope.() -> Unit,
                ): Boolean {
                    isTransferStarted = startOutgoingDrag(
                        transferData = transferData,
                        decorationSize = decorationSize,
                        drawDragDecoration = drawDragDecoration,
                    )
                    return isTransferStarted
                }
            }
        with(source) {
            startTransferScope.startDragAndDropTransfer(offset) { isTransferStarted }
        }
    }

    private fun startOutgoingDrag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit,
    ): Boolean {
        val files = transferData.files.takeIf { it.isNotEmpty() }
        val text = transferData.text?.takeIf { it.isNotEmpty() }
        if (files == null && text == null) {
            win32Log("drag: Source 没有 files/text，跳过 DoDragDrop")
            transferData.onTransferCompleted?.invoke(false)
            return false
        }
        val window = windowProvider()
        if (window == null) {
            win32Log("drag: 窗口尚未就绪，无法 DoDragDrop")
            transferData.onTransferCompleted?.invoke(false)
            return false
        }

        val (iconW, iconH, iconBgra, hotX, hotY) = renderDragDecoration(
            decorationSize = decorationSize,
            drawDragDecoration = drawDragDecoration,
        )

        // 不要在 pointer 派发栈里同步 DoDragDrop：嵌套 OLE 泵会立刻吃到队列里
        // IME 收起注入的 Escape / 空 keyState。排到 drainEvents 之后再跑。
        pendingOutgoing.add(
            PendingOutgoingDrag(
                window = window,
                files = files,
                text = text,
                iconW = iconW,
                iconH = iconH,
                iconBgra = iconBgra,
                hotX = hotX,
                hotY = hotY,
                onCompleted = transferData.onTransferCompleted,
            ),
        )
        win32Log(
            "drag: Source 已排队，待 drain 后 DoDragDrop" +
                if (iconBgra != null) " (icon ${iconW}x${iconH} hot=($hotX,$hotY))" else "",
        )
        window.wake()
        return true
    }

    /**
     * 对齐 [androidx.compose.ui.platform.AwtDragAndDropManager] / Linux
     * `Painter.toTrayIconBgra`：画进 [ImageBitmap]，再导出自上而下 BGRA。
     * 热点取装饰中心（相对左上角）。
     */
    private fun renderDragDecoration(
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit,
    ): DragIcon {
        val w = decorationSize.width.roundToInt().coerceAtLeast(1)
        val h = decorationSize.height.roundToInt().coerceAtLeast(1)
        val imageBitmap = ImageBitmap(w, h)
        val canvas = Canvas(imageBitmap)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(w.toFloat(), h.toFloat()),
            block = drawDragDecoration,
        )
        val argb = IntArray(w * h)
        imageBitmap.readPixels(argb)
        val bgra = ByteArray(w * h * 4)
        for (i in argb.indices) {
            val color = argb[i]
            bgra[i * 4 + 0] = (color and 0xFF).toByte()
            bgra[i * 4 + 1] = ((color shr 8) and 0xFF).toByte()
            bgra[i * 4 + 2] = ((color shr 16) and 0xFF).toByte()
            bgra[i * 4 + 3] = ((color shr 24) and 0xFF).toByte()
        }
        return DragIcon(
            width = w,
            height = h,
            bgra = bgra,
            hotX = w / 2,
            hotY = h / 2,
        )
    }

    private data class DragIcon(
        val width: Int,
        val height: Int,
        val bgra: ByteArray?,
        val hotX: Int,
        val hotY: Int,
    )

    private data class PendingOutgoingDrag(
        val window: Win32Window,
        val files: List<String>?,
        val text: String?,
        val iconW: Int,
        val iconH: Int,
        val iconBgra: ByteArray?,
        val hotX: Int,
        val hotY: Int,
        val onCompleted: ((Boolean) -> Unit)?,
    )

    companion object {
        private val pendingOutgoing = mutableListOf<PendingOutgoingDrag>()

        /**
         * 在共享泵 / 独占泵「本轮事件派发结束」后调用：真正进入 `DoDragDrop`。
         */
        fun flushPendingOutgoingDrags() {
            if (pendingOutgoing.isEmpty()) return
            val batch = pendingOutgoing.toList()
            pendingOutgoing.clear()
            for (pending in batch) {
                val effect = pending.window.doDragDrop(
                    files = pending.files,
                    text = pending.text,
                    allowedEffects = Win32Message.DROPEFFECT_COPY,
                    iconW = pending.iconW,
                    iconH = pending.iconH,
                    iconBgra = pending.iconBgra,
                    hotX = pending.hotX,
                    hotY = pending.hotY,
                )
                val success = effect > 0
                win32Log("drag: Source DoDragDrop effect=$effect success=$success")
                pending.onCompleted?.invoke(success)
            }
        }
    }
}
