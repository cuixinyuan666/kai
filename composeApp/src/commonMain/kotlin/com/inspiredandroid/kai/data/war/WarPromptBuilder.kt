package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.collaboration.CollaborationModelSnapshot

object WarPromptBuilder {

    const val ANALYST_SYSTEM_PROMPT =
        "你是多模型回答的仲裁分析员。请比较各模型对同一问题的方案，提取相同点与分歧方案。" +
            "必须只输出合法 JSON，不要 markdown 代码块，不要额外说明。" +
            "最多列出 6 个分歧方案。每个方案需有简短标题、描述，以及提出该方案的模型名称列表（必须与输入中的模型名称完全一致）。" +
            """JSON 格式：{"commonPoints":["方案A","方案C"], "aspects":[{"id":"d1","title":"方案B","description":"...","proposedBy":["模型我","模型你"]}]}""" +
            "commonPoints 为多数或全体模型都提出的方案；aspects 为仅部分模型提出的分歧方案。" +
            "若全体完全一致，aspects 为空数组。"

    const val MAX_ASPECTS = 6

    fun buildAnalysisPrompt(question: String, snapshots: List<CollaborationModelSnapshot>): String = buildString {
        appendLine("【原始问题】")
        appendLine(question)
        appendLine()
        appendLine("【各模型回答】")
        snapshots.forEach { snap ->
            appendLine()
            appendLine("--- ${snap.label} ---")
            if (snap.failed || snap.response.isNullOrBlank()) {
                appendLine("（调用失败，无回复）")
            } else {
                appendLine(snap.response)
            }
        }
        appendLine()
        appendLine("请分析以上回答的相同点与分歧方案，严格按 JSON 格式输出。")
        appendLine("每个分歧方案的 proposedBy 必须填写提出该方案的模型名称（与上方 --- 名称 --- 完全一致）。")
    }

    fun formatAnalysisForDisplay(result: WarAnalysisResult): String = buildString {
        appendLine("【相同点】")
        if (result.commonPoints.isEmpty()) {
            appendLine("（无）")
        } else {
            result.commonPoints.forEach { appendLine("- $it") }
        }
        appendLine()
        appendLine("【分歧方案】")
        if (result.aspects.isEmpty()) {
            appendLine("（无，全体一致）")
        } else {
            result.aspects.forEach { aspect ->
                val proposers = aspect.proposedByLabels.joinToString("、").ifBlank { "未标注提出方" }
                appendLine("- ${aspect.title}（提出方：$proposers）")
                if (aspect.description.isNotBlank()) appendLine("  ${aspect.description}")
            }
        }
    }.trimEnd()

    fun buildVotePrompt(aspects: List<WarAspect>): String = buildCrossVotePrompt(aspects, voteRound = 1)

    fun buildCrossVotePrompt(aspects: List<WarAspect>, voteRound: Int): String = buildString {
        appendLine("【第 ${voteRound} 轮交叉投票】")
        appendLine("以下分歧方案由其他模型提出，你在第 1 轮并未提出它们。请逐项表明是否认可该方案。")
        appendLine("必须只输出合法 JSON，不要 markdown 代码块。")
        appendLine("JSON 格式：")
        appendLine("""{"votes":[{"aspectId":"d1","agree":true,"reason":"简短理由"}, ...]}""")
        appendLine()
        appendAssignedAspects(aspects)
        appendLine("agree 为 true 表示同意采用该方案，false 表示不同意。每条必须给出 reason。")
    }

    fun buildDisagreementBriefing(aspectResults: List<WarAspectResult>): String = buildString {
        aspectResults.forEach { aspectResult ->
            val disagreements = aspectResult.disagreementVotes
            appendLine("异议点：[${aspectResult.aspect.id}] ${aspectResult.aspect.title}")
            appendLine("  ${aspectResult.aspect.description}")
            if (disagreements.isEmpty()) {
                appendLine("  （本方面上一轮无异议）")
            } else {
                disagreements.forEach { vote ->
                    appendLine("  - 提出方：${vote.modelLabel}")
                    val reason = vote.reason.trim().ifBlank { "（未给出理由）" }
                    appendLine("    异议理由：$reason")
                }
            }
            appendLine()
        }
    }.trimEnd()

    fun buildFollowUpVotePrompt(
        aspects: List<WarAspect>,
        previousRound: List<WarAspectResult>,
        voteRound: Int,
    ): String = buildFollowUpCrossVotePrompt(aspects, previousRound, voteRound)

    fun buildFollowUpCrossVotePrompt(
        aspects: List<WarAspect>,
        previousRound: List<WarAspectResult>,
        voteRound: Int,
    ): String = buildString {
        appendLine("【第 ${voteRound} 轮交叉投票】")
        appendLine("请再次对下列分歧方案表明是否同意。除方案本身外，必须结合上一轮对应的同意/不同意理由。")
        appendLine("必须只输出合法 JSON，不要 markdown 代码块。")
        appendLine("JSON 格式：")
        appendLine("""{"votes":[{"aspectId":"d1","agree":true,"reason":"简短理由"}, ...]}""")
        appendLine()
        appendLine("【上一轮理由】")
        val assignedIds = aspects.map { it.id }.toSet()
        val previousVotes = previousRound.filter { it.aspect.id in assignedIds }
        if (previousVotes.all { it.votes.isEmpty() }) {
            appendLine("上一轮没有附加理由。请再次确认你的立场。")
        } else {
            previousVotes.forEach { aspectResult ->
                appendLine("方案：${aspectResult.aspect.title}")
                aspectResult.votes.forEach { vote ->
                    val stance = when (vote.choice) {
                        WarVoteChoice.AGREE.name -> "同意"
                        WarVoteChoice.DISAGREE.name -> "不同意"
                        else -> "未表态"
                    }
                    val reason = vote.reason.trim().ifBlank { "（未给出理由）" }
                    appendLine("  - ${vote.modelLabel}：$stance — $reason")
                }
                appendLine()
            }
        }
        appendLine("【本轮需投票的分歧方案】")
        appendAssignedAspects(aspects)
        appendLine("agree 为 true 表示同意采用该方案，false 表示不同意。reason 必须针对上一轮理由给出你本轮自身的结论。")
    }

    fun buildFinalSummaryPrompt(
        question: String,
        commonPoints: List<String>,
        voteRoundResults: List<WarVoteRoundResult>,
    ): String = buildString {
        appendLine("请对本次战争模式任务做最终汇总。")
        appendLine("【原始问题】")
        appendLine(question)
        appendLine()
        appendLine("【相同点】")
        if (commonPoints.isEmpty()) appendLine("（无）") else commonPoints.forEach { appendLine("- $it") }
        appendLine()
        voteRoundResults.forEach { round ->
            appendLine("【第 ${round.round} 轮投票】")
            round.aspectResults.forEach { aspectResult ->
                appendLine(
                    "- ${aspectResult.aspect.title}：同意 ${aspectResult.agreeCount}/${aspectResult.validVoteCount}" +
                        "，不同意 ${aspectResult.disagreeCount}/${aspectResult.validVoteCount}",
                )
                aspectResult.votes.forEach { vote ->
                    val stance = when (vote.choice) {
                        WarVoteChoice.AGREE.name -> "同意"
                        WarVoteChoice.DISAGREE.name -> "不同意"
                        else -> "未表态"
                    }
                    val reason = if (vote.reason.isNotBlank()) " — ${vote.reason}" else ""
                    appendLine("    ${vote.modelLabel}：$stance$reason")
                }
            }
            appendLine()
        }
        appendLine("请用中文输出最终建议：保留哪些方案、放弃哪些方案、以及简要理由。不要输出 JSON。")
    }

    private fun StringBuilder.appendAssignedAspects(aspects: List<WarAspect>) {
        aspects.forEachIndexed { index, aspect ->
            val proposers = aspect.proposedByLabels.joinToString("、").ifBlank { "其他模型" }
            appendLine("${index + 1}. [${aspect.id}] ${aspect.title}")
            appendLine("   ${aspect.description}")
            appendLine("   提出方：$proposers")
            appendLine()
        }
    }
}
