package com.inspiredandroid.kai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun ProvideAwtWindow(window: java.awt.Window, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAwtWindow provides window) {
        content()
    }
}
