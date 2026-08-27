package com.inspiredandroid.kai.data.collaboration

import kotlinx.serialization.Serializable

/**
 * 聊天模式：单一模式（默认，单模型）与协作模式（多模型并行协作）。
 */
enum class ChatMode {
    SINGLE,
    COLLABORATION,
}

/**
 * 对某个大模型实例 + 具体分支的引用。
 * instanceId 对应 ServiceInstance，modelId 对应该实例下的模型分支。
 */
@Serializable
data class ModelRef(
    val instanceId: String,
    val modelId: String,
) {
    /** 稳定 key，用于关联自定义显示名等附加元数据。 */
    val key: String
        get() = "$instanceId::$modelId"
}

/**
 * 单个模型的评分。
 */
@Serializable
data class ModelScore(
    val instanceId: String,
    val modelId: String,
    val analysisScore: Double = 0.0,
    val userScore: Double? = null,
    val userWeight: Double = 0.5,
) {
    val finalScore: Double
        get() = if (userScore != null) {
            analysisScore * (1.0 - userWeight.coerceIn(0.0, 1.0)) + userScore * userWeight.coerceIn(0.0, 1.0)
        } else {
            analysisScore
        }
}

/**
 * 完整协作配置，持久化到 AppSettings。
 */
@Serializable
data class CollaborationConfig(
    val enabled: Boolean = false,
    /** 单次模型调用的最大等待时间（秒），默认 60。 */
    val maxWaitSeconds: Int = 60,
    val retryCount: Int = 2,
    val modelPrompt: String = DEFAULT_COLLABORATION_PROMPT,
    val notifyOnFailure: Boolean = true,
    val notifyOnComplete: Boolean = true,
    val scores: List<ModelScore> = emptyList(),
    val modelAliases: Map<String, String> = emptyMap(),
)

/**
 * 协作运行阶段。
 */
enum class CollaborationPhase {
    IDLE,
    DISTRIBUTE,
    RESPONDING,
    DONE,
    FAILED,
    CANCELLED,
}

data class CollaborationEvent(
    val round: Int,
    val phase: CollaborationPhase,
    val text: String,
    val sourceLabel: String? = null,
    val isAnswer: Boolean = false,
    /** 所属模型（ModelRef.key），为空表示全局事件。 */
    val sessionKey: String? = null,
)

/** 单轮协作快照，用于一键复制与下一轮筛选。 */
data class CollaborationRoundSnapshot(
    val round: Int,
    val responses: List<CollaborationModelSnapshot>,
)

data class CollaborationModelSnapshot(
    val ref: ModelRef,
    val label: String,
    val response: String?,
    val failed: Boolean,
)

interface CollaborationListener {
    fun onEvent(event: CollaborationEvent)

    fun onNotify(title: String, body: String)

    fun onScores(scores: List<ModelScore>)

    fun onRoundFinished(round: Int, snapshot: CollaborationRoundSnapshot, canContinue: Boolean)

    fun onFinished(summary: String, allSucceeded: Boolean)
}

/** 第二轮及之后转发给模型的固定格式：原始问题 + 上次回答 + 审阅要求。 */
fun formatFollowUpPrompt(question: String, previousAnswer: String): String = buildString {
    append("【原始问题】\n")
    append(question)
    append("\n\n【你上一次的回答】\n")
    append(previousAnswer)
    append("\n\n【审阅要求】\n")
    append("请审阅你上一次的回答是否存在问题。若存在问题请说明并修正你的回答；若认为没有问题请明确回复「没有问题」。")
}

/** 构建多轮协作的一键复制文本。 */
fun buildCollaborationCopyText(question: String, rounds: List<CollaborationRoundSnapshot>): String {
    if (rounds.isEmpty()) return question
    return buildString {
        appendLine("【用户提问】")
        appendLine(question)
        appendLine()
        rounds.forEach { round ->
            appendLine("—— 第 ${round.round} 轮 ——")
            round.responses.forEach { snap ->
                appendLine()
                appendLine("【${snap.label}】")
                if (snap.failed || snap.response.isNullOrBlank()) {
                    appendLine("（调用失败，无回复）")
                } else {
                    appendLine(snap.response)
                }
            }
            appendLine()
        }
    }.trimEnd()
}

const val DEFAULT_COLLABORATION_PROMPT = """
你是一个协作参与者。请认真理解问题并给出完整、可执行的方案或答案。
输出应结构清晰、重点突出。
请直接输出正文，不要添加角色自述或流程说明。
"""
