package com.inspiredandroid.kai.data

import androidx.compose.runtime.Immutable
import com.inspiredandroid.kai.TerminalLine
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * A single file attachment on a chat message. Used both in-memory on `History` and
 * persisted on `Conversation.Message`. Binary content is base64-encoded.
 */
@Immutable
@Serializable
data class Attachment(
    val data: String,
    val mimeType: String,
    val fileName: String? = null,
)

@Serializable
data class Conversation(
    val id: String,
    val messages: List<Message>,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String = "",
    val type: String = TYPE_CHAT,
    val shellTranscript: List<TerminalLine> = emptyList(),
    val parentId: String? = null,
    val metadataJson: String = "",
) {
    companion object {
        const val TYPE_CHAT = "chat"
        const val TYPE_HEARTBEAT = "heartbeat"
        const val TYPE_INTERACTIVE = "interactive"
        const val TYPE_FOLDER = "folder"
        const val TYPE_COLLABORATION_TASK = "collaboration_task"
        const val TYPE_COLLABORATION_MODEL = "collaboration_model"

        const val FOLDER_SINGLE_MODE_ID = "folder-single-mode"
        const val FOLDER_COLLABORATION_MODE_ID = "folder-collaboration-mode"
        const val FOLDER_SINGLE_MODE_TITLE = "单一模式"
        const val FOLDER_COLLABORATION_MODE_TITLE = "协作模式"
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Message(
        val id: String,
        val role: String,
        val content: String,
        val attachments: List<Attachment> = emptyList(),
        val uiSubmission: UiSubmission? = null,
        val isThinking: Boolean = false,
        // Most messages have no reasoning trace; skip the null to keep the persisted blob lean.
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val reasoningContent: String? = null,
        // Legacy single-file fields — retained for reading old persisted conversations.
        // New code writes only `attachments`; these remain null on newly saved messages.
        val mimeType: String? = null,
        val data: String? = null,
        val fileName: String? = null,
    )
}

/**
 * Snapshot of a kai-ui form the user submitted. Attached to the resulting User message so
 * the bubble renders as a frozen form (with the values the user picked) instead of the
 * cryptic "Responded with: ..." text. `sourceContent` holds the assistant message body that
 * originated the form — it's re-parsed at render time.
 */
@Immutable
@Serializable
data class UiSubmission(
    val sourceContent: String,
    val values: Map<String, String> = emptyMap(),
    val pressedEvent: String? = null,
)

@Serializable
data class ConversationsData(
    val version: Int = 3,
    val conversations: List<Conversation>,
)

/** 协作会话元数据，序列化在 [Conversation.metadataJson]。 */
@Serializable
data class ConversationMetadata(
    val collaborationQuestion: String? = null,
    val instanceId: String? = null,
    val modelId: String? = null,
    val status: String? = null,
    val userScore: Double? = null,
    val minScoreThreshold: Double? = null,
    val maxWaitSeconds: Int? = null,
    val retryCount: Int? = null,
    val notifyOnFailure: Boolean? = null,
    val notifyOnComplete: Boolean? = null,
)

enum class CollaborationModelStatus {
    RUNNING,
    COMPLETED,
    FAILED,
}

fun Conversation.metadata(): ConversationMetadata = try {
    if (metadataJson.isBlank()) ConversationMetadata()
    else SharedJson.decodeFromString<ConversationMetadata>(metadataJson)
} catch (_: Exception) {
    ConversationMetadata()
}

fun Conversation.withMetadata(metadata: ConversationMetadata): Conversation =
    copy(metadataJson = SharedJson.encodeToString(metadata))

fun ConversationMetadata.encode(): String = SharedJson.encodeToString(this)
