package com.inspiredandroid.kai.data.collaboration

import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.ServiceEntry
import io.github.vinceglb.filekit.PlatformFile

object CollaborationSupport {

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
                    if (score > minScore) {
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
    ): ModelRef? {
        val benchmarks = runCatching { repository.getModelBenchmarks() }
            .getOrDefault(emptyList())
            .associate { it.modelKey to it.totalScore }
        return eligible.maxByOrNull { benchmarks[it.key] ?: 0.0 }
    }

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
    ): String? {
        repeat(retryCount + 1) { attempt ->
            if (cancelled()) return null
            try {
                val result = repository.askInConversation(
                    conversationId = conversationId,
                    instanceId = ref.instanceId,
                    modelId = ref.modelId,
                    question = prompt,
                    timeoutMs = timeoutMs,
                    files = files,
                )
                if (result.isNotBlank()) return result
            } catch (_: Exception) {
                // retry
            }
            if (attempt < retryCount) {
                onRetryEvent(attempt + 1)
            }
        }
        if (notifyOnFailure) {
            onFailureNotify("模型调用失败", "$label 在重试 $retryCount 次后仍失败。")
        }
        return null
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
