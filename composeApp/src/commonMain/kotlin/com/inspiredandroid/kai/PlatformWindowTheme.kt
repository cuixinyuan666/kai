package com.inspiredandroid.kai

import androidx.compose.runtime.Composable

/** 将 Compose 主题同步到原生窗口（标题栏、背景等）。桌面端为实际实现，其他平台为空操作。 */
@Composable
expect fun SyncPlatformWindowTheme()

/** Windows 无边框窗口上绘制与当前主题同色的标题栏；其他平台为空。 */
@Composable
expect fun PlatformTitleBar()
