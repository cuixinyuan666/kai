package com.inspiredandroid.kai.speech

object SpeechLanguage {

    fun resolve(existingText: String, preferred: String): String {
        val hasCjk = existingText.any { it in '\u4e00'..'\u9fff' }
        val latinLetters = existingText.count { it in 'A'..'Z' || it in 'a'..'z' }
        val hasLatinWord = latinLetters >= 4 && !hasCjk
        return when {
            hasCjk -> "zh"
            hasLatinWord -> "en"
            preferred.equals("en", ignoreCase = true) -> "en"
            else -> "zh"
        }
    }

    fun cycle(current: String): String = if (current.startsWith("zh")) "en" else "zh"

    fun label(languageTag: String): String = if (languageTag.startsWith("zh")) "中" else "EN"

    fun normalizeTranscript(text: String, languageTag: String): String {
        val collapsed = text.trim()
            .replace("\uFFFD", "")
            .replace(Regex("\\s+"), " ")
        if (!languageTag.startsWith("zh")) return collapsed
        return collapsed.replace(Regex("(?<=[\\u4e00-\\u9fff])\\s+(?=[\\u4e00-\\u9fff])"), "")
    }

    fun looksMismatched(text: String, languageTag: String): Boolean {
        if (text.isBlank()) return true
        val replacement = text.count { it == '\uFFFD' }
        if (replacement > 0) return true
        val cjk = text.count { it in '\u4e00'..'\u9fff' }
        val latin = text.count { it in 'A'..'Z' || it in 'a'..'z' }
        return if (languageTag.startsWith("zh")) {
            cjk == 0 && latin >= 3
        } else {
            cjk >= 2 && latin < 3
        }
    }

    fun stats(text: String): Map<String, String> {
        val cjk = text.count { it in '\u4e00'..'\u9fff' }
        val latin = text.count { it in 'A'..'Z' || it in 'a'..'z' }
        val replacement = text.count { it == '\uFFFD' }
        val codes = text.take(6).map { it.code.toString(16) }.joinToString(",")
        return mapOf(
            "len" to text.length.toString(),
            "cjk" to cjk.toString(),
            "latin" to latin.toString(),
            "fffd" to replacement.toString(),
            "headCp" to codes,
        )
    }
}
