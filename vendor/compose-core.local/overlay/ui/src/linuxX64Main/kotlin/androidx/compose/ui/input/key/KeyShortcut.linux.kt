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
        append(key.toMenuKeyLabel())
    }
}

/** Display suffix for native menu text (Win32 `\tCtrl+S` style). */
fun KeyShortcut.toMenuAcceleratorLabel(): String = toString()

/** Whether [event] matches this shortcut (KeyDown only callers should filter type). */
fun KeyShortcut.matches(event: KeyEvent): Boolean =
    event.key == key &&
        event.isCtrlPressed == ctrl &&
        event.isMetaPressed == meta &&
        event.isAltPressed == alt &&
        event.isShiftPressed == shift

/**
 * Human-readable key label for menus. Native [Key.toString] is `"Key keyCode: N"` — do not scrape it.
 */
fun Key.toMenuKeyLabel(): String = MenuKeyLabels[this] ?: "Key${keyCode}"

private val MenuKeyLabels: Map<Key, String> = buildMap {
    fun put(k: Key, label: String) {
        put(k, label)
    }
    put(Key.A, "A"); put(Key.B, "B"); put(Key.C, "C"); put(Key.D, "D")
    put(Key.E, "E"); put(Key.F, "F"); put(Key.G, "G"); put(Key.H, "H")
    put(Key.I, "I"); put(Key.J, "J"); put(Key.K, "K"); put(Key.L, "L")
    put(Key.M, "M"); put(Key.N, "N"); put(Key.O, "O"); put(Key.P, "P")
    put(Key.Q, "Q"); put(Key.R, "R"); put(Key.S, "S"); put(Key.T, "T")
    put(Key.U, "U"); put(Key.V, "V"); put(Key.W, "W"); put(Key.X, "X")
    put(Key.Y, "Y"); put(Key.Z, "Z")
    put(Key.Zero, "0"); put(Key.One, "1"); put(Key.Two, "2"); put(Key.Three, "3")
    put(Key.Four, "4"); put(Key.Five, "5"); put(Key.Six, "6"); put(Key.Seven, "7")
    put(Key.Eight, "8"); put(Key.Nine, "9")
    put(Key.F1, "F1"); put(Key.F2, "F2"); put(Key.F3, "F3"); put(Key.F4, "F4")
    put(Key.F5, "F5"); put(Key.F6, "F6"); put(Key.F7, "F7"); put(Key.F8, "F8")
    put(Key.F9, "F9"); put(Key.F10, "F10"); put(Key.F11, "F11"); put(Key.F12, "F12")
    put(Key.Enter, "Enter")
    put(Key.Escape, "Esc")
    put(Key.Tab, "Tab")
    put(Key.Spacebar, "Space")
    put(Key.Backspace, "Backspace")
    put(Key.Delete, "Delete")
    put(Key.Insert, "Insert")
    put(Key.MoveHome, "Home")
    put(Key.MoveEnd, "End")
    put(Key.PageUp, "PgUp")
    put(Key.PageDown, "PgDn")
    put(Key.DirectionUp, "Up")
    put(Key.DirectionDown, "Down")
    put(Key.DirectionLeft, "Left")
    put(Key.DirectionRight, "Right")
    put(Key.Minus, "-")
    put(Key.Equals, "=")
    put(Key.Comma, ",")
    put(Key.Period, ".")
    put(Key.Slash, "/")
    put(Key.Backslash, "\\")
    put(Key.Semicolon, ";")
    put(Key.Apostrophe, "'")
    put(Key.Grave, "`")
    put(Key.LeftBracket, "[")
    put(Key.RightBracket, "]")
}
