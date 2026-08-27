package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.ServiceEntry
import com.inspiredandroid.kai.data.collaboration.CollaborationConfig
import com.inspiredandroid.kai.data.collaboration.DEFAULT_COLLABORATION_PROMPT
import com.inspiredandroid.kai.data.collaboration.ModelRef
import com.inspiredandroid.kai.data.collaboration.ModelScore
import com.inspiredandroid.kai.ui.KaiOutlinedTextField
import kotlin.math.roundToInt

/**
 * 协作模式设置页。
 * 同一条指令并行发送给所有模型测试总分 > 0 的模型。
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("参与规则", style = MaterialTheme.typography.titleMedium)
                Text(
                    "协作模式将同一条用户指令并行发送给所有已配置模型中「模型测试总分 > 0」的模型。请先运行模型测试；测试失败（无响应）的模型得分为 0，不会参与协作。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("运行参数", style = MaterialTheme.typography.titleMedium)
                NumberField(
                    label = "单次调用最大等待时间（秒，默认 60）",
                    value = config.maxWaitSeconds,
                    onValueChange = { n -> update { it.copy(maxWaitSeconds = n) } },
                )
                NumberField(
                    label = "模型失败重试次数",
                    value = config.retryCount,
                    onValueChange = { n -> update { it.copy(retryCount = n) } },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("模型失败显式提醒", modifier = Modifier.weight(1f))
                    Switch(checked = config.notifyOnFailure, onCheckedChange = { update { it.copy(notifyOnFailure = !it.notifyOnFailure) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("每轮结束提醒", modifier = Modifier.weight(1f))
                    Switch(checked = config.notifyOnComplete, onCheckedChange = { update { it.copy(notifyOnComplete = !it.notifyOnComplete) } })
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("提示词（可自定义）", style = MaterialTheme.typography.titleMedium)
                Text("协作模型系统提示词", style = MaterialTheme.typography.labelMedium)
                PromptField(
                    value = config.modelPrompt,
                    placeholder = DEFAULT_COLLABORATION_PROMPT,
                    onValueChange = { s -> update { it.copy(modelPrompt = s) } },
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
                "参考 freellmapi 风格自动分析得分；你可自定义打分（足够权重计入最终分）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (scores.isEmpty()) {
                Text("暂无评分数据，运行一次协作后将自动生成分析分。", style = MaterialTheme.typography.bodySmall)
            }
            scores.sortedByDescending { it.finalScore }.forEach { score ->
                val ref = ModelRef(score.instanceId, score.modelId)
                var userText by remember(score.userScore) { mutableStateOf(score.userScore?.toString() ?: "") }
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(labelResolver.label(ref), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val finalScoreText = (score.finalScore * 10).roundToInt().let { "${it / 10}.${it % 10}" }
                    Text("分析分：${score.analysisScore}　最终分：$finalScoreText", style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun cardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
