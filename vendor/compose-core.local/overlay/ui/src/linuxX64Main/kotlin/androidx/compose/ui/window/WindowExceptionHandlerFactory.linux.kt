/*
 * Copyright 2023 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * WindowExceptionHandlerFactory — Desktop 用 java.awt.Window；此处用 ComposeNativeWindowHandle。
 */

package androidx.compose.ui.window

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Default [WindowExceptionHandlerFactory]: print the error, request window close, rethrow.
 *
 * Desktop shows a Swing error dialog; ComposeKN has no Swing, so stderr + close request.
 */
@ExperimentalComposeUiApi
object DefaultWindowExceptionHandlerFactory : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: ComposeNativeWindowHandle) = WindowExceptionHandler { throwable ->
        val message = throwable.message ?: throwable::class.simpleName ?: "Unknown error"
        println("composekn: WindowExceptionHandler: $message")
        throwable.printStackTrace()
        // Desktop schedules a close via Swing invokeLater; we rethrow synchronously.
        // Custom factories may call window.dispose() / setOnCloseRequest as needed.
        throw throwable
    }
}

/**
 * Factory of window exception handlers.
 * These handlers catch exceptions during rendering frames, handling events, or processing
 * background Compose operations.
 */
@ExperimentalComposeUiApi
fun interface WindowExceptionHandlerFactory {
    /**
     * Create an exception handler for [window]. Handlers run on the UI thread.
     *
     * Desktop takes `java.awt.Window`; ComposeKN takes [ComposeNativeWindowHandle].
     */
    fun exceptionHandler(window: ComposeNativeWindowHandle): WindowExceptionHandler
}

/**
 * CompositionLocal that provides [WindowExceptionHandlerFactory].
 */
@ExperimentalComposeUiApi
val LocalWindowExceptionHandlerFactory = staticCompositionLocalOf<WindowExceptionHandlerFactory> {
    DefaultWindowExceptionHandlerFactory
}
