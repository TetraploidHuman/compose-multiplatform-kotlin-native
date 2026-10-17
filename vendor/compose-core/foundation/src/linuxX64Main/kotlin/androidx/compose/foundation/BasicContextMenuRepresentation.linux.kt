/*
 * Copyright 2021 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation

import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition

/**
 * Representation of a context menu that is suitable for light themes of the application.
 */
val LightDefaultContextMenuRepresentation = DefaultContextMenuRepresentation(
    backgroundColor = Color.White,
    textColor = Color.Black,
    itemHoverColor = Color.Black.copy(alpha = 0.04f),
)

/**
 * Representation of a context menu that is suitable for dark themes of the application.
 */
val DarkDefaultContextMenuRepresentation = DefaultContextMenuRepresentation(
    backgroundColor = Color(0xFF121212),
    textColor = Color.White,
    itemHoverColor = Color.White.copy(alpha = 0.04f),
)

/**
 * Custom representation of a context menu that allows to specify different colors.
 *
 * Swing [JPopupContextMenuRepresentation] is not available on Kotlin/Native.
 */
class DefaultContextMenuRepresentation(
    private val backgroundColor: Color,
    private val textColor: Color,
    private val itemHoverColor: Color,
    private val disabledTextColor: Color = textColor.copy(alpha = 0.38f),
) : ContextMenuRepresentation {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status !is ContextMenuState.Status.Open) return

        val session = remember(state) {
            object : TextContextMenuSession {
                override fun close() {
                    state.status = ContextMenuState.Status.Closed
                }
            }
        }
        val components by remember {
            derivedStateOf {
                items().map {
                    TextContextMenuItemWithComposableLeadingIcon(
                        key = it,
                        label = it.label,
                        enabled = it.enabled,
                        onClick = {
                            session.close()
                            it.onClick()
                        },
                    )
                }
            }
        }

        if (components.isEmpty()) {
            SideEffect { session.close() }
        } else {
            val colors = remember(backgroundColor, textColor, itemHoverColor, disabledTextColor) {
                ContextMenuColors(
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    iconColor = textColor,
                    disabledTextColor = disabledTextColor,
                    disabledIconColor = disabledTextColor,
                    hoverColor = itemHoverColor,
                )
            }
            DefaultOpenContextMenu(
                session = session,
                components = components,
                popupPositionProvider = rememberPopupPositionProviderAtPosition(status.rect.center),
                colors = colors,
            )
        }
    }
}
