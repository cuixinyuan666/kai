package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.collaboration.CollaborationModelSnapshot
import com.inspiredandroid.kai.data.collaboration.ModelRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WarVotingTest {

    @Test
    fun `cross vote skips models that proposed the solution`() {
        val aspect = WarAspect(
            id = "d1",
            title = "方案D",
            description = "",
            proposedByLabels = listOf("模型你"),
            proposedByKeys = listOf("you"),
        )
        val forMe = WarVoting.aspectsForModel(listOf(aspect), "me", "模型我")
        val forYou = WarVoting.aspectsForModel(listOf(aspect), "you", "模型你")
        val forHim = WarVoting.aspectsForModel(listOf(aspect), "him", "模型他")
        assertEquals(listOf("d1"), forMe.map { it.id })
        assertTrue(forYou.isEmpty())
        assertEquals(listOf("d1"), forHim.map { it.id })
    }

    @Test
    fun `trend mark compares agree ratios`() {
        val aspect = WarAspect("a1", "方案B", "")
        val first = WarAspectResult(
            aspect,
            listOf(
                WarModelVote("m1", "我", WarVoteChoice.AGREE.name),
                WarModelVote("m2", "他", WarVoteChoice.AGREE.name),
            ),
        )
        val second = WarAspectResult(
            aspect,
            listOf(
                WarModelVote("m1", "我", WarVoteChoice.AGREE.name),
                WarModelVote("m2", "他", WarVoteChoice.DISAGREE.name),
            ),
        )
        assertEquals("2/2", WarVoting.cellText(first, null))
        assertEquals("2/2-", WarVoting.cellText(first, first))
        assertEquals("1/2↓", WarVoting.cellText(second, first))
        assertEquals("2/2↑", WarVoting.cellText(first, second))
    }

    @Test
    fun `parent-only label matches chip pair without substring false positives`() {
        val agnes = WarAspect(
            id = "d1",
            title = "方案D",
            description = "",
            proposedByLabels = listOf("Agnes Al"),
            proposedByKeys = emptyList(),
        )
        val forAgnes = WarVoting.aspectsForModel(listOf(agnes), "svc::agnes-2.0-flash", "Agnes Al / agnes-2.0-flash")
        val forOther = WarVoting.aspectsForModel(listOf(agnes), "svc::other", "Other / other-1")
        assertTrue(forAgnes.isEmpty())
        assertEquals(listOf("d1"), forOther.map { it.id })
        assertFalse(WarVoting.isProposer(agnes, "svc::magnet", "Magnet / agnes-clone"))
        assertEquals("提出方，本轮交叉投票跳过（方案D）", WarVoting.skipReason("方案D"))
    }

    @Test
    fun `child model id matches either chip side`() {
        val aspect = WarAspect(
            id = "d1",
            title = "方案D",
            description = "",
            proposedByLabels = listOf("agnes-2.0-flash"),
        )
        assertTrue(WarVoting.isProposer(aspect, "k", "Agnes Al / agnes-2.0-flash"))
        assertFalse(WarVoting.isProposer(aspect, "k", "Gemini / gemini-2.0-flash"))
    }

    @Test
    fun `resolveProposers maps labels to keys`() {
        val snapshots = listOf(
            CollaborationModelSnapshot(ModelRef("s", "m1"), "模型我", "A", false),
            CollaborationModelSnapshot(ModelRef("s", "m2"), "模型你", "D", false),
        )
        val resolved = WarVoting.resolveProposers(
            listOf(WarAspect("d1", "方案D", "", proposedByLabels = listOf("模型你"))),
            snapshots,
        )
        assertEquals(listOf("s::m2"), resolved.single().proposedByKeys)
        assertFalse(WarVoting.isProposer(resolved.single(), "s::m1", "模型我"))
        assertTrue(WarVoting.isProposer(resolved.single(), "s::m2", "模型你"))
    }
}
