package com.inspiredandroid.kai.desktop

import androidx.compose.ui.graphics.Color
import com.inspiredandroid.kai.Platform
import com.inspiredandroid.kai.currentPlatform
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val DWMWA_CAPTION_COLOR = 35

private interface DwmapiLib : Library {
    fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, pvAttribute: Pointer, cbAttribute: Int): Int
}

private val dwmapi: DwmapiLib? = runCatching {
    Native.load("dwmapi", DwmapiLib::class.java)
}.getOrNull()

internal fun applyDesktopWindowTheme(window: Window, isDark: Boolean, background: Color) {
    val awtBackground = java.awt.Color(
        (background.red * 255).toInt().coerceIn(0, 255),
        (background.green * 255).toInt().coerceIn(0, 255),
        (background.blue * 255).toInt().coerceIn(0, 255),
    )
    window.background = awtBackground
    if (currentPlatform !is Platform.Desktop.Windows) return
    val hwnd = windowHwnd(window)
    if (hwnd == 0L) return
    val api = dwmapi ?: return
    runCatching {
        val hwndPointer = Pointer(hwnd)
        val darkValue = IntByReference(if (isDark) 1 else 0)
        api.DwmSetWindowAttribute(hwndPointer, DWMWA_USE_IMMERSIVE_DARK_MODE, darkValue.pointer, 4)
        val captionBgr = (awtBackground.blue shl 16) or (awtBackground.green shl 8) or awtBackground.red
        val colorRef = IntByReference(captionBgr)
        api.DwmSetWindowAttribute(hwndPointer, DWMWA_CAPTION_COLOR, colorRef.pointer, 4)
    }
}

private fun windowHwnd(window: Window): Long = runCatching {
    val peerField = java.awt.Component::class.java.getDeclaredField("peer")
    peerField.isAccessible = true
    val peer = peerField.get(window)
    val hwndMethod = peer.javaClass.getMethod("getHWnd")
    (hwndMethod.invoke(peer) as Number).toLong()
}.getOrDefault(0L)
