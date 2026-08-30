package com.inspiredandroid.kai.ui.chat

import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.ui.chat.composables.rootHistoryItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatHistoryTreeSheetTest {

    @Test
    fun `rootHistoryItems shows single chats at root without single mode folder`() {
        val chat = Conversation(
            id = "chat-1",
            messages = emptyList(),
            createdAt = 1000L,
            updatedAt = 2000L,
            title = "Hello",
            type = Conversation.TYPE_CHAT,
            parentId = Conversation.FOLDER_SINGLE_MODE_ID,
        )
        val hierarchy = ConversationFolderManager.ensureHierarchy(listOf(chat))

        val items = rootHistoryItems(hierarchy)

        assertEquals("chat-1", items.first().id)
        assertTrue(items.none { it.id == Conversation.FOLDER_SINGLE_MODE_ID })
        assertTrue(items.any { it.id == Conversation.FOLDER_COLLABORATION_MODE_ID })
        assertTrue(items.any { it.id == Conversation.FOLDER_WAR_MODE_ID })
    }

    @Test
    fun `rootHistoryItems keeps single chats sorted by updatedAt`() {
        val older = Conversation(
            id = "older",
            messages = emptyList(),
            createdAt = 1L,
            updatedAt = 100L,
            title = "older",
            type = Conversation.TYPE_CHAT,
            parentId = Conversation.FOLDER_SINGLE_MODE_ID,
        )
        val newer = Conversation(
            id = "newer",
            messages = emptyList(),
            createdAt = 2L,
            updatedAt = 200L,
            title = "newer",
            type = Conversation.TYPE_CHAT,
            parentId = Conversation.FOLDER_SINGLE_MODE_ID,
        )
        val hierarchy = ConversationFolderManager.ensureHierarchy(listOf(older, newer))

        val items = rootHistoryItems(hierarchy)

        assertEquals(listOf("newer", "older"), items.take(2).map { it.id })
    }
}
