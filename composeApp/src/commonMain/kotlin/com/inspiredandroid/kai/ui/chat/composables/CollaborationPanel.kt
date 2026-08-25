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
import androidx.compose.foundation.layout.Spacer
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

/** 协作面板视图模式：总览或按角色过滤。 */
private enum class CollaborationViewTab(val label: String, val roleKind: CollaborationRoleKind?) {
    OVERVIEW("总览", null),
    TASK("任务方", CollaborationRoleKind.TASK),
    TRANSMIT("传达方", CollaborationRoleKind.TRANSMIT),
    SUPERVISE("监督方", CollaborationRoleKind.SUPERVISE),
    FEEDBACK("回传方", CollaborationRoleKind.FEEDBACK),
}

/**
 * 协作运行面板：以树形图按轮次展示各方执行记录，附带显式提醒、停止按钮与最终汇总。
 * 支持按角色切换独立视图，各视图同步展示完整聊天记录与对应角色的执行记录。
 * 在聊天栏输入区上方显示，仅在协作模式或存在协作事件时出现。
 */
@Composable
internal fun CollaborationPanel(uiState: ChatUiState) {
    val show = uiState.chatMode == com.inspiredandroid.kai.data.collaboration.ChatMode.COLLABORATION ||
        uiState.collaborationEvents.isNotEmpty() || uiState.collaborationSummary != null
    if (!show) return

    val actions = uiState.actions
    var selectedTab by remember { mutableStateOf(CollaborationViewTab.OVERVIEW) }
    val filteredEvents = remember(uiState.collaborationEvents, selectedTab) {
        filterEventsForTab(uiState.collaborationEvents, selectedTab)
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

            // 角色独立视图切换
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CollaborationViewTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
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

            // 各角色独立视图与总览均展示用户原始提问（完整聊天记录的一部分）
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
                CollaborationTreeView(events = filteredEvents, viewLabel = selectedTab.label)
            } else if (uiState.collaborationEvents.isNotEmpty()) {
                Text(
                    "当前角色暂无执行记录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            uiState.collaborationSummary?.let { summary ->
                Text("最终回传汇总：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun filterEventsForTab(events: List<CollaborationEvent>, tab: CollaborationViewTab): List<CollaborationEvent> {
    val roleKind = tab.roleKind ?: return events
    return events.filter { event ->
        val eventRole = event.roleKind ?: event.phase.toRoleKind()
        eventRole == roleKind
    }
}

private fun CollaborationPhase.toRoleKind(): CollaborationRoleKind? = when (this) {
    CollaborationPhase.TASK -> CollaborationRoleKind.TASK
    CollaborationPhase.TRANSMIT -> CollaborationRoleKind.TRANSMIT
    CollaborationPhase.SUPERVISE -> CollaborationRoleKind.SUPERVISE
    CollaborationPhase.FEEDBACK -> CollaborationRoleKind.FEEDBACK
    else -> null
}

/**
 * 按轮次分组的树形视图。round==0 的事件（重试/兜底提示）归入当前进行中的轮次。
 */
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

/** 一轮的树形卡片：标题可折叠，展开后按阶段顺序列出各角色执行节点。 */
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

/** 树形单行：带角色色块、阶段标签与事件文本，左侧竖线表示层级。 */
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
                    .background(phaseColor(event.phase)),
            )
        }
        Text(
            text = "[${phaseLabel(event.phase)}] ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = phaseColor(event.phase),
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

private fun phaseLabel(phase: CollaborationPhase): String = when (phase) {
    CollaborationPhase.IDLE -> "待机"
    CollaborationPhase.DISTRIBUTE -> "分发"
    CollaborationPhase.TASK -> "任务方"
    CollaborationPhase.TRANSMIT -> "传达方"
    CollaborationPhase.SUPERVISE -> "监督方"
    CollaborationPhase.FEEDBACK -> "回传方"
    CollaborationPhase.DONE -> "完成"
    CollaborationPhase.FAILED -> "失败"
    CollaborationPhase.CANCELLED -> "取消"
}

private fun phaseColor(phase: CollaborationPhase): Color = when (phase) {
    CollaborationPhase.IDLE -> Color(0xFF757575)
    CollaborationPhase.DISTRIBUTE -> Color(0xFF1976D2)
    CollaborationPhase.TASK -> Color(0xFF1976D2)
    CollaborationPhase.TRANSMIT -> Color(0xFF7B1FA2)
    CollaborationPhase.SUPERVISE -> Color(0xFF388E3C)
    CollaborationPhase.FEEDBACK -> Color(0xFFF57C00)
    CollaborationPhase.DONE -> Color(0xFF2E7D32)
    CollaborationPhase.FAILED -> Color(0xFFC62828)
    CollaborationPhase.CANCELLED -> Color(0xFF616161)
}
