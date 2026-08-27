package com.inspiredandroid.kai.data.collaboration

import kotlin.test.Test
import kotlin.test.assertTrue

class CollaborationExtractorsTest {

    @Test
    fun formatFollowUpPrompt_containsQuestionAndAnswer() {
        val prompt = formatFollowUpPrompt("什么是 Kotlin？", "Kotlin 是一门编程语言。")
        assertTrue(prompt.contains("【原始问题】"))
        assertTrue(prompt.contains("什么是 Kotlin？"))
        assertTrue(prompt.contains("【你上一次的回答】"))
        assertTrue(prompt.contains("Kotlin 是一门编程语言。"))
        assertTrue(prompt.contains("是否存在问题"))
    }

    @Test
    fun buildCollaborationCopyText_includesAllRounds() {
        val ref = ModelRef("inst1", "model1")
        val rounds = listOf(
            CollaborationRoundSnapshot(
                round = 1,
                responses = listOf(
                    CollaborationModelSnapshot(ref = ref, label = "GPT", response = "回答一", failed = false),
                ),
            ),
            CollaborationRoundSnapshot(
                round = 2,
                responses = listOf(
                    CollaborationModelSnapshot(ref = ref, label = "GPT", response = "回答二", failed = false),
                ),
            ),
        )
        val text = buildCollaborationCopyText("用户问题", rounds)
        assertTrue(text.contains("【用户提问】"))
        assertTrue(text.contains("用户问题"))
        assertTrue(text.contains("第 1 轮"))
        assertTrue(text.contains("回答一"))
        assertTrue(text.contains("第 2 轮"))
        assertTrue(text.contains("回答二"))
    }
}
