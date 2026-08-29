package com.inspiredandroid.kai.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConversationFolderManagerTest {

    @Test
    fun `ensureHierarchy creates root folders and assigns orphan conversations`() {
        val chat = Conversation(
            id = "chat-1",
            messages = emptyList(),
            createdAt = 1000L,
            updatedAt = 2000L,
            title = "Hello",
            type = Conversation.TYPE_CHAT,
        )
        val task = Conversation(
            id = "task-1",
            messages = emptyList(),
            createdAt = 3000L,
            updatedAt = 4000L,
            title = "2026-08-29-任务1",
            type = Conversation.TYPE_COLLABORATION_TASK,
        )

        val result = ConversationFolderManager.ensureHierarchy(listOf(chat, task))

        assertNotNull(result.find { it.id == Conversation.FOLDER_SINGLE_MODE_ID })
        assertNotNull(result.find { it.id == Conversation.FOLDER_COLLABORATION_MODE_ID })
        assertEquals(Conversation.FOLDER_SINGLE_MODE_ID, result.find { it.id == "chat-1" }?.parentId)
        assertEquals(Conversation.FOLDER_COLLABORATION_MODE_ID, result.find { it.id == "task-1" }?.parentId)
    }

    @Test
    fun `ensureHierarchy assigns war tasks to war mode folder`() {
        val warTask = Conversation(
            id = "war-task-1",
            messages = emptyList(),
            createdAt = 1000L,
            updatedAt = 2000L,
            title = "2026-08-29-任务1",
            type = Conversation.TYPE_WAR_TASK,
        )
        val result = ConversationFolderManager.ensureHierarchy(listOf(warTask))
        assertNotNull(result.find { it.id == Conversation.FOLDER_WAR_MODE_ID })
        assertEquals(Conversation.FOLDER_WAR_MODE_ID, result.find { it.id == "war-task-1" }?.parentId)
    }

    @Test
    fun `childrenOf returns direct children sorted by updatedAt`() {
        val parentId = Conversation.FOLDER_COLLABORATION_MODE_ID
        val conversations = ConversationFolderManager.ensureHierarchy(
            listOf(
                Conversation(
                    id = "older",
                    messages = emptyList(),
                    createdAt = 1L,
                    updatedAt = 100L,
                    title = "older",
                    type = Conversation.TYPE_COLLABORATION_TASK,
                ),
                Conversation(
                    id = "newer",
                    messages = emptyList(),
                    createdAt = 2L,
                    updatedAt = 200L,
                    title = "newer",
                    type = Conversation.TYPE_COLLABORATION_TASK,
                ),
            ),
        )

        val children = ConversationFolderManager.childrenOf(parentId, conversations)
        assertEquals(listOf("newer", "older"), children.map { it.id })
    }

    @Test
    fun `ensureHierarchy is stable when roots already exist`() {
        val withRoots = ConversationFolderManager.ensureHierarchy(emptyList())
        val again = ConversationFolderManager.ensureHierarchy(withRoots)
        assertEquals(3, again.count { it.type == Conversation.TYPE_FOLDER })
        assertTrue(again.any { it.id == Conversation.FOLDER_SINGLE_MODE_ID })
        assertTrue(again.any { it.id == Conversation.FOLDER_COLLABORATION_MODE_ID })
        assertTrue(again.any { it.id == Conversation.FOLDER_WAR_MODE_ID })
    }
}
