@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlinx.cinterop.toKString
import platform.posix.getenv

/**
 * Linux：xdg-desktop-portal Settings / gsettings；
 * 可用 `COMPOSEKN_SYSTEM_THEME=dark|light|unknown` 覆盖。
 */
actual val currentSystemTheme: SystemTheme
    get() {
        systemThemeOverride()?.let { return it }
        return when (composekn_linux_system_theme()) {
            0 -> SystemTheme.LIGHT
            1 -> SystemTheme.DARK
            else -> SystemTheme.UNKNOWN
        }
    }

internal fun systemThemeOverride(): SystemTheme? {
    val raw = getenv("COMPOSEKN_SYSTEM_THEME")?.toKString()?.trim()?.lowercase() ?: return null
    return when (raw) {
        "dark", "1" -> SystemTheme.DARK
        "light", "0" -> SystemTheme.LIGHT
        "unknown", "2" -> SystemTheme.UNKNOWN
        else -> null
    }
}

@SymbolName("composekn_linux_system_theme")
internal external fun composekn_linux_system_theme(): Int
