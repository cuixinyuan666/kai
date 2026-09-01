package com.inspiredandroid.kai.data

import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 任务结束后的自动打分。用户自定义分数（[ModelBenchmark.isUserScore]）优先，自动分不会覆盖。
 *
 * 维度：字数/耗时比值、任务完成度、运行稳定性、回答质量。
 */
object TaskAutoScore {

    fun compute(
        modelKey: String,
        modelLabel: String,
        serviceId: String,
        response: String?,
        elapsedMs: Long,
        attempts: Int,
        failed: Boolean,
    ): ModelBenchmark {
        val text = response.orEmpty().trim()
        val charCount = text.length
        if (failed || charCount <= 0) {
            return ModelBenchmark(
                modelKey = modelKey,
                modelLabel = modelLabel,
                serviceId = serviceId,
                totalScore = 0.0,
                completion = 0.0,
                speed = 0.0,
                responseSpeed = 0.0,
                wordCount = 0.0,
                stability = 0.0,
                quality = 0.0,
                elapsedMs = elapsedMs.coerceAtLeast(0L),
                charCount = 0,
                testedAt = nowMs(),
            )
        }
        val completion = 100.0
        val speed = ((60_000 - elapsedMs).toDouble() / 60_000).coerceIn(0.0, 1.0) * 100.0
        val charsPerSec = if (elapsedMs > 0) charCount.toDouble() / (elapsedMs / 1000.0) else 0.0
        val responseSpeed = (charsPerSec / 50.0).coerceIn(0.0, 1.0) * 100.0
        val wordCount = (charCount / 500.0).coerceIn(0.0, 1.0) * 100.0
        val stability = when {
            attempts <= 1 -> 100.0
            attempts == 2 -> 70.0
            else -> (100.0 / attempts).coerceIn(20.0, 55.0)
        }
        val quality = qualityScore(text)
        val weights = ModelBenchmark.WEIGHTS
        val total = completion * (weights["completion"] ?: 0.0) +
            responseSpeed * (weights["responseSpeed"] ?: 0.0) +
            stability * (weights["stability"] ?: 0.0) +
            quality * (weights["quality"] ?: 0.0)
        return ModelBenchmark(
            modelKey = modelKey,
            modelLabel = modelLabel,
            serviceId = serviceId,
            totalScore = total.coerceIn(0.0, 100.0),
            completion = completion,
            speed = speed,
            responseSpeed = responseSpeed,
            wordCount = wordCount,
            stability = stability,
            quality = quality,
            elapsedMs = elapsedMs.coerceAtLeast(0L),
            charCount = charCount,
            testedAt = nowMs(),
        )
    }

    fun record(
        repository: DataRepository,
        serviceId: String,
        modelId: String,
        modelLabel: String,
        response: String?,
        elapsedMs: Long,
        attempts: Int = 1,
        failed: Boolean = false,
    ) {
        val key = if (serviceId.isNotBlank()) "$serviceId::$modelId" else modelId
        repository.upsertModelBenchmark(
            compute(
                modelKey = key,
                modelLabel = modelLabel.ifBlank { modelId },
                serviceId = serviceId,
                response = response,
                elapsedMs = elapsedMs,
                attempts = attempts.coerceAtLeast(1),
                failed = failed,
            ),
        )
    }

    fun qualityScore(text: String): Double {
        val t = text.trim()
        if (t.isEmpty()) return 0.0
        val lower = t.lowercase()
        if (t.length < 80 && (lower.contains("error") || lower.contains("失败") || lower.contains("exception"))) {
            return 20.0
        }
        val lengthScore = when {
            t.length < 20 -> 30.0
            t.length < 80 -> 55.0
            t.length <= 4000 -> 90.0
            else -> 72.0
        }
        val structureBonus = when {
            t.contains("```") || t.contains('\n') -> 10.0
            t.any { it == '。' || it == '.' || it == '!' || it == '？' || it == '?' } -> 6.0
            else -> 0.0
        }
        return (lengthScore + structureBonus).coerceIn(0.0, 100.0)
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
}

fun ModelBenchmark.displayTotal(): String = "${totalScore.roundToInt()}"
