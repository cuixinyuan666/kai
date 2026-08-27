package com.inspiredandroid.kai.speech

import com.inspiredandroid.kai.Platform
import com.inspiredandroid.kai.currentPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Windows 桌面语音转文字：使用 System.Speech（系统语音识别，支持已安装的中文/英文语言包）。
 */
internal class WindowsSpeechToText : SpeechToText {
    private val listening = AtomicBoolean(false)
    private var activeLanguageTag: String = "zh"

    override val isSupported: Boolean = currentPlatform is Platform.Desktop.Windows
    override val isListening: Boolean get() = listening.get()

    override suspend fun startListening(languageTag: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isSupported) return@withContext Result.failure(IllegalStateException("Speech not supported"))
        if (listening.get()) return@withContext Result.success(Unit)
        activeLanguageTag = languageTag
        listening.set(true)
        Result.success(Unit)
    }

    override suspend fun stopListening(): Result<String> = withContext(Dispatchers.IO) {
        if (!listening.get()) return@withContext Result.failure(IllegalStateException("Not listening"))
        listening.set(false)
        val culture = if (activeLanguageTag.startsWith("zh")) "zh-CN" else "en-US"
        val script = """
            [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
            ${'$'}OutputEncoding = [Console]::OutputEncoding
            Add-Type -AssemblyName System.Speech
            ${'$'}culture = New-Object System.Globalization.CultureInfo("$culture")
            ${'$'}recognizer = New-Object System.Speech.Recognition.SpeechRecognitionEngine(${'$'}culture)
            ${'$'}recognizer.SetInputToDefaultAudioDevice()
            ${'$'}result = ${'$'}recognizer.Recognize(TimeSpan.FromSeconds(15))
            if (${'$'}result -ne ${'$'}null) { ${'$'}result.Text } else { "" }
        """.trimIndent()
        try {
            val process = ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-OutputFormat",
                "Text",
                "-Command",
                script,
            )
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader(Charset.forName("UTF-8")).readText().trim()
            val code = process.waitFor()
            if (code != 0 && output.isBlank()) {
                Result.failure(IllegalStateException("语音识别失败"))
            } else {
                Result.success(output)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun cancel() {
        listening.set(false)
    }
}

actual fun createSpeechToText(): SpeechToText? =
    if (currentPlatform is Platform.Desktop.Windows) WindowsSpeechToText() else null
