package com.inspiredandroid.kai.data.collaboration

import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.data.DataRepository
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
        val eligible = CollaborationSupport.resolveEligibleModels(repository, params.minScoreThreshold)
        if (eligible.isEmpty()) {
            listener.onNotify("无法开始协作", "没有模型测试分数 > ${params.minScoreThreshold} 的模型。")
            listener.onTaskFinished("", "没有符合条件的模型。")
            return
        }

        val services = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        val labelResolver = CollaborationSupport.buildLabelResolver(services)

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
                    val result = CollaborationSupport.callWithRetry(
                        repository = repository,
                        conversationId = convId,
                        ref = ref,
                        prompt = params.question,
                        files = params.attachedFiles,
                        retryCount = params.retryCount,
                        timeoutMs = timeoutMs,
                        label = label,
                        notifyOnFailure = params.notifyOnFailure,
                        cancelled = { cancelled },
                        onRetryEvent = { attempt ->
                            listener.onEvent(
                                CollaborationEvent(
                                    round = 1,
                                    CollaborationPhase.RESPONDING,
                                    "$label 第 $attempt 次失败，重试中…",
                                    label,
                                    sessionKey = ref.key,
                                ),
                            )
                        },
                        onFailureNotify = listener::onNotify,
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
            CollaborationSupport.snapshotFromConversation(ref, label, convId, repository)
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
}
