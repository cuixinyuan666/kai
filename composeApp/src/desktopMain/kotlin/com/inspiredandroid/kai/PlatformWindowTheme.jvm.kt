@file:OptIn(ExperimentalFoundationApi::class)

package com.inspiredandroid.kai

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.inspiredandroid.kai.desktop.applyDesktopWindowTheme
import com.inspiredandroid.kai.desktop.applyWorkingAreaMaximize
import com.inspiredandroid.kai.desktop.installWorkingAreaMaximizeGuard
import com.inspiredandroid.kai.desktop.isFittedToWorkingArea
import com.inspiredandroid.kai.desktop.restoreWindowBounds
import com.inspiredandroid.kai.ui.handCursor
import java.awt.Frame
import java.awt.Rectangle

@Composable
actual fun SyncPlatformWindowTheme() {
    val awtWindow = LocalAwtWindow.current
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val isDarkTitleBar = background.luminance() < 0.45f
    LaunchedEffect(background, isDarkTitleBar, awtWindow) {
        val window = awtWindow ?: return@LaunchedEffect
        (window as? Frame)?.title = "Cui"
        applyDesktopWindowTheme(window, isDarkTitleBar, background)
    }
}

@Composable
actual fun PlatformTitleBar() {
    if (currentPlatform !is Platform.Desktop.Windows) return
    val scope = LocalDesktopWindowScope.current ?: return
    val windowState = LocalDesktopWindowState.current ?: return
    val onClose = LocalDesktopExitApp.current ?: return
    scope.DesktopTitleBar(windowState, onClose)
}

@Composable
private fun WindowScope.DesktopTitleBar(windowState: WindowState, onClose: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val content = MaterialTheme.colorScheme.onBackground
    val density = LocalDensity.current.density
    var restoreBounds by remember { mutableStateOf<Rectangle?>(null) }
    LaunchedEffect(window) {
        installWorkingAreaMaximizeGuard(window, windowState) { density }
    }
    WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(background)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cui",
                style = MaterialTheme.typography.labelLarge,
                color = content,
            )
            Spacer(Modifier.weight(1f))
            TitleBarButton(
                onClick = { windowState.isMinimized = true },
                contentColor = content,
            ) {
                Icon(
                    imageVector = Icons.Filled.Minimize,
                    contentDescription = "最小化",
                    modifier = Modifier.size(16.dp),
                )
            }
            TitleBarButton(
                onClick = {
                    if (isFittedToWorkingArea(window)) {
                        val fallback = Rectangle(
                            80,
                            80,
                            (1280 * density).toInt().coerceAtLeast(640),
                            (800 * density).toInt().coerceAtLeast(480),
                        )
                        restoreWindowBounds(window, windowState, restoreBounds ?: fallback, density)
                    } else {
                        restoreBounds = Rectangle(window.bounds)
                        applyWorkingAreaMaximize(window, windowState, density)
                    }
                },
                contentColor = content,
            ) {
                Icon(
                    imageVector = Icons.Filled.CropSquare,
                    contentDescription = "最大化",
                    modifier = Modifier.size(14.dp),
                )
            }
            TitleBarButton(
                onClick = onClose,
                contentColor = content,
                hoverContainer = Color(0xFFE81123),
                hoverContent = Color.White,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TitleBarButton(
    onClick: () -> Unit,
    contentColor: Color,
    hoverContainer: Color = contentColor.copy(alpha = 0.12f),
    hoverContent: Color = contentColor,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    IconButton(
        onClick = onClick,
        modifier = Modifier.width(46.dp).height(36.dp).handCursor(),
        interactionSource = interaction,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (hovered) hoverContent else contentColor,
            containerColor = if (hovered) hoverContainer else Color.Transparent,
        ),
    ) {
        Box { content() }
    }
}
