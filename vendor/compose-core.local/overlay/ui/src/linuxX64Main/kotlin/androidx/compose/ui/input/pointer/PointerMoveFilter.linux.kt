/*
 * Copyright 2020 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 */

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * Modifier allowing to track pointer (i.e. mouse or trackpad) move events.
 */
@ExperimentalComposeUiApi
@Deprecated(
    "Use onPointerEvent or hoverable",
    replaceWith = ReplaceWith(
        "onPointerEvent(PointerEventType.Move) {\n" +
            "        onMove(it.changes.first().position)\n" +
            "    }\n" +
            "    .onPointerEvent(PointerEventType.Enter) {\n" +
            "        onEnter()\n" +
            "    }\n" +
            "    .onPointerEvent(PointerEventType.Exit) {\n" +
            "        onExit()\n" +
            "    }",
        "androidx.compose.ui.input.pointer.onPointerEvent",
    ),
)
fun Modifier.pointerMoveFilter(
    onMove: (position: Offset) -> Boolean = { false },
    onExit: () -> Boolean = { false },
    onEnter: () -> Boolean = { false },
): Modifier = this
    .onPointerEvent(PointerEventType.Move) {
        onMove(it.changes.first().position)
    }
    .onPointerEvent(PointerEventType.Enter) {
        onEnter()
    }
    .onPointerEvent(PointerEventType.Exit) {
        onExit()
    }
