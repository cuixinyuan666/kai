package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.ConversationCopyFormatter
import com.inspiredandroid.kai.data.metadata
import com.inspiredandroid.kai.ui.chat.ChatActions
import com.inspiredandroid.kai.ui.rememberCopyToClipboard

/**
 * 协作模型任务界面（仿微信聊天气泡）。
 */
@Composable
internal fun CollaborationModelChatView(
    conversation: Conversation,
    actions: ChatActions,
    isLoading: Boolean,
    onBack: () -> Unit,
) {
    val copyToClipboard = rememberCopyToClipboard()
    val question = conversation.metadata().collaborationQuestion
        ?: conversation.messages.firstOrNull { it.role == "user" }?.content.orEmpty()
    val answer = conversation.messages.lastOrNull { it.role == "assistant" }?.content.orEmpty()
    var score by remember(conversation.metadata().userScore) {
        mutableFloatStateOf((conversation.metadata().userScore?.toFloat() ?: 50f))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(conversation.title, style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WeChatBubble(isUser = true, text = question)
            if (answer.isNotBlank()) {
                WeChatBubble(isUser = false, text = answer)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { copyToClipboard(ConversationCopyFormatter.copyLevel3(conversation)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text("复制")
                }
                Button(
                    onClick = { actions.retryCollaborationModel(conversation.id) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("重试")
                }
            }
            Text("打分", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = score,
                    onValueChange = { score = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                )
                Text("${score.toInt()}")
            }
            Button(
                onClick = { actions.setCollaborationModelScore(conversation.id, score.toDouble()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存评分")
            }
        }
    }
}

@Composable
private fun WeChatBubble(isUser: Boolean, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
