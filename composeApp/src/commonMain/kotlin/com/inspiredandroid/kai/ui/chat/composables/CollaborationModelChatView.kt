package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.ConversationCopyFormatter
import com.inspiredandroid.kai.data.metadata
import com.inspiredandroid.kai.ui.chat.ChatActions
import com.inspiredandroid.kai.ui.components.ModelPairChipsFromLabel
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForScroll
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
    hasPrevModel: Boolean,
    hasNextModel: Boolean,
    onPrevModel: () -> Unit,
    onNextModel: () -> Unit,
    modelBenchmarks: Map<String, Double> = emptyMap(),
    serviceIdForModel: String? = null,
    highlightMessageId: String? = null,
) {
    val copyToClipboard = rememberCopyToClipboard()
    val visibleMessages = conversation.messages.filter {
        (it.role == "user" || it.role == "assistant") && it.content.isNotBlank()
    }
    val questionFallback = conversation.metadata().collaborationQuestion.orEmpty()
    val meta = conversation.metadata()
    val isSummaryModel = meta.isSummaryModel
    val scrollState = rememberScrollState()
    var highlightY by remember(highlightMessageId, conversation.id) { mutableIntStateOf(0) }
    LaunchedEffect(highlightY, highlightMessageId) {
        if (highlightMessageId != null && highlightY > 0) {
            scrollState.animateScrollTo(highlightY)
        }
    }
    val benchmarkKey = if (serviceIdForModel != null && meta.modelId != null) {
        "$serviceIdForModel::${meta.modelId}"
    } else {
        null
    }
    val configuredScore = meta.userScore ?: benchmarkKey?.let { modelBenchmarks[it] }
    var score by remember(meta.userScore, benchmarkKey, modelBenchmarks) {
        mutableFloatStateOf((configuredScore?.toFloat() ?: 0f))
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (hasPrevModel) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onPrevModel) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上一个模型",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                ModelPairChipsFromLabel(
                    conversation.title,
                    modifier = Modifier.weight(1f),
                )
                if (isSummaryModel) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "总结",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (visibleMessages.isEmpty()) {
                    if (questionFallback.isNotBlank()) {
                        WeChatBubble(isUser = true, text = questionFallback)
                    }
                } else {
                    visibleMessages.forEach { message ->
                        val highlighted = highlightMessageId != null && message.id == highlightMessageId
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                if (highlighted) {
                                    highlightY = coords.positionInParent().y.toInt()
                                }
                            },
                        ) {
                            WeChatBubble(
                                isUser = message.role == "user",
                                text = message.content,
                                isSummary = isSummaryModel && message.role == "assistant",
                                highlighted = highlighted,
                            )
                        }
                    }
                }
            }
            VerticalScrollbarForScroll(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { actions.retryCollaborationModel(conversation.id) },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text("重试")
                    }
                    Button(
                        onClick = { copyToClipboard(ConversationCopyFormatter.copyLevel3(conversation)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Text("复制")
                    }
                }
                Text("打分", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = score,
                        onValueChange = { newScore ->
                            score = newScore
                            actions.setCollaborationModelScore(conversation.id, newScore.toDouble())
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${score.toInt()}", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        if (hasNextModel) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onNextModel) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下一个模型",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeChatBubble(
    isUser: Boolean,
    text: String,
    isSummary: Boolean = false,
    highlighted: Boolean = false,
) {
    val bubbleColor = when {
        isSummary -> MaterialTheme.colorScheme.tertiaryContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = when {
        isSummary -> MaterialTheme.colorScheme.onTertiaryContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val textStyle = if (isSummary) {
        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.85f)) {
            if (isSummary) {
                Text(
                    text = "总结模型",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                text = text,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bubbleColor)
                    .then(
                        if (highlighted) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        } else {
                            Modifier
                        },
                    )
                    .padding(12.dp),
                style = textStyle,
                color = textColor,
            )
        }
    }
}
