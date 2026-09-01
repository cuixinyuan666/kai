package com.inspiredandroid.kai

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import java.awt.Window

val LocalAwtWindow = staticCompositionLocalOf<Window?> { null }
val LocalDesktopWindowScope = staticCompositionLocalOf<WindowScope?> { null }
val LocalDesktopWindowState = staticCompositionLocalOf<WindowState?> { null }
val LocalDesktopExitApp = staticCompositionLocalOf<(() -> Unit)?> { null }
