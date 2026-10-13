/*
 * Copyright 2021 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * 声明式 Window：DisposableEffect 创建/销毁原生窗；系统关窗只调 onCloseRequest。
 */

@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.isSpecified
import kotlinx.coroutines.awaitCancellation

/**
 * Composes a platform window. Entering composition creates the native window;
 * leaving composition disposes it.
 *
 * 系统关窗（WM_CLOSE）**不会**销毁 HWND，只调用 [onCloseRequest]
 *（对齐 AWT DO_NOTHING_ON_CLOSE）。真正销毁发生在离开 composition 时。
 *
 * 签名对齐 Desktop（`icon` / `transparent` / `enabled` / `focusable` /
 * `onPreviewKeyEvent` / `onKeyEvent` / [WindowDecoration]）。
 */
@Composable
fun Window(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit,
) {
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        decoration = windowDecorationFromFlag(undecorated),
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content,
    )
}

/**
 * [Window] with explicit [WindowDecoration]（对齐 Desktop 1.12）。
 */
@Composable
fun Window(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    decoration: WindowDecoration,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit,
) {
    val undecorated = decoration.isUndecorated
    val backend = remember { ComposeNativeWindowBackendRegistry.requireBackend() }
    val handle = remember {
        backend.createWindow(
            ComposeNativeWindowCreateParams(
                title = title,
                size = state.size,
                position = state.position,
                placement = state.placement,
                isMinimized = state.isMinimized,
                undecorated = undecorated,
                resizable = resizable,
                alwaysOnTop = alwaysOnTop,
                enabled = enabled,
                focusable = focusable,
                transparent = transparent,
                onCloseRequest = onCloseRequest,
            ),
        )
    }

    val scope = remember(handle) {
        object : FrameWindowScope {
            override val window: ComposeNativeWindowHandle get() = handle
        }
    }

    val latestContent = rememberUpdatedState(content)
    val latestOnClose = rememberUpdatedState(onCloseRequest)
    val latestPreview = rememberUpdatedState(onPreviewKeyEvent)
    val latestKey = rememberUpdatedState(onKeyEvent)
    val currentState = rememberUpdatedState(state)

    val appliedState = remember {
        object {
            var size = state.size
            var position = state.position
            var placement = state.placement
            var isMinimized = state.isMinimized
        }
    }

    LaunchedEffect(handle) {
        awaitCancellation()
    }

    DisposableEffect(handle) {
        handle.setGeometryListener { snap ->
            val s = currentState.value
            s.size = snap.size
            s.position = snap.position
            s.placement = snap.placement
            s.isMinimized = snap.isMinimized
            appliedState.size = snap.size
            appliedState.position = snap.position
            appliedState.placement = snap.placement
            appliedState.isMinimized = snap.isMinimized
        }
        onDispose {
            handle.setGeometryListener(null)
        }
    }

    SideEffect {
        handle.setTitle(title)
        handle.setResizable(resizable)
        handle.setAlwaysOnTop(alwaysOnTop)
        handle.setEnabled(enabled)
        handle.setFocusable(focusable)
        handle.setTransparent(transparent)
        handle.setIcon(icon)
        handle.setVisible(visible)
        handle.setWindowKeyEventHandlers(
            onPreviewKeyEvent = { latestPreview.value(it) },
            onKeyEvent = { latestKey.value(it) },
        )
        handle.setOnCloseRequest { latestOnClose.value() }

        if (state.placement != appliedState.placement ||
            state.isMinimized != appliedState.isMinimized
        ) {
            handle.applyPlacement(state.placement, state.isMinimized)
            appliedState.placement = state.placement
            appliedState.isMinimized = state.isMinimized
        }
        if (state.size != appliedState.size &&
            state.size.width.isSpecified &&
            state.size.height.isSpecified
        ) {
            handle.applySize(state.size)
            appliedState.size = state.size
        }
        if (state.position != appliedState.position) {
            when (val pos = state.position) {
                is WindowPosition.Absolute -> handle.applyPosition(pos)
                is WindowPosition.Aligned -> handle.applyPosition(pos)
                WindowPosition.PlatformDefault -> Unit
            }
            appliedState.position = state.position
        }
    }

    DisposableEffect(handle) {
        handle.setContent {
            latestContent.value.invoke(scope)
        }
        onDispose {
            handle.dispose()
        }
    }
}
