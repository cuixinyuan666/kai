package com.inspiredandroid.kai.ui.chat

import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.ui.chat.composables.HistoryTimeSort
import com.inspiredandroid.kai.ui.chat.composables.rootHistoryItems
import com.inspiredandroid.kai.ui.chat.composables.sortByParentModelName
import com.inspiredandroid.kai.ui.chat.composables.sortHistoryItems
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

    @Test
    fun `sortHistoryItems oldest first keeps folders last at root`() {
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
        val oldestFirst = sortHistoryItems(items, HistoryTimeSort.OldestFirst, keepFoldersLast = true)

        assertEquals("older", oldestFirst.first().id)
        assertTrue(oldestFirst.takeLast(2).all { it.type == Conversation.TYPE_FOLDER })
    }

    @Test
    fun `sortByParentModelName pins war result then sorts parent names`() {
        val result = Conversation(
            id = "result",
            messages = emptyList(),
            createdAt = 1L,
            updatedAt = 300L,
            title = "任务结果",
            type = Conversation.TYPE_WAR_RESULT,
        )
        val zeta = Conversation(
            id = "z",
            messages = emptyList(),
            createdAt = 2L,
            updatedAt = 200L,
            title = "Zeta / z-1",
            type = Conversation.TYPE_WAR_MODEL,
        )
        val agnes = Conversation(
            id = "a",
            messages = emptyList(),
            createdAt = 3L,
            updatedAt = 100L,
            title = "Agnes Al / agnes-2.0-flash",
            type = Conversation.TYPE_WAR_MODEL,
        )
        val items = listOf(zeta, result, agnes)
        assertEquals(listOf("result", "a", "z"), sortByParentModelName(items, ascending = true).map { it.id })
        assertEquals(listOf("result", "z", "a"), sortByParentModelName(items, ascending = false).map { it.id })
    }
}
