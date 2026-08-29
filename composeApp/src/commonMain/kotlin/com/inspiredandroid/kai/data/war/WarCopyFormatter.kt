package com.inspiredandroid.kai.data.war

object WarCopyFormatter {

    fun format(result: WarTaskResult): String = buildString {
        appendLine("【任务】")
        appendLine(result.question)
        appendLine()

        if (result.commonPoints.isNotEmpty()) {
            appendLine("【相同点】")
            result.commonPoints.forEach { appendLine("- $it") }
            appendLine()
        }

        if (result.analysisFailed) {
            appendLine("【分析失败】${result.analysisError.orEmpty()}")
            appendLine()
        }

        if (result.aspectResults.isEmpty() && !result.analysisFailed) {
            appendLine("【结论】全体一致，无分歧方面。")
        }

        result.aspectResults.forEach { aspectResult ->
            val aspect = aspectResult.aspect
            val valid = aspectResult.validVoteCount
            appendLine("【${aspect.title}】同意 ${aspectResult.agreeCount}/$valid，不同意 ${aspectResult.disagreeCount}/$valid")
            appendLine(aspect.description)
            aspectResult.votes.forEach { vote ->
                val label = when (vote.choice) {
                    WarVoteChoice.AGREE.name -> "同意"
                    WarVoteChoice.DISAGREE.name -> "不同意"
                    else -> "未表态"
                }
                val reasonSuffix = if (vote.reason.isNotBlank()) " — ${vote.reason}" else ""
                appendLine("  - ${vote.modelLabel}：$label$reasonSuffix")
            }
            appendLine()
        }

        if (result.summaryModelLabel != null) {
            appendLine("【总结模型】${result.summaryModelLabel}")
        }
    }.trimEnd()
}
