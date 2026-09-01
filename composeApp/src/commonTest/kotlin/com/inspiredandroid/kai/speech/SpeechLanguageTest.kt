package com.inspiredandroid.kai.speech

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechLanguageTest {

    @Test
    fun `looksMismatched treats replacement chars as garbled`() {
        assertTrue(SpeechLanguage.looksMismatched("你好\uFFFD", "zh"))
        assertTrue(SpeechLanguage.looksMismatched("hello\uFFFD", "en"))
    }

    @Test
    fun `looksMismatched detects wrong script for selected language`() {
        assertTrue(SpeechLanguage.looksMismatched("hello there", "zh"))
        assertTrue(SpeechLanguage.looksMismatched("你好世界", "en"))
        assertFalse(SpeechLanguage.looksMismatched("你好世界", "zh"))
        assertFalse(SpeechLanguage.looksMismatched("hello there", "en"))
    }

    @Test
    fun `normalizeTranscript strips replacement and chinese gaps`() {
        val zh = SpeechLanguage.normalizeTranscript("你 好\uFFFD 世 界", "zh")
        assertTrue(" " !in zh)
        assertFalse("\uFFFD" in zh)
        assertTrue(zh.contains("你好"))
    }
}
