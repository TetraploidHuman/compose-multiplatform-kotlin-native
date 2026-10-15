/*
 * Copyright 2023 The Android Open Source Project
 * Copyright 2026 The ComposeKN Authors (linuxX64 / mingw port)
 *
 * DialogModalityType — 对齐 Desktop；无 AWT owner 树时 DocumentModal ≈ ApplicationModal。
 */

package androidx.compose.ui.window

import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Modal dialogs block all input to some windows.
 *
 * [DialogModalityType] defines which set of windows input is blocked for.
 *
 * ComposeKN note: without an AWT ownership hierarchy, [DocumentModal] and
 * [ApplicationModal] both soft-block every other application window; [Modeless]
 * does not register as a dialog for host modality.
 */
@ExperimentalComposeUiApi
class DialogModalityType private constructor(val name: String) {
    override fun toString() = name

    companion object {
        /** Non-modal: does not block other windows. */
        val Modeless = DialogModalityType("Modeless")

        /**
         * Block other windows from the same document (Desktop: top-level without owner).
         * On ComposeKN this soft-blocks all other windows in the process.
         */
        val DocumentModal = DialogModalityType("Document")

        /**
         * Block all other windows in the application.
         * On ComposeKN this matches [DocumentModal] (soft-block all other windows).
         */
        val ApplicationModal = DialogModalityType("Application")
    }
}

/** Soft-modal registration for hosts (`isDialog` on create params). */
@ExperimentalComposeUiApi
internal fun DialogModalityType.blocksOtherWindows(): Boolean =
    this != DialogModalityType.Modeless
