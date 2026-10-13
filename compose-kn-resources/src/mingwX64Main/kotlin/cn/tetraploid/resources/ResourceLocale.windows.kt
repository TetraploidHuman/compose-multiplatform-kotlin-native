@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.tetraploid.resources

import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.windows.GetUserDefaultLocaleName
import platform.windows.WCHARVar

/** Win32 [GetUserDefaultLocaleName] → `zh-CN` 风格。 */
internal actual fun systemLocaleTag(): String? = memScoped {
    val wbuf = allocArray<WCHARVar>(85) // LOCALE_NAME_MAX_LENGTH
    val n = GetUserDefaultLocaleName(wbuf, 85)
    if (n <= 0) return@memScoped null
    wbuf.toKString().takeIf { it.isNotEmpty() }
}
