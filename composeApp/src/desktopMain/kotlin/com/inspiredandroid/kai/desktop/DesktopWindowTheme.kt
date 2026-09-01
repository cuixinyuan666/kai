package com.inspiredandroid.kai.desktop

import androidx.compose.ui.graphics.Color
import com.inspiredandroid.kai.Platform
import com.inspiredandroid.kai.currentPlatform
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

private const val DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19
private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val DWMWA_BORDER_COLOR = 34
private const val DWMWA_CAPTION_COLOR = 35
private const val DWMWA_TEXT_COLOR = 36

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
    if (currentPlatform !is Platform.Desktop.Windows) {
        return
    }
    val hwnd = windowHwnd(window)
    if (hwnd == 0L) {
        return
    }
    val api = dwmapi
    if (api == null) {
        return
    }
    runCatching {
        val hwndPointer = Pointer(hwnd)
        val darkValue = IntByReference(if (isDark) 1 else 0)
        api.DwmSetWindowAttribute(
            hwndPointer,
            DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1,
            darkValue.pointer,
            4,
        )
        api.DwmSetWindowAttribute(
            hwndPointer,
            DWMWA_USE_IMMERSIVE_DARK_MODE,
            darkValue.pointer,
            4,
        )
        val captionBgr = (awtBackground.blue shl 16) or (awtBackground.green shl 8) or awtBackground.red
        val colorRef = IntByReference(captionBgr)
        api.DwmSetWindowAttribute(hwndPointer, DWMWA_CAPTION_COLOR, colorRef.pointer, 4)
        api.DwmSetWindowAttribute(hwndPointer, DWMWA_BORDER_COLOR, colorRef.pointer, 4)
        val textBgr = if (isDark) 0x00FFFFFF else 0x00000000
        val textRef = IntByReference(textBgr)
        api.DwmSetWindowAttribute(hwndPointer, DWMWA_TEXT_COLOR, textRef.pointer, 4)
    }
}

private fun windowHwnd(window: Window): Long {
    runCatching { Native.getComponentID(window) }.getOrNull()?.takeIf { it != 0L }?.let { return it }
    return runCatching {
        val peerField = java.awt.Component::class.java.getDeclaredField("peer")
        peerField.isAccessible = true
        val peer = peerField.get(window) ?: return@runCatching 0L
        val hwndMethod = peer.javaClass.methods.firstOrNull { it.name.equals("getHWnd", ignoreCase = true) }
            ?: return@runCatching 0L
        (hwndMethod.invoke(peer) as Number).toLong()
    }.getOrDefault(0L)
}
