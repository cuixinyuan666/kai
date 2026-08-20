package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.ServiceEntry
import com.inspiredandroid.kai.data.collaboration.CollaborationConfig
import com.inspiredandroid.kai.data.collaboration.CollaborationMode
import com.inspiredandroid.kai.data.collaboration.CollaborationRoleConfig
import com.inspiredandroid.kai.data.collaboration.DEFAULT_FEEDBACK_PROMPT
import com.inspiredandroid.kai.data.collaboration.DEFAULT_TRANSMITTER_PROMPT
import com.inspiredandroid.kai.data.collaboration.ModelRef
import com.inspiredandroid.kai.data.collaboration.ModelScore
import com.inspiredandroid.kai.ui.KaiOutlinedTextField
import com.inspiredandroid.kai.ui.handCursor
import kotlin.math.roundToInt

/**
 * 协作模式设置页。
 * 采用树形多级下拉：任务 → 任务方/传达方/监督方/回传方 → 各角色所选模型。
 * 任务方、监督方支持多选（多个模型）；传达方、回传方为单一模型。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollaborationSettings(
    dataRepository: DataRepository,
    services: List<ServiceEntry>,
) {
    var config by remember { mutableStateOf(dataRepository.getCollaborationConfig()) }

    fun update(block: (CollaborationConfig) -> CollaborationConfig) {
        config = block(config)
        dataRepository.setCollaborationConfig(config)
    }

    val labelResolver = remember(services) { ModelLabelResolver(services) }

    // 注意：SettingsScreen 外层已用 verticalScroll 包裹，这里必须用普通 Column，
    // 否则 LazyColumn 在无限高度约束下会抛
    // "Vertically scrollable component was measured with an infinity maximum height constraints" 崩溃。
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            // 协作角色分配模式：手动选择（原模式） / 分数门槛自动分配
            Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("协作角色分配模式", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "手动选择：自行指定各角色模型（原模式）。分数门槛：仅模型测试总分 ≥ 门槛的模型参与；任务方/监督方按比例自动分配，传达方/回传方保留手动指定（未指定时自动取达标最高分模型）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier.weight(1f).handCursor().clickable { update { it.copy(mode = CollaborationMode.MANUAL) } },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = config.mode == CollaborationMode.MANUAL, onClick = { update { it.copy(mode = CollaborationMode.MANUAL) } })
                            Text("手动选择（原模式）")
                        }
                        Row(
                            modifier = Modifier.weight(1f).handCursor().clickable { update { it.copy(mode = CollaborationMode.SCORE_GATED) } },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = config.mode == CollaborationMode.SCORE_GATED, onClick = { update { it.copy(mode = CollaborationMode.SCORE_GATED) } })
                            Text("分数门槛自动分配")
                        }
                    }
                    if (config.mode == CollaborationMode.SCORE_GATED) {
                        NumberField(
                            label = "参与门槛分数（0-100，默认 70）",
                            value = config.minScore.toInt().coerceIn(0, 100),
                            onValueChange = { n -> update { it.copy(minScore = n.toDouble().coerceIn(0.0, 100.0)) } },
                        )
                        NumberField(
                            label = "任务方占比 %（其余为监督方，默认 60）",
                            value = (config.taskRatio * 100).roundToInt().coerceIn(10, 90),
                            onValueChange = { n -> update { it.copy(taskRatio = (n / 100.0).coerceIn(0.1, 0.9)) } },
                        )
                        Text(
                            "说明：任务方/监督方将按比例自动从「总分 ≥ 门槛」的模型中分配（未测试过或低于门槛的模型不参与）；传达方/回传方仍可手动指定，留空则自动取达标最高分。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

        if (config.mode == CollaborationMode.MANUAL) {
            RoleCard(
                title = "任务方（可多选）",
                subtitle = "负责执行任务，可添加多个大模型",
                selected = config.roles.taskParties,
                multiSelect = true,
                labelResolver = labelResolver,
                modelAliases = config.modelAliases,
                onAliasChange = { ref, text ->
                    update {
                        it.copy(
                            modelAliases = if (text.isBlank()) it.modelAliases - ref.key
                            else it.modelAliases + (ref.key to text),
                        )
                    }
                },
                onAdd = { ref -> update { it.copy(roles = it.roles.copy(taskParties = it.roles.taskParties + ref)) } },
                onRemove = { ref -> update { it.copy(roles = it.roles.copy(taskParties = it.roles.taskParties - ref)) } },
                services = services,
            )
        }

            RoleCard(
                title = "传达方（单一）",
                subtitle = "汇总精简各任务方结果，过长则压缩但保留关键信息",
                selected = listOfNotNull(config.roles.transmitter),
                multiSelect = false,
                labelResolver = labelResolver,
                modelAliases = config.modelAliases,
                onAliasChange = { ref, text ->
                    update {
                        it.copy(
                            modelAliases = if (text.isBlank()) it.modelAliases - ref.key
                            else it.modelAliases + (ref.key to text),
                        )
                    }
                },
                onAdd = { ref -> update { it.copy(roles = it.roles.copy(transmitter = ref)) } },
                onRemove = { update { it.copy(roles = it.roles.copy(transmitter = null)) } },
                services = services,
            )

        if (config.mode == CollaborationMode.MANUAL) {
            RoleCard(
                title = "监督方（可多选）",
                subtitle = "对各任务方的方案或结果进行评估，可添加多个大模型",
                selected = config.roles.supervisors,
                multiSelect = true,
                labelResolver = labelResolver,
                modelAliases = config.modelAliases,
                onAliasChange = { ref, text ->
                    update {
                        it.copy(
                            modelAliases = if (text.isBlank()) it.modelAliases - ref.key
                            else it.modelAliases + (ref.key to text),
                        )
                    }
                },
                onAdd = { ref -> update { it.copy(roles = it.roles.copy(supervisors = it.roles.supervisors + ref)) } },
                onRemove = { ref -> update { it.copy(roles = it.roles.copy(supervisors = it.roles.supervisors - ref)) } },
                services = services,
            )
        }

            RoleCard(
                title = "回传方（单一）",
                subtitle = "汇总所有监督方回复并分发回相应任务方",
                selected = listOfNotNull(config.roles.feedback),
                multiSelect = false,
                labelResolver = labelResolver,
                modelAliases = config.modelAliases,
                onAliasChange = { ref, text ->
                    update {
                        it.copy(
                            modelAliases = if (text.isBlank()) it.modelAliases - ref.key
                            else it.modelAliases + (ref.key to text),
                        )
                    }
                },
                onAdd = { ref -> update { it.copy(roles = it.roles.copy(feedback = ref)) } },
                onRemove = { update { it.copy(roles = it.roles.copy(feedback = null)) } },
                services = services,
            )

            Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("运行参数", style = MaterialTheme.typography.titleMedium)
                    NumberField(
                        label = "最大循环轮次",
                        value = config.maxRounds,
                        onValueChange = { n -> update { it.copy(maxRounds = n) } },
                    )
                    NumberField(
                        label = "模型失败重试次数",
                        value = config.retryCount,
                        onValueChange = { n -> update { it.copy(retryCount = n) } },
                    )
                    NumberField(
                        label = "输出字数上限",
                        value = config.maxOutputChars,
                        onValueChange = { n -> update { it.copy(maxOutputChars = n) } },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("监督方全部确认即停止", modifier = Modifier.weight(1f))
                        Switch(checked = config.autoStopOnConfirm, onCheckedChange = { update { it.copy(autoStopOnConfirm = !it.autoStopOnConfirm) } })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("模型失败显式提醒", modifier = Modifier.weight(1f))
                        Switch(checked = config.notifyOnFailure, onCheckedChange = { update { it.copy(notifyOnFailure = !it.notifyOnFailure) } })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("任务结束提醒", modifier = Modifier.weight(1f))
                        Switch(checked = config.notifyOnComplete, onCheckedChange = { update { it.copy(notifyOnComplete = !it.notifyOnComplete) } })
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("提示词（可自定义）", style = MaterialTheme.typography.titleMedium)
                    Text("传达方提示词", style = MaterialTheme.typography.labelMedium)
                    PromptField(
                        value = config.transmitterPrompt,
                        placeholder = DEFAULT_TRANSMITTER_PROMPT,
                        onValueChange = { s -> update { it.copy(transmitterPrompt = s) } },
                    )
                    Text("回传方提示词", style = MaterialTheme.typography.labelMedium)
                    PromptField(
                        value = config.feedbackPrompt,
                        placeholder = DEFAULT_FEEDBACK_PROMPT,
                        onValueChange = { s -> update { it.copy(feedbackPrompt = s) } },
                    )
                }
            }

            ScoreCard(
                scores = config.scores,
                onUserScoreChange = { ref, score ->
                    val others = config.scores.filter { it.instanceId != ref.instanceId || it.modelId != ref.modelId }
                    update { it.copy(scores = others + ModelScore(ref.instanceId, ref.modelId, userScore = score)) }
                },
                onWeightChange = { ref, w ->
                    val existing = config.scores.firstOrNull { it.instanceId == ref.instanceId && it.modelId == ref.modelId }
                        ?: ModelScore(ref.instanceId, ref.modelId)
                    val others = config.scores.filter { it.instanceId != ref.instanceId || it.modelId != ref.modelId }
                    update { it.copy(scores = others + existing.copy(userWeight = w)) }
                },
                labelResolver = labelResolver,
            )
    }
}

private class ModelLabelResolver(services: List<ServiceEntry>) {
    private val map: Map<String, String> = buildMap {
        for (entry in services) {
            for (opt in entry.modelOptions) {
                put("${entry.instanceId}::${opt.id}", "${entry.serviceName} / ${opt.label}")
            }
        }
    }
    fun label(ref: ModelRef): String = map["${ref.instanceId}::${ref.modelId}"] ?: ref.modelId
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    selected: List<ModelRef>,
    multiSelect: Boolean,
    labelResolver: ModelLabelResolver,
    modelAliases: Map<String, String> = emptyMap(),
    onAliasChange: (ModelRef, String) -> Unit = { _, _ -> },
    onAdd: (ModelRef) -> Unit,
    onRemove: (ModelRef) -> Unit,
    services: List<ServiceEntry>,
) {
    var showPicker by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            selected.forEach { ref ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = modelAliases[ref.key]?.let { "${labelResolver.label(ref)}（${it}）" } ?: labelResolver.label(ref),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onRemove(ref) }) { Text("移除") }
                    }
                    // 自定义显示名（如 opencode-hy3），仅用于 UI，不进入发给大模型的 prompt。
                    val alias = modelAliases[ref.key] ?: ""
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { onAliasChange(ref, it) },
                        label = { Text("自定义名称（如 opencode-hy3）") },
                        placeholder = { Text("留空则使用默认名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }
            Button(onClick = { showPicker = true }, modifier = Modifier.handCursor()) {
                Text(if (selected.isEmpty()) "添加模型" else "继续添加")
            }
        }
    }

    if (showPicker) {
        ModelPickerSheet(
            services = services,
            exclude = selected,
            multiSelect = multiSelect,
            onPick = { ref ->
                onAdd(ref)
                if (!multiSelect) showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerSheet(
    services: List<ServiceEntry>,
    exclude: List<ModelRef>,
    multiSelect: Boolean,
    onPick: (ModelRef) -> Unit,
    onDismiss: () -> Unit,
) {
    val expanded = remember { mutableStateMapOf<String, Boolean>().apply { services.forEach { put(it.instanceId, false) } } }
    // 多选模式下的临时勾选集合（key = "$instanceId::$modelId"）。
    val checked = remember { mutableStateMapOf<String, Boolean>() }
    fun keyOf(ref: ModelRef) = "${ref.instanceId}::${ref.modelId}"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (multiSelect) {
                val checkedCount = checked.values.count { it }
                TextButton(
                    onClick = {
                        // 批量提交所有勾选的模型。
                        services.forEach { entry ->
                            entry.modelOptions.forEach { opt ->
                                val ref = ModelRef(entry.instanceId, opt.id)
                                if (checked[keyOf(ref)] == true) onPick(ref)
                            }
                        }
                        onDismiss()
                    },
                    enabled = checkedCount > 0,
                ) { Text("确认添加（$checkedCount）") }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
        dismissButton = if (multiSelect) {
            { TextButton(onClick = onDismiss) { Text("关闭") } }
        } else {
            null
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                items(services) { entry ->
                    Column {
                        // 服务分组标题行：可展开/折叠；多选模式下带「全选」按钮。
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                expanded[entry.instanceId] = !(expanded[entry.instanceId] ?: false)
                            }.handCursor(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = (if (expanded[entry.instanceId] == true) "▾ " else "▸ ") + entry.serviceName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f).padding(8.dp),
                            )
                            if (multiSelect && expanded[entry.instanceId] == true) {
                                val selectable = entry.modelOptions.map { ModelRef(entry.instanceId, it.id) }
                                    .filter { it !in exclude }
                                val allChecked = selectable.isNotEmpty() && selectable.all { checked[keyOf(it)] == true }
                                TextButton(
                                    onClick = {
                                        selectable.forEach { ref ->
                                            checked[keyOf(ref)] = !allChecked
                                        }
                                    },
                                    modifier = Modifier.handCursor(),
                                ) { Text(if (allChecked) "取消全选" else "全选") }
                            }
                        }
                        if (expanded[entry.instanceId] == true) {
                            entry.modelOptions.forEach { opt ->
                                val ref = ModelRef(entry.instanceId, opt.id)
                                val disabled = exclude.contains(ref)
                                val isSelected = checked[keyOf(ref)] == true
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                                        .clickable(enabled = !disabled) {
                                            if (multiSelect) {
                                                checked[keyOf(ref)] = !isSelected
                                            } else {
                                                onPick(ref)
                                                onDismiss()
                                            }
                                        }
                                        .handCursor(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (multiSelect) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null,
                                            enabled = !disabled,
                                        )
                                    } else {
                                        RadioButton(
                                            selected = false,
                                            onClick = null,
                                            enabled = !disabled,
                                        )
                                    }
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun NumberField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    KaiOutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.let { n -> if (n > 0) onValueChange(n) }
        },
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
private fun PromptField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value.ifEmpty { placeholder }) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onValueChange(it) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        maxLines = 10,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun ScoreCard(
    scores: List<ModelScore>,
    onUserScoreChange: (ModelRef, Double?) -> Unit,
    onWeightChange: (ModelRef, Double) -> Unit,
    labelResolver: ModelLabelResolver,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("模型评分", style = MaterialTheme.typography.titleMedium)
            Text(
                "参考 freellmapi 风格自动分析得分；你可自定义打分（足够权重计入最终分）。最终分 = 分析分×(1-权重) + 你的打分×权重。分数高的模型在显示时排在最上方。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (scores.isEmpty()) {
                Text("暂无评分数据，运行一次协作后将自动生成分析分。", style = MaterialTheme.typography.bodySmall)
            }
            // 按最终分降序：分数高的在前
            scores.sortedByDescending { it.finalScore }.forEach { score ->
                val ref = ModelRef(score.instanceId, score.modelId)
                var userText by remember(score.userScore) { mutableStateOf(score.userScore?.toString() ?: "") }
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(labelResolver.label(ref), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("分析分：${score.analysisScore}　最终分：${"%.1f".format(score.finalScore)}", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("你的打分", style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(
                            value = userText,
                            onValueChange = {
                                userText = it
                                onUserScoreChange(ref, it.toDoubleOrNull())
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Text("权重", style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(
                            value = score.userWeight.toString(),
                            onValueChange = { onWeightChange(ref, it.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: score.userWeight) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
            }
        }
    }
}

// region 小工具
@Composable
private fun cardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
// endregion
