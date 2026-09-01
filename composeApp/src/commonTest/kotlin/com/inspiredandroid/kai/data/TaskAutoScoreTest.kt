package com.inspiredandroid.kai.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskAutoScoreTest {

    @Test
    fun `failed or empty response scores zero`() {
        val failed = TaskAutoScore.compute("k", "L", "s", null, 1000, 1, failed = true)
        assertEquals(0.0, failed.totalScore)
        val empty = TaskAutoScore.compute("k", "L", "s", "  ", 1000, 1, failed = false)
        assertEquals(0.0, empty.totalScore)
    }

    @Test
    fun `successful long fast answer scores high and keeps user-score fields default`() {
        val text = "这是一段完整回答。\n".repeat(20)
        val score = TaskAutoScore.compute("k", "L", "s", text, elapsedMs = 800, attempts = 1, failed = false)
        assertTrue(score.totalScore > 70.0)
        assertEquals(100.0, score.completion)
        assertEquals(100.0, score.stability)
        assertTrue(score.quality >= 90.0)
        assertEquals(false, score.isUserScore)
    }

    @Test
    fun `retries lower stability`() {
        val text = "完整结构回答。\n代码如下:\n```kotlin\nval x = 1\n```"
        val once = TaskAutoScore.compute("k", "L", "s", text, 1200, 1, false)
        val retried = TaskAutoScore.compute("k", "L", "s", text, 1200, 3, false)
        assertTrue(retried.stability < once.stability)
    }
}
