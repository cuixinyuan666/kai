package com.inspiredandroid.kai.data.collaboration

/**
 * 判断监督方回复是否表示本轮对话可以结束。
 * 识别「没有问题」「可以完成」等结束类关键词。
 */
fun isSessionTerminationReply(reply: String): Boolean {
    val normalized = reply.trim()
    if (normalized.isEmpty()) return false

    // 先匹配明确的结束语，避免「没有问题」被「有问题」误判。
    val positives = listOf(
        "没有问题", "可以完成", "无异议", "没有疑问",
        "无需修改", "可以接受", "已完成", "可以结束",
    )
    if (positives.any { normalized.contains(it) }) return true

    val negatives = listOf(
        "不确认", "无法确认", "未确认", "有问题", "存在问题",
        "需要改进", "需要修正", "需要纠正", "仍有疑问", "还有疑问",
    )
    if (negatives.any { normalized.contains(it) }) return false

    return normalized.contains("确认")
}

/** @deprecated 保留兼容旧测试；请使用 [isSessionTerminationReply]。 */
fun isConfirmReply(reply: String): Boolean = isSessionTerminationReply(reply)

/** @deprecated 旧四角色模式遗留；新对话模式不再使用。 */
fun extractSupervisorVerdict(segment: String): String {
    val evalMarkers = listOf("评估：", "评估:", "结论：", "结论:")
    for (marker in evalMarkers) {
        val idx = segment.indexOf(marker)
        if (idx >= 0) return segment.substring(idx + marker.length).trim()
    }
    return segment.trim()
}

/** @deprecated 旧四角色模式遗留；新对话模式不再使用。 */
fun extractTaskPartySegment(text: String, taskPartyIndex: Int): String? {
    if (text.isBlank()) return null
    val patterns = listOf(
        Regex("对任务方$taskPartyIndex[的的]?回复[:：]\\s*(.*?)(?=对任务方\\d|任务方\\d|$)", RegexOption.DOT_MATCHES_ALL),
        Regex("任务方$taskPartyIndex\\s*[:：]\\s*(.*?)(?=任务方\\d|$)", RegexOption.DOT_MATCHES_ALL),
    )
    for (regex in patterns) {
        val segment = regex.find(text)?.groupValues?.getOrNull(1)?.trim()
        if (!segment.isNullOrBlank()) return segment
    }
    return null
}

/** @deprecated 旧四角色模式遗留；新对话模式不再使用。 */
fun extractFeedbackForTaskParty(feedbackSummary: String, taskPartyIndex: Int): String? =
    extractTaskPartySegment(feedbackSummary, taskPartyIndex)
