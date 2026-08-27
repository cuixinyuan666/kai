package com.inspiredandroid.kai

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.luminance
import com.inspiredandroid.kai.desktop.applyDesktopWindowTheme

@Composable
actual fun SyncPlatformWindowTheme() {
    val awtWindow = LocalAwtWindow.current ?: return
    val background = MaterialTheme.colorScheme.background
    val isDarkTitleBar = background.luminance() < 0.45f
    LaunchedEffect(background, isDarkTitleBar) {
        applyDesktopWindowTheme(awtWindow, isDarkTitleBar, background)
    }
}
