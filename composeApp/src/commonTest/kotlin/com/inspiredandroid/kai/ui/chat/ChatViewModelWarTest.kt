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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelWarTest {

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

    private fun createViewModel(): ChatViewModel {
        val chat = Conversation(
            id = "chat-1",
            messages = emptyList(),
            createdAt = 1000L,
            updatedAt = 2000L,
            title = "Hello",
            type = Conversation.TYPE_CHAT,
        )
        fakeRepository.savedConversations.value = ConversationFolderManager.ensureHierarchy(listOf(chat))
        return ChatViewModel(
            fakeRepository,
            TaskScheduler(fakeRepository, enabled = false),
            unconfinedDispatcher,
        )
    }

    @Test
    fun `openHistoryTreeAtRoot resets folder parent`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            advanceUntilIdle()
            skipItems(1)
            viewModel.state.value.actions.openHistoryFolder(Conversation.FOLDER_COLLABORATION_MODE_ID)
            advanceUntilIdle()
            viewModel.state.value.actions.openHistoryTreeAtRoot()
            advanceUntilIdle()
            assertNull(expectMostRecentItem().historyTreeParentId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadConversation clears war and collaboration overlays`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            advanceUntilIdle()
            skipItems(1)
            viewModel.state.value.actions.openWarResultView("war-task-1")
            viewModel.state.value.actions.openCollaborationModelView("model-1")
            advanceUntilIdle()
            viewModel.state.value.actions.loadConversation("chat-1")
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull(state.warResultViewTaskId)
            assertNull(state.collaborationModelViewId)
            assertFalse(state.showHistoryTree)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openWarTaskModels closes result overlay and opens task folder`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            advanceUntilIdle()
            skipItems(1)
            viewModel.state.value.actions.openWarResultView("war-task-1")
            advanceUntilIdle()
            viewModel.state.value.actions.openWarTaskModels("war-task-1")
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull(state.warResultViewTaskId)
            assertEquals("war-task-1", state.historyTreeParentId)
            assertEquals(true, state.showHistoryTree)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
