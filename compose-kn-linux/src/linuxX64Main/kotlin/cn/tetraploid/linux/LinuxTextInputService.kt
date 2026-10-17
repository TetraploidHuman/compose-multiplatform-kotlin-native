@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package cn.tetraploid.linux

import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.EditCommand
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.skiko.WaylandImeEvent
import org.jetbrains.skiko.WaylandWindow

/**
 * Bridges Compose text input to the Wayland `zwp_text_input_v3` IME protocol.
 *
 * When a [TextField] is focused, [startInputMethod] enables the text input and reports the
 * surrounding text + cursor rectangle to the compositor (so the IME, e.g. fcitx5, can anchor
 * its candidate window). Pending delete/commit are applied on [WaylandImeEvent.Done], in the
 * protocol order: delete surrounding text, then insert the commit string.
 *
 * Lengths from the compositor are UTF-8 bytes. Cursor/anchor reported upward are byte offsets
 * into that same encoding. Preedit stays on the IME side and is not inserted into the field.
 */
internal class LinuxTextInputService(
    private val window: WaylandWindow,
    private val contentScale: () -> Float,
) {
    var activeRequest: PlatformTextInputMethodRequest? = null
        private set

    private var pendingDeleteBefore = 0
    private var pendingDeleteAfter = 0
    private var hasPendingDelete = false
    private var pendingCommit: String? = null

    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        activeRequest = request
        window.textInputSetEnabled(true)
        updateImeContext(request)
        try {
            awaitCancellation()
        } finally {
            if (activeRequest === request) {
                activeRequest = null
                window.textInputSetEnabled(false)
            }
        }
    }

    /** Dispatch a text-input-v3 event to the focused field. Called from the event loop (main thread). */
    fun handleImeEvent(event: WaylandImeEvent) {
        when (event) {
            is WaylandImeEvent.Commit -> {
                imeLog("commit '${event.text}'")
                pendingCommit = event.text
            }
            is WaylandImeEvent.Preedit -> {
                // Composing text stays in the IME candidate window.
                if (event.text.isNotEmpty()) imeLog("preedit '${event.text}'")
            }
            is WaylandImeEvent.Delete -> {
                imeLog("delete before=${event.beforeLength} after=${event.afterLength}")
                pendingDeleteBefore = event.beforeLength.coerceAtLeast(0)
                pendingDeleteAfter = event.afterLength.coerceAtLeast(0)
                hasPendingDelete = pendingDeleteBefore > 0 || pendingDeleteAfter > 0
            }
            is WaylandImeEvent.Done -> applyPending()
            is WaylandImeEvent.Enter -> {
                // Text-input focus is driven by startInputMethod cancellation/enable.
                imeLog("enter (text input enabled)")
            }
            is WaylandImeEvent.Leave -> {
                // Focus loss is handled by startInputMethod cancellation.
                imeLog("leave (text input disabled)")
            }
        }
    }

    private fun imeLog(message: String) {
        println("composekn: ime $message")
    }

    private fun applyPending() {
        val request = activeRequest
        val commands = ArrayList<EditCommand>(2)
        if (request != null && hasPendingDelete) {
            val value = request.value()
            val before = utf16UnitsForUtf8Bytes(
                text = value.text,
                from = value.selection.min,
                bytes = pendingDeleteBefore,
                forward = false,
            )
            val after = utf16UnitsForUtf8Bytes(
                text = value.text,
                from = value.selection.max,
                bytes = pendingDeleteAfter,
                forward = true,
            )
            if (before > 0 || after > 0) {
                commands += DeleteSurroundingTextCommand(before, after)
            }
        }
        val commit = pendingCommit
        if (!commit.isNullOrEmpty()) {
            commands += CommitTextCommand(commit, 1)
        }
        pendingDeleteBefore = 0
        pendingDeleteAfter = 0
        hasPendingDelete = false
        pendingCommit = null
        if (request != null && commands.isNotEmpty()) {
            request.onEditCommand(commands)
            refreshContext()
        }
    }

    private fun refreshContext() {
        activeRequest?.let { updateImeContext(it) }
    }

    private fun updateImeContext(request: PlatformTextInputMethodRequest) {
        val value = request.value()
        window.textInputSetSurroundingText(
            text = value.text,
            cursor = utf8ByteOffset(value.text, value.selection.max),
            anchor = utf8ByteOffset(value.text, value.selection.min),
        )
        val rect = request.focusedRectInRoot()
        val scale = contentScale()
        window.textInputSetCursorRectangle(
            x = ((rect?.left ?: 0f) * scale).toInt(),
            y = ((rect?.top ?: 0f) * scale).toInt(),
            width = (((rect?.width ?: 8f).coerceAtLeast(1f)) * scale).toInt(),
            height = (((rect?.height ?: 18f).coerceAtLeast(1f)) * scale).toInt(),
        )
    }
}

/** UTF-16 index → UTF-8 byte offset (text-input-v3 cursor/anchor). */
private fun utf8ByteOffset(text: String, utf16Index: Int): Int {
    val end = utf16Index.coerceIn(0, text.length)
    if (end == 0) return 0
    return text.substring(0, end).encodeToByteArray().size
}

/**
 * How many UTF-16 code units cover [bytes] of UTF-8 moving from [from].
 * [forward] false walks toward the start of [text] (before the selection).
 */
private fun utf16UnitsForUtf8Bytes(text: String, from: Int, bytes: Int, forward: Boolean): Int {
    if (bytes <= 0 || text.isEmpty()) return 0
    var remaining = bytes
    var index = from.coerceIn(0, text.length)
    var units = 0
    if (forward) {
        while (index < text.length && remaining > 0) {
            val size = utf16CharSize(text, index)
            val encoded = text.substring(index, index + size).encodeToByteArray().size
            if (encoded > remaining) break
            remaining -= encoded
            units += size
            index += size
        }
    } else {
        while (index > 0 && remaining > 0) {
            val start = utf16CharStart(text, index)
            val encoded = text.substring(start, index).encodeToByteArray().size
            if (encoded > remaining) break
            remaining -= encoded
            units += index - start
            index = start
        }
    }
    return units
}

private fun utf16CharSize(text: String, index: Int): Int {
    val ch = text[index]
    return if (ch.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) 2 else 1
}

private fun utf16CharStart(text: String, index: Int): Int {
    if (index <= 0) return 0
    val prev = index - 1
    val ch = text[prev]
    return if (ch.isLowSurrogate() && prev > 0 && text[prev - 1].isHighSurrogate()) prev - 1 else prev
}
