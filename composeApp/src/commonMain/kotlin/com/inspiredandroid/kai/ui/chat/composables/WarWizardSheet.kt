@file:OptIn(ExperimentalMaterial3Api::class)

package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.collaboration.CollaborationConfig
import com.inspiredandroid.kai.data.ServiceEntry
import com.inspiredandroid.kai.data.collaboration.ModelRef
import com.inspiredandroid.kai.data.war.WarWizardParams
import com.inspiredandroid.kai.speech.SpeechToText
import com.inspiredandroid.kai.ui.KaiOutlinedTextField
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun WarWizardSheet(
    defaultConfig: CollaborationConfig,
    supportedFileExtensions: ImmutableList<String>,
    speechSupported: Boolean,
    isOptimizingPrompt: Boolean,
    pendingPromptText: String?,
    eligibleModelOptions: ImmutableList<Pair<ModelRef, String>>? = null,
    availableServices: ImmutableList<ServiceEntry> = kotlinx.collections.immutable.persistentListOf(),
    modelBenchmarks: ImmutableMap<String, Double>,
    onOptimizePrompt: (String) -> Unit,
    onPendingPromptConsumed: () -> Unit,
    onDismiss: () -> Unit,
    onStart: (WarWizardParams) -> Unit,
    speechToText: SpeechToText? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        var step by remember { mutableIntStateOf(0) }
        var questionInputText by remember { mutableStateOf(TextFieldValue("")) }
        val wizardFiles = remember { mutableStateListOf<PlatformFile>() }
        var isSpeechListening by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var minScoreText by remember { mutableStateOf("0") }
        var maxWait by remember { mutableIntStateOf(defaultConfig.maxWaitSeconds) }
        var retryCount by remember { mutableIntStateOf(defaultConfig.retryCount) }
        var notifyFailure by remember { mutableStateOf(defaultConfig.notifyOnFailure) }
        var notifyComplete by remember { mutableStateOf(defaultConfig.notifyOnComplete) }
        var summaryExpanded by remember { mutableStateOf(false) }
        var summarySelection by remember { mutableStateOf<ModelRef?>(null) }

        val minScore = minScoreText.toDoubleOrNull() ?: 0.0
        val resolvedEligible = eligibleModelOptions ?: remember(availableServices, modelBenchmarks, minScore) {
            buildList {
                for (entry in availableServices) {
                    val modelIds = entry.modelOptions.map { it.id }.ifEmpty { listOfNotNull(entry.modelId) }
                    for (modelId in modelIds.distinct()) {
                        val score = modelBenchmarks["${entry.serviceId}::$modelId"] ?: 0.0
                        if (score > minScore) {
                            val ref = ModelRef(entry.instanceId, modelId)
                            val label = entry.modelOptions.find { it.id == modelId }?.label ?: modelId
                            add(ref to "${entry.serviceName} / $label")
                        }
                    }
                }
            }.toImmutableList()
        }

        LaunchedEffect(pendingPromptText) {
            val text = pendingPromptText
            if (text != null) {
                questionInputText = TextFieldValue(text)
                onPendingPromptConsumed()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("战争模式向导", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            when (step) {
                0 -> {
                    Text(
                        "请输入任务。第 1 轮所有达标模型并行作答，总结模型提取分歧，第 2 轮各模型对分歧投票。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    QuestionInput(
                        files = wizardFiles.toImmutableList(),
                        addFile = { wizardFiles.add(it) },
                        removeFile = { wizardFiles.remove(it) },
                        ask = { },
                        supportedFileExtensions = supportedFileExtensions,
                        textState = questionInputText,
                        onTextStateChange = { questionInputText = it },
                        onOptimizePrompt = { onOptimizePrompt(questionInputText.text) },
                        isOptimizingPrompt = isOptimizingPrompt,
                        speechSupported = speechSupported,
                        isSpeechListening = isSpeechListening,
                        onToggleSpeechInput = {
                            val stt = speechToText
                            if (stt != null) {
                                scope.launch {
                                    if (isSpeechListening) {
                                        isSpeechListening = false
                                        stt.stopListening().onSuccess { text ->
                                            if (text.isNotBlank()) {
                                                questionInputText = TextFieldValue(questionInputText.text + text)
                                            }
                                        }
                                    } else {
                                        isSpeechListening = true
                                        val lang = if (questionInputText.text.any { it.code > 127 }) "zh" else "en"
                                        stt.startListening(lang).onFailure {
                                            isSpeechListening = false
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
                1 -> {
                    Text("选择参与战争的模型：仅模型测试分数严格大于该值的模型会收到指令。", style = MaterialTheme.typography.bodyMedium)
                    KaiOutlinedTextField(
                        value = minScoreText,
                        onValueChange = { minScoreText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("分数门槛（>）") },
                        singleLine = true,
                    )
                }
                2 -> {
                    Text("设置运行参数", style = MaterialTheme.typography.titleMedium)
                    WarNumberField("单次调用最大等待时间（秒，默认 60）", maxWait) { maxWait = it }
                    WarNumberField("模型失败重试次数", retryCount) { retryCount = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("模型失败显式提醒", modifier = Modifier.weight(1f))
                        Switch(checked = notifyFailure, onCheckedChange = { notifyFailure = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("任务结束提醒", modifier = Modifier.weight(1f))
                        Switch(checked = notifyComplete, onCheckedChange = { notifyComplete = it })
                    }
                    Text("总结模型（分析相同点与分歧）", style = MaterialTheme.typography.bodyMedium)
                    val autoLabel = "自动（参与模型中最高分）"
                    val selectedLabel = summarySelection?.let { ref ->
                        resolvedEligible.find { it.first == ref }?.second
                            ?: ref.modelId
                    } ?: autoLabel
                    ExposedDropdownMenuBox(
                        expanded = summaryExpanded,
                        onExpandedChange = { summaryExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        KaiOutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("总结模型") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = summaryExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = summaryExpanded,
                            onDismissRequest = { summaryExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(autoLabel) },
                                onClick = {
                                    summarySelection = null
                                    summaryExpanded = false
                                },
                            )
                            resolvedEligible.forEach { (ref, label) ->
                                val score = modelBenchmarks[ref.key]?.toInt()?.toString() ?: "?"
                                DropdownMenuItem(
                                    text = { Text("$label ($score)") },
                                    onClick = {
                                        summarySelection = ref
                                        summaryExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (step > 0) {
                        TextButton(onClick = { step -= 1 }) { Text("上一步") }
                    }
                    if (step < 2) {
                        Button(
                            onClick = {
                                if (step == 0 && questionInputText.text.isBlank()) return@Button
                                step += 1
                            },
                            enabled = step != 0 || questionInputText.text.isNotBlank(),
                        ) { Text("下一步") }
                    } else {
                        Button(
                            onClick = {
                                val minScore = minScoreText.toDoubleOrNull() ?: 0.0
                                onStart(
                                    WarWizardParams(
                                        question = questionInputText.text.trim(),
                                        minScoreThreshold = minScore,
                                        maxWaitSeconds = maxWait.coerceAtLeast(1),
                                        retryCount = retryCount.coerceAtLeast(0),
                                        notifyOnFailure = notifyFailure,
                                        notifyOnComplete = notifyComplete,
                                        attachedFiles = wizardFiles.toList(),
                                        summaryModelOverride = summarySelection,
                                    ),
                                )
                            },
                        ) { Text("开始") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarNumberField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    KaiOutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.let { n -> if (n >= 0) onValueChange(n) }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
