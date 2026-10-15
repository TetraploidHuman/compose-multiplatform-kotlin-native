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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent

/**
 * Deprecated alias of [DialogWindow] (Desktop migration compatibility).
 *
 * Swing/AWT `Dialog(create/dispose/…)` overloads are intentionally omitted on Kotlin/Native.
 */
@Deprecated(
    message = "Replaced by DialogWindow",
    replaceWith = ReplaceWith(
        "DialogWindow(onCloseRequest, state, visible, title, " +
            "icon, undecorated, transparent, resizable, enabled, focusable, " +
            "onPreviewKeyEvent, onKeyEvent, content)",
    ),
)
@Composable
fun Dialog(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable DialogWindowScope.() -> Unit,
) = DialogWindow(
    onCloseRequest = onCloseRequest,
    state = state,
    visible = visible,
    title = title,
    icon = icon,
    undecorated = undecorated,
    transparent = transparent,
    resizable = resizable,
    enabled = enabled,
    focusable = focusable,
    alwaysOnTop = false,
    onPreviewKeyEvent = onPreviewKeyEvent,
    onKeyEvent = onKeyEvent,
    content = content,
)
