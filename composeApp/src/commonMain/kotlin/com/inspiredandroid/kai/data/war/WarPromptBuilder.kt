package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.collaboration.CollaborationModelSnapshot

object WarPromptBuilder {

    const val ANALYST_SYSTEM_PROMPT =
        "你是多模型回答的仲裁分析员。请比较各模型对同一问题的回答，提取相同点与分歧方面。" +
            "必须只输出合法 JSON，不要 markdown 代码块，不要额外说明。" +
            "最多列出 6 个分歧方面。每个方面需有简短标题与描述（说明各模型在此方面的不同立场）。" +
            """JSON 格式：{"commonPoints":["..."], "aspects":[{"id":"a1","title":"方面1：...","description":"..."}]}""" +
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
        appendLine("请分析以上回答的相同点与分歧方面，严格按 JSON 格式输出。")
    }

    fun buildVotePrompt(aspects: List<WarAspect>): String = buildString {
        appendLine("【分歧方面投票】")
        appendLine("以下是总结模型从各模型回答中提取的分歧方面。请对每一条表明你是否同意该方面的描述/立场。")
        appendLine("必须只输出合法 JSON，不要 markdown 代码块。")
        appendLine("JSON 格式：")
        appendLine("""{"votes":[{"aspectId":"a1","agree":true,"reason":"简短理由"}, ...]}""")
        appendLine()
        aspects.forEachIndexed { index, aspect ->
            appendLine("${index + 1}. [${aspect.id}] ${aspect.title}")
            appendLine("   ${aspect.description}")
            appendLine()
        }
        appendLine("agree 为 true 表示同意该方面描述/立场，false 表示不同意。每条必须给出 reason。")
    }
}
