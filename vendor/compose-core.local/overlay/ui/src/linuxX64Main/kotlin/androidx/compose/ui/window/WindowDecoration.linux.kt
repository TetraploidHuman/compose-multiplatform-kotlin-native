/*
 * Copyright 2024 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * Port of WindowDecoration.desktop.kt (no AWT).
 */

package androidx.compose.ui.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowDecoration.Companion.Undecorated
import androidx.compose.ui.window.WindowDecoration.SystemDefault

/** Defines the options for window decoration. */
@ExperimentalComposeUiApi
sealed interface WindowDecoration {

    /** Default system decoration. */
    data object SystemDefault : WindowDecoration

    companion object {
        /**
         * Undecorated window. If resizable, [resizerThickness] is used for edge resizers
         * (host may honor or approximate).
         */
        fun Undecorated(
            resizerThickness: Dp = WindowDecorationDefaults.ResizerThickness,
        ): WindowDecoration = UndecoratedWindowDecoration(resizerThickness)
    }
}

@Immutable
internal class UndecoratedWindowDecoration(val resizerThickness: Dp) : WindowDecoration {
    override fun equals(other: Any?): Boolean {
        if (other !is UndecoratedWindowDecoration) return false
        return other.resizerThickness == resizerThickness
    }

    override fun hashCode(): Int = resizerThickness.hashCode()
}

/** Default values for window decoration. */
@ExperimentalComposeUiApi
object WindowDecorationDefaults {
    val ResizerThickness: Dp = 8.dp
}

internal val WindowDecoration.resizerThickness: Dp
    get() = when {
        this is UndecoratedWindowDecoration -> resizerThickness
        else -> WindowDecorationDefaults.ResizerThickness
    }

internal fun windowDecorationFromFlag(undecorated: Boolean): WindowDecoration =
    if (undecorated) Undecorated() else SystemDefault

internal val WindowDecoration.isUndecorated: Boolean
    get() = this is UndecoratedWindowDecoration
