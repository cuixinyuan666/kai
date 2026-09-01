@file:OptIn(ExperimentalMaterial3Api::class)

package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.data.metadata
import com.inspiredandroid.kai.data.war.WarVoting
import com.inspiredandroid.kai.ui.chat.ChatActions
import com.inspiredandroid.kai.ui.components.ModelPairChipsFromLabel
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForList
import com.inspiredandroid.kai.ui.handCursor
import kotlinx.collections.immutable.ImmutableList

/**
 * Root history lists single-mode chats directly (matching pre-folder UX) plus
 * collaboration and war mode folders. The single-mode folder itself is omitted
 * here because its children are already shown at the root.
 */
internal fun rootHistoryItems(
    conversations: List<Conversation>,
    rootIds: List<String> = listOf(
        Conversation.FOLDER_SINGLE_MODE_ID,
        Conversation.FOLDER_COLLABORATION_MODE_ID,
        Conversation.FOLDER_WAR_MODE_ID,
    ),
): List<Conversation> {
    val singleChats = ConversationFolderManager.childrenOf(
        Conversation.FOLDER_SINGLE_MODE_ID,
        conversations,
    ).filter {
        it.type == Conversation.TYPE_CHAT || it.type == Conversation.TYPE_INTERACTIVE
    }
    val modeFolders = conversations.filter {
        it.id in rootIds && it.id != Conversation.FOLDER_SINGLE_MODE_ID
    }
    return singleChats + modeFolders
}

internal enum class HistoryTimeSort {
    NewestFirst,
    OldestFirst,
}

internal enum class HistoryNameSort {
    Asc,
    Desc,
}

internal fun sortHistoryItems(
    items: List<Conversation>,
    sort: HistoryTimeSort,
    keepFoldersLast: Boolean,
): List<Conversation> {
    val (folders, rest) = if (keepFoldersLast) {
        items.partition { it.type == Conversation.TYPE_FOLDER }
    } else {
        emptyList<Conversation>() to items
    }
    val sortedRest = if (sort == HistoryTimeSort.NewestFirst) {
        rest.sortedByDescending { it.updatedAt }
    } else {
        rest.sortedBy { it.updatedAt }
    }
    val sortedFolders = if (sort == HistoryTimeSort.NewestFirst) {
        folders.sortedByDescending { it.updatedAt }
    } else {
        folders.sortedBy { it.updatedAt }
    }
    return sortedRest + sortedFolders
}

internal fun sortByParentModelName(
    items: List<Conversation>,
    ascending: Boolean,
): List<Conversation> {
    val pinned = items.filter { it.type == Conversation.TYPE_WAR_RESULT }
    val rest = items.filter { it.type != Conversation.TYPE_WAR_RESULT }
    val sorted = rest.sortedWith { a, b ->
        val byParent = WarVoting.parentName(a.title).compareTo(WarVoting.parentName(b.title), ignoreCase = true)
        if (byParent != 0) byParent else a.title.compareTo(b.title, ignoreCase = true)
    }
    val ordered = if (ascending) sorted else sorted.asReversed()
    return pinned + ordered
}

@Composable
internal fun ChatHistoryTreeSheet(
    conversations: ImmutableList<Conversation>,
    treeParentId: String?,
    actions: ChatActions,
    onDismiss: () -> Unit,
    onOpenModelView: (String) -> Unit,
    onOpenWarResult: (String) -> Unit,
    onCopy: (String, Int) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val rootIds = listOf(
            Conversation.FOLDER_SINGLE_MODE_ID,
            Conversation.FOLDER_COLLABORATION_MODE_ID,
            Conversation.FOLDER_WAR_MODE_ID,
        )
        val parentId = treeParentId
        val parentConv = conversations.find { it.id == parentId }
        val sortByParentName = parentConv?.type == Conversation.TYPE_WAR_TASK
        var newestFirst by remember(parentId) { mutableStateOf(true) }
        var nameAsc by remember(parentId) { mutableStateOf(true) }
        var sortMenuOpen by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (parentId != null) {
                    IconButton(onClick = { actions.closeHistoryFolder() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    text = when (parentId) {
                        null -> "聊天记录"
                        Conversation.FOLDER_SINGLE_MODE_ID -> Conversation.FOLDER_SINGLE_MODE_TITLE
                        Conversation.FOLDER_COLLABORATION_MODE_ID -> Conversation.FOLDER_COLLABORATION_MODE_TITLE
                        Conversation.FOLDER_WAR_MODE_ID -> Conversation.FOLDER_WAR_MODE_TITLE
                        else -> conversations.find { it.id == parentId }?.title ?: "文件夹"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Box {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = if (sortByParentName) {
                                if (nameAsc) "按母模型字母正序" else "按母模型字母反序"
                            } else {
                                if (newestFirst) "最新任务置顶" else "按时间从早到晚"
                            },
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                    ) {
                        if (sortByParentName) {
                            DropdownMenuItem(
                                text = { Text("母模型名称 A→Z") },
                                onClick = {
                                    nameAsc = true
                                    sortMenuOpen = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("母模型名称 Z→A") },
                                onClick = {
                                    nameAsc = false
                                    sortMenuOpen = false
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("按时间从晚到早（最新置顶）") },
                                onClick = {
                                    newestFirst = true
                                    sortMenuOpen = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("按时间从早到晚") },
                                onClick = {
                                    newestFirst = false
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                }
                if (parentId != null) {
                    IconButton(onClick = { onCopy(parentId, if (parentId in rootIds) 1 else 2) }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            val rawItems = when (parentId) {
                null -> rootHistoryItems(conversations, rootIds)
                else -> ConversationFolderManager.childrenOf(parentId, conversations)
            }
            val items = remember(rawItems, newestFirst, nameAsc, parentId, sortByParentName) {
                if (sortByParentName) {
                    sortByParentModelName(rawItems, nameAsc)
                } else {
                    val sort = if (newestFirst) HistoryTimeSort.NewestFirst else HistoryTimeSort.OldestFirst
                    sortHistoryItems(
                        items = rawItems,
                        sort = sort,
                        keepFoldersLast = parentId == null,
                    )
                }
            }

            val isCollaborationTaskLevel = parentId == Conversation.FOLDER_COLLABORATION_MODE_ID
            val isWarTaskLevel = parentId == Conversation.FOLDER_WAR_MODE_ID
            val isInsideTaskFolder = parentId != null && parentId !in rootIds

            val listState = rememberLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 520.dp),
            ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(items, key = { it.id }) { item ->
                    HistoryTreeRow(
                        conversation = item,
                        showTaskActions = (isCollaborationTaskLevel && item.type == Conversation.TYPE_COLLABORATION_TASK) ||
                            (isWarTaskLevel && item.type == Conversation.TYPE_WAR_TASK),
                        onClick = {
                            when {
                                item.type == Conversation.TYPE_FOLDER ->
                                    actions.openHistoryFolder(item.id)
                                item.type == Conversation.TYPE_COLLABORATION_TASK || isCollaborationTaskLevel ->
                                    actions.openHistoryFolder(item.id)
                                item.type == Conversation.TYPE_WAR_TASK || isWarTaskLevel ->
                                    onOpenWarResult(item.id)
                                item.type == Conversation.TYPE_WAR_RESULT ->
                                    onOpenWarResult(item.parentId ?: item.id)
                                item.type == Conversation.TYPE_COLLABORATION_MODEL ||
                                    item.type == Conversation.TYPE_WAR_MODEL ||
                                    isInsideTaskFolder ->
                                    onOpenModelView(item.id)
                                else -> {
                                    actions.loadConversation(item.id)
                                    onDismiss()
                                }
                            }
                        },
                        onCopy = {
                            val level = when (item.type) {
                                Conversation.TYPE_FOLDER -> 1
                                Conversation.TYPE_COLLABORATION_TASK,
                                Conversation.TYPE_WAR_TASK,
                                -> 2
                                Conversation.TYPE_COLLABORATION_MODEL,
                                Conversation.TYPE_WAR_MODEL,
                                Conversation.TYPE_WAR_RESULT,
                                -> 3
                                else -> 1
                            }
                            onCopy(item.id, level)
                        },
                        onDelete = { actions.deleteConversation(item.id) },
                        onRetry = {
                            if (item.type == Conversation.TYPE_WAR_TASK) {
                                // War retry not implemented yet — open result view
                                onOpenWarResult(item.id)
                            } else {
                                actions.retryCollaborationTask(item.id)
                            }
                        },
                    )
                }
            }
            VerticalScrollbarForList(
                listState = listState,
                modifier = Modifier.align(CenterEnd).fillMaxHeight(),
            )
            }
        }
    }
}

@Composable
private fun HistoryTreeRow(
    conversation: Conversation,
    showTaskActions: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
) {
    val meta = conversation.metadata()
    val status = meta.status?.let { runCatching { CollaborationModelStatus.valueOf(it) }.getOrNull() }
    val statusColor = when (status) {
        CollaborationModelStatus.RUNNING -> Color(0xFFF9A825)
        CollaborationModelStatus.COMPLETED -> Color(0xFF43A047)
        CollaborationModelStatus.FAILED -> Color(0xFFE53935)
        null -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .handCursor()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (statusColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (
                conversation.type == Conversation.TYPE_COLLABORATION_MODEL ||
                conversation.type == Conversation.TYPE_WAR_MODEL
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ModelPairChipsFromLabel(conversation.title, compact = true)
                    if (meta.isSummaryModel) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "总结",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (conversation.type == Conversation.TYPE_COLLABORATION_TASK && meta.collaborationQuestion != null) {
                Text(
                    text = meta.collaborationQuestion!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (conversation.type == Conversation.TYPE_WAR_TASK && meta.collaborationQuestion != null) {
                Text(
                    text = meta.collaborationQuestion!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showTaskActions) {
            IconButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = "重试任务", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(20.dp))
            }
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(20.dp))
        }
    }
}
