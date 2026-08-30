package com.inspiredandroid.kai.data

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * 确保聊天记录根文件夹存在，并将无父级的普通会话归入「单一模式」文件夹。
 */
object ConversationFolderManager {

    fun ensureHierarchy(conversations: List<Conversation>): List<Conversation> {
        val now = Clock.System.now().toEpochMilliseconds()
        val result = conversations.toMutableList()
        val byId = result.associateBy { it.id }.toMutableMap()

        fun ensureFolder(id: String, title: String): Conversation {
            val existing = byId[id]
            if (existing != null) return existing
            val folder = Conversation(
                id = id,
                messages = emptyList(),
                createdAt = now,
                updatedAt = now,
                title = title,
                type = Conversation.TYPE_FOLDER,
                parentId = null,
            )
            result.add(folder)
            byId[id] = folder
            return folder
        }

        ensureFolder(Conversation.FOLDER_SINGLE_MODE_ID, Conversation.FOLDER_SINGLE_MODE_TITLE)
        ensureFolder(Conversation.FOLDER_COLLABORATION_MODE_ID, Conversation.FOLDER_COLLABORATION_MODE_TITLE)
        ensureFolder(Conversation.FOLDER_WAR_MODE_ID, Conversation.FOLDER_WAR_MODE_TITLE)

        val orphans = result.filter { conv ->
            conv.parentId == null &&
                conv.type != Conversation.TYPE_FOLDER &&
                conv.type != Conversation.TYPE_HEARTBEAT
        }
        for (orphan in orphans) {
            val normalized = normalizeConversationType(orphan)
            val parentId = when (normalized.type) {
                Conversation.TYPE_COLLABORATION_TASK,
                Conversation.TYPE_COLLABORATION_MODEL,
                -> Conversation.FOLDER_COLLABORATION_MODE_ID
                Conversation.TYPE_WAR_TASK,
                Conversation.TYPE_WAR_MODEL,
                Conversation.TYPE_WAR_RESULT,
                -> Conversation.FOLDER_WAR_MODE_ID
                else -> Conversation.FOLDER_SINGLE_MODE_ID
            }
            val idx = result.indexOfFirst { it.id == orphan.id }
            if (idx >= 0) {
                result[idx] = normalized.copy(parentId = parentId)
            }
        }

        return result.map { normalizeConversationType(it) }
    }

    /**
     * Repairs legacy rows whose [Conversation.type] stayed `chat` after being moved under
     * collaboration/war folders — without this, history clicks fall through to
     * [loadConversation] and show a blank screen.
     */
    internal fun normalizeConversationType(conv: Conversation): Conversation {
        if (conv.type == Conversation.TYPE_FOLDER || conv.type == Conversation.TYPE_HEARTBEAT) {
            return conv
        }
        if (conv.type != Conversation.TYPE_CHAT && conv.type != Conversation.TYPE_INTERACTIVE) {
            return conv
        }

        val meta = conv.metadata()
        return when (conv.parentId) {
            Conversation.FOLDER_COLLABORATION_MODE_ID -> conv.copy(type = Conversation.TYPE_COLLABORATION_TASK)
            Conversation.FOLDER_WAR_MODE_ID -> conv.copy(type = Conversation.TYPE_WAR_TASK)
            null -> conv
            else -> when {
                conv.title == Conversation.WAR_RESULT_TITLE ->
                    conv.copy(type = Conversation.TYPE_WAR_RESULT)
                meta.taskMode == "war" ->
                    conv.copy(type = Conversation.TYPE_WAR_MODEL)
                meta.instanceId != null || meta.collaborationQuestion != null ->
                    conv.copy(type = Conversation.TYPE_COLLABORATION_MODEL)
                else -> conv
            }
        }
    }

    fun childrenOf(parentId: String, conversations: List<Conversation>): List<Conversation> =
        conversations.filter { it.parentId == parentId }.sortedByDescending { it.updatedAt }

    fun nextTaskFolderTitle(conversations: List<Conversation>, nowMs: Long = Clock.System.now().toEpochMilliseconds()): String =
        nextTaskFolderTitleForType(conversations, Conversation.TYPE_COLLABORATION_TASK, nowMs)

    fun nextWarTaskFolderTitle(conversations: List<Conversation>, nowMs: Long = Clock.System.now().toEpochMilliseconds()): String =
        nextTaskFolderTitleForType(conversations, Conversation.TYPE_WAR_TASK, nowMs)

    private fun nextTaskFolderTitleForType(
        conversations: List<Conversation>,
        taskType: String,
        nowMs: Long,
    ): String {
        val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val datePrefix = "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
        val existing = conversations.count { conv ->
            conv.type == taskType &&
                conv.title.startsWith(datePrefix)
        }
        return "$datePrefix-任务${existing + 1}"
    }

    fun modelFolderTitle(serviceName: String, modelLabel: String): String {
        val serviceSlug = slug(serviceName)
        val modelSlug = slug(modelLabel)
        return if (modelSlug.startsWith(serviceSlug)) modelSlug else "$serviceSlug-$modelSlug"
    }

    private fun slug(raw: String): String {
        val lowered = raw.lowercase().trim()
        val replaced = lowered.replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return replaced.ifEmpty { "model" }
    }
}
