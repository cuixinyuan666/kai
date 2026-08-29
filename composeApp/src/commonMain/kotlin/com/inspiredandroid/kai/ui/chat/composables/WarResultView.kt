package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.war.WarAspectResult
import com.inspiredandroid.kai.data.war.WarCopyFormatter
import com.inspiredandroid.kai.data.war.WarEvent
import com.inspiredandroid.kai.data.war.WarTaskResult
import com.inspiredandroid.kai.data.war.WarVoteChoice
import com.inspiredandroid.kai.ui.chat.ChatActions
import com.inspiredandroid.kai.ui.handCursor

@Composable
internal fun WarResultView(
    result: WarTaskResult?,
    warEvents: List<WarEvent>,
    isRunning: Boolean,
    actions: ChatActions,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenModelFolder: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "战争模式 · 任务结果",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (result != null) {
                IconButton(onClick = { onCopy(WarCopyFormatter.format(result)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制报告")
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            result?.question?.let { question ->
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (isRunning) {
                val latestEvent = warEvents.lastOrNull()
                Text(
                    text = latestEvent?.text ?: "准备中…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (result == null && !isRunning) {
                Text("暂无结果数据。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            val displayResult = result ?: return@Column

            if (displayResult.analysisFailed) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = displayResult.analysisError ?: "分析失败",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            if (displayResult.commonPoints.isNotEmpty()) {
                Text("相同点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                displayResult.commonPoints.forEach { point ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF43A047))
                        Text(point, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (displayResult.aspectResults.isEmpty() && !displayResult.analysisFailed) {
                Card {
                    Text(
                        text = "全体一致，无分歧方面。",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            displayResult.aspectResults.forEach { aspectResult ->
                WarAspectCard(aspectResult)
            }

            if (displayResult.summaryModelLabel != null) {
                Text(
                    text = "总结模型：${displayResult.summaryModelLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(onClick = onOpenModelFolder, modifier = Modifier.handCursor()) {
                Text("查看各模型对话")
            }
        }
    }
}

@Composable
private fun WarAspectCard(aspectResult: WarAspectResult) {
    var expanded by remember { mutableStateOf(false) }
    val valid = aspectResult.validVoteCount.coerceAtLeast(1)
    val agreeRatio = aspectResult.agreeCount.toFloat() / valid

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .handCursor(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = aspectResult.aspect.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = aspectResult.aspect.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "同意 ${aspectResult.agreeCount}/${aspectResult.validVoteCount} · 不同意 ${aspectResult.disagreeCount}/${aspectResult.validVoteCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                )
            }
            LinearProgressIndicator(
                progress = { agreeRatio },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF43A047),
                trackColor = Color(0xFFE53935).copy(alpha = 0.4f),
            )
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    aspectResult.votes.forEach { vote ->
                        val (icon, label, tint) = when (vote.choice) {
                            WarVoteChoice.AGREE.name -> Triple(Icons.Default.Check, "同意", Color(0xFF43A047))
                            WarVoteChoice.DISAGREE.name -> Triple(Icons.Default.Close, "不同意", Color(0xFFE53935))
                            else -> Triple(null, "未表态", MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (icon != null) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp))
                            }
                            Text(
                                text = buildString {
                                    append(vote.modelLabel)
                                    append("：")
                                    append(label)
                                    if (vote.reason.isNotBlank()) append(" — ${vote.reason}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
