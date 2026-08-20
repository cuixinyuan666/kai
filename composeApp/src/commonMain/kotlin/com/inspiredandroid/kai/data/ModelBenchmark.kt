package com.inspiredandroid.kai.data

import kotlinx.serialization.Serializable

/**
 * 一键测试所有大模型的结果（0..100 分制）。
 *
 * 打分项（权重见 [WEIGHTS]）：
 * - [completion] 完成度：是否成功返回非空响应（0 或 100）
 * - [speed] 速度：总耗时越短分越高
 * - [responseSpeed] 响应速度：字符产出速率（字符/秒）越高分越高
 * - [wordCount] 字数：回复字数越多分越高（封顶）
 */
@Serializable
data class ModelBenchmark(
    val modelKey: String = "",
    val modelLabel: String = "",
    val serviceId: String = "",
    /** 0..100 加权总分 */
    val totalScore: Double = 0.0,
    val completion: Double = 0.0,
    val speed: Double = 0.0,
    val responseSpeed: Double = 0.0,
    val wordCount: Double = 0.0,
    /** 响应耗时毫秒 */
    val elapsedMs: Long = 0L,
    /** 响应字符数 */
    val charCount: Int = 0,
    val testedAt: Long = 0L,
) {
    companion object {
        /** 各打分项权重（总和 1.0） */
        val WEIGHTS: Map<String, Double> = mapOf(
            "completion" to 0.35,
            "speed" to 0.20,
            "responseSpeed" to 0.20,
            "wordCount" to 0.25,
        )
    }
}

/** 供 UI 层读取分数的便捷函数 */
fun ModelBenchmark?.totalScoreOrNull(): Double? = this?.totalScore

/** 按总分大小给分数上色：高分绿、中分黄、低分红 */
fun benchmarkScoreColor(score: Double, isLightTheme: Boolean = true): Long = when {
    score >= 80.0 -> 0xFF2E7D32   // green
    score >= 60.0 -> 0xFF558B2F   // light green
    score >= 40.0 -> 0xFFF9A825   // amber
    else -> 0xFFC62828            // red
}
