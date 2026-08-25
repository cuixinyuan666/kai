package com.inspiredandroid.kai.data.collaboration

/**
 * 从各角色结构化输出中提取指定任务方的段落。
 * 支持多种常见格式，按优先级依次尝试，提升跨模型兼容性。
 */
fun extractTaskPartySegment(text: String, taskPartyIndex: Int): String? {
    if (text.isBlank()) return null
    val patterns = listOf(
        // 监督方格式：对任务方1的回复：…
        Regex("对任务方$taskPartyIndex[的的]?回复[:：]\\s*(.*?)(?=对任务方\\d|任务方\\d|$)", RegexOption.DOT_MATCHES_ALL),
        // 回传方格式：任务方1：… 或 任务方1\n- 监督方A：…
        Regex("任务方$taskPartyIndex\\s*[:：]\\s*(.*?)(?=任务方\\d|$)", RegexOption.DOT_MATCHES_ALL),
        // 传达方格式：任务方1：<…>
        Regex("任务方$taskPartyIndex\\s*[：:]<\\s*(.*?)\\s*>", RegexOption.DOT_MATCHES_ALL),
        Regex("任务方$taskPartyIndex\\s*[:：]\\s*(.*?)(?=任务方\\d|【|$)", RegexOption.DOT_MATCHES_ALL),
        // 标记块格式：<<<TASK_PARTY_1>>>…<<<END>>>
        Regex("<<<TASK_PARTY_$taskPartyIndex>>>\\s*(.*?)\\s*<<<END>>>", RegexOption.DOT_MATCHES_ALL),
    )
    for (regex in patterns) {
        val segment = regex.find(text)?.groupValues?.getOrNull(1)?.trim()
        if (!segment.isNullOrBlank()) return segment
    }
    return null
}

/**
 * 从回传方汇总中提取指定任务方的反馈段落（与 [extractTaskPartySegment] 等价，语义别名）。
 */
fun extractFeedbackForTaskParty(feedbackSummary: String, taskPartyIndex: Int): String? =
    extractTaskPartySegment(feedbackSummary, taskPartyIndex)

/**
 * 从监督方回复中提取「评估结论」部分（去掉独立执行简述，仅保留确认/纠正）。
 */
fun extractSupervisorVerdict(segment: String): String {
    val evalMarkers = listOf("评估：", "评估:", "结论：", "结论:")
    for (marker in evalMarkers) {
        val idx = segment.indexOf(marker)
        if (idx >= 0) return segment.substring(idx + marker.length).trim()
    }
    return segment.trim()
}

/**
 * 判断监督方针对某任务方的回复是否表示「确认」（无问题）。
 */
fun isConfirmReply(reply: String): Boolean {
    val verdict = extractSupervisorVerdict(reply)
    val normalized = verdict.trim()
    if (normalized.isEmpty()) return false
    // 明确否定优先
    val negatives = listOf("不确认", "无法确认", "未确认", "有问题", "存在问题", "需要改进", "需要修正", "需要纠正")
    if (negatives.any { normalized.contains(it) }) return false
    // 仅含「确认」且无否定词
    return normalized.contains("确认")
}
