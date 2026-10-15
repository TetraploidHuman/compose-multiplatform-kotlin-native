/*
 * Copyright 2020 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * Deprecated mouseScrollFilter without java.awt.MouseWheelEvent (always Line units).
 */

@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("DEPRECATION")

package androidx.compose.ui.input.mouse

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

@Deprecated(
    "Use Modifier.pointerInput + PointerEventType.Scroll." +
        "See the comment to mouseScrollFilter. Will be removed in 1.3",
)
sealed class MouseScrollUnit {
    @Deprecated(
        "Use Modifier.pointerInput + PointerEventType.Scroll." +
            "See the comment to mouseScrollFilter. Will be removed in 1.3",
    )
    data class Line(val value: Float) : MouseScrollUnit()

    @Deprecated(
        "Use Modifier.pointerInput + PointerEventType.Scroll." +
            "See the comment to mouseScrollFilter. Will be removed in 1.3",
    )
    data class Page(val value: Float) : MouseScrollUnit()
}

@Deprecated(
    "Use Modifier.pointerInput + PointerEventType.Scroll." +
        "See the comment to mouseScrollFilter. Will be removed in 1.3",
)
class MouseScrollEvent(
    val delta: MouseScrollUnit,
    val orientation: MouseScrollOrientation,
)

/**
 * Intercept scroll events from mouse wheel and touchpad.
 *
 * ComposeKN: always maps [scrollDelta] to [MouseScrollUnit.Line] (no AWT page/line distinction).
 */
@Deprecated(
    "Use Modifier.pointerInput + PointerEventType.Scroll. Will be removed in 1.3",
    replaceWith = ReplaceWith(
        "pointerInput(Unit) { \n" +
            "     awaitPointerEventScope {\n" +
            "         while (true) {\n" +
            "             val event = awaitPointerEvent()\n" +
            "             if (event.type == PointerEventType.Scroll) {\n" +
            "                 val scrollDelta = event.changes.first().scrollDelta\n" +
            "                 val bounds = this.size\n" +
            "                 if (onMouseScroll(scrollDelta, bounds)) {\n" +
            "                      event.changes.first().consume()\n" +
            "                 }\n" +
            "             }\n" +
            "         }\n" +
            "     }\n" +
            "}",
        "androidx.compose.ui.input.pointer.pointerInput",
    ),
)
fun Modifier.mouseScrollFilter(
    onMouseScroll: (event: MouseScrollEvent, bounds: IntSize) -> Boolean,
): Modifier = pointerInput(onMouseScroll) {
    while (true) {
        val event = awaitScrollEvent()
        val mouseChange = event.changes.find { it.type == PointerType.Mouse }
        if (mouseChange != null && !mouseChange.isConsumed) {
            val legacyEvent = mouseChange.scrollDelta.toLegacyEvent()
            if (onMouseScroll(legacyEvent, size)) {
                mouseChange.consume()
            }
        }
    }
}

private suspend fun PointerInputScope.awaitScrollEvent(): PointerEvent = awaitPointerEventScope {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (event.type != PointerEventType.Scroll)
    event
}

private fun Offset.toLegacyEvent(): MouseScrollEvent {
    val value = if (x != 0f) x else y
    return MouseScrollEvent(
        delta = MouseScrollUnit.Line(value),
        orientation = if (x != 0f) {
            MouseScrollOrientation.Horizontal
        } else {
            MouseScrollOrientation.Vertical
        },
    )
}

@Deprecated(
    "Use Modifier.pointerInput + PointerEventType.Scroll." +
        "See the comment to mouseScrollFilter. Will be removed in 1.3",
)
enum class MouseScrollOrientation {
    Vertical, Horizontal
}
