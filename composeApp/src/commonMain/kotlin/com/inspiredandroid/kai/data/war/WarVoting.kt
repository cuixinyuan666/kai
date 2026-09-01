package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.collaboration.CollaborationModelSnapshot
import kotlin.math.abs

object WarVoting {

    fun aspectsForModel(
        aspects: List<WarAspect>,
        modelKey: String,
        modelLabel: String,
    ): List<WarAspect> = aspects.filter { aspect ->
        !isProposer(aspect, modelKey, modelLabel)
    }

    fun isProposer(aspect: WarAspect, modelKey: String, modelLabel: String): Boolean {
        if (aspect.proposedByKeys.isEmpty() && aspect.proposedByLabels.isEmpty()) return false
        if (modelKey.isNotBlank() && modelKey in aspect.proposedByKeys) return true
        return aspect.proposedByLabels.any { label -> labelsMatch(label, modelLabel) }
    }

    fun resolveProposers(
        aspects: List<WarAspect>,
        snapshots: List<CollaborationModelSnapshot>,
    ): List<WarAspect> = aspects.map { aspect ->
        if (aspect.proposedByKeys.isNotEmpty()) return@map aspect
        val keys = aspect.proposedByLabels.mapNotNull { label ->
            snapshots.find { snap -> labelsMatch(label, snap.label) }?.ref?.key
        }.distinct()
        aspect.copy(proposedByKeys = keys)
    }

    fun trendMark(current: WarAspectResult, previous: WarAspectResult?): String {
        if (previous == null) return ""
        val cur = agreeRatio(current)
        val prev = agreeRatio(previous)
        return when {
            abs(cur - prev) < 0.0001 -> "-"
            cur > prev -> "↑"
            else -> "↓"
        }
    }

    fun cellText(current: WarAspectResult, previous: WarAspectResult?): String {
        val base = "${current.agreeCount}/${current.validVoteCount}"
        val mark = trendMark(current, previous)
        return if (mark.isEmpty()) base else "$base$mark"
    }

    fun previousAspect(previousRound: WarVoteRoundResult?, aspectId: String): WarAspectResult? =
        previousRound?.aspectResults?.find { it.aspect.id == aspectId }

    private fun agreeRatio(result: WarAspectResult): Double =
        if (result.validVoteCount <= 0) 0.0 else result.agreeCount.toDouble() / result.validVoteCount

    private fun labelsMatch(a: String, b: String): Boolean {
        val left = a.trim()
        val right = b.trim()
        if (left.isEmpty() || right.isEmpty()) return false
        if (left.equals(right, ignoreCase = true)) return true
        val (p1, c1) = splitPair(left)
        val (p2, c2) = splitPair(right)
        if (c1 != null && c1.equals(right, ignoreCase = true)) return true
        if (c2 != null && c2.equals(left, ignoreCase = true)) return true
        if (c1 != null && c2 != null) {
            return c1.equals(c2, ignoreCase = true)
        }
        return p1.equals(p2, ignoreCase = true)
    }

    private fun splitPair(label: String): Pair<String, String?> {
        val idx = label.indexOf(" / ")
        return if (idx >= 0) {
            label.substring(0, idx).trim() to label.substring(idx + 3).trim().ifBlank { null }
        } else {
            label.trim() to null
        }
    }

    fun parentName(title: String): String = splitPair(title).first

    fun skipReason(aspectTitle: String): String = "提出方，本轮交叉投票跳过（$aspectTitle）"
}
