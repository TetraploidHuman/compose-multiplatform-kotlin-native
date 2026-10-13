package cn.tetraploid.linux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.toMenuAcceleratorLabel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.NativeMenuBarModel
import androidx.compose.ui.window.NativeMenuNode

/**
 * Wayland 上无系统 HMENU：用 Compose 画一条顶栏菜单（对齐 Desktop MenuBar 树模型）。
 *
 * 支持 Item / Checkbox / Radio / Separator / 一层嵌套 [NativeMenuNode.Menu]；
 * shortcut 标签显示在项右侧，真正触发走宿主 [LinuxComposeWindow.tryMenuShortcut]。
 */
@Composable
fun LinuxComposeMenuBar(
    model: NativeMenuBarModel,
    modifier: Modifier = Modifier,
) {
    if (model.menus.isEmpty()) return
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color(0xFF3C3C3C)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        model.menus.forEachIndexed { index, menu ->
            Box {
                TextButton(
                    onClick = {
                        expandedIndex = if (expandedIndex == index) null else index
                    },
                    enabled = menu.enabled,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text(
                        text = menu.text,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (menu.enabled) Color.White else Color(0xFF888888),
                    )
                }
                DropdownMenu(
                    expanded = expandedIndex == index,
                    onDismissRequest = { expandedIndex = null },
                ) {
                    LinuxMenuNodes(
                        nodes = menu.children,
                        onDismiss = { expandedIndex = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun LinuxMenuNodes(
    nodes: List<NativeMenuNode>,
    onDismiss: () -> Unit,
) {
    for (node in nodes) {
        when (node) {
            is NativeMenuNode.Separator -> HorizontalDivider()
            is NativeMenuNode.Item -> {
                val accel = node.shortcut?.toMenuAcceleratorLabel()
                val label = buildString {
                    append(itemLeading(node))
                    append(node.text)
                    if (accel != null) {
                        append("    ")
                        append(accel)
                    }
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    enabled = node.enabled,
                    onClick = {
                        node.onClick()
                        onDismiss()
                    },
                )
            }
            is NativeMenuNode.Menu -> {
                var subExpanded by remember(node) { mutableStateOf(false) }
                Box {
                    DropdownMenuItem(
                        text = { Text(node.text + " ▸") },
                        enabled = node.enabled,
                        onClick = { subExpanded = true },
                    )
                    DropdownMenu(
                        expanded = subExpanded,
                        onDismissRequest = { subExpanded = false },
                    ) {
                        LinuxMenuNodes(
                            nodes = node.children,
                            onDismiss = {
                                subExpanded = false
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun itemLeading(node: NativeMenuNode.Item): String = when (node.checked) {
    true -> "✓ "
    false -> "    "
    null -> ""
}
