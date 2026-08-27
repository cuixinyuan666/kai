package com.inspiredandroid.kai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inspiredandroid.kai.data.CollaborationModelStatus
import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.metadata
import com.inspiredandroid.kai.data.ConversationCopyFormatter
import com.inspiredandroid.kai.data.ConversationFolderManager
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.FreeMode
import com.inspiredandroid.kai.data.Service
import com.inspiredandroid.kai.data.ServiceEntry
import com.inspiredandroid.kai.data.TaskScheduler
import com.inspiredandroid.kai.data.UiSubmission
import com.inspiredandroid.kai.data.collaboration.CollaborationEvent
import com.inspiredandroid.kai.data.collaboration.CollaborationListener
import com.inspiredandroid.kai.data.collaboration.CollaborationTaskRunner
import com.inspiredandroid.kai.data.ModelBenchmark
import com.inspiredandroid.kai.data.collaboration.CollaborationWizardParams
import com.inspiredandroid.kai.data.collaboration.ModelRef
import com.inspiredandroid.kai.data.collaboration.ChatMode
import com.inspiredandroid.kai.getBackgroundDispatcher
import com.inspiredandroid.kai.network.UiError
import com.inspiredandroid.kai.network.shouldShowFreeProviderSuggestions
import com.inspiredandroid.kai.network.toUiError
import com.inspiredandroid.kai.tools.AppPermission
import com.inspiredandroid.kai.tools.PermissionController
import com.inspiredandroid.kai.tools.isLocalNetworkUrl
import com.inspiredandroid.kai.ui.markdown.KaiUiBlock
import com.inspiredandroid.kai.ui.markdown.KaiUiError
import com.inspiredandroid.kai.ui.markdown.parseMarkdown
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.isDirectory
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.conversation_untitled
import kai.composeapp.generated.resources.error_local_network_permission
import kai.composeapp.generated.resources.error_unsupported_file_type
import kai.composeapp.generated.resources.litert_no_model_warning
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class ChatViewModel(
    private val dataRepository: DataRepository,
    private val taskScheduler: TaskScheduler,
    private val backgroundDispatcher: CoroutineContext = getBackgroundDispatcher(),
    private val localNetworkPermissionController: PermissionController = PermissionController(AppPermission.LOCAL_NETWORK),
) : ViewModel() {

    private val actions = ChatActions(
        ask = ::ask,
        retry = ::retry,
        toggleSpeechOutput = ::toggleSpeechOutput,
        clearHistory = ::clearHistory,
        setIsSpeaking = ::setIsSpeaking,
        addFile = ::addFile,
        removeFile = ::removeFile,
        startNewChat = ::startNewChat,
        regenerate = ::regenerate,
        cancel = ::cancel,
        selectService = ::selectService,
        selectModel = ::selectModel,
        loadConversation = ::loadConversation,
        deleteConversation = ::deleteConversation,
        clearUnreadHeartbeat = ::clearUnreadHeartbeat,
        clearSnackbar = ::clearSnackbar,
        undoDeleteConversation = ::undoDeleteConversation,
        submitUiCallback = ::submitUiCallback,
        resubmit = ::resubmit,
        enterInteractiveMode = ::enterInteractiveMode,
        exitInteractiveMode = ::exitInteractiveMode,
        goBackInteractiveMode = ::goBackInteractiveMode,
        sendSmsDraft = ::sendSmsDraft,
        discardSmsDraft = ::discardSmsDraft,
        openCollaborationWizard = ::openCollaborationWizard,
        dismissCollaborationWizard = ::dismissCollaborationWizard,
        startCollaborationTask = ::startCollaborationTask,
        stopCollaboration = ::stopCollaboration,
        clearCollaborationNotification = ::clearCollaborationNotification,
        openCollaborationModelView = ::openCollaborationModelView,
        closeCollaborationModelView = ::closeCollaborationModelView,
        openHistoryFolder = ::openHistoryFolder,
        closeHistoryFolder = ::closeHistoryFolder,
        closeHistoryTreeSheet = ::closeHistoryTreeSheet,
        copyConversationBranch = ::copyConversationBranch,
        clearPendingCopyText = ::clearPendingCopyText,
        retryCollaborationModel = ::retryCollaborationModel,
        retryCollaborationTask = ::retryCollaborationTask,
        setCollaborationModelScore = ::setCollaborationModelScore,
        navigateCollaborationModel = ::navigateCollaborationModel,
        optimizePrompt = ::optimizePrompt,
        clearPendingPromptText = ::clearPendingPromptText,
        resendUserMessage = ::resendUserMessage,
    )
    private val freeModeNames: Map<FreeMode, String> = FreeMode.entries.associateWith { "Free ${it.modelId.replaceFirstChar { c -> c.uppercase() }}" }
    private var currentJob: Job? = null
    private var pendingConversationDeleteJob: Job? = null
    private var collaborationTaskRunner: CollaborationTaskRunner? = null
    private val _state = MutableStateFlow(
        ChatUiState(
            actions = actions,
            showPrivacyInfo = dataRepository.isUsingSharedKey(),
            chatMode = dataRepository.getChatMode(),
            collaborationConfig = dataRepository.getCollaborationConfig(),
        ),
    )

    init {
        updateAvailableServices()

        // Keep restoreCurrentConversation off the main thread; see issue #197 (large persisted
        // tool outputs caused ANRs when JSON-decoded synchronously during VM construction).
        // ChatScreen gates the interactive-mode branch on !isRestoring to avoid a flash.
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.loadConversations()
            dataRepository.restoreCurrentConversation()
            presetInteractiveModeForCurrentConversation()
            _state.update { it.copy(isRestoring = false) }
        }

        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.connectEnabledMcpServers()
        }
        viewModelScope.launch {
            dataRepository.fallbackStatus.collect { status ->
                _state.update { it.copy(fallbackStatus = status) }
            }
        }
        taskScheduler.isLoadingCheck = { _state.value.isLoading }
        taskScheduler.start()

        viewModelScope.launch {
            dataRepository.smsDrafts.collect { drafts ->
                _state.update { it.copy(smsDrafts = drafts.toImmutableList()) }
            }
        }

        viewModelScope.launch {
            dataRepository.openHeartbeatRequested
                .filter { it }
                .collect {
                    val heartbeatId = dataRepository.savedConversations.value
                        .firstOrNull { it.type == Conversation.TYPE_HEARTBEAT }?.id
                    if (heartbeatId != null) {
                        loadConversation(heartbeatId)
                        clearUnreadHeartbeat()
                    }
                    dataRepository.consumeOpenHeartbeatRequest()
                }
        }

        viewModelScope.launch {
            dataRepository.openAssistRequested
                .filter { it }
                .collect {
                    startNewChat()
                    dataRepository.consumeOpenAssistRequest()
                }
        }

        // 模型基准测试分数实时同步到聊天状态（模型选择器显示颜色分数）
        viewModelScope.launch {
            dataRepository.modelBenchmarks.collect { benchmarks ->
                _state.update {
                    it.copy(
                        modelBenchmarks = benchmarks
                            .associate { bm -> bm.modelKey to bm.totalScore }
                            .toImmutableMap(),
                    )
                }
            }
        }
    }

    val state = combine(
        _state,
        dataRepository.chatHistory,
        dataRepository.savedConversations,
        dataRepository.currentConversationId,
        dataRepository.hasUnreadHeartbeat,
    ) { state, history, conversations, conversationId, hasUnreadHeartbeat ->
        val summaries = conversations
            .filter {
                it.type != Conversation.TYPE_HEARTBEAT &&
                    it.type != Conversation.TYPE_FOLDER &&
                    it.type != Conversation.TYPE_COLLABORATION_TASK &&
                    it.type != Conversation.TYPE_COLLABORATION_MODEL
            }
            .sortedByDescending { it.updatedAt }
            .map {
                val isHeartbeat = it.type == Conversation.TYPE_HEARTBEAT
                val isInteractive = it.type == Conversation.TYPE_INTERACTIVE
                val meta = it.metadata()
                ConversationSummary(
                    id = it.id,
                    title = if (isHeartbeat) "" else it.title.ifEmpty { getString(Res.string.conversation_untitled) },
                    updatedAt = it.updatedAt,
                    isHeartbeat = isHeartbeat,
                    isInteractive = isInteractive,
                    type = it.type,
                    parentId = it.parentId,
                    collaborationStatus = meta.status?.let { name ->
                        runCatching { CollaborationModelStatus.valueOf(name) }.getOrNull()
                    },
                    userScore = meta.userScore,
                    collaborationQuestion = meta.collaborationQuestion,
                )
            }
        state.copy(
            history = history.toImmutableList(),
            supportedFileExtensions = dataRepository.supportedFileExtensions().toImmutableList(),
            savedConversations = summaries.toImmutableList(),
            folderConversations = conversations.toImmutableList(),
            currentConversationId = conversationId,
            hasUnreadHeartbeat = hasUnreadHeartbeat,
            installedSkills = dataRepository.getInstalledSkills().toImmutableList(),
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _state.value,
    )

    private fun submitUiCallback(event: String, data: Map<String, String>) {
        val message = if (data.isNotEmpty()) {
            val formattedData = data.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            "Responded with: $formattedData"
        } else {
            "Pressed: $event"
        }
        val lastAssistant = dataRepository.chatHistory.value.lastRenderedAssistant()
        val submission = lastAssistant?.let {
            UiSubmission(sourceContent = it.content, values = data, pressedEvent = event)
        }
        askInternal(message, submission)
    }

    private fun ask(question: String?) {
        askInternal(question, null)
    }

    /** 用户消息“重新发送”：截断到该消息之前，再以原文重新提问。 */
    private fun resendUserMessage(messageId: String, text: String) {
        dataRepository.truncateFrom(messageId)
        askInternal(text, null)
    }

    private fun askInternal(question: String?, uiSubmission: UiSubmission?) {
        // Prevent concurrent requests
        if (_state.value.isLoading) return

        // Capture files before launching coroutine to avoid race with files being cleared
        val files = _state.value.files

        val (strippedQuestion, activeSkillId) = parseSkillInvocation(question)

        currentJob = viewModelScope.launch(backgroundDispatcher) {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    showFreeProviderSuggestions = false,
                    files = persistentListOf(),
                )
            }
            // Android 17+ blocks LAN traffic without the local network permission — without
            // asking first, requests to self-hosted servers silently never leave the device.
            if (!ensureLocalNetworkPermission()) {
                _state.update {
                    it.copy(
                        error = UiError.Resource(Res.string.error_local_network_permission),
                        showFreeProviderSuggestions = false,
                        isLoading = false,
                    )
                }
                return@launch
            }
            try {
                dataRepository.ask(strippedQuestion, files, uiSubmission, activeSkillId)

                // Auto-retry in interactive mode if the response has no valid kai-ui
                if (_state.value.isInteractiveMode) {
                    retryIfNoValidKaiUi()
                }

                _state.update {
                    it.copy(isLoading = false)
                }
            } catch (exception: Exception) {
                // CancellationException must be re-thrown to properly propagate coroutine cancellation
                if (exception is CancellationException) throw exception

                val showUpsell = shouldShowFreeProviderSuggestions(
                    noConfiguredServices = dataRepository.getConfiguredServiceInstances().isEmpty(),
                    exception = exception,
                )
                _state.update {
                    it.copy(
                        error = exception.toUiError(),
                        showFreeProviderSuggestions = showUpsell,
                        isLoading = false,
                    )
                }
            }
        }
    }

    /**
     * True unless the active service points at a local network host and the user
     * denied the local network permission. Cheap no-op on non-Android platforms.
     */
    private suspend fun ensureLocalNetworkPermission(): Boolean {
        val instance = dataRepository.getConfiguredServiceInstances().firstOrNull() ?: return true
        val baseUrl = dataRepository.getInstanceBaseUrl(instance.instanceId, Service.fromId(instance.serviceId))
        if (!isLocalNetworkUrl(baseUrl)) return true
        return localNetworkPermissionController.requestPermission()
    }

    private suspend fun retryIfNoValidKaiUi(maxRetries: Int = 2) {
        repeat(maxRetries) {
            currentCoroutineContext().ensureActive()
            val lastAssistant = dataRepository.chatHistory.value.lastRenderedAssistant() ?: return

            val blocks = parseMarkdown(lastAssistant.content).blocks
            val hasValidUi = blocks.any { it is KaiUiBlock }
            if (hasValidUi) return

            // Build error feedback for the AI
            val errorBlock = blocks.filterIsInstance<KaiUiError>().firstOrNull()
            val errorDetail = if (errorBlock != null) {
                "JSON parse error in: ${errorBlock.rawJson.take(200)}"
            } else {
                "No kai-ui code fence found in your response."
            }
            val retryMessage = "[SYSTEM] Your previous response failed to render as interactive UI. $errorDetail " +
                "Remember: respond with ONLY a single ```kai-ui code fence containing valid JSON. No text outside the fence."

            dataRepository.ask(retryMessage, emptyList())
        }
    }

    private fun clearHistory() {
        dataRepository.clearHistory()
        _state.update {
            it.copy(error = null, showFreeProviderSuggestions = false)
        }
    }

    /**
     * If [text] begins with `/<skill-id>`, look up the skill among the currently-
     * installed-and-enabled skills and return its id alongside the verbatim user
     * text. The text is sent unchanged so the conversation visibly reflects what
     * the user typed; the skill's instructions in the system prompt tell the model
     * how to parse the args after the slash command. Falls through with null skill
     * id when no match — slash commands are opt-in.
     */
    private fun parseSkillInvocation(text: String?): Pair<String?, String?> {
        if (text == null) return null to null
        val trimmed = text.trimStart()
        if (!trimmed.startsWith('/')) return text to null
        val firstSpace = trimmed.indexOfFirst { it.isWhitespace() }
        val rawId = if (firstSpace < 0) trimmed.substring(1) else trimmed.substring(1, firstSpace)
        if (rawId.isEmpty()) return text to null
        val skill = dataRepository.getInstalledSkills().firstOrNull { it.id.equals(rawId, ignoreCase = true) }
            ?: return text to null
        return text to skill.id
    }

    private fun setIsSpeaking(isSpeaking: Boolean, contentId: String) {
        _state.update {
            it.copy(
                isSpeaking = isSpeaking,
                isSpeakingContentId = if (isSpeaking) {
                    contentId
                } else {
                    it.isSpeakingContentId
                },
            )
        }
    }

    private fun addFile(file: PlatformFile) {
        // 目录（文件夹）直接作为附件，不做扩展名校验；文件则校验受支持类型。
        if (!file.isDirectory()) {
            val ext = file.extension.lowercase()
            val supported = dataRepository.supportedFileExtensions()
            if (ext.isEmpty() || ext !in supported) {
                _state.update {
                    it.copy(snackbarMessage = Res.string.error_unsupported_file_type)
                }
                return
            }
        }
        _state.update {
            it.copy(files = (it.files + file).toImmutableList())
        }
    }

    private fun removeFile(file: PlatformFile) {
        _state.update {
            it.copy(files = it.files.filterNot { f -> f == file }.toImmutableList())
        }
    }

    private fun clearSnackbar() {
        _state.update {
            it.copy(snackbarMessage = null)
        }
    }

    private fun retry() {
        ask(null)
    }

    private fun toggleSpeechOutput() {
        _state.update {
            it.copy(
                isSpeechOutputEnabled = !it.isSpeechOutputEnabled,
            )
        }
    }

    private fun cancel() {
        currentJob?.cancel()
        currentJob = null
        _state.update {
            it.copy(isLoading = false)
        }
    }

    private fun selectService(instanceId: String) {
        val freeMode = FREE_MODE_INSTANCE_IDS[instanceId]
        if (freeMode != null) {
            dataRepository.setFreeMode(freeMode)
            dataRepository.setFreeServicePrimary(true)
            updateAvailableServices()
            return
        }

        dataRepository.setFreeServicePrimary(false)
        val instances = dataRepository.getConfiguredServiceInstances()
        val currentIds = instances.map { it.instanceId }
        if (instanceId !in currentIds) return
        val reordered = listOf(instanceId) + currentIds.filter { it != instanceId }
        dataRepository.reorderConfiguredServices(reordered)
        updateAvailableServices()
    }

    /**
     * Switches to [instanceId] (making it the primary/active service) and selects
     * one of its model branches. Drives the two-level chat model dropdown where a
     * 总类 (service) exposes all of its 分支 (models) for direct selection.
     */
    private fun selectModel(instanceId: String, modelId: String) {
        val instances = dataRepository.getConfiguredServiceInstances()
        val instance = instances.firstOrNull { it.instanceId == instanceId } ?: return
        val service = Service.fromId(instance.serviceId)

        dataRepository.setFreeServicePrimary(false)
        val currentIds = instances.map { it.instanceId }
        val reordered = listOf(instanceId) + currentIds.filter { it != instanceId }
        dataRepository.reorderConfiguredServices(reordered)
        dataRepository.updateInstanceSelectedModel(instanceId, service, modelId)
        updateAvailableServices()
    }

    // region 协作模式
    private fun openCollaborationWizard() {
        _state.update { it.copy(showCollaborationWizard = true) }
    }

    private fun dismissCollaborationWizard() {
        _state.update { it.copy(showCollaborationWizard = false) }
    }

    private fun startCollaborationTask(params: CollaborationWizardParams) {
        if (params.question.isBlank()) return
        stopCollaboration()
        _state.update {
            it.copy(
                showCollaborationWizard = false,
                isCollaborating = true,
                collaborationEvents = emptyList(),
                collaborationSummary = null,
                collaborationNotification = null,
            )
        }
        val runner = CollaborationTaskRunner(
            repository = dataRepository,
            listener = object : CollaborationListener {
                override fun onEvent(event: CollaborationEvent) {
                    _state.update { s -> s.copy(collaborationEvents = s.collaborationEvents + event) }
                }

                override fun onNotify(title: String, body: String) {
                    _state.update { s -> s.copy(collaborationNotification = "$title：$body") }
                }

                override fun onModelStatusChanged(conversationId: String, status: CollaborationModelStatus) {
                    // State refreshes via savedConversations flow
                }

                override fun onTaskFinished(taskId: String, summary: String) {
                    _state.update { s ->
                        s.copy(
                            isCollaborating = false,
                            collaborationSummary = summary,
                            historyTreeParentId = Conversation.FOLDER_COLLABORATION_MODE_ID,
                        )
                    }
                }
            },
        )
        collaborationTaskRunner = runner
        viewModelScope.launch(backgroundDispatcher) {
            runner.runTask(params)
        }
    }

    private fun stopCollaboration() {
        collaborationTaskRunner?.cancel()
        collaborationTaskRunner = null
        _state.update { it.copy(isCollaborating = false) }
    }

    private fun clearCollaborationNotification() {
        _state.update { it.copy(collaborationNotification = null) }
    }

    private fun openCollaborationModelView(conversationId: String) {
        _state.update { it.copy(collaborationModelViewId = conversationId) }
    }

    private fun closeCollaborationModelView() {
        val modelId = _state.value.collaborationModelViewId
        val taskParentId = dataRepository.savedConversations.value
            .find { it.id == modelId }
            ?.parentId
        _state.update {
            it.copy(
                collaborationModelViewId = null,
                historyTreeParentId = taskParentId ?: it.historyTreeParentId,
                showHistoryTree = taskParentId != null,
            )
        }
    }

    private fun openHistoryFolder(folderId: String) {
        _state.update { it.copy(historyTreeParentId = folderId, showHistoryTree = true) }
    }

    private fun closeHistoryFolder() {
        val parentId = _state.value.historyTreeParentId ?: return
        val conversations = dataRepository.savedConversations.value
        val newParent = when (parentId) {
            Conversation.FOLDER_SINGLE_MODE_ID,
            Conversation.FOLDER_COLLABORATION_MODE_ID,
            -> null
            else -> conversations.find { it.id == parentId }?.parentId
        }
        _state.update { it.copy(historyTreeParentId = newParent) }
    }

    fun dismissHistoryTree() {
        _state.update { it.copy(showHistoryTree = false, historyTreeParentId = null) }
    }

    private fun closeHistoryTreeSheet() {
        dismissHistoryTree()
    }

    private fun copyConversationBranch(conversationId: String, level: Int) {
        val conversations = dataRepository.savedConversations.value
        val text = when (level) {
            1 -> ConversationCopyFormatter.copyLevel1(conversations, conversationId)
            2 -> conversations.find { it.id == conversationId }?.let {
                ConversationCopyFormatter.copyLevel2(conversations, it)
            } ?: ""
            3 -> conversations.find { it.id == conversationId }?.let {
                ConversationCopyFormatter.copyLevel3(it)
            } ?: ""
            else -> ""
        }
        if (text.isNotBlank()) {
            _state.update { it.copy(pendingCopyText = text) }
        }
    }

    private fun clearPendingCopyText() {
        _state.update { it.copy(pendingCopyText = null) }
    }

    private fun retryCollaborationModel(conversationId: String) {
        val conversation = dataRepository.savedConversations.value.find { it.id == conversationId }
        val timeoutSec = conversation?.metadata()?.maxWaitSeconds ?: 60
        val timeout = timeoutSec.toLong() * 1000L
        viewModelScope.launch(backgroundDispatcher) {
            _state.update { it.copy(isCollaborating = true) }
            dataRepository.retryCollaborationModel(conversationId, timeout)
            _state.update { it.copy(isCollaborating = false) }
        }
    }

    private fun setCollaborationModelScore(conversationId: String, score: Double) {
        dataRepository.setCollaborationModelUserScore(conversationId, score)
    }

    private fun retryCollaborationTask(taskId: String) {
        val task = dataRepository.savedConversations.value.find { it.id == taskId } ?: return
        val meta = task.metadata()
        val question = meta.collaborationQuestion ?: return
        val params = CollaborationWizardParams(
            question = question,
            minScoreThreshold = meta.minScoreThreshold ?: 0.0,
            maxWaitSeconds = meta.maxWaitSeconds ?: dataRepository.getCollaborationConfig().maxWaitSeconds,
            retryCount = meta.retryCount ?: dataRepository.getCollaborationConfig().retryCount,
            notifyOnFailure = meta.notifyOnFailure ?: true,
            notifyOnComplete = meta.notifyOnComplete ?: true,
        )
        startCollaborationTask(params)
    }

    private fun navigateCollaborationModel(delta: Int) {
        val currentId = _state.value.collaborationModelViewId ?: return
        val conversations = dataRepository.savedConversations.value
        val current = conversations.find { it.id == currentId } ?: return
        val siblings = ConversationFolderManager.childrenOf(current.parentId ?: return, conversations)
            .filter { it.type == Conversation.TYPE_COLLABORATION_MODEL }
            .sortedBy { it.title.lowercase() }
        val index = siblings.indexOfFirst { it.id == currentId }
        if (index < 0) return
        val nextIndex = index + delta
        if (nextIndex in siblings.indices) {
            _state.update { it.copy(collaborationModelViewId = siblings[nextIndex].id) }
        }
    }

    private fun optimizePrompt(currentText: String) {
        if (currentText.isBlank()) return
        viewModelScope.launch(backgroundDispatcher) {
            _state.update { it.copy(isOptimizingPrompt = true) }
            val folderHint = dataRepository.savedConversations.value
                .flatMap { it.messages }
                .mapNotNull { msg ->
                    val match = Regex("""(/[\w/\\.-]+)""").find(msg.content)
                    match?.groupValues?.getOrNull(1)
                }
                .lastOrNull()
            val scoreMap = dataRepository.getModelBenchmarks().associate { it.modelKey to it.totalScore }
            val targets = buildList {
                for (entry in dataRepository.getServiceEntries()) {
                    val modelIds = entry.modelOptions.map { it.id }.ifEmpty { listOfNotNull(entry.modelId) }
                    for (modelId in modelIds.distinct()) {
                        val key = "${entry.serviceId}::$modelId"
                        add(Triple(entry.instanceId, modelId, scoreMap[key] ?: 0.0))
                    }
                }
            }.sortedByDescending { it.third }

            val system = buildString {
                append("你是提示词优化助手。请根据用户原始提示词，输出更精准、更适合当前工程上下文的版本。")
                if (folderHint != null) append(" 工作目录或文件夹上下文：$folderHint")
                append(" 只输出优化后的提示词正文，不要解释。")
            }
            var optimized = currentText
            for ((instanceId, modelId, _) in targets) {
                try {
                    val result = dataRepository.askWithInstanceModel(
                        instanceId = instanceId,
                        modelId = modelId,
                        prompt = currentText,
                        systemPrompt = system,
                        timeoutMs = 45_000L,
                    )
                    if (result.isNotBlank()) {
                        optimized = result.trim()
                        break
                    }
                } catch (_: Exception) {
                    continue
                }
            }
            _state.update { it.copy(isOptimizingPrompt = false, pendingPromptText = optimized) }
        }
    }

    private fun clearPendingPromptText() {
        _state.update { it.copy(pendingPromptText = null) }
    }
    // endregion

    private fun updateAvailableServices() {
        val configuredEntries = dataRepository.getServiceEntries()
        val currentFreeMode = dataRepository.getFreeMode()
        val freeIsPrimary = dataRepository.isFreeServicePrimary() || configuredEntries.isEmpty()

        val freeModes = (listOf(currentFreeMode) + FreeMode.entries.filter { it != currentFreeMode }).map { mode ->
            ServiceEntry(
                instanceId = mode.instanceId,
                serviceId = Service.Free.id,
                serviceName = freeModeNames.getValue(mode),
                modelId = "",
                icon = mode.icon,
            )
        }

        val entries = if (freeIsPrimary) {
            freeModes + configuredEntries
        } else {
            configuredEntries + freeModes
        }.toImmutableList()

        val primaryService = entries.firstOrNull()?.let { Service.fromId(it.serviceId) }
        val warning = if (primaryService?.isOnDevice == true && dataRepository.getLocalDownloadedModels().isEmpty()) {
            Res.string.litert_no_model_warning
        } else {
            null
        }
        _state.update { it.copy(availableServices = entries, warning = warning, showPrivacyInfo = dataRepository.isUsingSharedKey()) }
    }

    companion object {
        private val FREE_MODE_INSTANCE_IDS = FreeMode.entries.associateBy { it.instanceId }
    }

    private fun regenerate() {
        dataRepository.regenerate()
        ask(null)
    }

    private fun loadConversation(id: String) {
        currentJob?.cancel()
        currentJob = null
        val conversation = dataRepository.savedConversations.value.find { it.id == id }
        val isInteractive = conversation?.type == Conversation.TYPE_INTERACTIVE
        dataRepository.setInteractiveMode(isInteractive)
        dataRepository.loadConversation(id)
        _state.update {
            it.copy(
                error = null,
                showFreeProviderSuggestions = false,
                isInteractiveMode = isInteractive,
                isLoading = false,
            )
        }
    }

    private fun deleteConversation(id: String) {
        commitPendingConversationDeletion()
        _state.update { it.copy(pendingConversationDeletion = id) }
        pendingConversationDeleteJob = viewModelScope.launch(backgroundDispatcher) {
            delay(4.seconds)
            dataRepository.deleteConversation(id)
            _state.update { it.copy(pendingConversationDeletion = null) }
        }
    }

    private fun undoDeleteConversation() {
        pendingConversationDeleteJob?.cancel()
        pendingConversationDeleteJob = null
        _state.update { it.copy(pendingConversationDeletion = null) }
    }

    private fun commitPendingConversationDeletion() {
        pendingConversationDeleteJob?.cancel()
        pendingConversationDeleteJob = null
        val pendingId = _state.value.pendingConversationDeletion ?: return
        _state.update { it.copy(pendingConversationDeletion = null) }
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.deleteConversation(pendingId)
        }
    }

    override fun onCleared() {
        commitPendingConversationDeletion()
        // The scheduler lives longer than this ViewModel (it's a singleton driving the
        // Android foreground service). Reset the predicate so the daemon path keeps
        // running without a stale reference to a dead state flow. The foreground-visible
        // signal (`appInForeground`) is tracked separately via `ProcessLifecycleOwner`
        // on Android — ViewModel lifecycle is too narrow (survives backgrounding).
        taskScheduler.isLoadingCheck = { false }
        super.onCleared()
    }

    private fun clearUnreadHeartbeat() {
        dataRepository.clearUnreadHeartbeat()
    }

    private fun sendSmsDraft(draftId: String) {
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.sendSmsDraft(draftId)
        }
    }

    private fun discardSmsDraft(draftId: String) {
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.discardSmsDraft(draftId)
        }
    }

    private fun startNewChat() {
        // 终止可能正在运行的协作，确保左上角"+"能开启新一轮对话（单一或协作均可）。
        stopCollaboration()
        currentJob?.cancel()
        currentJob = null
        dataRepository.startNewChat()
        dataRepository.setInteractiveMode(false)
        _state.update {
            it.copy(
                error = null,
                showFreeProviderSuggestions = false,
                isInteractiveMode = false,
                isLoading = false,
                collaborationEvents = emptyList(),
                collaborationSummary = null,
                isCollaborating = false,
                collaborationNotification = null,
                showCollaborationWizard = false,
                collaborationModelViewId = null,
            )
        }
    }

    private fun enterInteractiveMode() {
        dataRepository.startNewChat()
        dataRepository.setInteractiveMode(true)
        _state.update {
            it.copy(
                isInteractiveMode = true,
                error = null,
                showFreeProviderSuggestions = false,
            )
        }
    }

    private fun exitInteractiveMode() {
        currentJob?.cancel()
        currentJob = null
        dataRepository.startNewChat()
        dataRepository.setInteractiveMode(false)
        _state.update {
            it.copy(
                isInteractiveMode = false,
                isLoading = false,
                error = null,
                showFreeProviderSuggestions = false,
            )
        }
    }

    private fun resubmit(messageId: String, event: String, data: Map<String, String>) {
        if (_state.value.isLoading) return
        dataRepository.truncateFrom(messageId)
        submitUiCallback(event, data)
    }

    private fun goBackInteractiveMode() {
        val userCount = dataRepository.chatHistory.value.count { it.role == History.Role.USER }
        if (userCount <= 1) {
            // Go back to initial prompt — clear history but stay in interactive mode
            dataRepository.clearHistory()
        } else {
            dataRepository.popLastExchange()
        }
    }

    fun refreshSettings() {
        updateAvailableServices()
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.restoreCurrentConversation()
            presetInteractiveModeForCurrentConversation()
        }
    }

    /**
     * Resolves the interactive mode flag from the currently-loaded conversation, or — when
     * there is no loaded conversation (new empty chat) — falls back to the persisted flag.
     */
    private fun presetInteractiveModeForCurrentConversation() {
        val currentId = dataRepository.currentConversationId.value
        val conversation = dataRepository.savedConversations.value.find { it.id == currentId }
        val isInteractive = if (conversation != null) {
            conversation.type == Conversation.TYPE_INTERACTIVE
        } else {
            dataRepository.isInteractiveModeActive()
        }
        dataRepository.setInteractiveMode(isInteractive)
        _state.update { it.copy(isInteractiveMode = isInteractive) }
    }
}
