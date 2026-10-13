/*
 * Copyright 2021 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * Port of KeyShortcut.desktop.kt without Swing/AWT.
 */

package androidx.compose.ui.input.key

/**
 * Key combination that should be pressed to trigger an action (menu accelerator, etc.).
 */
class KeyShortcut(
    val key: Key,
    val ctrl: Boolean = false,
    val meta: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyShortcut) return false
        return key == other.key &&
            ctrl == other.ctrl &&
            meta == other.meta &&
            alt == other.alt &&
            shift == other.shift
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + ctrl.hashCode()
        result = 31 * result + meta.hashCode()
        result = 31 * result + alt.hashCode()
        result = 31 * result + shift.hashCode()
        return result
    }

    override fun toString() = buildString {
        if (ctrl) append("Ctrl+")
        if (meta) append("Meta+")
        if (alt) append("Alt+")
        if (shift) append("Shift+")
        append(key)
    }
}

/** Display suffix for native menu text (Win32 `\tCtrl+S` style). */
fun KeyShortcut.toMenuAcceleratorLabel(): String = buildString {
    if (ctrl) append("Ctrl+")
    if (meta) append("Meta+")
    if (alt) append("Alt+")
    if (shift) append("Shift+")
    append(key.toString().substringAfterLast('.').substringAfter("Key: ").ifEmpty { key.toString() })
}

/** Whether [event] matches this shortcut (KeyDown only callers should filter type). */
fun KeyShortcut.matches(event: KeyEvent): Boolean =
    event.key == key &&
        event.isCtrlPressed == ctrl &&
        event.isMetaPressed == meta &&
        event.isAltPressed == alt &&
        event.isShiftPressed == shift
