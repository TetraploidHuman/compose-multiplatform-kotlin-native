/*
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * FrameWindowScope.MenuBar：composition 用稳定 slot 收集树 → SideEffect → setMenuBar。
 * Win32 → 原生 HMENU；Wayland → Compose 顶栏（LinuxComposeMenuBar）。
 * 无边框 Win 窗不挂系统 HMENU（宿主忽略）。
 *
 * API 对齐 Desktop MenuScope：Item / CheckboxItem / RadioButtonItem +
 * icon / mnemonic / shortcut（icon 忽略；Win mnemonic→`&` / shortcut→`\t` 标签）。
 */

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.toMenuAcceleratorLabel

/**
 * 原生菜单树节点（平台无关模型；Windows 上映射为 HMENU）。
 */
sealed class NativeMenuNode {
    data class Menu(
        val text: String,
        val enabled: Boolean = true,
        val mnemonic: Char? = null,
        val children: List<NativeMenuNode> = emptyList(),
    ) : NativeMenuNode()

    data class Item(
        val text: String,
        val enabled: Boolean = true,
        val mnemonic: Char? = null,
        val shortcut: KeyShortcut? = null,
        /** null = 普通项；true/false = 勾选态（Checkbox / Radio）。 */
        val checked: Boolean? = null,
        val onClick: () -> Unit,
    ) : NativeMenuNode()

    data object Separator : NativeMenuNode()
}

/**
 * 整棵菜单栏模型（顶层必须是 [NativeMenuNode.Menu]）。
 */
data class NativeMenuBarModel(
    val menus: List<NativeMenuNode.Menu>,
)

/**
 * 结构指纹（忽略 onClick）：宿主用它避免动画每帧 DestroyMenu/CreateMenu。
 */
fun NativeMenuBarModel.structureKey(): String = buildString {
    fun walk(nodes: List<NativeMenuNode>) {
        for (n in nodes) {
            when (n) {
                is NativeMenuNode.Separator -> append('|')
                is NativeMenuNode.Item -> {
                    append('I')
                    append(n.text.length).append(':').append(n.text)
                    append(if (n.enabled) '1' else '0')
                    append(n.mnemonic ?: '-')
                    append(n.shortcut?.toString() ?: "-")
                    append(
                        when (n.checked) {
                            null -> 'n'
                            true -> 't'
                            false -> 'f'
                        },
                    )
                }
                is NativeMenuNode.Menu -> {
                    append('M')
                    append(n.text.length).append(':').append(n.text)
                    append(if (n.enabled) '1' else '0')
                    append(n.mnemonic ?: '-')
                    append('{')
                    walk(n.children)
                    append('}')
                }
            }
        }
    }
    walk(menus)
}

/** 收集带 shortcut 的菜单项（窗级键盘分发用）。 */
fun NativeMenuBarModel.shortcutActions(): List<Pair<KeyShortcut, () -> Unit>> {
    val out = mutableListOf<Pair<KeyShortcut, () -> Unit>>()
    fun walk(nodes: List<NativeMenuNode>) {
        for (n in nodes) {
            when (n) {
                is NativeMenuNode.Separator -> Unit
                is NativeMenuNode.Item -> {
                    val s = n.shortcut
                    if (s != null && n.enabled) out += s to n.onClick
                }
                is NativeMenuNode.Menu -> if (n.enabled) walk(n.children)
            }
        }
    }
    walk(menus)
    return out
}

/** 组合期稳定挂载点：同一 slot 重复 ensure 不会打乱顺序。 */
internal class MenuSlot(var node: NativeMenuNode)

/** MenuBar / Tray 共用：收集 Menu / Item / Separator 树。 */
internal class MenuCollector {
    private val slots = mutableListOf<MenuSlot>()

    fun ensure(slot: MenuSlot) {
        if (slot !in slots) slots.add(slot)
    }

    fun remove(slot: MenuSlot) {
        slots.remove(slot)
    }

    fun toList(): List<NativeMenuNode> = slots.map { it.node }

    fun toMenus(): List<NativeMenuNode.Menu> =
        slots.mapNotNull { it.node as? NativeMenuNode.Menu }
}

internal val LocalMenuCollector =
    compositionLocalOf<MenuCollector> {
        error("Menu/Item 只能用在 MenuBar / Tray / Menu 内容里")
    }

/**
 * 在窗口标题栏下挂菜单栏（Win32 = HMENU；Wayland = Compose 顶栏）。
 *
 * Win 无边框（`undecorated = true`）不挂系统 HMENU；Linux 仍画 Compose 条。
 */
@Composable
fun FrameWindowScope.MenuBar(content: @Composable MenuBarScope.() -> Unit) {
    val roots = remember { MenuCollector() }
    CompositionLocalProvider(LocalMenuCollector provides roots) {
        MenuBarScope().content()
    }
    SideEffect {
        window.setMenuBar(NativeMenuBarModel(roots.toMenus()))
    }
    DisposableEffect(Unit) {
        onDispose { window.setMenuBar(null) }
    }
}

/** [FrameWindowScope.MenuBar] 的 receiver：往菜单栏追加顶层 [Menu]。 */
class MenuBarScope internal constructor() {
    @Composable
    fun Menu(
        text: String,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        content: @Composable MenuScope.() -> Unit,
    ) {
        menuNode(text, enabled, mnemonic, content)
    }
}

/** 单个弹出菜单 / 子菜单的 receiver。 */
class MenuScope internal constructor() {
    @Composable
    fun Menu(
        text: String,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        content: @Composable MenuScope.() -> Unit,
    ) {
        menuNode(text, enabled, mnemonic, content)
    }

    @Composable
    fun Separator() {
        val parent = LocalMenuCollector.current
        val slot = remember { MenuSlot(NativeMenuNode.Separator) }
        slot.node = NativeMenuNode.Separator
        parent.ensure(slot)
        DisposableEffect(parent) {
            onDispose { parent.remove(slot) }
        }
    }

    @Composable
    fun Item(
        text: String,
        icon: Painter? = null,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        shortcut: KeyShortcut? = null,
        onClick: () -> Unit,
    ) {
        // icon：原生 HMENU 无廉价位图项，忽略（API 对齐 Desktop）。
        @Suppress("UNUSED_PARAMETER")
        val ignoredIcon = icon
        itemNode(text, enabled, mnemonic, shortcut, checked = null, onClick = onClick)
    }

    @Composable
    fun CheckboxItem(
        text: String,
        checked: Boolean,
        icon: Painter? = null,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        shortcut: KeyShortcut? = null,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        @Suppress("UNUSED_PARAMETER")
        val ignoredIcon = icon
        itemNode(
            text = text,
            enabled = enabled,
            mnemonic = mnemonic,
            shortcut = shortcut,
            checked = checked,
            onClick = { onCheckedChange(!checked) },
        )
    }

    @Composable
    fun RadioButtonItem(
        text: String,
        selected: Boolean,
        icon: Painter? = null,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        shortcut: KeyShortcut? = null,
        onClick: () -> Unit,
    ) {
        @Suppress("UNUSED_PARAMETER")
        val ignoredIcon = icon
        itemNode(
            text = text,
            enabled = enabled,
            mnemonic = mnemonic,
            shortcut = shortcut,
            checked = selected,
            onClick = onClick,
        )
    }
}

@Composable
private fun itemNode(
    text: String,
    enabled: Boolean,
    mnemonic: Char?,
    shortcut: KeyShortcut?,
    checked: Boolean?,
    onClick: () -> Unit,
) {
    val parent = LocalMenuCollector.current
    val latestOnClick by rememberUpdatedState(onClick)
    val slot = remember {
        MenuSlot(
            NativeMenuNode.Item(
                text = text,
                enabled = enabled,
                mnemonic = mnemonic,
                shortcut = shortcut,
                checked = checked,
                onClick = { latestOnClick() },
            ),
        )
    }
    slot.node = NativeMenuNode.Item(
        text = text,
        enabled = enabled,
        mnemonic = mnemonic,
        shortcut = shortcut,
        checked = checked,
        onClick = { latestOnClick() },
    )
    parent.ensure(slot)
    DisposableEffect(parent) {
        onDispose { parent.remove(slot) }
    }
}

@Composable
private fun menuNode(
    text: String,
    enabled: Boolean,
    mnemonic: Char?,
    content: @Composable MenuScope.() -> Unit,
) {
    val parent = LocalMenuCollector.current
    val children = remember { MenuCollector() }
    CompositionLocalProvider(LocalMenuCollector provides children) {
        MenuScope().content()
    }
    val slot = remember {
        MenuSlot(
            NativeMenuNode.Menu(
                text = text,
                enabled = enabled,
                mnemonic = mnemonic,
                children = emptyList(),
            ),
        )
    }
    slot.node = NativeMenuNode.Menu(
        text = text,
        enabled = enabled,
        mnemonic = mnemonic,
        children = children.toList(),
    )
    parent.ensure(slot)
    DisposableEffect(parent) {
        onDispose { parent.remove(slot) }
    }
}

/** 把 mnemonic / shortcut 编进原生菜单显示串（`&F` + `\tCtrl+S`）。 */
fun formatNativeMenuLabel(
    text: String,
    mnemonic: Char?,
    shortcut: KeyShortcut?,
): String {
    val withMnemonic = if (mnemonic != null) {
        val idx = text.indexOf(mnemonic, ignoreCase = true)
        if (idx >= 0) {
            text.substring(0, idx) + "&" + text.substring(idx)
        } else {
            "&$mnemonic $text"
        }
    } else {
        text
    }
    return if (shortcut != null) {
        "$withMnemonic\t${shortcut.toMenuAcceleratorLabel()}"
    } else {
        withMnemonic
    }
}
