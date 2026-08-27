package com.inspiredandroid.kai.data.collaboration

import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.ServiceEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.concurrent.Volatile

/**
 * 协作模式编排引擎。
 *
 * 将同一条指令并行发送给所有「模型测试总分 > 0」的模型；一轮结束后，
 * 可仅向上一轮成功作答的模型继续发送（含原始问题、上次回答与审阅格式）。
 */
class CollaborationOrchestrator(
    private val repository: DataRepository,
    private val listener: CollaborationListener,
) {
    @Volatile
    private var cancelled: Boolean = false

    fun cancel() {
        cancelled = true
    }

    private fun buildLabelResolver(): Map<String, String> {
        val entries: List<ServiceEntry> = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        val map = mutableMapOf<String, String>()
        for (entry in entries) {
            for (opt in entry.modelOptions) {
                map[ModelRef(entry.instanceId, opt.id).key] = opt.label
            }
        }
        return map
    }

    private fun modelLabel(ref: ModelRef, aliases: Map<String, String>, labelResolver: Map<String, String>): String {
        val modelName = aliases[ref.key] ?: labelResolver[ref.key] ?: ref.modelId
        return modelName
    }

    /**
     * 执行一轮协作。
     *
     * @param originalQuestion 用户原始提问（始终保留在跟进轮次中）
     * @param roundNumber 当前轮次（从 1 开始）
     * @param targetModels 本轮目标模型；首轮为空时自动选取测试分 > 0 的模型
     * @param previousAnswers 上一轮各模型的成功回答，用于构建跟进提示
     */
    suspend fun runRound(
        originalQuestion: String,
        config: CollaborationConfig,
        roundNumber: Int,
        targetModels: List<ModelRef> = emptyList(),
        previousAnswers: Map<ModelRef, String> = emptyMap(),
    ) {
        val aliases = config.modelAliases
        val labelResolver = buildLabelResolver()

        val models = when {
            targetModels.isNotEmpty() -> targetModels
            roundNumber == 1 -> resolveEligibleModels()
            else -> emptyList()
        }

        if (models.isEmpty()) {
            val message = if (roundNumber == 1) {
                "没有模型测试总分 > 0，请先运行「模型测试」。"
            } else {
                "上一轮没有成功作答的模型，无法继续。"
            }
            listener.onEvent(CollaborationEvent(roundNumber, CollaborationPhase.FAILED, message))
            listener.onRoundFinished(
                roundNumber,
                CollaborationRoundSnapshot(roundNumber, emptyList()),
                canContinue = false,
            )
            listener.onFinished(message, allSucceeded = false)
            return
        }

        listener.onEvent(
            CollaborationEvent(
                roundNumber,
                CollaborationPhase.DISTRIBUTE,
                "第 $roundNumber 轮：向 ${models.size} 个模型并行发送指令。",
            ),
        )

        val timeoutMs = config.maxWaitSeconds.coerceAtLeast(1).toLong() * 1000L
        val analysisScores = mutableMapOf<ModelRef, Double>()

        val results = coroutineScope {
            models.map { ref ->
                async {
                    val label = modelLabel(ref, aliases, labelResolver)
                    val prompt = if (roundNumber == 1) {
                        originalQuestion
                    } else {
                        val prev = previousAnswers[ref]
                        if (prev.isNullOrBlank()) {
                            return@async ref to (label to null)
                        }
                        formatFollowUpPrompt(originalQuestion, prev)
                    }

                    listener.onEvent(
                        CollaborationEvent(
                            roundNumber,
                            CollaborationPhase.RESPONDING,
                            if (roundNumber == 1) "作答中…" else "审阅跟进中…",
                            label,
                            sessionKey = ref.key,
                        ),
                    )

                    val result = callWithRetry(
                        ref = ref,
                        prompt = prompt,
                        systemPrompt = config.modelPrompt,
                        retryCount = config.retryCount,
                        timeoutMs = timeoutMs,
                        roundNumber = roundNumber,
                        sourceLabel = label,
                        notifyOnFailure = config.notifyOnFailure,
                    )
                    ref to (label to result)
                }
            }.awaitAll()
        }

        val snapshots = mutableListOf<CollaborationModelSnapshot>()
        var anyFailed = false

        results.forEach { (ref, pair) ->
            val (label, response) = pair
            val failed = response.isNullOrBlank()
            if (failed) {
                anyFailed = true
                listener.onEvent(
                    CollaborationEvent(
                        roundNumber,
                        CollaborationPhase.FAILED,
                        "$label 调用失败且重试耗尽。",
                        label,
                        sessionKey = ref.key,
                    ),
                )
            } else {
                analysisScores[ref] = (analysisScores[ref] ?: 0.0) + scoreResponse(response)
                listener.onEvent(
                    CollaborationEvent(
                        roundNumber,
                        CollaborationPhase.RESPONDING,
                        "${label} 完成（${response.length} 字）。",
                        label,
                        sessionKey = ref.key,
                    ),
                )
                listener.onEvent(
                    CollaborationEvent(
                        roundNumber,
                        CollaborationPhase.RESPONDING,
                        response,
                        label,
                        isAnswer = true,
                        sessionKey = ref.key,
                    ),
                )
            }
            snapshots += CollaborationModelSnapshot(ref = ref, label = label, response = response, failed = failed)
        }

        val successfulCount = snapshots.count { !it.failed && !it.response.isNullOrBlank() }
        val canContinue = successfulCount > 0

        listener.onEvent(
            CollaborationEvent(
                roundNumber,
                CollaborationPhase.DONE,
                "第 $roundNumber 轮结束：成功 $successfulCount / ${models.size}。",
            ),
        )

        val scores = analysisScores.map { (ref, v) ->
            ModelScore(instanceId = ref.instanceId, modelId = ref.modelId, analysisScore = v)
        }
        if (scores.isNotEmpty()) {
            listener.onScores(scores)
        }

        val snapshot = CollaborationRoundSnapshot(round = roundNumber, responses = snapshots)
        listener.onRoundFinished(roundNumber, snapshot, canContinue)

        val summary = buildString {
            append("第 $roundNumber 轮：${successfulCount}/${models.size} 个模型成功作答。")
            if (canContinue) {
                append(" 可继续下一轮（仅向成功模型发送）。")
            }
        }

        if (config.notifyOnComplete) {
            listener.onNotify(
                "协作第 $roundNumber 轮结束",
                when {
                    anyFailed && successfulCount == 0 -> "所有模型均失败，请查看记录。"
                    anyFailed -> "部分模型失败，成功 $successfulCount 个。"
                    else -> "全部 ${models.size} 个模型均已回复。"
                },
            )
        }
        listener.onFinished(summary, allSucceeded = !anyFailed)
    }

    /** 仅模型测试总分严格大于 0 的模型参与首轮。 */
    private fun resolveEligibleModels(): List<ModelRef> {
        val benchmarks = runCatching { repository.getModelBenchmarks() }
            .getOrDefault(emptyList())
            .associate { it.modelKey to it.totalScore }
        val entries: List<ServiceEntry> = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        return buildList {
            for (entry in entries) {
                val modelIds = entry.modelOptions.map { it.id }.ifEmpty { listOfNotNull(entry.modelId) }
                for (modelId in modelIds.distinct()) {
                    val score = benchmarks["${entry.serviceId}::$modelId"] ?: 0.0
                    if (score > 0.0) {
                        add(ModelRef(entry.instanceId, modelId))
                    }
                }
            }
        }
    }

    private suspend fun callWithRetry(
        ref: ModelRef,
        prompt: String,
        systemPrompt: String?,
        retryCount: Int,
        timeoutMs: Long,
        roundNumber: Int,
        sourceLabel: String?,
        notifyOnFailure: Boolean,
    ): String? {
        repeat(retryCount + 1) { attempt ->
            if (cancelled) return null
            try {
                val result = repository.askWithInstanceModel(
                    instanceId = ref.instanceId,
                    modelId = ref.modelId,
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    timeoutMs = timeoutMs,
                )
                if (result.isNotBlank()) return result
            } catch (_: Exception) {
                // fall through to retry
            }
            if (attempt < retryCount) {
                listener.onEvent(
                    CollaborationEvent(
                        roundNumber,
                        CollaborationPhase.RESPONDING,
                        "${sourceLabel ?: ref.modelId} 第 ${attempt + 1} 次失败，重试中…",
                        sourceLabel,
                        sessionKey = ref.key,
                    ),
                )
            }
        }
        if (notifyOnFailure) {
            listener.onNotify("模型调用失败", "${sourceLabel ?: ref.modelId} 在重试 $retryCount 次后仍失败。")
        }
        return null
    }

    private fun scoreResponse(response: String): Double {
        if (response.isBlank()) return 0.0
        val len = response.length
        return when {
            len < 20 -> 30.0
            len < 200 -> 70.0
            len <= 4000 -> 90.0
            else -> 75.0
        }
    }
}
