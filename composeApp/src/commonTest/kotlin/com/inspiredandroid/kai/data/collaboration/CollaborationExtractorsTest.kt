package com.inspiredandroid.kai.data.collaboration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollaborationExtractorsTest {

    @Test
    fun isSessionTerminationReply_positiveKeywords() {
        assertTrue(isSessionTerminationReply("没有问题"))
        assertTrue(isSessionTerminationReply("可以完成"))
        assertTrue(isSessionTerminationReply("确认，方案可行"))
        assertTrue(isSessionTerminationReply("没有疑问，可以接受"))
        assertTrue(isSessionTerminationReply("评估：确认"))
    }

    @Test
    fun isSessionTerminationReply_negativeKeywords() {
        assertFalse(isSessionTerminationReply(""))
        assertFalse(isSessionTerminationReply("存在问题，需要改进"))
        assertFalse(isSessionTerminationReply("不确认，需要修改"))
        assertFalse(isSessionTerminationReply("仍有疑问，请补充"))
    }

    @Test
    fun formatRelayMessages() {
        val q = formatSupervisorQuestionForTask("请说明边界条件")
        assertTrue(q.contains("针对你的上次回答，监督方提出的问题是：请说明边界条件"))
        val a = formatTaskAnswerForSupervisor("边界条件如下…")
        assertTrue(a.contains("针对你的上一次疑问，任务方的回答是：边界条件如下…"))
    }
}
