/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.jetbrains.skiko.currentSystemTheme

/**
 * Provides [LocalSystemTheme] from Skiko [currentSystemTheme].
 * When [ComposeUiFlags.pollSystemTheme] is true, re-reads about once per second.
 *
 * Public for ComposeKN hosts / [androidx.compose.ui.window.Window] (Desktop keeps this
 * internal to the scene mediator).
 */
@InternalComposeUiApi
@Composable
fun ProvideSystemTheme(content: @Composable () -> Unit) {
    val themeState = remember {
        mutableStateOf(currentSystemTheme)
    }
    if (ComposeUiFlags.pollSystemTheme) {
        LaunchedEffect(Unit) {
            while (true) {
                themeState.value = currentSystemTheme
                delay(1.seconds)
            }
        }
    }
    CompositionLocalProvider(
        @Suppress("DEPRECATION")
        LocalSystemTheme provides themeState.value.asComposeSystemTheme(),
        content = content,
    )
}
