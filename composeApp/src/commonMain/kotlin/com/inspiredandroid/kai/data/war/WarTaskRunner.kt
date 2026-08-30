package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.collaboration.CollaborationSupport
import com.inspiredandroid.kai.data.collaboration.ModelRef
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.concurrent.Volatile

/**
 * 战争模式编排：第 1 轮并行作答 → 总结分析 → 第 2 轮分歧投票 → 持久化结果。
 */
class WarTaskRunner(
    private val repository: DataRepository,
    private val listener: WarListener,
) {
    @Volatile
    private var cancelled: Boolean = false

    fun cancel() {
        cancelled = true
    }

    suspend fun runTask(params: WarWizardParams) {
        val eligible = CollaborationSupport.resolveEligibleModels(repository, params.minScoreThreshold)
        if (eligible.isEmpty()) {
            listener.onNotify("无法开始战争模式", "没有模型测试分数 > ${params.minScoreThreshold} 的模型。")
            listener.onTaskFinished("", "没有符合条件的模型。")
            return
        }

        val services = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        val labelResolver = CollaborationSupport.buildLabelResolver(services)
        val summaryRef = params.summaryModelOverride
            ?: CollaborationSupport.pickHighestScoredModel(eligible, repository)
            ?: eligible.first()
        val summaryLabel = labelResolver[summaryRef.key] ?: summaryRef.modelId

        val taskId = repository.createWarTask(
            question = params.question,
            params = params,
            summaryRef = summaryRef,
        )
        repository.createWarResultConversation(taskId)
        listener.onTaskStarted(taskId)

        val timeoutMs = params.maxWaitSeconds.coerceAtLeast(1).toLong() * 1000L

        // --- Round 1 ---
        listener.onEvent(WarEvent(WarPhase.ROUND1_DISTRIBUTE, "第 1 轮：向 ${eligible.size} 个模型并行发送任务。"))

        val modelConversations = eligible.map { ref ->
            val label = labelResolver[ref.key] ?: ref.modelId
            val convId = repository.createWarModelConversation(
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

        coroutineScope {
            modelConversations.map { (ref, pair) ->
                val (convId, label) = pair
                async {
                    if (cancelled) return@async
                    listener.onModelStatusChanged(convId, CollaborationModelStatus.RUNNING)
                    listener.onEvent(
                        WarEvent(
                            WarPhase.ROUND1_RESPONDING,
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
                                WarEvent(
                                    WarPhase.ROUND1_RESPONDING,
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
                }
            }.awaitAll()
        }

        if (cancelled) {
            listener.onEvent(WarEvent(WarPhase.CANCELLED, "任务已取消。"))
            listener.onTaskFinished(taskId, "任务已取消。")
            return
        }

        val snapshots = modelConversations.map { (ref, pair) ->
            val (convId, label) = pair
            CollaborationSupport.snapshotFromConversation(ref, label, convId, repository)
        }
        val successSnapshots = snapshots.filter { !it.failed }
        val successCount = successSnapshots.size

        if (successCount == 0) {
            val failedResult = WarTaskResult(
                question = params.question,
                summaryModelKey = summaryRef.key,
                summaryModelLabel = summaryLabel,
                analysisFailed = true,
                analysisError = "所有模型第 1 轮均失败。",
                phase = WarPhase.FAILED.name,
                round1SuccessCount = 0,
                round1TotalCount = eligible.size,
            )
            repository.saveWarTaskResult(taskId, failedResult)
            listener.onEvent(WarEvent(WarPhase.FAILED, failedResult.analysisError!!))
            listener.onTaskFinished(taskId, failedResult.analysisError!!)
            return
        }

        // --- Analysis ---
        listener.onEvent(WarEvent(WarPhase.ANALYZING, "总结模型 $summaryLabel 正在分析各模型回答…"))
        val analysisPrompt = WarPromptBuilder.buildAnalysisPrompt(params.question, successSnapshots)
        var analysisResult = runAnalysis(summaryRef, analysisPrompt, timeoutMs)
        if (analysisResult == null) {
            analysisResult = runAnalysis(summaryRef, analysisPrompt, timeoutMs)
        }

        if (analysisResult == null) {
            val failedResult = WarTaskResult(
                question = params.question,
                summaryModelKey = summaryRef.key,
                summaryModelLabel = summaryLabel,
                analysisFailed = true,
                analysisError = "总结模型未能输出有效 JSON 分析结果。",
                phase = WarPhase.DONE.name,
                round1SuccessCount = successCount,
                round1TotalCount = eligible.size,
            )
            repository.saveWarTaskResult(taskId, failedResult)
            listener.onEvent(WarEvent(WarPhase.DONE, failedResult.analysisError!!))
            if (params.notifyOnComplete) listener.onNotify("战争模式结束", failedResult.analysisError!!)
            listener.onTaskFinished(taskId, failedResult.analysisError!!)
            return
        }

        val commonPoints = analysisResult.commonPoints
        val aspects = analysisResult.aspects

        if (aspects.isEmpty()) {
            val unanimousResult = WarTaskResult(
                question = params.question,
                commonPoints = commonPoints,
                aspectResults = emptyList(),
                summaryModelKey = summaryRef.key,
                summaryModelLabel = summaryLabel,
                phase = WarPhase.DONE.name,
                round1SuccessCount = successCount,
                round1TotalCount = eligible.size,
            )
            repository.saveWarTaskResult(taskId, unanimousResult)
            val summary = "战争模式完成：全体一致，无分歧方面。"
            listener.onEvent(WarEvent(WarPhase.DONE, summary))
            if (params.notifyOnComplete) listener.onNotify("战争模式结束", summary)
            listener.onTaskFinished(taskId, summary)
            return
        }

        // --- Round 2 ---
        listener.onEvent(WarEvent(WarPhase.ROUND2_DISTRIBUTE, "第 2 轮：向 ${successSnapshots.size} 个模型发送 ${aspects.size} 个分歧方面投票。"))
        val votePrompt = WarPromptBuilder.buildVotePrompt(aspects)
        val successfulConversations = modelConversations.filter { (ref, _) ->
            successSnapshots.any { it.ref.key == ref.key }
        }

        val allVotes = coroutineScope {
            successfulConversations.map { (ref, pair) ->
                val (convId, label) = pair
                async {
                    if (cancelled) return@async emptyList<WarModelVote>()
                    listener.onEvent(
                        WarEvent(
                            WarPhase.ROUND2_RESPONDING,
                            "投票中…",
                            label,
                            sessionKey = ref.key,
                        ),
                    )
                    val voteRaw = CollaborationSupport.callWithRetry(
                        repository = repository,
                        conversationId = convId,
                        ref = ref,
                        prompt = votePrompt,
                        files = emptyList(),
                        retryCount = params.retryCount,
                        timeoutMs = timeoutMs,
                        label = label,
                        notifyOnFailure = params.notifyOnFailure,
                        cancelled = { cancelled },
                        onRetryEvent = { attempt ->
                            listener.onEvent(
                                WarEvent(
                                    WarPhase.ROUND2_RESPONDING,
                                    "$label 投票第 $attempt 次失败，重试中…",
                                    label,
                                    sessionKey = ref.key,
                                ),
                            )
                        },
                        onFailureNotify = listener::onNotify,
                    )
                    if (voteRaw.isNullOrBlank()) {
                        aspects.map { aspect ->
                            WarModelVote(
                                modelKey = ref.key,
                                modelLabel = label,
                                choice = WarVoteChoice.ABSTAIN.name,
                                reason = "",
                            )
                        }
                    } else {
                        WarAnalysisParser.parseVotes(voteRaw, aspects, ref.key, label)
                    }
                }
            }.awaitAll()
        }

        val aspectResults = WarAnalysisParser.aggregateAspectResults(aspects, allVotes)
        val finalResult = WarTaskResult(
            question = params.question,
            commonPoints = commonPoints,
            aspectResults = aspectResults,
            summaryModelKey = summaryRef.key,
            summaryModelLabel = summaryLabel,
            phase = WarPhase.DONE.name,
            round1SuccessCount = successCount,
            round1TotalCount = eligible.size,
        )
        repository.saveWarTaskResult(taskId, finalResult)

        val summary = buildString {
            append("战争模式完成：${aspects.size} 个分歧方面")
            aspectResults.forEach { ar ->
                append("；${ar.aspect.title} 同意 ${ar.agreeCount}/${ar.validVoteCount}")
            }
        }
        listener.onEvent(WarEvent(WarPhase.DONE, summary))
        if (params.notifyOnComplete) listener.onNotify("战争模式结束", summary)
        listener.onTaskFinished(taskId, summary)
    }

    private suspend fun runAnalysis(
        summaryRef: ModelRef,
        prompt: String,
        timeoutMs: Long,
    ): WarAnalysisResult? {
        val raw = repository.askWithInstanceModel(
            instanceId = summaryRef.instanceId,
            modelId = summaryRef.modelId,
            prompt = prompt,
            systemPrompt = WarPromptBuilder.ANALYST_SYSTEM_PROMPT,
            timeoutMs = timeoutMs,
        )
        return WarAnalysisParser.parseAnalysis(raw)
    }
}
