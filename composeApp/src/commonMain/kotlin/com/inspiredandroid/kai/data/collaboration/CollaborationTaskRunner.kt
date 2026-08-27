package com.inspiredandroid.kai.data.collaboration

import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.data.ConversationMetadata
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.ServiceEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.concurrent.Volatile

/**
 * 协作任务编排：为每个达标模型创建独立会话，并以单一模式完整流水线并行作答。
 */
class CollaborationTaskRunner(
    private val repository: DataRepository,
    private val listener: CollaborationListener,
) {
    @Volatile
    private var cancelled: Boolean = false

    fun cancel() {
        cancelled = true
    }

    suspend fun runTask(params: CollaborationWizardParams) {
        val eligible = resolveEligibleModels(params.minScoreThreshold)
        if (eligible.isEmpty()) {
            listener.onNotify("无法开始协作", "没有模型测试分数 > ${params.minScoreThreshold} 的模型。")
            listener.onTaskFinished("", "没有符合条件的模型。")
            return
        }

        val services = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        val labelResolver = buildLabelResolver(services)

        val taskId = repository.createCollaborationTask(
            question = params.question,
            params = params,
        )

        listener.onEvent(
            CollaborationEvent(
                round = 1,
                CollaborationPhase.DISTRIBUTE,
                "向 ${eligible.size} 个模型并行发送指令。",
            ),
        )

        val modelConversations = eligible.map { ref ->
            val label = labelResolver[ref.key] ?: ref.modelId
            val convId = repository.createCollaborationModelConversation(
                taskId = taskId,
                ref = ref,
                folderTitle = ConversationFolderManager.modelFolderTitle(
                    label.substringBefore("/").trim(),
                    label.substringAfter("/", label),
                ),
                question = params.question,
                params = params,
            )
            ref to (convId to label)
        }

        val timeoutMs = params.maxWaitSeconds.coerceAtLeast(1).toLong() * 1000L

        coroutineScope {
            modelConversations.map { (ref, pair) ->
                val (convId, label) = pair
                async {
                    if (cancelled) return@async
                    listener.onModelStatusChanged(convId, CollaborationModelStatus.RUNNING)
                    listener.onEvent(
                        CollaborationEvent(
                            round = 1,
                            CollaborationPhase.RESPONDING,
                            "作答中…",
                            label,
                            sessionKey = ref.key,
                        ),
                    )
                    val result = callWithRetry(
                        conversationId = convId,
                        ref = ref,
                        prompt = params.question,
                        retryCount = params.retryCount,
                        timeoutMs = timeoutMs,
                        label = label,
                        notifyOnFailure = params.notifyOnFailure,
                    )
                    val status = if (result.isNullOrBlank()) {
                        CollaborationModelStatus.FAILED
                    } else {
                        CollaborationModelStatus.COMPLETED
                    }
                    repository.updateCollaborationModelStatus(convId, status, result)
                    listener.onModelStatusChanged(convId, status)
                    if (status == CollaborationModelStatus.COMPLETED) {
                        listener.onEvent(
                            CollaborationEvent(
                                round = 1,
                                CollaborationPhase.RESPONDING,
                                result!!,
                                label,
                                isAnswer = true,
                                sessionKey = ref.key,
                            ),
                        )
                    } else {
                        listener.onEvent(
                            CollaborationEvent(
                                round = 1,
                                CollaborationPhase.FAILED,
                                "$label 调用失败。",
                                label,
                                sessionKey = ref.key,
                            ),
                        )
                    }
                }
            }.awaitAll()
        }

        val snapshots = modelConversations.map { (ref, pair) ->
            val (convId, label) = pair
            val conv = repository.savedConversations.value.find { it.id == convId }
            val response = conv?.messages?.lastOrNull { it.role == "assistant" }?.content
            CollaborationModelSnapshot(
                ref = ref,
                label = label,
                response = response,
                failed = response.isNullOrBlank(),
            )
        }
        val successCount = snapshots.count { !it.failed }
        val summary = "协作任务完成：$successCount / ${eligible.size} 个模型成功作答。"

        listener.onEvent(
            CollaborationEvent(
                round = 1,
                CollaborationPhase.DONE,
                summary,
            ),
        )

        if (params.notifyOnComplete) {
            listener.onNotify(
                "协作任务结束",
                when {
                    successCount == 0 -> "所有模型均失败，请查看记录。"
                    successCount < eligible.size -> "部分模型失败，成功 $successCount 个。"
                    else -> "全部 ${eligible.size} 个模型均已回复。"
                },
            )
        }

        listener.onTaskFinished(taskId, summary)
    }

    private fun resolveEligibleModels(minScore: Double): List<ModelRef> {
        val benchmarks = runCatching { repository.getModelBenchmarks() }
            .getOrDefault(emptyList())
            .associate { it.modelKey to it.totalScore }
        val entries: List<ServiceEntry> = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        return buildList {
            for (entry in entries) {
                val modelIds = entry.modelOptions.map { it.id }.ifEmpty { listOfNotNull(entry.modelId) }
                for (modelId in modelIds.distinct()) {
                    val score = benchmarks["${entry.serviceId}::$modelId"] ?: 0.0
                    if (score > minScore) {
                        add(ModelRef(entry.instanceId, modelId))
                    }
                }
            }
        }
    }

    private fun buildLabelResolver(entries: List<ServiceEntry>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (entry in entries) {
            for (opt in entry.modelOptions) {
                map[ModelRef(entry.instanceId, opt.id).key] = "${entry.serviceName} / ${opt.label}"
            }
        }
        return map
    }

    private suspend fun callWithRetry(
        conversationId: String,
        ref: ModelRef,
        prompt: String,
        retryCount: Int,
        timeoutMs: Long,
        label: String,
        notifyOnFailure: Boolean,
    ): String? {
        repeat(retryCount + 1) { attempt ->
            if (cancelled) return null
            try {
                val result = repository.askInConversation(
                    conversationId = conversationId,
                    instanceId = ref.instanceId,
                    modelId = ref.modelId,
                    question = prompt,
                    timeoutMs = timeoutMs,
                )
                if (result.isNotBlank()) return result
            } catch (_: Exception) {
                // retry
            }
            if (attempt < retryCount) {
                listener.onEvent(
                    CollaborationEvent(
                        round = 1,
                        CollaborationPhase.RESPONDING,
                        "$label 第 ${attempt + 1} 次失败，重试中…",
                        label,
                        sessionKey = ref.key,
                    ),
                )
            }
        }
        if (notifyOnFailure) {
            listener.onNotify("模型调用失败", "$label 在重试 $retryCount 次后仍失败。")
        }
        return null
    }
}
