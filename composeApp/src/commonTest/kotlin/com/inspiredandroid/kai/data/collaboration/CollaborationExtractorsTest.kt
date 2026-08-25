package com.inspiredandroid.kai.data.collaboration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollaborationExtractorsTest {

    @Test
    fun extractTaskPartySegment_fromSupervisorReply() {
        val text = """
            对任务方1的回复：我独立执行后认为方案可行；评估：确认
            对任务方2的回复：我的方案略有不同；评估：建议补充边界条件说明
        """.trimIndent()
        assertEquals("我独立执行后认为方案可行；评估：确认", extractTaskPartySegment(text, 1))
        assertEquals("我的方案略有不同；评估：建议补充边界条件说明", extractTaskPartySegment(text, 2))
    }

    @Test
    fun extractTaskPartySegment_fromFeedbackReply() {
        val text = """
            任务方1：
            - 监督方A的回复：确认
            - 监督方B的回复：需要补充测试用例

            任务方2：
            - 监督方A的回复：确认
        """.trimIndent()
        val seg1 = extractFeedbackForTaskParty(text, 1)
        assertNotNull(seg1)
        assertTrue(seg1.contains("监督方A"))
        assertTrue(seg1.contains("监督方B"))
    }

    @Test
    fun extractSupervisorVerdict_stripsPreamble() {
        val segment = "我独立执行后认为可行；评估：确认"
        assertEquals("确认", extractSupervisorVerdict(segment))
    }

    @Test
    fun isConfirmReply_positiveAndNegative() {
        assertTrue(isConfirmReply("评估：确认"))
        assertFalse(isConfirmReply("评估：不确认，需要修改"))
        assertFalse(isConfirmReply("评估：存在问题，需要改进"))
        assertTrue(isConfirmReply("对任务方1的回复：…；评估：确认"))
    }
}
