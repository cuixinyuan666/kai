package com.inspiredandroid.kai.data.collaboration

import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.ServiceEntry
import io.github.vinceglb.filekit.PlatformFile

object CollaborationSupport {

    const val DEFAULT_MIN_SCORE_THRESHOLD = 50.0

    fun resolveEligibleModels(repository: DataRepository, minScore: Double): List<ModelRef> {
        val benchmarks = runCatching { repository.getModelBenchmarks() }
            .getOrDefault(emptyList())
            .associate { it.modelKey to it.totalScore }
        val entries: List<ServiceEntry> = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        return buildList {
            for (entry in entries) {
                val modelIds = entry.modelOptions.map { it.id }.ifEmpty { listOfNotNull(entry.modelId) }
                for (modelId in modelIds.distinct()) {
                    val score = benchmarks["${entry.serviceId}::$modelId"] ?: 0.0
                    if (score >= minScore) {
                        add(ModelRef(entry.instanceId, modelId))
                    }
                }
            }
        }
    }

    fun buildLabelResolver(entries: List<ServiceEntry>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (entry in entries) {
            for (opt in entry.modelOptions) {
                map[ModelRef(entry.instanceId, opt.id).key] = "${entry.serviceName} / ${opt.label}"
            }
        }
        return map
    }

    fun pickHighestScoredModel(
        eligible: List<ModelRef>,
        repository: DataRepository,
    ): ModelRef? = rankSummaryCandidates(eligible, repository).firstOrNull()

    /**
     * War-mode summary order: optional wizard pick first, then remaining models by
     * test score high → low. Benchmark keys are `serviceId::modelId`.
     */
    fun rankSummaryCandidates(
        eligible: List<ModelRef>,
        repository: DataRepository,
        preferredFirst: ModelRef? = null,
    ): List<ModelRef> = rankSummaryCandidates(
        eligible = eligible,
        scoresByRefKey = scoresByRefKey(eligible, repository),
        preferredFirst = preferredFirst,
    )

    fun rankSummaryCandidates(
        eligible: List<ModelRef>,
        scoresByRefKey: Map<String, Double>,
        preferredFirst: ModelRef? = null,
    ): List<ModelRef> {
        val ranked = eligible.sortedWith(
            compareByDescending<ModelRef> { scoresByRefKey[it.key] ?: 0.0 }
                .thenBy { it.key },
        )
        val preferred = preferredFirst?.takeIf { want -> ranked.any { it.key == want.key } }
            ?: return ranked
        return listOf(preferred) + ranked.filter { it.key != preferred.key }
    }

    private fun scoresByRefKey(
        eligible: List<ModelRef>,
        repository: DataRepository,
    ): Map<String, Double> {
        val benchmarks = runCatching { repository.getModelBenchmarks() }
            .getOrDefault(emptyList())
            .associate { it.modelKey to it.totalScore }
        val serviceIdByInstance = runCatching { repository.getServiceEntries() }
            .getOrDefault(emptyList())
            .associate { it.instanceId to it.serviceId }
        return eligible.associate { ref ->
            val serviceId = serviceIdByInstance[ref.instanceId]
            val score = serviceId?.let { benchmarks["$it::${ref.modelId}"] }
                ?: benchmarks[ref.key]
                ?: 0.0
            ref.key to score
        }
    }

    data class RetryCallResult(val text: String?, val attempts: Int)

    suspend fun callWithRetry(
        repository: DataRepository,
        conversationId: String,
        ref: ModelRef,
        prompt: String,
        files: List<PlatformFile>,
        retryCount: Int,
        timeoutMs: Long,
        label: String,
        notifyOnFailure: Boolean,
        cancelled: () -> Boolean,
        onRetryEvent: (attempt: Int) -> Unit,
        onFailureNotify: (title: String, body: String) -> Unit,
    ): RetryCallResult {
        var used = 0
        repeat(retryCount + 1) { attempt ->
            used = attempt + 1
            if (cancelled()) return RetryCallResult(null, used)
            try {
                val result = repository.askInConversation(
                    conversationId = conversationId,
                    instanceId = ref.instanceId,
                    modelId = ref.modelId,
                    question = prompt,
                    timeoutMs = timeoutMs,
                    files = files,
                )
                if (result.isNotBlank()) return RetryCallResult(result, used)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            if (attempt < retryCount) {
                onRetryEvent(attempt + 1)
            }
        }
        if (notifyOnFailure) {
            onFailureNotify("模型调用失败", "$label 在重试 $retryCount 次后仍失败。")
        }
        return RetryCallResult(null, used)
    }

    fun snapshotFromConversation(
        ref: ModelRef,
        label: String,
        conversationId: String,
        repository: DataRepository,
    ): CollaborationModelSnapshot {
        val conv = repository.savedConversations.value.find { it.id == conversationId }
        val response = conv?.messages?.lastOrNull { it.role == "assistant" }?.content
        return CollaborationModelSnapshot(
            ref = ref,
            label = label,
            response = response,
            failed = response.isNullOrBlank(),
        )
    }
}
