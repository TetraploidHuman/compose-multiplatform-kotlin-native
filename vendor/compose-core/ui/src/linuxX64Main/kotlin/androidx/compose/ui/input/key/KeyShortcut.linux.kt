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
    // 勿用局部 fun put：会遮蔽 MutableMap.put 并自递归（菜单 KeyShortcut 标签时栈溢出）。
    fun entry(k: Key, label: String) {
        put(k, label)
    }
    entry(Key.A, "A"); entry(Key.B, "B"); entry(Key.C, "C"); entry(Key.D, "D")
    entry(Key.E, "E"); entry(Key.F, "F"); entry(Key.G, "G"); entry(Key.H, "H")
    entry(Key.I, "I"); entry(Key.J, "J"); entry(Key.K, "K"); entry(Key.L, "L")
    entry(Key.M, "M"); entry(Key.N, "N"); entry(Key.O, "O"); entry(Key.P, "P")
    entry(Key.Q, "Q"); entry(Key.R, "R"); entry(Key.S, "S"); entry(Key.T, "T")
    entry(Key.U, "U"); entry(Key.V, "V"); entry(Key.W, "W"); entry(Key.X, "X")
    entry(Key.Y, "Y"); entry(Key.Z, "Z")
    entry(Key.Zero, "0"); entry(Key.One, "1"); entry(Key.Two, "2"); entry(Key.Three, "3")
    entry(Key.Four, "4"); entry(Key.Five, "5"); entry(Key.Six, "6"); entry(Key.Seven, "7")
    entry(Key.Eight, "8"); entry(Key.Nine, "9")
    entry(Key.F1, "F1"); entry(Key.F2, "F2"); entry(Key.F3, "F3"); entry(Key.F4, "F4")
    entry(Key.F5, "F5"); entry(Key.F6, "F6"); entry(Key.F7, "F7"); entry(Key.F8, "F8")
    entry(Key.F9, "F9"); entry(Key.F10, "F10"); entry(Key.F11, "F11"); entry(Key.F12, "F12")
    entry(Key.Enter, "Enter")
    entry(Key.Escape, "Esc")
    entry(Key.Tab, "Tab")
    entry(Key.Spacebar, "Space")
    entry(Key.Backspace, "Backspace")
    entry(Key.Delete, "Delete")
    entry(Key.Insert, "Insert")
    entry(Key.MoveHome, "Home")
    entry(Key.MoveEnd, "End")
    entry(Key.PageUp, "PgUp")
    entry(Key.PageDown, "PgDn")
    entry(Key.DirectionUp, "Up")
    entry(Key.DirectionDown, "Down")
    entry(Key.DirectionLeft, "Left")
    entry(Key.DirectionRight, "Right")
    entry(Key.Minus, "-")
    entry(Key.Equals, "=")
    entry(Key.Comma, ",")
    entry(Key.Period, ".")
    entry(Key.Slash, "/")
    entry(Key.Backslash, "\\")
    entry(Key.Semicolon, ";")
    entry(Key.Apostrophe, "'")
    entry(Key.Grave, "`")
    entry(Key.LeftBracket, "[")
    entry(Key.RightBracket, "]")
}
