package com.inspiredandroid.kai.data.collaboration

import kotlinx.serialization.Serializable

/**
 * 聊天模式：单一模式（默认，单模型）与协作模式（多模型多角色协作）。
 */
enum class ChatMode {
    SINGLE,
    COLLABORATION,
}

/**
 * 协作角色分配模式：
 * - [MANUAL]：手动选择任务方与监督方。
 * - [SCORE_GATED]：分数门槛模式——只有模型测试总分 ≥ [CollaborationConfig.minScore] 的模型才参与；
 *   任务方与监督方按 [CollaborationConfig.taskRatio] 比例从达标模型中自动分配。
 */
@Serializable
enum class CollaborationMode {
    MANUAL,
    SCORE_GATED,
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
 * 协作角色配置：
 * - 任务方 taskParties：一个或多个，负责执行任务并与监督方直接对话。
 * - 监督方 supervisors：一个或多个，负责与任务方一对一审阅对话。
 */
@Serializable
data class CollaborationRoleConfig(
    val taskParties: List<ModelRef> = emptyList(),
    val supervisors: List<ModelRef> = emptyList(),
)

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
    val mode: CollaborationMode = CollaborationMode.MANUAL,
    val minScore: Double = 70.0,
    val taskRatio: Double = 0.6,
    val roles: CollaborationRoleConfig = CollaborationRoleConfig(),
    /** 每个任务方-监督方会话的最大对话轮次。 */
    val maxRounds: Int = 5,
    val retryCount: Int = 2,
    val taskPartyPrompt: String = DEFAULT_TASK_PARTY_PROMPT,
    val supervisorPrompt: String = DEFAULT_SUPERVISOR_PROMPT,
    /** 监督方回复结束类关键词时是否自动结束会话。 */
    val autoStopOnConfirm: Boolean = true,
    val notifyOnFailure: Boolean = true,
    val notifyOnComplete: Boolean = true,
    val scores: List<ModelScore> = emptyList(),
    val modelAliases: Map<String, String> = emptyMap(),
)

/**
 * 任务方与监督方的一对一会话标识。
 * 每个任务方与每个监督方之间独立保存一份聊天记录，互不混杂。
 */
data class CollaborationSessionKey(
    val taskIndex: Int,
    val supervisorIndex: Int,
) {
    val storageKey: String
        get() = "task${taskIndex + 1}-supervisor${supervisorIndex + 1}"

    fun displayLabel(taskLabel: String, supervisorLabel: String): String = "$taskLabel · $supervisorLabel"
}

/**
 * 协作运行阶段。
 */
enum class CollaborationPhase {
    IDLE,
    DISTRIBUTE,
    TASK,
    SUPERVISE,
    DIALOGUE,
    DONE,
    FAILED,
    CANCELLED,
}

/**
 * 协作角色类型，用于按角色过滤独立视图。
 */
enum class CollaborationRoleKind {
    TASK,
    SUPERVISE,
    SYSTEM,
}

data class CollaborationEvent(
    val round: Int,
    val phase: CollaborationPhase,
    val text: String,
    val sourceLabel: String? = null,
    val isAnswer: Boolean = false,
    val roleKind: CollaborationRoleKind? = null,
    /** 所属会话（任务方×监督方配对），为空表示全局事件。 */
    val sessionKey: String? = null,
)

interface CollaborationListener {
    fun onEvent(event: CollaborationEvent)

    fun onNotify(title: String, body: String)

    fun onScores(scores: List<ModelScore>)

    fun onFinished(summary: String, allConfirmed: Boolean)
}

/** 系统转发给任务方的固定格式。 */
fun formatSupervisorQuestionForTask(supervisorQuestion: String): String =
    "针对你的上次回答，监督方提出的问题是：$supervisorQuestion，你对此的回答是什么？"

/** 系统转发给监督方的固定格式。 */
fun formatTaskAnswerForSupervisor(taskAnswer: String): String =
    "针对你的上一次疑问，任务方的回答是：$taskAnswer，你对此还有哪些疑问？"

const val DEFAULT_TASK_PARTY_PROMPT = """
你是一个"任务方"。你将与用户问题及监督方的审阅反馈进行直接对话。
你的职责：
1. 认真理解问题，独立给出完整、可执行的方案或答案；
2. 若收到监督方的疑问，请逐条回应并改进你的方案；
3. 输出应结构清晰、重点突出，便于监督方继续审阅。

请直接输出你的方案或答案正文，不要添加角色自述或流程说明。
"""

const val DEFAULT_SUPERVISOR_PROMPT = """
你是一个"监督方"。你将与任务方进行直接对话，审阅其作答并提出疑问。
你的职责：
1. 审阅任务方的方案或结果，指出问题、疑点或需要补充之处；
2. 以提问形式与任务方交流，帮助其完善方案；
3. 若认为没有问题、可以完成，请明确回复「没有问题」或「可以完成」等结束语。

请直接输出你的审阅意见或疑问，不要添加角色自述或流程说明。
"""
