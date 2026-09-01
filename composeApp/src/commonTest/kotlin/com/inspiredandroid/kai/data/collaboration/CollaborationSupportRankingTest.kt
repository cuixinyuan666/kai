package com.inspiredandroid.kai.data.collaboration

import kotlin.test.Test
import kotlin.test.assertEquals

class CollaborationSupportRankingTest {

    private val a = ModelRef("inst-a", "m1")
    private val b = ModelRef("inst-b", "m2")
    private val c = ModelRef("inst-c", "m3")
    private val scores = mapOf(
        a.key to 90.0,
        b.key to 50.0,
        c.key to 80.0,
    )

    @Test
    fun ranksByScoreDescendingThenKey() {
        val ranked = CollaborationSupport.rankSummaryCandidates(
            eligible = listOf(b, a, c),
            scoresByRefKey = scores,
        )
        assertEquals(listOf(a, c, b), ranked)
    }

    @Test
    fun preferredFirstThenRemainingByScore() {
        val ranked = CollaborationSupport.rankSummaryCandidates(
            eligible = listOf(a, b, c),
            scoresByRefKey = scores,
            preferredFirst = b,
        )
        assertEquals(listOf(b, a, c), ranked)
    }

    @Test
    fun ignoresPreferredOutsideEligible() {
        val ranked = CollaborationSupport.rankSummaryCandidates(
            eligible = listOf(a, c),
            scoresByRefKey = scores,
            preferredFirst = b,
        )
        assertEquals(listOf(a, c), ranked)
    }
}
