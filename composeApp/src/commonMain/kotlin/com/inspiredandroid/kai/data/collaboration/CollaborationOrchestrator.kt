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
 * 流程（每轮）：
 *  提问 → 分发到各任务方（第2轮起附带上一轮回传方针对该任务方的反馈）→
 *  等待任务方回答（失败按 retryCount 重试）→
 *  汇总为"任务方1的执行结果：…；任务方2的执行结果：…" →
 *  传达方汇总精简（格式见 DEFAULT_TRANSMITTER_PROMPT）→ 分发到各监督方 →
 *  监督方对各任务方分别回复（格式：对任务方1的回复：…）→
 *  等待所有监督方回复 → 回传方汇总分发回任务方（失败则调用空闲模型）→
 *  从回传方汇总中提取各任务方对应的反馈段落，作为下一轮任务方的上下文 →
 *  若所有监督方均"确认"（显式提醒）或达到 maxRounds，则结束。
 *
 * 模型失败处理：
 *  - 任务方 / 监督方：按 retryCount 重试，耗尽后显式提醒（notifyOnFailure）。
 *  - 传达方 / 回传方：重试耗尽后调用"空闲模型"（其它已配置模型）兜底。
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

    /**
     * 组装角色标签（仅用于协作模式任务过程界面展示）：格式为「任务方1-opencode-hy3」。
     * 模型名优先取自定义别名（modelAliases），否则取服务目录中的模型显示名（opt.label），
     * 再否则取模型 id。别名/模型名仅用于 UI 展示（sourceLabel），不会进入发给大模型的 prompt。
     */
    private fun roleLabel(base: String, ref: ModelRef, aliases: Map<String, String>, labelResolver: Map<String, String>): String {
        val modelName = aliases[ref.key] ?: labelResolver[ref.key] ?: ref.modelId
        return "$base-$modelName"
    }

    /** 构建 instanceId::modelId → 模型显示名（opt.label）的映射，用于任务过程界面展示模型名。 */
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
        // 分数门槛模式：按测试分数自动分配角色；手动模式直接使用配置的角色。
        // 门槛模式达标模型不足时 resolveScoreGatedRoles 内部已通知并结束，这里直接返回。
        val roles = if (config.mode == CollaborationMode.SCORE_GATED) {
            resolveScoreGatedRoles(config) ?: return
        } else {
            config.roles
        }
        if (roles.taskParties.isEmpty() || roles.transmitter == null ||
            roles.supervisors.isEmpty() || roles.feedback == null
        ) {
            listener.onNotify("协作模式未配置完整", "请设置任务方、传达方、监督方与回传方。")
            listener.onFinished("协作配置不完整，未执行。", allConfirmed = false)
            return
        }

        // 构建任务方标签：任务方1、任务方2…（展示为「任务方1-opencode-hy3」格式）
        val aliases = config.modelAliases
        val labelResolver = buildLabelResolver()
        val taskLabels = roles.taskParties.mapIndexed { i, ref -> roleLabel("任务方${i + 1}", ref, aliases, labelResolver) }
        // 监督方标签：监督方A、监督方B…
        val supervisorLabels = roles.supervisors.mapIndexed { i, ref -> roleLabel("监督方${'A' + i}", ref, aliases, labelResolver) }
        val transmitterLabel = roleLabel("传达方", roles.transmitter!!, aliases, labelResolver)
        val feedbackLabel = roleLabel("回传方", roles.feedback!!, aliases, labelResolver)

        val idleModels = collectIdleModels(exclude = roles.taskParties + listOfNotNull(roles.transmitter, roles.feedback) + roles.supervisors)

        var allConfirmed = false
        var failed = false
        var lastSummary = ""
        // 上一轮回传方针对各任务方（按索引）的反馈；第1轮为空。
        var lastFeedbackPerTask: Map<Int, String>? = null
        val analysisScores = mutableMapOf<ModelRef, Double>()

        for (round in 1..config.maxRounds) {
            if (cancelled) {
                listener.onEvent(CollaborationEvent(round, CollaborationPhase.CANCELLED, "用户取消了协作。"))
                listener.onFinished("协作已被用户取消。", allConfirmed = false)
                return
            }
            listener.onEvent(CollaborationEvent(round, CollaborationPhase.DISTRIBUTE, "第 $round 轮：分发问题到 ${taskLabels.size} 个任务方。"))

            // 1) 任务方执行：统一并行发放——所有任务方同时收到问题并独立完成，最后统一统计。
            val taskOutcomes = coroutineScope {
                roles.taskParties.mapIndexed { idx, ref ->
                    async {
                        val label = taskLabels[idx]
                        val taskPrompt = buildTaskPrompt(question, round, idx, lastFeedbackPerTask)
                        val result = callWithRetry(
                            ref = ref,
                            prompt = taskPrompt,
                            systemPrompt = null,
                            retryCount = config.retryCount,
                            failoverToIdle = false,
                            idleModels = idleModels,
                            phase = CollaborationPhase.TASK,
                            sourceLabel = label,
                            notifyOnFailure = config.notifyOnFailure,
                        )
                        idx to result
                    }
                }.awaitAll()
            }
            val taskResults = arrayOfNulls<String?>(roles.taskParties.size)
            taskOutcomes.forEach { (idx, result) -> taskResults[idx] = result }
            // 统一统计与展示（顺序化输出，保持日志稳定）
            roles.taskParties.forEachIndexed { idx, ref ->
                val label = taskLabels[idx]
                val result = taskResults[idx]
                if (result != null) {
                    analysisScores[ref] = (analysisScores[ref] ?: 0.0) + scoreResponse(result, adequate = true)
                    listener.onEvent(CollaborationEvent(round, CollaborationPhase.TASK, "${label} 执行完成（${result.length} 字）。", label))
                    // 展示该任务方的实际回答正文（仅 UI 展示，不回传给其它模型）。
                    listener.onEvent(CollaborationEvent(round, CollaborationPhase.TASK, result, "$label 的回答", isAnswer = true))
                } else {
                    listener.onEvent(CollaborationEvent(round, CollaborationPhase.TASK, "${label} 执行失败且重试耗尽。", label))
                }
            }

            val succeededTasks = taskResults.filterNotNull()
            if (succeededTasks.isEmpty()) {
                listener.onEvent(CollaborationEvent(round, CollaborationPhase.FAILED, "所有任务方均执行失败，协作已于该轮停止。"))
                allConfirmed = false
                failed = true
                lastSummary = "第 $round 轮：所有任务方执行失败，协作已停止。"
                break
            }

            // 2) 汇总任务方结果
            val combined = buildString {
                taskLabels.forEachIndexed { idx, label ->
                    val r = taskResults[idx]
                    append("$label 的执行结果：${r ?: "（失败）"}；\n")
                }
            }.trimEnd()

            // 3) 传达方汇总精简
            listener.onEvent(CollaborationEvent(round, CollaborationPhase.TRANSMIT, "传达方汇总精简中…", transmitterLabel))
            val transmitterPrompt = buildString {
                append(config.transmitterPrompt)
                append("\n\n原始问题：\n")
                append(question)
                append("\n\n各任务方执行结果：\n")
                append(combined)
                append("\n\n【长度与核心任务约束】你的核心任务是【内容精简】：在保留关键结论、关键数据与差异点的前提下压缩篇幅，不得扩充。你的输出（不含“若没有问题，请回复\"确认\"；若有问题，请给出你的补充或纠正。”这一句）不得超过 ${config.maxOutputChars} 字。")
            }
            val transmitSummary = callWithRetry(
                ref = roles.transmitter!!,
                prompt = transmitterPrompt,
                systemPrompt = null,
                retryCount = config.retryCount,
                failoverToIdle = true,
                idleModels = idleModels,
                phase = CollaborationPhase.TRANSMIT,
                sourceLabel = transmitterLabel,
                notifyOnFailure = config.notifyOnFailure,
            )
            if (transmitSummary == null) {
                listener.onEvent(CollaborationEvent(round, CollaborationPhase.FAILED, "传达方失败且无空闲模型可兜底，协作已于该轮停止。"))
                allConfirmed = false
                failed = true
                lastSummary = "第 $round 轮：传达方失败，协作已停止。"
                break
            }
            listener.onEvent(CollaborationEvent(round, CollaborationPhase.TRANSMIT, "传达方汇总完成（${transmitSummary.length} 字）。", transmitterLabel))

            // 4) 监督方评估：统一并行发放，所有监督方同时收到传达方汇总并独立评估。
            listener.onEvent(CollaborationEvent(round, CollaborationPhase.SUPERVISE, "分发到 ${supervisorLabels.size} 个监督方评估…"))
            val supervisorOutcomes = coroutineScope {
                roles.supervisors.mapIndexed { sIdx, ref ->
                    async {
                        val slabel = supervisorLabels[sIdx]
                        val reply = callWithRetry(
                            ref = ref,
                            prompt = transmitSummary,
                            systemPrompt = null,
                            retryCount = config.retryCount,
                            failoverToIdle = false,
                            idleModels = idleModels,
                            phase = CollaborationPhase.SUPERVISE,
                            sourceLabel = slabel,
                            notifyOnFailure = config.notifyOnFailure,
                        )
                        sIdx to reply
                    }
                }.awaitAll()
            }
            val supervisorReplies = arrayOfNulls<String?>(roles.supervisors.size)
            supervisorOutcomes.forEach { (sIdx, reply) -> supervisorReplies[sIdx] = reply }
            roles.supervisors.forEachIndexed { sIdx, ref ->
                val slabel = supervisorLabels[sIdx]
                val reply = supervisorReplies[sIdx]
                if (reply != null) {
                    analysisScores[ref] = (analysisScores[ref] ?: 0.0) + scoreResponse(reply, adequate = true)
                    listener.onEvent(CollaborationEvent(round, CollaborationPhase.SUPERVISE, "$slabel 评估完成（${reply.length} 字）。", slabel))
                } else {
                    listener.onEvent(CollaborationEvent(round, CollaborationPhase.SUPERVISE, "$slabel 评估失败且重试耗尽。", slabel))
                }
            }

            val succeededSupervisors = supervisorReplies.filterNotNull()
            if (succeededSupervisors.isEmpty()) {
                listener.onEvent(CollaborationEvent(round, CollaborationPhase.FAILED, "所有监督方均失败，协作已于该轮停止。"))
                allConfirmed = false
                failed = true
                lastSummary = "第 $round 轮：所有监督方执行失败，协作已停止。"
                break
            }

            // 5) 回传方汇总分发
            listener.onEvent(CollaborationEvent(round, CollaborationPhase.FEEDBACK, "回传方汇总监督方回复并分发…", feedbackLabel))
            val feedbackPrompt = buildString {
                append(config.feedbackPrompt)
                append("\n\n各监督方回复如下：\n")
                supervisorLabels.forEachIndexed { sIdx, slabel ->
                    append("$slabel 的回复：\n${supervisorReplies[sIdx] ?: "（失败）"}\n\n")
                }
                append("\n\n【长度与核心任务约束】你的核心任务是【内容精简】：汇总时压缩篇幅、保留关键信息与分歧点，不得扩充。你的输出不得超过 ${config.maxOutputChars} 字。")
            }
            val feedbackSummary = callWithRetry(
                ref = roles.feedback!!,
                prompt = feedbackPrompt,
                systemPrompt = null,
                retryCount = config.retryCount,
                failoverToIdle = true,
                idleModels = idleModels,
                phase = CollaborationPhase.FEEDBACK,
                sourceLabel = feedbackLabel,
                notifyOnFailure = config.notifyOnFailure,
            )
            if (feedbackSummary == null) {
                listener.onEvent(CollaborationEvent(round, CollaborationPhase.FAILED, "回传方失败且无空闲模型可兜底，协作已于该轮停止。"))
                allConfirmed = false
                failed = true
                lastSummary = "第 $round 轮：回传方失败，协作已停止。"
                break
            }
            listener.onEvent(CollaborationEvent(round, CollaborationPhase.FEEDBACK, "回传方分发完成（${feedbackSummary.length} 字）。", feedbackLabel))
            lastSummary = feedbackSummary

            // 从回传方汇总中提取各任务方对应的反馈段落，作为下一轮任务方上下文。
            lastFeedbackPerTask = (0 until taskLabels.size).associateWith { tIdx ->
                extractTaskPartySegment(feedbackSummary, tIdx + 1) ?: feedbackSummary
            }
            // 显式记录"已下发到各任务方"的事件，避免用户误以为回传方结果未分发。
            listener.onEvent(
                CollaborationEvent(
                    round,
                    CollaborationPhase.FEEDBACK,
                    "回传方反馈已下发到 ${taskLabels.size} 个任务方，将在下一轮作为改进依据。",
                    feedbackLabel,
                ),
            )

            // 决定是否停止
            val perTaskConfirmed = (0 until taskLabels.size).map { tIdx ->
                succeededSupervisors.all { reply ->
                    val seg = extractTaskPartySegment(reply, tIdx + 1) ?: reply
                    isConfirmReply(seg)
                }
            }
            allConfirmed = perTaskConfirmed.all { it }
            listener.onEvent(
                CollaborationEvent(
                    round,
                    CollaborationPhase.SUPERVISE,
                    "本轮确认情况：${perTaskConfirmed.joinToString { if (it) "确认" else "待改进" }}",
                ),
            )

            if (allConfirmed) {
                // 无论 autoStopOnConfirm 是否开启，监督方全部确认都显式提醒。
                listener.onNotify(
                    "监督方全部确认",
                    "第 $round 轮：所有监督方对所有任务方均已确认，无需继续改进。",
                )
                listener.onEvent(CollaborationEvent(round, CollaborationPhase.DONE, "所有监督方确认，协作结束。"))
                break
            }
            if (!config.autoStopOnConfirm && round == config.maxRounds) {
                listener.onEvent(CollaborationEvent(round, CollaborationPhase.DONE, "已达最大轮次且未开启自动停止，协作结束。"))
                break
            }
        }

        // 收尾
        val scores = analysisScores.map { (ref, v) ->
            ModelScore(instanceId = ref.instanceId, modelId = ref.modelId, analysisScore = v)
        }
        listener.onScores(scores)
        if (config.notifyOnComplete) {
            listener.onNotify(
                "协作任务结束",
                if (failed) lastSummary else if (allConfirmed) "所有监督方已确认，共 ${config.maxRounds} 轮上限内达成一致。" else "已达最大轮次，未能全部确认。",
            )
        }
        listener.onFinished(lastSummary.ifEmpty { "协作结束，但各轮均未产生有效回传结果。" }, allConfirmed)
    }

    /**
     * 构建任务方的执行 prompt。第 1 轮只含原始问题；第 2 轮起附带上一轮回传方
     * 针对该任务方的反馈，使其据此改进。feedback 为空时退化为原始问题。
     */
    private fun buildTaskPrompt(
        question: String,
        round: Int,
        taskIndex: Int,
        lastFeedbackPerTask: Map<Int, String>?,
    ): String {
        if (round <= 1 || lastFeedbackPerTask == null) return question
        val feedback = lastFeedbackPerTask[taskIndex]?.takeIf { it.isNotBlank() } ?: return question
        return buildString {
            append(question)
            append("\n\n[来自回传方的反馈（请据此改进你的方案）]\n")
            append(feedback)
        }
    }

    /**
     * 调用指定模型，失败按 retryCount 重试；failoverToIdle 为 true 时（传达方/回传方）
     * 重试耗尽后调用空闲模型兜底；否则重试耗尽返回 null（由调用方决定提醒）。
     */
    private suspend fun callWithRetry(
        ref: ModelRef,
        prompt: String,
        systemPrompt: String?,
        retryCount: Int,
        failoverToIdle: Boolean,
        idleModels: List<ModelRef>,
        phase: CollaborationPhase,
        sourceLabel: String?,
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
                listener.onEvent(CollaborationEvent(0, phase, "${sourceLabel ?: ref.modelId} 第 ${attempt + 1} 次失败，重试中…", sourceLabel))
            }
        }
        if (failoverToIdle) {
            for (idle in idleModels) {
                if (cancelled) return null
                try {
                    val result = repository.askWithInstanceModel(idle.instanceId, idle.modelId, prompt, systemPrompt)
                    if (result.isNotBlank()) {
                        listener.onEvent(CollaborationEvent(0, phase, "${sourceLabel ?: ref.modelId} 由空闲模型 ${idle.modelId} 兜底成功。", sourceLabel))
                        return result
                    }
                } catch (_: Exception) {
                    // try next idle model
                }
            }
        }
        if (notifyOnFailure) {
            listener.onNotify("模型调用失败", "${sourceLabel ?: ref.modelId} 在重试 $retryCount 次后仍失败。")
        }
        return null
    }

    /** 收集所有已配置模型（用于空闲兜底），排除 exclude 中的引用。 */
    private fun collectIdleModels(exclude: List<ModelRef>): List<ModelRef> {
        val entries: List<ServiceEntry> = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        val result = mutableListOf<ModelRef>()
        for (entry in entries) {
            for (opt in entry.modelOptions) {
                val ref = ModelRef(entry.instanceId, opt.id)
                if (exclude.none { it.instanceId == ref.instanceId && it.modelId == ref.modelId }) {
                    result.add(ref)
                }
            }
        }
        return result
    }

    /**
     * 分数门槛模式：按模型测试总分自动分配角色。
     * 只允许 总分 ≥ config.minScore 的模型参与。
     * 传达方/回传方：手动指定（且达标）优先，否则自动取达标池中的最高分模型；
     * 任务方/监督方：按 config.taskRatio 比例切分达标池（分数降序，前 taskCount 个为任务方，其余为监督方）。
     * 达标模型不足（无法构成 任务方+监督方+传达方+回传方）时返回 null。
     */
    private fun resolveScoreGatedRoles(config: CollaborationConfig): CollaborationRoleConfig? {
        val pool = buildScoredPool(config)
        if (pool.isEmpty()) {
            listener.onEvent(
                CollaborationEvent(1, CollaborationPhase.DISTRIBUTE, "分数门槛模式：没有任何模型的测试总分 ≥ ${config.minScore}，请先运行「模型测试」或调低门槛。"),
            )
            return null
        }

        val used = mutableSetOf<ModelRef>()
        // 传达方/回传方：手动指定（且达标）优先，否则取达标池中最高分（互不重复）。
        fun pick(existing: ModelRef?): ModelRef? {
            if (existing != null && pool.any { it.first == existing }) {
                used += existing
                return existing
            }
            val next = pool.firstOrNull { it.first !in used }?.first ?: return null
            used += next
            return next
        }
        val transmitter = pick(config.roles.transmitter)
        val feedback = pick(config.roles.feedback)
        if (transmitter == null || feedback == null) {
            listener.onEvent(
                CollaborationEvent(1, CollaborationPhase.DISTRIBUTE, "分数门槛模式：达标模型不足，无法指定传达方与回传方。"),
            )
            return null
        }

        val rest = pool.filter { it.first !in used }
        if (rest.size < 2) {
            listener.onEvent(
                CollaborationEvent(1, CollaborationPhase.DISTRIBUTE, "分数门槛模式：达标模型不足，至少还需 1 个任务方与 1 个监督方。"),
            )
            return null
        }
        val ratio = config.taskRatio.coerceIn(0.1, 0.9)
        var taskCount = (rest.size * ratio).roundToInt().coerceAtLeast(1)
        if (rest.size - taskCount < 1) taskCount = rest.size - 1
        val taskParties = rest.take(taskCount).map { it.first }
        val supervisors = rest.drop(taskCount).map { it.first }

        listener.onEvent(
            CollaborationEvent(
                1,
                CollaborationPhase.DISTRIBUTE,
                "分数门槛模式：${pool.size} 个模型总分 ≥ ${config.minScore} 分，按比例自动分配 → 任务方 ${taskParties.size} 个、监督方 ${supervisors.size} 个；传达方/回传方取达标最高分。",
            ),
        )
        return CollaborationRoleConfig(
            taskParties = taskParties,
            transmitter = transmitter,
            supervisors = supervisors,
            feedback = feedback,
        )
    }

    /** 构建"已配置模型 + 测试总分"达标池（总分 ≥ minScore），按总分降序。 */
    private fun buildScoredPool(config: CollaborationConfig): List<Triple<ModelRef, Double, String>> {
        val benchmarks = runCatching { repository.getModelBenchmarks() }
            .getOrDefault(emptyList())
            .associate { it.modelKey to it.totalScore } // "serviceId::modelId" -> 总分
        val entries: List<ServiceEntry> = runCatching { repository.getServiceEntries() }.getOrDefault(emptyList())
        return buildList {
            for (entry in entries) {
                val modelIds = entry.modelOptions.map { it.id }.ifEmpty { listOfNotNull(entry.modelId) }
                for (modelId in modelIds.distinct()) {
                    val score = benchmarks["${entry.serviceId}::${modelId}"] ?: continue // 未测试过不参与
                    if (score >= config.minScore) {
                        add(Triple(ModelRef(entry.instanceId, modelId), score, "${entry.serviceName}/${modelId}"))
                    }
                }
            }
        }.sortedByDescending { it.second }
    }

    /** 简单响应质量评分（0..100），供 freellmapi 风格分析参考。 */
    private fun scoreResponse(response: String, adequate: Boolean): Double {
        if (!adequate || response.isBlank()) return 0.0
        // 长度适中（50~4000 字）给较高基础分，过短或过长递减。
        val len = response.length
        return when {
            len < 20 -> 30.0
            len < 200 -> 70.0
            len <= 4000 -> 90.0
            else -> 75.0
        }
    }

    /**
     * 从监督方回复中提取针对第 taskPartyIndex（从 1 开始）任务方的段落。
     * 监督方格式：对任务方1的回复：…\n对任务方2的回复：…
     */
    private fun extractTaskPartySegment(reply: String, taskPartyIndex: Int): String? {
        val regex = Regex("对任务方$taskPartyIndex[的的]?回复[:：]\\s*(.*?)(?=对任务方\\d|$)", RegexOption.DOT_MATCHES_ALL)
        return regex.find(reply)?.groupValues?.getOrNull(1)?.trim()
    }
}
