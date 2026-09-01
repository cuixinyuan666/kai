package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.war.WarCopyFormatter
import com.inspiredandroid.kai.data.war.WarEvent
import com.inspiredandroid.kai.data.war.WarTaskResult
import com.inspiredandroid.kai.data.war.WarVoteChoice
import com.inspiredandroid.kai.data.war.WarVoting
import com.inspiredandroid.kai.data.war.displayVoteRounds
import com.inspiredandroid.kai.ui.chat.ChatActions
import com.inspiredandroid.kai.ui.components.HorizontalScrollbarForScroll
import com.inspiredandroid.kai.ui.components.ModelPairChipsFromLabel
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForScroll
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
    val bg = MaterialTheme.colorScheme.background
    val isDarkBg = bg.luminance() < 0.45f
    val fg = if (isDarkBg) Color.White else Color.Black
    val forcedScheme = MaterialTheme.colorScheme.copy(
        background = bg,
        onBackground = fg,
        onSurface = fg,
        onSurfaceVariant = if (isDarkBg) Color(0xFFE6E1E5) else MaterialTheme.colorScheme.onSurfaceVariant,
        surfaceContainerHigh = if (isDarkBg) Color(0xFF2B2930) else MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    MaterialTheme(colorScheme = forcedScheme) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bg,
        contentColor = fg,
    ) {
    CompositionLocalProvider(LocalContentColor provides fg) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(contentColor = fg),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = fg,
                )
            }
            Text(
                text = "战争模式 · 任务结果",
                style = MaterialTheme.typography.titleLarge,
                color = fg,
                modifier = Modifier.weight(1f),
            )
            if (result != null) {
                IconButton(
                    onClick = { onCopy(WarCopyFormatter.format(result)) },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = fg),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制报告", tint = fg)
                }
            }
        }

        val bodyScroll = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(bodyScroll)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(end = 12.dp),
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
                Text(
                    "相同点",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            displayResult.commonPoints.forEach { point ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                    )
                                    Text(
                                        point,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (displayResult.displayVoteRounds().isEmpty() && displayResult.aspectResults.isEmpty() && !displayResult.analysisFailed) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(
                        text = "全体一致，无分歧方案。",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            displayResult.displayVoteRounds().takeIf { it.isNotEmpty() }?.let { rounds ->
                WarVoteTable(
                    rounds = rounds,
                    onOpenModelMessage = { conversationId, messageId ->
                        actions.openWarModelMessage(conversationId, messageId)
                    },
                )
            }

            displayResult.finalSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                Text(
                    text = "最终汇总",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Text(
                        text = summary,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (displayResult.summaryModelLabel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = if (displayResult.summaryConversationId != null) {
                        Modifier.clickable {
                            actions.openWarModelMessage(displayResult.summaryConversationId, "")
                        }.handCursor()
                    } else {
                        Modifier
                    },
                ) {
                    Text(
                        text = "总结模型：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ModelPairChipsFromLabel(displayResult.summaryModelLabel, compact = true)
                }
            }

            TextButton(onClick = onOpenModelFolder, modifier = Modifier.handCursor()) {
                Text("查看各模型对话")
            }
        }
        VerticalScrollbarForScroll(
            scrollState = bodyScroll,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
        }
    }
    }
    }
    }
}

@Composable
private fun WarVoteTable(
    rounds: List<com.inspiredandroid.kai.data.war.WarVoteRoundResult>,
    onOpenModelMessage: (conversationId: String, messageId: String) -> Unit,
) {
    val aspects = rounds.lastOrNull()?.aspectResults?.map { it.aspect } ?: return
    var expanded by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val cellWidth = 96.dp
    val labelWidth = 72.dp
    Text(
        text = "各方案投票统计",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        val tableScroll = rememberScrollState()
        Column(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.horizontalScroll(tableScroll)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "轮次",
                    modifier = Modifier.width(labelWidth).padding(4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                aspects.forEach { aspect ->
                    Text(
                        text = aspect.title,
                        modifier = Modifier.width(cellWidth).padding(4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                }
            }
            rounds.forEachIndexed { index, round ->
                val previous = rounds.getOrNull(index - 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "第${round.round}轮",
                        modifier = Modifier.width(labelWidth).padding(4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    aspects.forEach { aspect ->
                        val current = round.aspectResults.find { it.aspect.id == aspect.id }
                        val prev = previous?.aspectResults?.find { it.aspect.id == aspect.id }
                        val text = current?.let { WarVoting.cellText(it, prev) } ?: "-"
                        val selected = expanded == (round.round to aspect.id)
                        Text(
                            text = text,
                            modifier = Modifier
                                .width(cellWidth)
                                .clickable(enabled = current != null) {
                                    expanded = if (selected) null else round.round to aspect.id
                                }
                                .handCursor()
                                .padding(4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
            }
            HorizontalScrollbarForScroll(
                scrollState = tableScroll,
                modifier = Modifier.fillMaxWidth(),
            )
        expanded?.let { (roundNumber, aspectId) ->
            val aspectResult = rounds.find { it.round == roundNumber }
                ?.aspectResults
                ?.find { it.aspect.id == aspectId }
            if (aspectResult != null) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "第${roundNumber}轮 · ${aspectResult.aspect.title} 投票模型",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (aspectResult.votes.isEmpty()) {
                        Text("本轮没有模型参与该方案投票。", style = MaterialTheme.typography.bodySmall)
                    } else {
                        aspectResult.votes.forEach { vote ->
                            val stance = when {
                                vote.reason.startsWith("提出方") -> "提出方（不投票）"
                                vote.choice == WarVoteChoice.AGREE.name -> "同意"
                                vote.choice == WarVoteChoice.DISAGREE.name -> "不同意"
                                else -> "未表态"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (vote.conversationId.isNotBlank()) {
                                            onOpenModelMessage(vote.conversationId, vote.messageId)
                                        }
                                    }
                                    .handCursor()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ModelPairChipsFromLabel(vote.modelLabel, compact = true)
                                Text(
                                    text = stance,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (vote.reason.isNotBlank()) {
                                    Text(
                                        text = vote.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
