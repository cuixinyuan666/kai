@file:OptIn(ExperimentalMaterial3Api::class)

package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.collaboration.CollaborationConfig
import com.inspiredandroid.kai.data.collaboration.CollaborationWizardParams
import com.inspiredandroid.kai.ui.KaiOutlinedTextField

@Composable
internal fun CollaborationWizardSheet(
    defaultConfig: CollaborationConfig,
    onDismiss: () -> Unit,
    onStart: (CollaborationWizardParams) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        var step by remember { mutableIntStateOf(0) }
        var question by remember { mutableStateOf("") }
        var minScoreText by remember { mutableStateOf("0") }
        var maxWait by remember { mutableIntStateOf(defaultConfig.maxWaitSeconds) }
        var retryCount by remember { mutableIntStateOf(defaultConfig.retryCount) }
        var notifyFailure by remember { mutableStateOf(defaultConfig.notifyOnFailure) }
        var notifyComplete by remember { mutableStateOf(defaultConfig.notifyOnComplete) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("协作模式向导", style = MaterialTheme.typography.titleLarge)

            when (step) {
                0 -> {
                    Text("请输入要发送给各模型的问题或任务。", style = MaterialTheme.typography.bodyMedium)
                    KaiOutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("问题") },
                        minLines = 4,
                    )
                }
                1 -> {
                    Text("选择参与协作的模型：仅模型测试分数严格大于该值的模型会收到指令。", style = MaterialTheme.typography.bodyMedium)
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
                    NumberField("单次调用最大等待时间（秒，默认 60）", maxWait) { maxWait = it }
                    NumberField("模型失败重试次数", retryCount) { retryCount = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("模型失败显式提醒", modifier = Modifier.weight(1f))
                        Switch(checked = notifyFailure, onCheckedChange = { notifyFailure = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("每轮结束提醒", modifier = Modifier.weight(1f))
                        Switch(checked = notifyComplete, onCheckedChange = { notifyComplete = it })
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
                                if (step == 0 && question.isBlank()) return@Button
                                step += 1
                            },
                            enabled = step != 0 || question.isNotBlank(),
                        ) { Text("下一步") }
                    } else {
                        Button(
                            onClick = {
                                val minScore = minScoreText.toDoubleOrNull() ?: 0.0
                                onStart(
                                    CollaborationWizardParams(
                                        question = question.trim(),
                                        minScoreThreshold = minScore,
                                        maxWaitSeconds = maxWait.coerceAtLeast(1),
                                        retryCount = retryCount.coerceAtLeast(0),
                                        notifyOnFailure = notifyFailure,
                                        notifyOnComplete = notifyComplete,
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
private fun NumberField(label: String, value: Int, onValueChange: (Int) -> Unit) {
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
