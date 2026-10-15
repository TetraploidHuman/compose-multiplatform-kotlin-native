/*
 * Copyright 2021 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * 声明式 DialogWindow：独立原生窗 + 软模态（对齐 Desktop DialogModalityType）。
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
 * Composes a platform dialog window. Entering composition creates a native window;
 * leaving composition disposes it.
 *
 * Soft modality defaults to [DialogModalityType.DocumentModal] (blocks other windows).
 */
@Composable
fun DialogWindow(
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
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable DialogWindowScope.() -> Unit,
) {
    DialogWindow(
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
        modalityType = DialogModalityType.DocumentModal,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content,
    )
}

@Composable
fun DialogWindow(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
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
    content: @Composable DialogWindowScope.() -> Unit,
) {
    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        decoration = decoration,
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        modalityType = DialogModalityType.DocumentModal,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content,
    )
}

/**
 * [DialogWindow] with explicit [DialogModalityType]（对齐 Desktop 1.12 Experimental API）.
 *
 * ComposeKN: [DialogModalityType.Modeless] → no soft-modal; Document/Application →
 * soft-block other windows (`isDialog=true`).
 */
@Composable
fun DialogWindow(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    decoration: WindowDecoration,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    modalityType: DialogModalityType,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable DialogWindowScope.() -> Unit,
) {
    val undecorated = decoration.isUndecorated
    val backend = remember { ComposeNativeWindowBackendRegistry.requireBackend() }
    val isModal = modalityType.blocksOtherWindows()
    val handle = remember {
        backend.createWindow(
            ComposeNativeWindowCreateParams(
                title = title,
                size = state.size,
                position = state.position,
                undecorated = undecorated,
                resizable = resizable,
                alwaysOnTop = alwaysOnTop,
                enabled = enabled,
                focusable = focusable,
                transparent = transparent,
                isDialog = isModal,
                onCloseRequest = onCloseRequest,
            ),
        )
    }

    val scope = remember(handle) {
        object : DialogWindowScope {
            override val window: ComposeNativeWindowHandle get() = handle
        }
    }

    val latestContent = rememberUpdatedState(content)
    val latestOnClose = rememberUpdatedState(onCloseRequest)
    val latestPreview = rememberUpdatedState(onPreviewKeyEvent)
    val latestKey = rememberUpdatedState(onKeyEvent)
    val currentState = rememberUpdatedState(state)
    val exceptionHandlerFactory = LocalWindowExceptionHandlerFactory.current

    val appliedState = remember {
        object {
            var size = state.size
            var position = state.position
        }
    }

    val observedSize = state.size
    val observedPosition = state.position

    LaunchedEffect(handle) {
        awaitCancellation()
    }

    DisposableEffect(handle) {
        handle.setGeometryListener { snap ->
            val s = currentState.value
            s.size = snap.size
            s.position = snap.position
            appliedState.size = snap.size
            appliedState.position = snap.position
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
        handle.setResizerThicknessDp(decoration.resizerThickness.value)
        handle.setDialogModality(isModal)
        handle.setExceptionHandler(exceptionHandlerFactory.exceptionHandler(handle))
        handle.setWindowKeyEventHandlers(
            onPreviewKeyEvent = { latestPreview.value(it) },
            onKeyEvent = { latestKey.value(it) },
        )
        handle.setOnCloseRequest { latestOnClose.value() }

        if (observedSize != appliedState.size &&
            observedSize.width.isSpecified &&
            observedSize.height.isSpecified
        ) {
            handle.applySize(observedSize)
            appliedState.size = observedSize
        }
        if (observedPosition != appliedState.position) {
            when (val pos = observedPosition) {
                is WindowPosition.Absolute -> handle.applyPosition(pos)
                is WindowPosition.Aligned -> handle.applyPosition(pos)
                WindowPosition.PlatformDefault -> Unit
            }
            appliedState.position = observedPosition
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
