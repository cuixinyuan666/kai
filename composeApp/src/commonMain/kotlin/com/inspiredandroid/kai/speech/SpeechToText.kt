package com.inspiredandroid.kai.speech

/**
 * 语音转文字（桌面端实现；其他平台可为 null）。
 */
interface SpeechToText {
    val isSupported: Boolean
    val isListening: Boolean

    /** 开始录音识别；[languageTag] 为 zh 或 en */
    suspend fun startListening(languageTag: String): Result<Unit>

    /** 停止并返回识别文本 */
    suspend fun stopListening(): Result<String>

    fun cancel()
}

/** 平台语音转文字；桌面 Windows 可用，其他平台返回 null。 */
expect fun createSpeechToText(): SpeechToText?
