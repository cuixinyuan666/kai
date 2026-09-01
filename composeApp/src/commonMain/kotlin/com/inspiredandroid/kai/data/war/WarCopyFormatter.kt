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

        val rounds = result.displayVoteRounds()
        if (rounds.isEmpty() && !result.analysisFailed) {
            appendLine("【结论】全体一致，无分歧方案。")
        }

        if (rounds.isNotEmpty()) {
            val aspects = rounds.last().aspectResults.map { it.aspect }
            appendLine("【投票表】")
            append("轮次")
            aspects.forEach { append("\t${it.title}") }
            appendLine()
            rounds.forEachIndexed { index, round ->
                val previous = rounds.getOrNull(index - 1)
                append("第${round.round}轮")
                aspects.forEach { aspect ->
                    val current = round.aspectResults.find { it.aspect.id == aspect.id }
                    val prev = previous?.aspectResults?.find { it.aspect.id == aspect.id }
                    append("\t${current?.let { WarVoting.cellText(it, prev) } ?: "-"}")
                }
                appendLine()
            }
            appendLine()
            rounds.forEach { round ->
                appendLine("【第 ${round.round} 轮明细】")
                round.aspectResults.forEach { aspectResult ->
                    appendLine("- ${aspectResult.aspect.title} ${WarVoting.cellText(aspectResult, null)}")
                    aspectResult.votes.forEach { vote ->
                        val stance = when (vote.choice) {
                            WarVoteChoice.AGREE.name -> "同意"
                            WarVoteChoice.DISAGREE.name -> "不同意"
                            else -> "未表态"
                        }
                        val reasonSuffix = if (vote.reason.isNotBlank()) " — ${vote.reason}" else ""
                        appendLine("  - ${vote.modelLabel}：$stance$reasonSuffix")
                    }
                }
                appendLine()
            }
        }

        if (!result.finalSummary.isNullOrBlank()) {
            appendLine("【最终汇总】")
            appendLine(result.finalSummary)
            appendLine()
        }

        if (result.summaryModelLabel != null) {
            appendLine("【总结模型】${result.summaryModelLabel}")
        }
    }.trimEnd()
}
