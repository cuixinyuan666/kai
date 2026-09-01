package com.inspiredandroid.kai.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener
import java.util.Collections
import java.util.WeakHashMap

private val guardedWindows: MutableSet<Window> = Collections.synchronizedSet(
    Collections.newSetFromMap(WeakHashMap()),
)

internal fun workingAreaBounds(window: Window): Rectangle {
    val gc = window.graphicsConfiguration
    val screen = gc.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
    return Rectangle(
        screen.x + insets.left,
        screen.y + insets.top,
        screen.width - insets.left - insets.right,
        screen.height - insets.top - insets.bottom,
    )
}

internal fun isFittedToWorkingArea(window: Window, slop: Int = 8): Boolean {
    val working = workingAreaBounds(window)
    val b = window.bounds
    return kotlin.math.abs(b.x - working.x) <= slop &&
        kotlin.math.abs(b.y - working.y) <= slop &&
        kotlin.math.abs(b.width - working.width) <= slop &&
        kotlin.math.abs(b.height - working.height) <= slop
}

internal fun applyWorkingAreaMaximize(window: Window, windowState: WindowState, density: Float) {
    val working = workingAreaBounds(window)
    (window as? Frame)?.extendedState = Frame.NORMAL
    windowState.placement = WindowPlacement.Floating
    val safeDensity = density.takeIf { it > 0.01f } ?: 1f
    windowState.position = WindowPosition((working.x / safeDensity).dp, (working.y / safeDensity).dp)
    windowState.size = DpSize((working.width / safeDensity).dp, (working.height / safeDensity).dp)
    window.bounds = working
}

internal fun restoreWindowBounds(
    window: Window,
    windowState: WindowState,
    restore: Rectangle,
    density: Float,
) {
    (window as? Frame)?.extendedState = Frame.NORMAL
    windowState.placement = WindowPlacement.Floating
    val safeDensity = density.takeIf { it > 0.01f } ?: 1f
    windowState.position = WindowPosition((restore.x / safeDensity).dp, (restore.y / safeDensity).dp)
    windowState.size = DpSize((restore.width / safeDensity).dp, (restore.height / safeDensity).dp)
    window.bounds = restore
}

internal fun installWorkingAreaMaximizeGuard(window: Window, windowState: WindowState, density: () -> Float) {
    if (!guardedWindows.add(window)) return
    window.addWindowStateListener(
        WindowStateListener { event: WindowEvent ->
            val frame = window as? Frame ?: return@WindowStateListener
            if (event.newState and Frame.MAXIMIZED_BOTH == Frame.MAXIMIZED_BOTH) {
                applyWorkingAreaMaximize(window, windowState, density())
            }
        },
    )
}
