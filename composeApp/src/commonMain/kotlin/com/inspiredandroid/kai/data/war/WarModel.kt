package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.collaboration.ModelRef
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.Serializable

/** 战争模式向导一次性任务参数。 */
data class WarWizardParams(
    val question: String,
    val minScoreThreshold: Double,
    val maxWaitSeconds: Int,
    val retryCount: Int,
    val notifyOnFailure: Boolean,
    val notifyOnComplete: Boolean,
    val attachedFiles: List<PlatformFile> = emptyList(),
    /** null 表示自动选择参与模型中测试分数最高者。 */
    val summaryModelOverride: ModelRef? = null,
)

enum class WarPhase {
    IDLE,
    ROUND1_DISTRIBUTE,
    ROUND1_RESPONDING,
    ANALYZING,
    ROUND2_DISTRIBUTE,
    ROUND2_RESPONDING,
    DONE,
    FAILED,
    CANCELLED,
}

data class WarEvent(
    val phase: WarPhase,
    val text: String,
    val sourceLabel: String? = null,
    val isAnswer: Boolean = false,
    val sessionKey: String? = null,
)

@Serializable
data class WarAspect(
    val id: String,
    val title: String,
    val description: String,
)

enum class WarVoteChoice {
    AGREE,
    DISAGREE,
    ABSTAIN,
}

@Serializable
data class WarModelVote(
    val modelKey: String,
    val modelLabel: String,
    val choice: String,
    val reason: String = "",
)

@Serializable
data class WarAspectResult(
    val aspect: WarAspect,
    val votes: List<WarModelVote> = emptyList(),
) {
    val agreeCount: Int get() = votes.count { it.choice == WarVoteChoice.AGREE.name }
    val disagreeCount: Int get() = votes.count { it.choice == WarVoteChoice.DISAGREE.name }
    val validVoteCount: Int get() = votes.count { it.choice != WarVoteChoice.ABSTAIN.name }
}

@Serializable
data class WarTaskResult(
    val question: String,
    val commonPoints: List<String> = emptyList(),
    val aspectResults: List<WarAspectResult> = emptyList(),
    val summaryModelKey: String? = null,
    val summaryModelLabel: String? = null,
    val analysisFailed: Boolean = false,
    val analysisError: String? = null,
    val phase: String = WarPhase.DONE.name,
    val round1SuccessCount: Int = 0,
    val round1TotalCount: Int = 0,
)

interface WarListener {
    fun onTaskStarted(taskId: String)
    fun onEvent(event: WarEvent)
    fun onNotify(title: String, body: String)
    fun onModelStatusChanged(conversationId: String, status: CollaborationModelStatus)
    fun onTaskFinished(taskId: String, summary: String)
}

fun WarTaskResult.encodeJson(): String = com.inspiredandroid.kai.data.SharedJson.encodeToString(this)

fun decodeWarTaskResult(json: String): WarTaskResult? = runCatching {
    com.inspiredandroid.kai.data.SharedJson.decodeFromString<WarTaskResult>(json)
}.getOrNull()
