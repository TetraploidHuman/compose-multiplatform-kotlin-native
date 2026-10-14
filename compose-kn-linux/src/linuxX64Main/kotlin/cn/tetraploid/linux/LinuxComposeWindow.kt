package cn.tetraploid.linux

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.matches
import androidx.compose.ui.window.NativeMenuBarModel
import androidx.compose.ui.window.shortcutActions
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.WaylandEvent
import org.jetbrains.skiko.WaylandEventType
import org.jetbrains.skiko.WaylandWindow
import org.jetbrains.skiko.flushMainUIDispatcher
import org.jetbrains.skiko.initLinuxMainThread

/**
 * Wayland window + SkiaLayer with vsync frame callbacks and input events.
 *
 * 支持两种驱动：
 * - [run]：独占 poll 循环（旧 demo / 自检）
 * - [attachToHost] + [LinuxApplicationHost.runSharedPump]：多窗口共享泵
 */
class LinuxComposeWindow(
    private val title: String,
    private val width: Int = 960,
    private val height: Int = 640,
) {
    val window = WaylandWindow(title, width, height)
    val layer = SkiaLayer()

    /**
     * 系统关窗回调（对齐 Desktop DO_NOTHING_ON_CLOSE）。
     * xdg_toplevel_close / requestClose 只调此回调，**不** destroy。
     */
    var onCloseRequest: (() -> Unit)? = null

    /** 原生几何变化提示（resize / maximize）；声明式 Window 写回 WindowState。 */
    internal var onGeometryHint: (() -> Unit)? = null

    /**
     * true = [androidx.compose.ui.window.DialogWindow]；Host 据此做软模态。
     * 变更时刷新 [LinuxApplicationHost.refreshDialogModality]。
     */
    internal var isDialogWindow: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (isHostAttached) {
                LinuxApplicationHost.refreshDialogModality()
            }
        }

    /**
     * 软模态输入开关（对齐 Win32 EnableWindow）。
     * 有任一 DialogWindow 时，非对话框由 Host 置为 false，派发侧跳过 pointer/key/touch。
     */
    internal var inputEnabled: Boolean = true

    /**
     * 用户侧 [androidx.compose.ui.window.Window] `enabled`（与 [inputEnabled] AND）。
     */
    internal var userEnabled: Boolean = true

    /**
     * Desktop `visible`。Wayland 无廉价 unmap：软隐藏 = 跳过输入/绘制（surface 仍挂着）。
     */
    internal var userVisible: Boolean = true

    /** 读 [userVisible]（自检 / 诊断用）。 */
    val isUserVisible: Boolean get() = userVisible

    /**
     * Desktop `focusable`。标准 xdg-shell 无法禁止 compositor 给焦点；
     * 宿主侧在 false 时丢弃 Key/IME（指针仍可达），并打一次日志。
     */
    internal var focusable: Boolean = true
        private set

    fun setFocusable(enabled: Boolean) {
        focusable = enabled
    }

    /**
     * Desktop `transparent`：空 wl opaque region + Skia clear(TRANSPARENT)。
     * EGL/Vulkan 已带 ALPHA_SIZE。
     */
    internal var transparent: Boolean = false
        private set

    fun setTransparent(enabled: Boolean) {
        if (transparent == enabled) {
            window.setTransparent(enabled)
            return
        }
        transparent = enabled
        window.setTransparent(enabled)
    }

    /** CSD 边缘缩放带宽（逻辑 dp）；Wayland beginResize 不按像素带宽，仅记账。 */
    internal var resizerThicknessDp: Float = 8f

    /**
     * Desktop [androidx.compose.ui.window.FrameWindowScope.MenuBar] 树。
     * Wayland 用 Compose 顶栏绘制（见 [LinuxComposeMenuBar]）。
     */
    var menuBarModel: NativeMenuBarModel? by mutableStateOf(null)
        private set

    fun setMenuBar(model: NativeMenuBarModel?) {
        menuBarModel = model
    }

    /** 尝试消费菜单 shortcut（KeyDown）；命中则调 onClick。 */
    fun tryMenuShortcut(event: KeyEvent): Boolean {
        val model = menuBarModel ?: return false
        for ((shortcut, action) in model.shortcutActions()) {
            if (shortcut.matches(event)) {
                action()
                return true
            }
        }
        return false
    }

    /** 窗级键盘钩子（对齐 Desktop onPreviewKeyEvent / onKeyEvent）。 */
    internal var onPreviewKeyEvent: (KeyEvent) -> Boolean = { false }
    internal var onKeyEvent: (KeyEvent) -> Boolean = { false }

    /** 有效输入开关 = 软模态 ∧ 用户 enabled ∧ visible。 */
    internal val effectivelyInputEnabled: Boolean
        get() = inputEnabled && userEnabled && userVisible

    var resizable: Boolean = true
    var alwaysOnTop: Boolean = false

    /** 是否已挂到 [LinuxApplicationHost]（共享泵）；与独占 [run] 互斥。 */
    var isHostAttached: Boolean = false
        private set

    private var hostEventHandler: ((WaylandEvent) -> Unit)? = null
    private var layerAttached: Boolean = false
    private var destroyed: Boolean = false
    /** Frame→renderImmediately 嵌套深度；>0 时 detach 推迟 native destroy，避免重入 UAF。 */
    private var renderDepth: Int = 0
    private var detachDeferred: Boolean = false
    private var afterDetach: (() -> Unit)? = null

    fun setTitle(title: String) {
        window.setTitle(title)
    }

    private fun beginHostRender() {
        renderDepth++
    }

    private fun endHostRender() {
        renderDepth--
        if (renderDepth <= 0) {
            renderDepth = 0
            if (detachDeferred) {
                detachDeferred = false
                finishDetachFromHost()
            }
        }
    }

    private fun renderFrameGuarded() {
        if (!userVisible) return
        beginHostRender()
        try {
            layer.renderImmediately()
        } finally {
            endHostRender()
        }
    }

    /**
     * 创建 attach SkiaLayer，并注册到共享宿主。**不**进入阻塞循环。
     */
    fun attachToHost(onEvent: (WaylandEvent) -> Unit) {
        check(!isHostAttached) { "already attached to LinuxApplicationHost" }
        check(!destroyed) { "window already destroyed" }
        initLinuxMainThread()
        ensureLayerAttached()
        hostEventHandler = onEvent
        isHostAttached = true
        window.onEvent = { event ->
            when (event.type) {
                WaylandEventType.Frame -> renderFrameGuarded()
                WaylandEventType.Scale -> {
                    onGeometryHint?.invoke()
                    onEvent(event)
                }
                WaylandEventType.Focus -> {
                    LinuxApplicationHost.noteFocus(this, event.pressed)
                    onEvent(event)
                }
                WaylandEventType.PointerButton -> {
                    if (event.pressed) {
                        LinuxApplicationHost.notePointerActivity(this)
                    }
                    onEvent(event)
                }
                else -> onEvent(event)
            }
        }
        LinuxApplicationHost.register(this)
        layer.needRender()
        // 首帧不依赖 compositor FRAME（headless / 首 commit 无 buffer 时回调可能永不来）。
        renderFrameGuarded()
        println("composekn: attachToHost ready ${window.width}x${window.height}")
    }

    /**
     * 共享泵：poll + 关窗回调。返回 false 表示 display 失败。
     * Close：只调 [onCloseRequest]，**不** destroy。
     */
    fun pollAndDispatchForHost(): Boolean {
        if (destroyed || !isHostAttached) return false
        if (!window.poll()) return false
        if (window.consumeCloseRequested()) {
            onCloseRequest?.invoke()
        }
        if (window.consumeResized()) {
            layer.needRender()
            onGeometryHint?.invoke()
        }
        return true
    }

    /**
     * Host 在 poll 失败并 [LinuxApplicationHost.unregister] 后调用：
     * 只清宿主标志，**不** destroy（留给 [detachFromHost] / dispose）。
     */
    internal fun markHostDetachedAfterPollFailure() {
        isHostAttached = false
        hostEventHandler = null
        window.onEvent = null
    }

    /**
     * 从共享宿主摘掉并销毁 Wayland surface。
     *
     * @param afterNativeDestroyed 在 layer.detach + native destroy 完成之后调用
     *（用于 [LinuxComposeApplication.close]；若正处于 Frame 渲染中则推迟到帧结束）。
     */
    fun detachFromHost(afterNativeDestroyed: (() -> Unit)? = null) {
        if (!isHostAttached && !layerAttached && destroyed) {
            afterNativeDestroyed?.invoke()
            return
        }
        if (detachDeferred) {
            if (afterNativeDestroyed != null) {
                val prev = afterDetach
                afterDetach = {
                    prev?.invoke()
                    afterNativeDestroyed()
                }
            }
            return
        }
        println("composekn: detachFromHost")
        if (isHostAttached) {
            LinuxApplicationHost.unregister(this)
        }
        isHostAttached = false
        hostEventHandler = null
        window.onEvent = null
        afterDetach = afterNativeDestroyed
        // renderImmediately 内部可能重入 flush Main → exitApplication → 本函数；
        // 推迟 layer.detach / native destroy / after，等帧结束再拆。
        if (renderDepth > 0) {
            detachDeferred = true
            return
        }
        finishDetachFromHost()
    }

    private fun finishDetachFromHost() {
        if (layerAttached) {
            layer.detach()
            layerAttached = false
        }
        destroyNative()
        val cb = afterDetach
        afterDetach = null
        cb?.invoke()
    }

    /**
     * 独占消息循环（单窗口遗留路径）。
     *
     * 系统关窗默认退出本循环并 destroy（若未另设 [onCloseRequest] 且未挂宿主）。
     */
    fun run(onEvent: (WaylandEvent) -> Unit) {
        check(!isHostAttached) { "use shared host or exclusive run, not both" }
        initLinuxMainThread()
        ensureLayerAttached()
        check(layer.renderDelegate != null) {
            "SkiaLayer.renderDelegate must be set before LinuxComposeWindow.run()"
        }
        // 命令式默认：关窗 = 退出循环（随后 destroy）
        if (onCloseRequest == null) {
            onCloseRequest = { /* exclusive: exit loop via consumeCloseRequested */ }
        }
        window.onEvent = { event ->
            when (event.type) {
                WaylandEventType.Frame -> renderFrameGuarded()
                else -> onEvent(event)
            }
        }
        layer.needRender()
        var running = true
        while (running && !destroyed) {
            if (!window.poll()) {
                running = false
                break
            }
            if (window.consumeCloseRequested()) {
                onCloseRequest?.invoke()
                // 独占：关窗即退出（对齐旧 poll==false）；共享宿主路径不会进这里
                if (!isHostAttached) {
                    running = false
                }
            }
            flushMainUIDispatcher()
            LinuxDragAndDropManager.flushPendingOutgoingDrags()
            LinuxDragAndDropManager.pollOutgoingDragResults()
            if (window.consumeResized()) {
                layer.needRender()
                onGeometryHint?.invoke()
            }
        }
        if (layerAttached) {
            layer.detach()
            layerAttached = false
        }
        destroyNative()
    }

    /** Legacy Skia-only loop without Compose scene. */
    fun runSkiaOnly(drawFrame: (Canvas, Int, Int) -> Unit) {
        layer.renderDelegate = object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                drawFrame(canvas, width, height)
            }
        }
        run(onEvent = {})
    }

    fun destroy() {
        if (isHostAttached) {
            detachFromHost()
            return
        }
        if (layerAttached) {
            layer.detach()
            layerAttached = false
        }
        destroyNative()
    }

    private fun ensureLayerAttached() {
        if (layerAttached) return
        layer.attachTo(window)
        layerAttached = true
    }

    private fun destroyNative() {
        if (destroyed) return
        destroyed = true
        window.destroy()
    }
}

fun drawHelloFrame(canvas: Canvas, width: Int, height: Int) {
    val paint = org.jetbrains.skia.Paint().apply { color = org.jetbrains.skia.Color.makeRGB(30, 120, 220) }
    canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(40f, 40f, width.toFloat() - 80f, height.toFloat() - 80f), paint)
    val font = org.jetbrains.skia.Font(null, 28f)
    val textPaint = org.jetbrains.skia.Paint().apply { color = org.jetbrains.skia.Color.WHITE }
    canvas.drawString("ComposeKN / Wayland", 60f, 100f, font, textPaint)
}
