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
 * - [MANUAL]：手动选择任务方/传达方/监督方/回传方（原模式）。
 * - [SCORE_GATED]：分数门槛模式——只有模型测试总分 ≥ [CollaborationConfig.minScore] 的模型才参与；
 *   任务方与监督方按 [CollaborationConfig.taskRatio] 比例从达标模型中自动分配，
 *   传达方/回传方保留手动指定（未指定时自动取达标模型中的最高分）。
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
 * - 任务方 taskParties：一个或多个，负责执行任务。
 * - 传达方 transmitter：一个，负责汇总精简。
 * - 监督方 supervisors：一个或多个，负责评估任务方。
 * - 回传方 feedback：一个，负责汇总监督方回复并分发。
 */
@Serializable
data class CollaborationRoleConfig(
    val taskParties: List<ModelRef> = emptyList(),
    val transmitter: ModelRef? = null,
    val supervisors: List<ModelRef> = emptyList(),
    val feedback: ModelRef? = null,
)

/**
 * 单个模型的评分。
 * - analysisScore：系统自动分析得分（参考 freellmapi 风格，例如基于回答质量/一致性/耗时等）。
 * - userScore：用户自定义打分（可空）。
 * - userWeight：用户打分权重（0..1），计算最终得分时用户分占比。
 */
@Serializable
data class ModelScore(
    val instanceId: String,
    val modelId: String,
    val analysisScore: Double = 0.0,
    val userScore: Double? = null,
    val userWeight: Double = 0.5,
) {
    /** 最终得分：用户未打分时等于分析分；否则按权重融合。 */
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
    /** 是否启用协作模式（与 ChatMode 联动）。 */
    val enabled: Boolean = false,
    /** 协作角色分配模式：手动选择（原模式）或分数门槛自动分配。 */
    val mode: CollaborationMode = CollaborationMode.MANUAL,
    /** 分数门槛模式：参与门槛分数（0..100），仅测试总分 ≥ 该值的模型参与。 */
    val minScore: Double = 70.0,
    /** 分数门槛模式：任务方在"任务方+监督方"中的占比（0.1..0.9，默认 0.6 = 任务方60%/监督方40%）。 */
    val taskRatio: Double = 0.6,
    val roles: CollaborationRoleConfig = CollaborationRoleConfig(),
    /** 最大循环轮次。 */
    val maxRounds: Int = 3,
    /** 单个模型调用失败时的重试次数。 */
    val retryCount: Int = 2,
    /** 传达方/回传方（核心任务为内容精简）输出字数上限，运行时注入到提示词约束中。 */
    val maxOutputChars: Int = 1500,
    /** 传达方默认提示词（可自定义）。 */
    val transmitterPrompt: String = DEFAULT_TRANSMITTER_PROMPT,
    /** 回传方默认提示词（可自定义）。 */
    val feedbackPrompt: String = DEFAULT_FEEDBACK_PROMPT,
    /** 监督方全部回复"确认"时是否自动停止。 */
    val autoStopOnConfirm: Boolean = true,
    /** 模型失败（重试耗尽）时是否显式提醒。 */
    val notifyOnFailure: Boolean = true,
    /** 任务结束时是否显式提醒。 */
    val notifyOnComplete: Boolean = true,
    /** 各模型评分表。 */
    val scores: List<ModelScore> = emptyList(),
    /**
     * 各模型的自定义显示名（如 opencode-hy3），key 为 [ModelRef.key]（"$instanceId::$modelId"）。
     * 仅用于 UI 展示（如「任务方1（opencode-hy3）」），不会进入发给大模型的 prompt。
     */
    val modelAliases: Map<String, String> = emptyMap(),
)

    /**
     * 协作运行阶段。
     */
    enum class CollaborationPhase {
    IDLE,
    DISTRIBUTE,   // 下发问题到任务方
    TASK,         // 任务方执行
    TRANSMIT,     // 传达方汇总精简
    SUPERVISE,    // 监督方评估
    FEEDBACK,     // 回传方分发
    DONE,         // 正常结束
    FAILED,       // 失败结束
    CANCELLED,    // 用户取消
}

/**
 * 协作过程事件，供 UI 实时展示与日志。
 */
data class CollaborationEvent(
    val round: Int,
    val phase: CollaborationPhase,
    val text: String,
    /** 来源模型引用标签（如 "任务方1 / opencode-hy3"），可为空。 */
    val sourceLabel: String? = null,
    /** 是否为某方的实际回答正文（用于 UI 单独渲染为可滚动块）。 */
    val isAnswer: Boolean = false,
)

/**
 * 协作运行监听器：由调用层（ViewModel / Android 通知）实现。
 */
interface CollaborationListener {
    /** 过程日志事件。 */
    fun onEvent(event: CollaborationEvent)

    /** 显式提醒（失败 / 完成 等）。 */
    fun onNotify(title: String, body: String)

    /** 各模型自动分析得分（运行结束后回调）。 */
    fun onScores(scores: List<ModelScore>)

    /** 整体运行结束。 */
    fun onFinished(summary: String, allConfirmed: Boolean)
}

/**
 * 默认传达方提示词（已按用户描述润色）。
 * 负责将多个任务方的执行结果汇总精简，并保留关键信息，分发给监督方。
 */
const val DEFAULT_TRANSMITTER_PROMPT = """
你是一个"传达方"。下方汇集了多个任务方针对【同一个问题】的执行结果。
你的职责：
1. 对每一个任务方的结果进行【精简与保留关键信息】（你的核心任务是内容精简），避免文本过长，但不得丢失核心结论、关键数据与差异点；
2. 汇总为统一的结构化汇报，逐任务方独立成段；
3. 你需对每个任务方"先独立按照自己的方式执行该问题"，再判断其方案或结果是否有问题。

请严格按以下格式输出（每个任务方一段）：

对于问题：<原始问题>

任务方1的执行结果为：<精简后的关键结论>
你首先无视任务方1，按照你自己的方式独立执行该问题；然后判断任务方1的方案或执行结果是否存在问题——若没有问题，请回复"确认"；若有问题，请给出你的补充或纠正。

任务方2的执行结果为：<精简后的关键结论>
你首先无视任务方2，按照你自己的方式独立执行该问题；然后判断任务方2的方案或执行结果是否存在问题——若没有问题，请回复"确认"；若有问题，请给出你的补充或纠正。

（依此类推，覆盖全部任务方）
"""

/**
 * 默认回传方提示词（已按用户描述润色）。
 * 负责汇总所有监督方的回复，过长则精简，并将对应回复分发到各任务方。
 */
const val DEFAULT_FEEDBACK_PROMPT = """
你是一个"回传方"。下方汇集了多个【监督方】对各个【任务方】的评估与回复。
你的职责：
1. 汇总所有监督方的回复，若内容过长请【精简但保留关键信息与分歧点】（你的核心任务是内容精简）；
2. 将每个监督方对各任务方的回复，整理后【分发回对应的任务方】，使每个任务方都能看到针对自己的全部评估；
3. 保持原始判断（确认 / 纠正）不变，不擅自修改监督方的结论。

请严格按以下格式输出（每个任务方一段，段内列出各监督方对该任务方的回复）：

任务方1：
- 监督方A的回复：<原文要点>
- 监督方B的回复：<原文要点>

任务方2：
- 监督方A的回复：<原文要点>
- 监督方B的回复：<原文要点>

（依此类推，覆盖全部任务方与监督方）
"""

/**
 * 判断监督方回复是否表示"确认"（无问题）。
 */
fun isConfirmReply(reply: String): Boolean {
    return reply.contains("确认") && !reply.contains("不确认") && !reply.contains("无法确认")
}
