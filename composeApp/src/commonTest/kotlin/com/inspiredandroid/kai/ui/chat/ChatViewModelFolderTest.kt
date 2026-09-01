package com.inspiredandroid.kai.ui.chat

import app.cash.turbine.test
import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.data.TaskScheduler
import com.inspiredandroid.kai.testutil.FakeDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelFolderTest {

    private val testDispatcher = StandardTestDispatcher()
    private val unconfinedDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeDataRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDataRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `folder conversations appear in folderConversations but not savedConversations summaries`() = runTest {
        val chat = Conversation(
            id = "chat-1",
            messages = emptyList(),
            createdAt = 1000L,
            updatedAt = 2000L,
            title = "",
            type = Conversation.TYPE_CHAT,
        )
        val hierarchy = ConversationFolderManager.ensureHierarchy(listOf(chat))
        fakeRepository.savedConversations.value = hierarchy

        val viewModel = ChatViewModel(
            fakeRepository,
            TaskScheduler(fakeRepository, enabled = false),
            unconfinedDispatcher,
        )

        viewModel.state.test {
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(hierarchy.size, state.folderConversations.size)
            assertTrue(state.folderConversations.any { it.id == Conversation.FOLDER_SINGLE_MODE_ID })
            assertTrue(state.folderConversations.any { it.id == Conversation.FOLDER_COLLABORATION_MODE_ID })
            assertTrue(state.folderConversations.any { it.id == Conversation.FOLDER_WAR_MODE_ID })
            assertEquals(1, state.savedConversations.size)
            assertEquals("chat-1", state.savedConversations.single().id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
