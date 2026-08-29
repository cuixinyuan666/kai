@file:OptIn(ExperimentalMaterial3Api::class)

package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.data.metadata
import com.inspiredandroid.kai.ui.chat.ChatActions
import com.inspiredandroid.kai.ui.handCursor
import kotlinx.collections.immutable.ImmutableList

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
        var sortReverse by remember(parentId) { mutableStateOf(false) }

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
                IconButton(onClick = { sortReverse = !sortReverse }) {
                    Icon(Icons.Default.Sort, contentDescription = if (sortReverse) "字母正序" else "字母反序")
                }
                if (parentId != null) {
                    IconButton(onClick = { onCopy(parentId, if (parentId in rootIds) 1 else 2) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                    }
                }
            }

            val rawItems = when (parentId) {
                null -> conversations.filter { it.id in rootIds }
                else -> ConversationFolderManager.childrenOf(parentId, conversations)
            }
            val items = remember(rawItems, sortReverse) {
                val sorted = rawItems.sortedBy { it.title.lowercase() }
                if (sortReverse) sorted.reversed() else sorted
            }

            val isCollaborationTaskLevel = parentId == Conversation.FOLDER_COLLABORATION_MODE_ID
            val isWarTaskLevel = parentId == Conversation.FOLDER_WAR_MODE_ID

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items, key = { it.id }) { item ->
                    HistoryTreeRow(
                        conversation = item,
                        showTaskActions = (isCollaborationTaskLevel && item.type == Conversation.TYPE_COLLABORATION_TASK) ||
                            (isWarTaskLevel && item.type == Conversation.TYPE_WAR_TASK),
                        onClick = {
                            when (item.type) {
                                Conversation.TYPE_FOLDER -> actions.openHistoryFolder(item.id)
                                Conversation.TYPE_COLLABORATION_TASK -> actions.openHistoryFolder(item.id)
                                Conversation.TYPE_WAR_TASK -> onOpenWarResult(item.id)
                                Conversation.TYPE_WAR_RESULT -> onOpenWarResult(item.parentId ?: item.id)
                                Conversation.TYPE_COLLABORATION_MODEL,
                                Conversation.TYPE_WAR_MODEL,
                                -> onOpenModelView(item.id)
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
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
