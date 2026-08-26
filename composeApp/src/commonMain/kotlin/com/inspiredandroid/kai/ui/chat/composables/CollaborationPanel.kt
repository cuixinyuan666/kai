package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.collaboration.CollaborationEvent
import com.inspiredandroid.kai.data.collaboration.CollaborationPhase
import com.inspiredandroid.kai.data.collaboration.CollaborationRoleKind
import com.inspiredandroid.kai.ui.chat.ChatUiState
import com.inspiredandroid.kai.ui.handCursor

private const val OVERVIEW_TAB_KEY = "__overview__"

/**
 * 协作运行面板：按会话（任务方×监督方）展示隔离的对话记录。
 */
@Composable
internal fun CollaborationPanel(uiState: ChatUiState) {
    val show = uiState.chatMode == com.inspiredandroid.kai.data.collaboration.ChatMode.COLLABORATION ||
        uiState.collaborationEvents.isNotEmpty() || uiState.collaborationSummary != null
    if (!show) return

    val actions = uiState.actions
    val sessionKeys = remember(uiState.collaborationEvents) {
        uiState.collaborationEvents
            .mapNotNull { it.sessionKey }
            .distinct()
            .sortedWith(compareBy({ parseSessionKey(it).first }, { parseSessionKey(it).second }))
    }
    val sessionLabels = remember(sessionKeys) {
        sessionKeys.associateWith { formatSessionTabLabel(it) }
    }
    var selectedTab by remember { mutableStateOf(OVERVIEW_TAB_KEY) }
    val filteredEvents = remember(uiState.collaborationEvents, selectedTab) {
        if (selectedTab == OVERVIEW_TAB_KEY) {
            uiState.collaborationEvents
        } else {
            uiState.collaborationEvents.filter { it.sessionKey == selectedTab || it.sessionKey == null }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("协作模式运行", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (uiState.isCollaborating) {
                    Button(onClick = actions.stopCollaboration, modifier = Modifier.handCursor()) { Text("停止") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = selectedTab == OVERVIEW_TAB_KEY,
                    onClick = { selectedTab = OVERVIEW_TAB_KEY },
                    label = { Text("总览") },
                    modifier = Modifier.handCursor(),
                )
                sessionKeys.forEach { key ->
                    FilterChip(
                        selected = selectedTab == key,
                        onClick = { selectedTab = key },
                        label = { Text(sessionLabels[key] ?: key) },
                        modifier = Modifier.handCursor(),
                    )
                }
            }

            uiState.collaborationNotification?.let { note ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(note, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = actions.clearCollaborationNotification) { Text("知道了") }
                    }
                }
            }

            uiState.collaborationQuestion?.let { question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("用户提问", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(question, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (filteredEvents.isNotEmpty()) {
                val viewLabel = if (selectedTab == OVERVIEW_TAB_KEY) "总览" else (sessionLabels[selectedTab] ?: selectedTab)
                CollaborationTreeView(events = filteredEvents, viewLabel = viewLabel)
            } else if (uiState.collaborationEvents.isNotEmpty()) {
                Text(
                    "当前会话暂无记录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            uiState.collaborationSummary?.let { summary ->
                Text("协作汇总：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** 将 sessionKey（如 task1-supervisor2）格式化为可读标签。 */
private fun formatSessionTabLabel(sessionKey: String): String {
    val (taskIdx, supIdx) = parseSessionKey(sessionKey)
    return "任务方${taskIdx + 1}·监督方${supIdx + 1}"
}

private fun parseSessionKey(sessionKey: String): Pair<Int, Int> {
    val taskMatch = Regex("task(\\d+)").find(sessionKey)
    val supMatch = Regex("supervisor(\\d+)").find(sessionKey)
    val taskIdx = taskMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
    val supIdx = supMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
    return taskIdx to supIdx
}

@Composable
private fun CollaborationTreeView(events: List<CollaborationEvent>, viewLabel: String) {
    val rounds = remember(events) { groupByRound(events).entries.map { it.key to it.value } }
    val expanded = remember { mutableStateMapOf<Int, Boolean>().apply { rounds.forEach { put(it.first, true) } } }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(rounds) { (round, evs) ->
            val isExpanded = expanded[round] ?: true
            RoundTreeCard(
                round = round,
                events = evs,
                isExpanded = isExpanded,
                onToggle = { expanded[round] = !isExpanded },
                viewLabel = viewLabel,
            )
        }
    }
}

@Composable
private fun RoundTreeCard(
    round: Int,
    events: List<CollaborationEvent>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    viewLabel: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.handCursor().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isExpanded) "▼ " else "▶ ",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "第 $round 轮 · $viewLabel",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                val done = events.lastOrNull { it.phase == CollaborationPhase.DONE }
                val failed = events.lastOrNull { it.phase == CollaborationPhase.FAILED }
                when {
                    done != null -> PhaseBadge("完成", Color(0xFF2E7D32))
                    failed != null -> PhaseBadge("失败", Color(0xFFC62828))
                    else -> PhaseBadge("进行中", Color(0xFF1976D2))
                }
            }

            AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    events.forEach { event ->
                        TreeNodeRow(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeNodeRow(event: CollaborationEvent) {
    if (event.isAnswer) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(
                text = event.sourceLabel ?: "回答",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(8.dp),
            ) {
                Text(
                    text = event.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        return
    }
    val isSystem = event.roleKind == CollaborationRoleKind.SYSTEM
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .heightIn(min = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(phaseColor(event.phase, isSystem)),
            )
        }
        Text(
            text = "[${phaseLabel(event.phase, isSystem)}] ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = phaseColor(event.phase, isSystem),
        )
        Text(
            text = (event.sourceLabel?.let { "$it：" } ?: "") + event.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PhaseBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun groupByRound(events: List<CollaborationEvent>): Map<Int, List<CollaborationEvent>> {
    val result = LinkedHashMap<Int, MutableList<CollaborationEvent>>()
    var current = 0
    for (e in events) {
        if (e.round > 0) current = e.round
        result.getOrPut(current) { mutableListOf() }.add(e)
    }
    return result
}

private fun phaseLabel(phase: CollaborationPhase, isSystem: Boolean): String = when {
    isSystem -> "系统转发"
    phase == CollaborationPhase.IDLE -> "待机"
    phase == CollaborationPhase.DISTRIBUTE -> "分发"
    phase == CollaborationPhase.TASK -> "任务方"
    phase == CollaborationPhase.SUPERVISE -> "监督方"
    phase == CollaborationPhase.DIALOGUE -> "对话"
    phase == CollaborationPhase.DONE -> "完成"
    phase == CollaborationPhase.FAILED -> "失败"
    phase == CollaborationPhase.CANCELLED -> "取消"
    else -> phase.name
}

private fun phaseColor(phase: CollaborationPhase, isSystem: Boolean): Color = when {
    isSystem -> Color(0xFF6D4C41)
    phase == CollaborationPhase.TASK -> Color(0xFF1976D2)
    phase == CollaborationPhase.SUPERVISE -> Color(0xFF388E3C)
    phase == CollaborationPhase.DIALOGUE -> Color(0xFF7B1FA2)
    phase == CollaborationPhase.DONE -> Color(0xFF2E7D32)
    phase == CollaborationPhase.FAILED -> Color(0xFFC62828)
    phase == CollaborationPhase.CANCELLED -> Color(0xFF616161)
    else -> Color(0xFF757575)
}
