package com.inspiredandroid.kai.data

/**
 * 按层级遍历会话树并生成格式化复制文本。
 */
object ConversationCopyFormatter {

    fun copyLevel1(conversations: List<Conversation>, folderId: String): String {
        val folder = conversations.find { it.id == folderId }
        val folderTitle = folder?.title ?: folderId
        val children = ConversationFolderManager.childrenOf(folderId, conversations)
        return buildString {
            appendLine("【$folderTitle】")
            appendLine()
            children.forEach { child ->
                when (child.type) {
                    Conversation.TYPE_COLLABORATION_TASK -> append(copyLevel2(conversations, child))
                    Conversation.TYPE_CHAT,
                    Conversation.TYPE_INTERACTIVE,
                    -> append(copyChatConversation(child))
                    else -> Unit
                }
                appendLine()
            }
        }.trimEnd()
    }

    fun copyLevel2(conversations: List<Conversation>, task: Conversation): String {
        val question = task.metadata().collaborationQuestion ?: task.title
        val models = ConversationFolderManager.childrenOf(task.id, conversations)
        return buildString {
            appendLine("协作模式中的${task.title}，针对问题：$question")
            models.forEach { modelConv ->
                appendLine()
                append(copyLevel3(modelConv, question))
            }
        }.trimEnd()
    }

    fun copyLevel3(conversation: Conversation, question: String? = null): String {
        val title = conversation.title
        val turns = conversation.messages.filter {
            (it.role == "user" || it.role == "assistant") && it.content.isNotBlank()
        }
        return buildString {
            appendLine("【$title】")
            if (turns.isEmpty()) {
                val q = question ?: conversation.metadata().collaborationQuestion ?: firstUserMessage(conversation)
                val answer = lastAssistantMessage(conversation)
                if (q.isNotBlank()) appendLine("问题：$q")
                append("回答：")
                append(if (answer.isNullOrBlank()) "（无回复或调用失败）" else answer)
            } else {
                turns.forEach { message ->
                    when (message.role) {
                        "user" -> appendLine("用户：${message.content}")
                        else -> appendLine("模型：${message.content}")
                    }
                    appendLine()
                }
            }
        }.trimEnd()
    }

    fun copyChatConversation(conversation: Conversation): String {
        val question = firstUserMessage(conversation)
        val answer = lastAssistantMessage(conversation)
        return buildString {
            appendLine("【${conversation.title.ifEmpty { conversation.id }}】")
            if (question.isNotBlank()) appendLine("问题：$question")
            if (!answer.isNullOrBlank()) appendLine("回答：$answer")
        }.trimEnd()
    }

    private fun firstUserMessage(conversation: Conversation): String =
        conversation.messages.firstOrNull { it.role == "user" }?.content.orEmpty()

    private fun lastAssistantMessage(conversation: Conversation): String? =
        conversation.messages.lastOrNull { it.role == "assistant" && it.content.isNotBlank() }?.content
}
