package com.inspiredandroid.kai.data.collaboration

import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.ServiceEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.concurrent.Volatile
import kotlin.math.roundToInt

/**
 * 协作模式编排引擎。
 *
 * 任务方与监督方直接对话，为每个「任务方 × 监督方」配对生成相互隔离的独立会话。
 *
 * 对话流转（每个会话内）：
 *  1. 任务方输出作答内容，提交给监督方审阅；
 *  2. 监督方提问后，系统按固定格式转发给任务方；
 *  3. 任务方答复后，系统按固定格式转发给监督方；
 *  4. 循环直至监督方回复结束类关键词或达到最大轮次。
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

    private fun roleLabel(base: String, ref: ModelRef, aliases: Map<String, String>, labelResolver: Map<String, String>): String {
        val modelName = aliases[ref.key] ?: labelResolver[ref.key] ?: ref.modelId
        return "$base-$modelName"
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

    suspend fun run(question: String, config: CollaborationConfig) {
        val roles = if (config.mode == CollaborationMode.SCORE_GATED) {
            resolveScoreGatedRoles(config) ?: return
        } else {
            config.roles
        }
        if (roles.taskParties.isEmpty() || roles.supervisors.isEmpty()) {
            listener.onNotify("协作模式未配置完整", "请设置至少一个任务方与一个监督方。")
            listener.onFinished("协作配置不完整，未执行。", allConfirmed = false)
            return
        }

        val aliases = config.modelAliases
        val labelResolver = buildLabelResolver()
        val taskLabels = roles.taskParties.mapIndexed { i, ref -> roleLabel("任务方${i + 1}", ref, aliases, labelResolver) }
        val supervisorLabels = roles.supervisors.mapIndexed { i, ref -> roleLabel("监督方${i + 1}", ref, aliases, labelResolver) }

        val sessionCount = roles.taskParties.size * roles.supervisors.size
        listener.onEvent(
            CollaborationEvent(
                1,
                CollaborationPhase.DISTRIBUTE,
                "将创建 ${sessionCount} 个独立会话（${taskLabels.size} 个任务方 × ${supervisorLabels.size} 个监督方）。",
            ),
        )

        val analysisScores = mutableMapOf<ModelRef, Double>()
        var allConfirmed = true
        var anyFailed = false
        val sessionSummaries = mutableListOf<String>()

        // 1) 各任务方独立作答（每个任务方只作答一次，作为各会话的起点）
        listener.onEvent(CollaborationEvent(1, CollaborationPhase.TASK, "各任务方独立作答中…"))
        val initialTaskAnswers = coroutineScope {
            roles.taskParties.mapIndexed { idx, ref ->
                async {
                    val label = taskLabels[idx]
                    val result = callWithRetry(
                        ref = ref,
                        prompt = question,
                        systemPrompt = config.taskPartyPrompt,
                        retryCount = config.retryCount,
                        phase = CollaborationPhase.TASK,
                        sourceLabel = label,
                        sessionKey = null,
                        notifyOnFailure = config.notifyOnFailure,
                    )
                    idx to (label to result)
                }
            }.awaitAll()
        }.toMap()

        initialTaskAnswers.forEach { (idx, pair) ->
            val (label, result) = pair
            if (result != null) {
                analysisScores[roles.taskParties[idx]] = (analysisScores[roles.taskParties[idx]] ?: 0.0) + scoreResponse(result)
                listener.onEvent(
                    CollaborationEvent(1, CollaborationPhase.TASK, "${label} 作答完成（${result.length} 字）。", label),
                )
                listener.onEvent(
                    CollaborationEvent(
                        1,
                        CollaborationPhase.TASK,
                        result,
                        "$label 的作答",
                        isAnswer = true,
                        roleKind = CollaborationRoleKind.TASK,
                    ),
                )
            } else {
                anyFailed = true
                allConfirmed = false
                listener.onEvent(CollaborationEvent(1, CollaborationPhase.TASK, "${label} 作答失败且重试耗尽。", label))
            }
        }

        if (initialTaskAnswers.values.all { it.second == null }) {
            listener.onEvent(CollaborationEvent(1, CollaborationPhase.FAILED, "所有任务方均作答失败，协作已停止。"))
            listener.onFinished("所有任务方作答失败，协作已停止。", allConfirmed = false)
            return
        }

        // 2) 为每个任务方×监督方配对并行运行独立会话
        val sessionResults = coroutineScope {
            roles.taskParties.indices.flatMap { tIdx ->
                roles.supervisors.indices.map { sIdx ->
                    async {
                        runIsolatedSession(
                            question = question,
                            config = config,
                            sessionKey = CollaborationSessionKey(tIdx, sIdx),
                            taskRef = roles.taskParties[tIdx],
                            supervisorRef = roles.supervisors[sIdx],
                            taskLabel = taskLabels[tIdx],
                            supervisorLabel = supervisorLabels[sIdx],
                            initialTaskAnswer = initialTaskAnswers[tIdx]?.second,
                            analysisScores = analysisScores,
                        )
                    }
                }
            }.awaitAll()
        }

        sessionResults.forEach { result ->
            sessionSummaries += result.summary
            if (!result.confirmed) allConfirmed = false
            if (result.failed) anyFailed = true
        }

        val scores = analysisScores.map { (ref, v) ->
            ModelScore(instanceId = ref.instanceId, modelId = ref.modelId, analysisScore = v)
        }
        listener.onScores(scores)

        val summary = sessionSummaries.joinToString("\n\n")
        if (config.notifyOnComplete) {
            listener.onNotify(
                "协作任务结束",
                when {
                    anyFailed && !allConfirmed -> "部分会话未正常完成，请查看各会话记录。"
                    allConfirmed -> "所有会话均已结束（监督方确认或无更多疑问）。"
                    else -> "已达最大轮次，部分会话仍有待澄清内容。"
                },
            )
        }
        listener.onFinished(summary.ifEmpty { "协作结束。" }, allConfirmed)
    }

    private data class SessionResult(
        val summary: String,
        val confirmed: Boolean,
        val failed: Boolean,
    )

    /**
     * 运行单个隔离会话：任务方[tIdx] 与 监督方[sIdx] 之间的直接对话。
     */
    private suspend fun runIsolatedSession(
        question: String,
        config: CollaborationConfig,
        sessionKey: CollaborationSessionKey,
        taskRef: ModelRef,
        supervisorRef: ModelRef,
        taskLabel: String,
        supervisorLabel: String,
        initialTaskAnswer: String?,
        analysisScores: MutableMap<ModelRef, Double>,
    ): SessionResult {
        val key = sessionKey.storageKey
        val sessionTitle = sessionKey.displayLabel(taskLabel, supervisorLabel)

        if (cancelled) {
            listener.onEvent(
                CollaborationEvent(0, CollaborationPhase.CANCELLED, "用户取消了协作。", sessionKey = key),
            )
            return SessionResult("$sessionTitle：已取消", confirmed = false, failed = true)
        }

        if (initialTaskAnswer.isNullOrBlank()) {
            listener.onEvent(
                CollaborationEvent(1, CollaborationPhase.FAILED, "$sessionTitle：任务方初始作答失败，会话跳过。", sessionKey = key),
            )
            return SessionResult("$sessionTitle：任务方初始作答失败", confirmed = false, failed = true)
        }

        listener.onEvent(
            CollaborationEvent(1, CollaborationPhase.DIALOGUE, "会话开始：$sessionTitle", sessionKey = key),
        )
        listener.onEvent(
            CollaborationEvent(
                1,
                CollaborationPhase.TASK,
                initialTaskAnswer,
                "$taskLabel 提交作答",
                isAnswer = true,
                roleKind = CollaborationRoleKind.TASK,
                sessionKey = key,
            ),
        )

        var taskAnswer = initialTaskAnswer
        var round = 1
        var confirmed = false

        // 监督方首次审阅
        val initialSupervisorPrompt = buildInitialSupervisorPrompt(question, taskAnswer)
        var supervisorReply: String = callWithRetry(
            ref = supervisorRef,
            prompt = initialSupervisorPrompt,
            systemPrompt = config.supervisorPrompt,
            retryCount = config.retryCount,
            phase = CollaborationPhase.SUPERVISE,
            sourceLabel = supervisorLabel,
            sessionKey = key,
            notifyOnFailure = config.notifyOnFailure,
        ) ?: run {
            listener.onEvent(
                CollaborationEvent(round, CollaborationPhase.FAILED, "$sessionTitle：监督方首次审阅失败。", sessionKey = key),
            )
            return SessionResult("$sessionTitle：监督方审阅失败", confirmed = false, failed = true)
        }

        analysisScores[supervisorRef] = (analysisScores[supervisorRef] ?: 0.0) + scoreResponse(supervisorReply)
        emitSupervisorReply(round, supervisorReply, supervisorLabel, key)

        if (config.autoStopOnConfirm && isSessionTerminationReply(supervisorReply)) {
            confirmed = true
            listener.onEvent(
                CollaborationEvent(round, CollaborationPhase.DONE, "$sessionTitle：监督方确认完成，会话结束。", sessionKey = key),
            )
            return SessionResult("$sessionTitle：监督方确认完成（第 $round 轮）", confirmed = true, failed = false)
        }

        // 对话循环
        while (round < config.maxRounds) {
            if (cancelled) {
                listener.onEvent(
                    CollaborationEvent(round, CollaborationPhase.CANCELLED, "$sessionTitle：用户取消。", sessionKey = key),
                )
                return SessionResult("$sessionTitle：已取消", confirmed = false, failed = true)
            }

            round++

            // 系统格式化转发监督方问题给任务方
            val relayToTask = formatSupervisorQuestionForTask(supervisorReply)
            listener.onEvent(
                CollaborationEvent(
                    round,
                    CollaborationPhase.DIALOGUE,
                    relayToTask,
                    "系统 → $taskLabel",
                    roleKind = CollaborationRoleKind.SYSTEM,
                    sessionKey = key,
                ),
            )

            val taskReply = callWithRetry(
                ref = taskRef,
                prompt = relayToTask,
                systemPrompt = config.taskPartyPrompt,
                retryCount = config.retryCount,
                phase = CollaborationPhase.TASK,
                sourceLabel = taskLabel,
                sessionKey = key,
                notifyOnFailure = config.notifyOnFailure,
            )
            if (taskReply == null) {
                listener.onEvent(
                    CollaborationEvent(round, CollaborationPhase.FAILED, "$sessionTitle：任务方回复失败。", sessionKey = key),
                )
                return SessionResult("$sessionTitle：任务方回复失败（第 $round 轮）", confirmed = false, failed = true)
            }
            taskAnswer = taskReply
            analysisScores[taskRef] = (analysisScores[taskRef] ?: 0.0) + scoreResponse(taskReply)
            listener.onEvent(
                CollaborationEvent(
                    round,
                    CollaborationPhase.TASK,
                    taskReply,
                    "$taskLabel 的回复",
                    isAnswer = true,
                    roleKind = CollaborationRoleKind.TASK,
                    sessionKey = key,
                ),
            )

            // 系统格式化转发任务方回答给监督方
            val relayToSupervisor = formatTaskAnswerForSupervisor(taskAnswer)
            listener.onEvent(
                CollaborationEvent(
                    round,
                    CollaborationPhase.DIALOGUE,
                    relayToSupervisor,
                    "系统 → $supervisorLabel",
                    roleKind = CollaborationRoleKind.SYSTEM,
                    sessionKey = key,
                ),
            )

            supervisorReply = callWithRetry(
                ref = supervisorRef,
                prompt = relayToSupervisor,
                systemPrompt = config.supervisorPrompt,
                retryCount = config.retryCount,
                phase = CollaborationPhase.SUPERVISE,
                sourceLabel = supervisorLabel,
                sessionKey = key,
                notifyOnFailure = config.notifyOnFailure,
            ) ?: run {
                listener.onEvent(
                    CollaborationEvent(round, CollaborationPhase.FAILED, "$sessionTitle：监督方回复失败。", sessionKey = key),
                )
                return SessionResult("$sessionTitle：监督方回复失败（第 $round 轮）", confirmed = false, failed = true)
            }

            analysisScores[supervisorRef] = (analysisScores[supervisorRef] ?: 0.0) + scoreResponse(supervisorReply)
            emitSupervisorReply(round, supervisorReply, supervisorLabel, key)

            if (config.autoStopOnConfirm && isSessionTerminationReply(supervisorReply)) {
                confirmed = true
                listener.onEvent(
                    CollaborationEvent(round, CollaborationPhase.DONE, "$sessionTitle：监督方确认完成，会话结束。", sessionKey = key),
                )
                return SessionResult("$sessionTitle：监督方确认完成（第 $round 轮）", confirmed = true, failed = false)
            }
        }

        listener.onEvent(
            CollaborationEvent(round, CollaborationPhase.DONE, "$sessionTitle：已达最大轮次 $round，会话结束。", sessionKey = key),
        )
        return SessionResult("$sessionTitle：已达最大轮次（$round 轮）", confirmed = false, failed = false)
    }

    private fun emitSupervisorReply(round: Int, reply: String, supervisorLabel: String, sessionKey: String) {
        listener.onEvent(
            CollaborationEvent(round, CollaborationPhase.SUPERVISE, "$supervisorLabel 审阅完成（${reply.length} 字）。", supervisorLabel, sessionKey = sessionKey),
        )
        listener.onEvent(
            CollaborationEvent(
                round,
                CollaborationPhase.SUPERVISE,
                reply,
                "$supervisorLabel 的审阅",
                isAnswer = true,
                roleKind = CollaborationRoleKind.SUPERVISE,
                sessionKey = sessionKey,
            ),
        )
    }

    private fun buildInitialSupervisorPrompt(question: String, taskAnswer: String): String = buildString {
        append("【原始问题】\n")
        append(question)
        append("\n\n【任务方作答】\n")
        append(taskAnswer)
        append("\n\n请审阅任务方的作答并提出疑问。若认为没有问题、可以完成，请明确说明。")
    }

    private suspend fun callWithRetry(
        ref: ModelRef,
        prompt: String,
        systemPrompt: String?,
        retryCount: Int,
        phase: CollaborationPhase,
        sourceLabel: String?,
        sessionKey: String?,
        notifyOnFailure: Boolean,
    ): String? {
        repeat(retryCount + 1) { attempt ->
            if (cancelled) return null
            try {
                val result = repository.askWithInstanceModel(ref.instanceId, ref.modelId, prompt, systemPrompt)
                if (result.isNotBlank()) return result
            } catch (_: Exception) {
                // fall through to retry
            }
            if (attempt < retryCount) {
                listener.onEvent(
                    CollaborationEvent(
                        0,
                        phase,
                        "${sourceLabel ?: ref.modelId} 第 ${attempt + 1} 次失败，重试中…",
                        sourceLabel,
                        sessionKey = sessionKey,
                    ),
                )
            }
        }
        if (notifyOnFailure) {
            listener.onNotify("模型调用失败", "${sourceLabel ?: ref.modelId} 在重试 $retryCount 次后仍失败。")
        }
        return null
    }

    private fun resolveScoreGatedRoles(config: CollaborationConfig): CollaborationRoleConfig? {
        val pool = buildScoredPool(config)
        if (pool.isEmpty()) {
            listener.onEvent(
                CollaborationEvent(
                    1,
                    CollaborationPhase.DISTRIBUTE,
                    "分数门槛模式：没有任何模型的测试总分 ≥ ${config.minScore}，请先运行「模型测试」或调低门槛。",
                ),
            )
            return null
        }

        if (pool.size < 2) {
            listener.onEvent(
                CollaborationEvent(1, CollaborationPhase.DISTRIBUTE, "分数门槛模式：达标模型不足，至少需要 1 个任务方与 1 个监督方。"),
            )
            return null
        }

        val ratio = config.taskRatio.coerceIn(0.1, 0.9)
        var taskCount = (pool.size * ratio).roundToInt().coerceAtLeast(1)
        if (pool.size - taskCount < 1) taskCount = pool.size - 1
        val taskParties = pool.take(taskCount).map { it.first }
        val supervisors = pool.drop(taskCount).map { it.first }

        listener.onEvent(
            CollaborationEvent(
                1,
                CollaborationPhase.DISTRIBUTE,
                "分数门槛模式：${pool.size} 个模型总分 ≥ ${config.minScore}，自动分配 → 任务方 ${taskParties.size} 个、监督方 ${supervisors.size} 个。",
            ),
        )
        return CollaborationRoleConfig(taskParties = taskParties, supervisors = supervisors)
    }

    private fun buildScoredPool(config: CollaborationConfig): List<Triple<ModelRef, Double, String>> {
        val benchmarks = runCatching { repository.getModelBenchmarks() }
            .getOrDefault(emptyList())
            .associate { it.modelKey to it.totalScore }
        val entries: List<ServiceEntry> = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        return buildList {
            for (entry in entries) {
                val modelIds = entry.modelOptions.map { it.id }.ifEmpty { listOfNotNull(entry.modelId) }
                for (modelId in modelIds.distinct()) {
                    val score = benchmarks["${entry.serviceId}::$modelId"] ?: continue
                    if (score >= config.minScore) {
                        add(Triple(ModelRef(entry.instanceId, modelId), score, "${entry.serviceName}/$modelId"))
                    }
                }
            }
        }.sortedByDescending { it.second }
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
