package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.FolderAttachments
import com.inspiredandroid.kai.data.TaskAutoScore
import com.inspiredandroid.kai.data.collaboration.CollaborationSupport
import com.inspiredandroid.kai.data.collaboration.ModelRef
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.time.TimeSource

/**
 * 战争模式编排：第 1 轮并行作答 → 总结分析 → 按配置轮次对分歧投票（默认 2 轮）→ 持久化结果。
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
            listener.onNotify("无法开始战争模式", "没有模型测试分数 ≥ ${params.minScoreThreshold} 的模型。")
            listener.onTaskFinished("", "没有符合条件的模型。")
            return
        }

        val services = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        val labelResolver = CollaborationSupport.buildLabelResolver(services)
        val summaryCandidates = CollaborationSupport.rankSummaryCandidates(
            eligible = eligible,
            repository = repository,
            preferredFirst = params.summaryModelOverride,
        )
        var summaryRef = summaryCandidates.first()
        var summaryLabel = labelResolver[summaryRef.key] ?: summaryRef.modelId

        val promptPrefix = FolderAttachments.promptPrefix(params.attachedFiles)
        val leafFiles = FolderAttachments.withoutDirectories(params.attachedFiles)
        val prompt = promptPrefix + params.question

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
                question = prompt,
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
                    val started = TimeSource.Monotonic.markNow()
                    val call = CollaborationSupport.callWithRetry(
                        repository = repository,
                        conversationId = convId,
                        ref = ref,
                        prompt = prompt,
                        files = leafFiles,
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
                    val result = call.text
                    val status = if (result.isNullOrBlank()) {
                        CollaborationModelStatus.FAILED
                    } else {
                        CollaborationModelStatus.COMPLETED
                    }
                    repository.updateCollaborationModelStatus(convId, status, result)
                    listener.onModelStatusChanged(convId, status)
                    scoreCall(ref, label, result, started.elapsedNow().inWholeMilliseconds, call.attempts, status == CollaborationModelStatus.FAILED)
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
        val analysisPrompt = WarPromptBuilder.buildAnalysisPrompt(params.question, successSnapshots)
        var analysisResult: WarAnalysisResult? = null
        val attempted = mutableListOf<String>()
        for (candidate in summaryCandidates) {
            if (cancelled) break
            val label = labelResolver[candidate.key] ?: candidate.modelId
            attempted += label
            listener.onEvent(WarEvent(WarPhase.ANALYZING, "总结模型 $label 正在分析各模型回答…"))
            var parsed = runAnalysis(candidate, analysisPrompt, timeoutMs)
            if (parsed == null) {
                parsed = runAnalysis(candidate, analysisPrompt, timeoutMs)
            }
            if (parsed != null) {
                analysisResult = parsed
                summaryRef = candidate
                summaryLabel = label
                break
            }
            listener.onEvent(WarEvent(WarPhase.ANALYZING, "$label 总结失败，改用下一模型。"))
        }
        if (analysisResult == null) {
        }

        if (analysisResult == null) {
            val failedResult = WarTaskResult(
                question = params.question,
                summaryModelKey = summaryRef.key,
                summaryModelLabel = summaryLabel,
                analysisFailed = true,
                analysisError = "所有候选总结模型均未能输出有效 JSON 分析结果。",
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
        val aspects = WarVoting.resolveProposers(analysisResult.aspects, successSnapshots)
        val summaryConvId = repository.createWarModelConversation(
            taskId = taskId,
            ref = summaryRef,
            folderTitle = ConversationFolderManager.modelFolderTitle(
                summaryLabel.substringBefore("/").trim(),
                summaryLabel.substringAfter("/", summaryLabel).trim(),
            ),
            question = params.question,
            params = params,
            isSummaryModel = true,
        )
        repository.appendConversationExchange(
            conversationId = summaryConvId,
            userContent = "请汇总各模型方案的相同点与分歧方案。",
            assistantContent = WarPromptBuilder.formatAnalysisForDisplay(
                WarAnalysisResult(commonPoints = commonPoints, aspects = aspects),
            ),
        )
        repository.updateCollaborationModelStatus(summaryConvId, CollaborationModelStatus.COMPLETED, null)

        fun currentResult(
            phase: String,
            aspectResults: List<WarAspectResult> = emptyList(),
            voteRoundResults: List<WarVoteRoundResult> = emptyList(),
            finalSummary: String? = null,
        ) = WarTaskResult(
            question = params.question,
            commonPoints = commonPoints,
            aspectResults = aspectResults,
            summaryModelKey = summaryRef.key,
            summaryModelLabel = summaryLabel,
            phase = phase,
            round1SuccessCount = successCount,
            round1TotalCount = eligible.size,
            voteRoundResults = voteRoundResults,
            voteRoundCount = params.voteRounds.coerceIn(1, 10),
            finalSummary = finalSummary,
            summaryConversationId = summaryConvId,
        )

        repository.saveWarTaskResult(
            taskId,
            currentResult(
                phase = if (aspects.isEmpty()) WarPhase.DONE.name else WarPhase.ROUND2_DISTRIBUTE.name,
                aspectResults = aspects.map { WarAspectResult(aspect = it) },
            ),
        )

        if (aspects.isEmpty()) {
            val summary = "战争模式完成：全体一致，无分歧方案。"
            repository.saveWarTaskResult(
                taskId,
                currentResult(phase = WarPhase.DONE.name, finalSummary = summary),
            )
            listener.onEvent(WarEvent(WarPhase.DONE, summary))
            if (params.notifyOnComplete) listener.onNotify("战争模式结束", summary)
            listener.onTaskFinished(taskId, summary)
            if (taskId.isNotBlank()) {
                repository.completeTaskConversation(taskId, CollaborationModelStatus.COMPLETED)
            }
            return
        }

        val voteRoundCount = params.voteRounds.coerceIn(1, 10)
        val successfulConversations = modelConversations.filter { (ref, _) ->
            successSnapshots.any { it.ref.key == ref.key }
        }
        val voteRoundResults = mutableListOf<WarVoteRoundResult>()
        var previousAspectResults: List<WarAspectResult> = emptyList()

        for (voteRound in 1..voteRoundCount) {
            if (cancelled) {
                listener.onEvent(WarEvent(WarPhase.CANCELLED, "任务已取消。"))
                listener.onTaskFinished(taskId, "任务已取消。")
                return
            }
            listener.onEvent(
                WarEvent(
                    WarPhase.ROUND2_DISTRIBUTE,
                    "第 $voteRound 轮交叉投票：将分歧方案下发给未提出该方案的模型。",
                ),
            )
            val aspectResults = collectVoteRound(
                taskId = taskId,
                params = params,
                aspects = aspects,
                successfulConversations = successfulConversations,
                timeoutMs = timeoutMs,
                commonPoints = commonPoints,
                summaryRef = summaryRef,
                summaryLabel = summaryLabel,
                successCount = successCount,
                eligibleSize = eligible.size,
                voteRound = voteRound,
                previousRoundResults = voteRoundResults.toList(),
                previousAspectResults = previousAspectResults,
                voteRoundCount = voteRoundCount,
                summaryConvId = summaryConvId,
            )
            voteRoundResults += WarVoteRoundResult(voteRound, aspectResults)
            previousAspectResults = aspectResults
        }

        val lastAspectResults = previousAspectResults
        val finalPrompt = WarPromptBuilder.buildFinalSummaryPrompt(
            question = params.question,
            commonPoints = commonPoints,
            voteRoundResults = voteRoundResults,
        )
        listener.onEvent(WarEvent(WarPhase.ANALYZING, "总结模型正在输出最终汇总…"))
        val finalCall = CollaborationSupport.callWithRetry(
            repository = repository,
            conversationId = summaryConvId,
            ref = summaryRef,
            prompt = finalPrompt,
            files = emptyList(),
            retryCount = params.retryCount,
            timeoutMs = timeoutMs,
            label = summaryLabel,
            notifyOnFailure = params.notifyOnFailure,
            cancelled = { cancelled },
            onRetryEvent = { attempt ->
                listener.onEvent(WarEvent(WarPhase.ANALYZING, "$summaryLabel 最终汇总第 $attempt 次失败，重试中…"))
            },
            onFailureNotify = listener::onNotify,
        )
        val finalSummary = finalCall.text?.takeIf { it.isNotBlank() }
            ?: "已完成 $voteRoundCount 轮交叉投票。"
        repository.updateCollaborationModelStatus(summaryConvId, CollaborationModelStatus.COMPLETED, null)

        val finalResult = currentResult(
            phase = WarPhase.DONE.name,
            aspectResults = lastAspectResults,
            voteRoundResults = voteRoundResults.toList(),
            finalSummary = finalSummary,
        )
        repository.saveWarTaskResult(taskId, finalResult)

        val summary = buildString {
            append("战争模式完成：${aspects.size} 个分歧方案，共 ${voteRoundCount} 轮交叉投票")
            voteRoundResults.forEach { round ->
                append("；第 ${round.round} 轮")
                round.aspectResults.forEach { ar ->
                    append(" ${ar.aspect.title} ${WarVoting.cellText(ar, null)}")
                }
            }
        }
        listener.onEvent(WarEvent(WarPhase.DONE, summary))
        if (params.notifyOnComplete) listener.onNotify("战争模式结束", summary)
        listener.onTaskFinished(taskId, summary)
        if (taskId.isNotBlank()) {
            repository.completeTaskConversation(taskId, CollaborationModelStatus.COMPLETED)
        }
    }

    private suspend fun collectVoteRound(
        taskId: String,
        params: WarWizardParams,
        aspects: List<WarAspect>,
        successfulConversations: List<Pair<ModelRef, Pair<String, String>>>,
        timeoutMs: Long,
        commonPoints: List<String>,
        summaryRef: ModelRef,
        summaryLabel: String,
        successCount: Int,
        eligibleSize: Int,
        voteRound: Int,
        previousRoundResults: List<WarVoteRoundResult>,
        previousAspectResults: List<WarAspectResult>,
        voteRoundCount: Int,
        summaryConvId: String,
    ): List<WarAspectResult> {
        val allVotes = coroutineScope {
            val voteMutex = Mutex()
            val voteBatches = mutableListOf<List<WarModelVote>>()
            successfulConversations.map { (ref, pair) ->
                val (convId, label) = pair
                async {
                    if (cancelled) return@async emptyList<WarModelVote>()
                    val assigned = WarVoting.aspectsForModel(aspects, ref.key, label)
                    val skippedAspects = aspects.filter { aspect -> assigned.none { it.id == aspect.id } }
                    if (assigned.isEmpty()) {
                        val skipBody = buildString {
                            appendLine("按交叉投票规则，你是本轮全部分歧方案的提出方，无需再投。")
                            skippedAspects.forEach { appendLine("- ${it.title}") }
                        }.trimEnd()
                        repository.appendConversationExchange(
                            conversationId = convId,
                            userContent = "第 $voteRound 轮交叉投票",
                            assistantContent = skipBody,
                        )
                        val skipMessage = repository.savedConversations.value
                            .find { it.id == convId }
                            ?.messages
                            ?.lastOrNull { it.role == "assistant" }
                        val skipVotes = skippedAspects.map { aspect ->
                            WarModelVote(
                                modelKey = ref.key,
                                modelLabel = label,
                                choice = WarVoteChoice.ABSTAIN.name,
                                reason = WarVoting.skipReason(aspect.title),
                                aspectId = aspect.id,
                                conversationId = convId,
                                messageId = skipMessage?.id.orEmpty(),
                            )
                        }
                        voteMutex.withLock { voteBatches += skipVotes }
                        listener.onEvent(
                            WarEvent(
                                WarPhase.ROUND2_RESPONDING,
                                "第 $voteRound 轮：提出方，跳过交叉投票。",
                                label,
                                sessionKey = ref.key,
                            ),
                        )
                        return@async skipVotes
                    }
                    val votePrompt = if (voteRound >= 2) {
                        WarPromptBuilder.buildFollowUpCrossVotePrompt(assigned, previousAspectResults, voteRound)
                    } else {
                        WarPromptBuilder.buildCrossVotePrompt(assigned, voteRound)
                    }
                    listener.onEvent(
                        WarEvent(
                            WarPhase.ROUND2_RESPONDING,
                            "第 $voteRound 轮交叉投票中（${assigned.size} 个方案）…",
                            label,
                            sessionKey = ref.key,
                        ),
                    )
                    val voteCall = CollaborationSupport.callWithRetry(
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
                                    "$label 第 $voteRound 轮投票第 $attempt 次失败，重试中…",
                                    label,
                                    sessionKey = ref.key,
                                ),
                            )
                        },
                        onFailureNotify = listener::onNotify,
                    )
                    val voteRaw = voteCall.text
                    val message = repository.savedConversations.value
                        .find { it.id == convId }
                        ?.messages
                        ?.lastOrNull { it.role == "assistant" }
                    val voted = if (voteRaw.isNullOrBlank()) {
                        assigned.map {
                            WarModelVote(
                                modelKey = ref.key,
                                modelLabel = label,
                                choice = WarVoteChoice.ABSTAIN.name,
                                reason = "",
                                aspectId = it.id,
                                conversationId = convId,
                                messageId = message?.id.orEmpty(),
                            )
                        }
                    } else {
                        WarAnalysisParser.parseVotes(voteRaw, assigned, ref.key, label).map { vote ->
                            vote.copy(conversationId = convId, messageId = message?.id.orEmpty())
                        }
                    }
                    val skipVotes = skippedAspects.map { aspect ->
                        WarModelVote(
                            modelKey = ref.key,
                            modelLabel = label,
                            choice = WarVoteChoice.ABSTAIN.name,
                            reason = WarVoting.skipReason(aspect.title),
                            aspectId = aspect.id,
                            conversationId = convId,
                            messageId = message?.id.orEmpty(),
                        )
                    }
                    val votes = voted + skipVotes
                    voteMutex.withLock {
                        voteBatches += votes
                        val partial = WarAnalysisParser.aggregateAspectResults(aspects, voteBatches.toList())
                        repository.saveWarTaskResult(
                            taskId,
                            WarTaskResult(
                                question = params.question,
                                commonPoints = commonPoints,
                                aspectResults = partial,
                                summaryModelKey = summaryRef.key,
                                summaryModelLabel = summaryLabel,
                                phase = WarPhase.ROUND2_RESPONDING.name,
                                round1SuccessCount = successCount,
                                round1TotalCount = eligibleSize,
                                voteRoundResults = previousRoundResults + WarVoteRoundResult(voteRound, partial),
                                voteRoundCount = voteRoundCount,
                                summaryConversationId = summaryConvId,
                            ),
                        )
                    }
                    votes
                }
            }.awaitAll()
        }
        return WarAnalysisParser.aggregateAspectResults(aspects, allVotes)
    }

    private suspend fun runAnalysis(
        summaryRef: ModelRef,
        prompt: String,
        timeoutMs: Long,
    ): WarAnalysisResult? {
        val raw = try {
            repository.askWithInstanceModel(
                instanceId = summaryRef.instanceId,
                modelId = summaryRef.modelId,
                prompt = prompt,
                systemPrompt = WarPromptBuilder.ANALYST_SYSTEM_PROMPT,
                timeoutMs = timeoutMs,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return null
        }
        if (raw.isBlank()) return null
        return WarAnalysisParser.parseAnalysis(raw)
    }

    private fun scoreCall(
        ref: ModelRef,
        label: String,
        response: String?,
        elapsedMs: Long,
        attempts: Int,
        failed: Boolean,
    ) {
        val serviceId = runCatching { repository.getServiceEntries() }
            .getOrDefault(emptyList())
            .find { it.instanceId == ref.instanceId }
            ?.serviceId
            .orEmpty()
        TaskAutoScore.record(
            repository = repository,
            serviceId = serviceId,
            modelId = ref.modelId,
            modelLabel = label,
            response = response,
            elapsedMs = elapsedMs,
            attempts = attempts,
            failed = failed,
        )
    }
}
