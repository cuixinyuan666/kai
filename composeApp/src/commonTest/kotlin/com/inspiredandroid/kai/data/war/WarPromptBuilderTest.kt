package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.collaboration.CollaborationModelSnapshot
import com.inspiredandroid.kai.data.collaboration.ModelRef
import kotlin.test.Test
import kotlin.test.assertTrue

class WarPromptBuilderTest {

    @Test
    fun `analysis prompt includes question and model answers`() {
        val prompt = WarPromptBuilder.buildAnalysisPrompt(
            question = "如何设计 API？",
            snapshots = listOf(
                CollaborationModelSnapshot(
                    ref = ModelRef("svc", "m1"),
                    label = "OpenCode / hy3",
                    response = "使用 REST",
                    failed = false,
                ),
            ),
        )
        assertTrue(prompt.contains("如何设计 API？"))
        assertTrue(prompt.contains("OpenCode / hy3"))
        assertTrue(prompt.contains("使用 REST"))
        assertTrue(prompt.contains("proposedBy"))
    }

    @Test
    fun `vote prompt lists all aspects`() {
        val prompt = WarPromptBuilder.buildVotePrompt(
            listOf(
                WarAspect("a1", "方面1：性能", "模型分歧描述"),
            ),
        )
        assertTrue(prompt.contains("a1"))
        assertTrue(prompt.contains("方面1：性能"))
        assertTrue(prompt.contains("\"votes\""))
    }

    @Test
    fun `follow-up vote prompt includes previous disagreement briefing`() {
        val aspects = listOf(WarAspect("a1", "方面1：性能", "模型分歧描述"))
        val previous = listOf(
            WarAspectResult(
                aspect = aspects.first(),
                votes = listOf(
                    WarModelVote("m1", "OpenCode / hy3", WarVoteChoice.DISAGREE.name, "延迟不可接受"),
                    WarModelVote("m2", "OpenCode / glm", WarVoteChoice.AGREE.name, "ok"),
                ),
            ),
        )
        val prompt = WarPromptBuilder.buildFollowUpVotePrompt(aspects, previous, voteRound = 2)
        assertTrue(prompt.contains("第 2 轮交叉投票"))
        assertTrue(prompt.contains("上一轮理由"))
        assertTrue(prompt.contains("OpenCode / hy3"))
        assertTrue(prompt.contains("延迟不可接受"))
        assertTrue(prompt.contains("a1"))
    }
}
