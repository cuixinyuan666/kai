package com.inspiredandroid.kai.data.war

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WarAnalysisParserTest {

    @Test
    fun `parseAnalysis handles plain JSON`() {
        val raw = """
            {"commonPoints":["都同意使用 Kotlin"],"aspects":[{"id":"a1","title":"方面1：架构","description":"A 主张微服务，B 主张单体"}]}
        """.trimIndent()
        val result = WarAnalysisParser.parseAnalysis(raw)
        assertNotNull(result)
        assertEquals(listOf("都同意使用 Kotlin"), result.commonPoints)
        assertEquals(1, result.aspects.size)
        assertEquals("a1", result.aspects[0].id)
    }

    @Test
    fun `parseAnalysis strips markdown fence`() {
        val raw = """
            ```json
            {"commonPoints":[],"aspects":[{"id":"a1","title":"T","description":"D"}]}
            ```
        """.trimIndent()
        val result = WarAnalysisParser.parseAnalysis(raw)
        assertNotNull(result)
        assertEquals(1, result.aspects.size)
    }

    @Test
    fun `parseVotes maps agree and disagree`() {
        val aspects = listOf(
            WarAspect("a1", "方面1", "描述1"),
            WarAspect("a2", "方面2", "描述2"),
        )
        val raw = """
            {"votes":[{"aspectId":"a1","agree":true,"reason":"合理"},{"aspectId":"a2","agree":false,"reason":"不认同"}]}
        """.trimIndent()
        val votes = WarAnalysisParser.parseVotes(raw, aspects, "svc::m1", "Model A")
        assertEquals(2, votes.size)
        assertEquals(WarVoteChoice.AGREE.name, votes[0].choice)
        assertEquals(WarVoteChoice.DISAGREE.name, votes[1].choice)
    }

    @Test
    fun `parseVotes marks missing entries as abstain`() {
        val aspects = listOf(WarAspect("a1", "方面1", "描述1"))
        val votes = WarAnalysisParser.parseVotes("{}", aspects, "k", "L")
        assertEquals(WarVoteChoice.ABSTAIN.name, votes.single().choice)
    }

    @Test
    fun `aggregateAspectResults groups by aspect index`() {
        val aspects = listOf(
            WarAspect("a1", "方面1", "D1"),
            WarAspect("a2", "方面2", "D2"),
        )
        val allVotes = listOf(
            listOf(
                WarModelVote("m1", "M1", WarVoteChoice.AGREE.name),
                WarModelVote("m1", "M1", WarVoteChoice.DISAGREE.name),
            ),
            listOf(
                WarModelVote("m2", "M2", WarVoteChoice.DISAGREE.name),
                WarModelVote("m2", "M2", WarVoteChoice.AGREE.name),
            ),
        )
        val results = WarAnalysisParser.aggregateAspectResults(aspects, allVotes)
        assertEquals(2, results.size)
        assertEquals(1, results[0].agreeCount)
        assertEquals(1, results[0].disagreeCount)
        assertEquals(2, results[0].validVoteCount)
    }

    @Test
    fun `copy formatter includes vote stats`() {
        val result = WarTaskResult(
            question = "任务A",
            commonPoints = listOf("相同"),
            aspectResults = listOf(
                WarAspectResult(
                    aspect = WarAspect("a1", "方面1", "描述"),
                    votes = listOf(
                        WarModelVote("m1", "M1", WarVoteChoice.AGREE.name, "ok"),
                        WarModelVote("m2", "M2", WarVoteChoice.DISAGREE.name, "no"),
                    ),
                ),
            ),
        )
        val text = WarCopyFormatter.format(result)
        assertTrue(text.contains("任务A"))
        assertTrue(text.contains("同意 1/2"))
    }
}
