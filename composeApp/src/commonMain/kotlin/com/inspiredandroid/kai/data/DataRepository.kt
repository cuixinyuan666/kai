package com.inspiredandroid.kai.data

import com.inspiredandroid.kai.inference.DownloadError
import com.inspiredandroid.kai.inference.DownloadedModel
import com.inspiredandroid.kai.inference.EngineState
import com.inspiredandroid.kai.inference.LocalModel
import com.inspiredandroid.kai.inference.ModelImportError
import com.inspiredandroid.kai.inference.ModelImportResult
import com.inspiredandroid.kai.linux.LinuxDistro
import com.inspiredandroid.kai.data.collaboration.ChatMode
import com.inspiredandroid.kai.data.collaboration.CollaborationConfig
import com.inspiredandroid.kai.data.collaboration.CollaborationWizardParams
import com.inspiredandroid.kai.data.collaboration.ModelRef
import com.inspiredandroid.kai.mcp.McpServerConfig
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.skills.RegistrySkillEntry
import com.inspiredandroid.kai.skills.SkillManifest
import com.inspiredandroid.kai.ui.chat.History
import com.inspiredandroid.kai.ui.settings.SettingsModel
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.StateFlow

interface DataRepository {
    val chatHistory: StateFlow<List<History>>
    val currentConversationId: StateFlow<String?>
    val fallbackStatus: StateFlow<FallbackStatus?>

    // Configured services management
    fun getConfiguredServiceInstances(): List<ServiceInstance>
    fun addConfiguredService(serviceId: String): ServiceInstance
    fun removeConfiguredService(instanceId: String)
    fun reorderConfiguredServices(orderedInstanceIds: List<String>)
    fun getServiceDisplayOrder(): List<String>
    fun getServiceEntries(): List<ServiceEntry>
    fun isFreeFallbackEnabled(): Boolean
    fun setFreeFallbackEnabled(enabled: Boolean)
    fun getFreeMode(): FreeMode
    fun setFreeMode(mode: FreeMode)
    fun isFreeServicePrimary(): Boolean
    fun setFreeServicePrimary(primary: Boolean)

    // Per-instance settings
    fun getInstanceApiKey(instanceId: String): String
    fun updateInstanceApiKey(instanceId: String, apiKey: String)
    fun getInstanceBaseUrl(instanceId: String, service: Service): String
    fun updateInstanceBaseUrl(instanceId: String, baseUrl: String)
    fun getInstanceModels(instanceId: String, service: Service): StateFlow<List<SettingsModel>>
    fun updateInstanceSelectedModel(instanceId: String, service: Service, modelId: String)
    fun getInstanceUseCustomModel(instanceId: String): Boolean
    fun updateInstanceUseCustomModel(instanceId: String, useCustom: Boolean)
    fun getInstanceCustomModelId(instanceId: String): String
    fun updateInstanceCustomModelId(instanceId: String, modelId: String)
    fun clearInstanceModels(instanceId: String, service: Service)
    suspend fun validateConnection(service: Service, instanceId: String)

    suspend fun ask(
        question: String?,
        files: List<PlatformFile>,
        uiSubmission: UiSubmission? = null,
        activeSkillId: String? = null,
    )
    fun clearHistory()
    fun currentService(): Service
    fun isUsingSharedKey(): Boolean
    fun supportedFileExtensions(): List<String>

    // Conversation management
    val savedConversations: StateFlow<List<Conversation>>
    fun loadConversations()
    fun loadConversation(id: String)
    suspend fun deleteConversation(id: String)
    fun startNewChat()
    fun regenerate()
    fun popLastExchange()
    fun truncateFrom(messageId: String)
    fun restoreCurrentConversation()

    // Tool management
    fun getToolDefinitions(): List<ToolInfo>
    fun setToolEnabled(toolId: String, enabled: Boolean)

    // MCP servers
    fun getMcpServers(): List<McpServerConfig>
    suspend fun addMcpServer(name: String, url: String, headers: Map<String, String>): McpServerConfig
    fun removeMcpServer(serverId: String)
    fun setMcpServerEnabled(serverId: String, enabled: Boolean)
    suspend fun connectMcpServer(serverId: String): Result<List<ToolInfo>>
    fun getMcpToolsForServer(serverId: String): List<ToolInfo>
    fun isMcpServerConnected(serverId: String): Boolean
    suspend fun connectEnabledMcpServers()

    // Skills (stored in the Linux sandbox at ~/skills/<id>/; Android-only)
    fun getInstalledSkills(): List<SkillManifest>
    suspend fun uninstallSkill(id: String)
    suspend fun browseSkillMarketplaces(): Result<List<RegistrySkillEntry>>
    suspend fun installBrowsedSkill(entry: RegistrySkillEntry): Result<SkillManifest>
    suspend fun installGitHubSkill(owner: String, repo: String, ref: String, path: String): Result<SkillManifest>

    // Soul (system prompt)
    fun getSoulText(): String
    fun setSoulText(text: String)
    suspend fun getActiveSystemPrompt(variant: SystemPromptVariant = SystemPromptVariant.CHAT_REMOTE): String?

    // Memory management
    fun isMemoryEnabled(): Boolean
    fun setMemoryEnabled(enabled: Boolean)
    fun getMemories(): List<MemoryEntry>
    suspend fun deleteMemory(key: String)
    suspend fun updateMemoryContent(key: String, content: String)

    // Scheduling management
    fun isSchedulingEnabled(): Boolean
    fun setSchedulingEnabled(enabled: Boolean)
    fun getScheduledTasks(): List<ScheduledTask>
    suspend fun cancelScheduledTask(id: String)

    // Dynamic UI
    fun isDynamicUiEnabled(): Boolean
    fun setDynamicUiEnabled(enabled: Boolean)

    // Theme mode
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
    fun getSettingsTab(): String?
    fun setSettingsTab(tab: String)

    // Interactive mode
    fun setInteractiveMode(enabled: Boolean)
    fun isInteractiveModeActive(): Boolean

    // Daemon mode
    fun isDaemonEnabled(): Boolean
    fun setDaemonEnabled(enabled: Boolean)

    // Linux Sandbox
    fun isSandboxEnabled(): Boolean
    fun setSandboxEnabled(enabled: Boolean)

    /** Distro a fresh sandbox install would use. An existing install keeps its own. */
    fun getSandboxDistro(): LinuxDistro
    fun setSandboxDistro(distro: LinuxDistro)

    // Kai Build

    /** Agent a freshly opened Kai Build project starts with; null is a plain shell. */
    fun getKaiBuildLaunchAgent(): String?
    fun setKaiBuildLaunchAgent(agentId: String?)

    // Heartbeat
    fun getHeartbeatConfig(): HeartbeatConfig
    fun setHeartbeatEnabled(enabled: Boolean)
    fun setHeartbeatIntervalMinutes(minutes: Int)
    fun setHeartbeatActiveHours(start: Int, end: Int)
    fun getHeartbeatPrompt(): String
    fun setHeartbeatPrompt(text: String)
    fun getHeartbeatLog(): List<HeartbeatLogEntry>
    fun getHeartbeatInstanceId(): String?
    fun setHeartbeatInstanceId(instanceId: String?)

    // Email
    fun isEmailEnabled(): Boolean
    fun setEmailEnabled(enabled: Boolean)
    fun getEmailAccounts(): List<EmailAccount>
    suspend fun removeEmailAccount(id: String)
    fun getEmailPollIntervalMinutes(): Int
    fun setEmailPollIntervalMinutes(minutes: Int)
    fun getPendingEmailCount(): Int
    fun getEmailSyncStates(): Map<String, EmailSyncState>
    suspend fun pollEmailAccount(accountId: String)

    // SMS (FOSS-only on Android; other platforms return stub values).
    // Read and send are independent opt-ins with separate runtime permissions.
    fun isSmsEnabled(): Boolean
    fun setSmsEnabled(enabled: Boolean)
    fun getSmsPollIntervalMinutes(): Int
    fun setSmsPollIntervalMinutes(minutes: Int)
    fun getPendingSmsCount(): Int
    fun getSmsSyncState(): SmsSyncState
    fun hasSmsPermission(): Boolean
    suspend fun requestSmsPermission(): Boolean
    suspend fun pollSms()

    fun isSmsSendEnabled(): Boolean
    fun setSmsSendEnabled(enabled: Boolean)
    fun hasSmsSendPermission(): Boolean
    suspend fun requestSmsSendPermission(): Boolean
    val smsDrafts: StateFlow<List<SmsDraft>>
    suspend fun sendSmsDraft(draftId: String): Boolean
    suspend fun discardSmsDraft(draftId: String)

    // Notifications (FOSS-only on Android; other platforms return stub values).
    // Per-app filtering is delegated to the system Notification Access "Apps" picker.
    fun isNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)
    fun isNotificationListenerAccessGranted(): Boolean
    fun openNotificationListenerSettings()
    fun getPendingNotificationCount(): Int
    fun getNotificationSyncState(): NotificationSyncState
    suspend fun clearPendingNotifications()

    // UI Scale
    fun getUiScale(): Float
    fun setUiScale(scale: Float)

    // Export/Import
    fun exportSettingsToJson(sections: Set<ImportSection> = ImportSection.entries.toSet()): String
    fun getExportPreview(): Map<ImportSection, String?>
    fun importSettingsFromJson(json: String, sections: Set<ImportSection>, replace: Boolean): Int

    // Background ask with tools (no chat history update, supports tool-calling loop).
    // When `conversationIdOverride` is set, tool calls during this run route to that
    // conversation's sandbox session instead of inheriting the active chat's id —
    // used by the heartbeat / scheduled tasks so their shell commands don't land
    // in the user's currently-viewed chat shell.
    suspend fun askWithTools(prompt: String, instanceId: String? = null, conversationIdOverride: String? = null): String

    // Silent ask (no tools, no chat history update)
    suspend fun askSilently(question: String): String
    suspend fun askSilentlyWithInstance(instanceId: String, prompt: String, timeoutMs: Long = 0L): String

    /**
     * 对指定实例 + 指定模型分支发起一次静默调用（用于协作模式的定向调用）。
     * 不写入聊天历史、不触发工具循环。失败由调用方（编排器）负责重试/失败转移。
     */
    suspend fun askWithInstanceModel(
        instanceId: String,
        modelId: String,
        prompt: String,
        systemPrompt: String? = null,
        timeoutMs: Long = 0L,
    ): String

    /**
     * 在指定会话中以单一模式完整流水线（工具、记忆等）发起一次问答，不污染当前聊天界面历史。
     */
    suspend fun askInConversation(
        conversationId: String,
        instanceId: String,
        modelId: String,
        question: String,
        timeoutMs: Long = 0L,
        files: List<io.github.vinceglb.filekit.PlatformFile> = emptyList(),
    ): String

    suspend fun retryCollaborationModel(conversationId: String, timeoutMs: Long): String

    fun setCollaborationModelUserScore(conversationId: String, score: Double)

    suspend fun createCollaborationTask(question: String, params: CollaborationWizardParams): String

    suspend fun createCollaborationModelConversation(
        taskId: String,
        ref: ModelRef,
        folderTitle: String,
        question: String,
        params: CollaborationWizardParams,
    ): String

    fun updateCollaborationModelStatus(conversationId: String, status: CollaborationModelStatus, response: String?)

    fun completeTaskConversation(taskId: String, status: CollaborationModelStatus)

    suspend fun createWarTask(
        question: String,
        params: com.inspiredandroid.kai.data.war.WarWizardParams,
        summaryRef: ModelRef,
    ): String

    suspend fun createWarModelConversation(
        taskId: String,
        ref: ModelRef,
        folderTitle: String,
        question: String,
        params: com.inspiredandroid.kai.data.war.WarWizardParams,
        isSummaryModel: Boolean = false,
    ): String

    suspend fun createWarResultConversation(taskId: String): String

    fun saveWarTaskResult(taskId: String, result: com.inspiredandroid.kai.data.war.WarTaskResult)

    fun appendConversationExchange(conversationId: String, userContent: String, assistantContent: String)

    // 协作模式配置 / 聊天模式
    fun getChatMode(): ChatMode
    fun setChatMode(mode: ChatMode)
    fun getCollaborationConfig(): CollaborationConfig
    fun setCollaborationConfig(config: CollaborationConfig)
    suspend fun addAssistantMessage(content: String)

    // 模型基准测试（一键测试所有大模型）
    val modelBenchmarks: StateFlow<List<ModelBenchmark>>
    fun getModelBenchmarks(): List<ModelBenchmark>
    fun upsertModelBenchmark(benchmark: ModelBenchmark)
    fun clearModelBenchmarks()

    // OpenCode 终端 thinking/plan/build 调节
    fun getOpenCodeTerminalThinking(): Boolean
    fun setOpenCodeTerminalThinking(enabled: Boolean)
    fun getOpenCodeTerminalMode(): String
    fun setOpenCodeTerminalMode(mode: String)

    /**
     * 将一条用户消息追加到【当前会话】的聊天历史并持久化（区别于写入 heartbeat 的
     * `addAssistantMessage`）。用于协作模式把用户提问记录到聊天记录中。
     */
    suspend fun appendUserMessageToCurrentChat(content: String)

    /**
     * 将一条助手消息追加到【当前会话】的聊天历史并持久化。用于协作模式把最终汇总
     * 结果记录到聊天记录中，使协作模式也有可回看的聊天记录。
     */
    suspend fun appendAssistantMessageToCurrentChat(content: String)

    /**
     * Resolve the persistent heartbeat conversation's id, creating an empty
     * [Conversation] with [Conversation.TYPE_HEARTBEAT] if none exists yet.
     * Used by the scheduler to bind the heartbeat / scheduled-task tool calls
     * to a stable sandbox session before the AI starts emitting tool calls.
     */
    suspend fun getOrCreateHeartbeatConversationId(): String

    // Heartbeat notification
    val hasUnreadHeartbeat: StateFlow<Boolean>
    fun clearUnreadHeartbeat()

    /**
     * Pulse that fires when the user taps a heartbeat push notification while the app is
     * not already on the heartbeat conversation. `true` means "load the heartbeat
     * conversation now, then call [consumeOpenHeartbeatRequest]". Collected by
     * `ChatViewModel` in its init block.
     */
    val openHeartbeatRequested: StateFlow<Boolean>
    fun requestOpenHeartbeat()
    fun consumeOpenHeartbeatRequest()

    /**
     * Pulse that fires when the app is launched via the Android assist gesture
     * (long-press home / power button) with `ACTION_ASSIST`. `true` means "start a
     * fresh chat now, then call [consumeOpenAssistRequest]". Collected by
     * `ChatViewModel` in its init block.
     */
    val openAssistRequested: StateFlow<Boolean>
    fun requestOpenAssist()
    fun consumeOpenAssistRequest()

    // On-device inference (LiteRT)
    fun isLocalInferenceAvailable(): Boolean
    fun getLocalEngineState(): StateFlow<EngineState>?
    fun getLocalDownloadedModels(): List<DownloadedModel>
    fun getLocalAvailableModels(): List<LocalModel>
    fun getLocalImportedModels(): List<LocalModel>
    fun getLocalFreeSpaceBytes(): Long
    fun getTotalDeviceMemoryBytes(): Long
    fun getModelContextTokens(modelId: String): Int
    fun setModelContextTokens(modelId: String, contextTokens: Int)
    suspend fun releaseLocalEngine()
    fun getLocalDownloadingModelId(): StateFlow<String?>?
    fun getLocalDownloadProgress(): StateFlow<Float?>?
    fun getLocalDownloadError(): StateFlow<DownloadError?>?
    fun getLocalImportingFileName(): StateFlow<String?>?
    fun getLocalImportProgress(): StateFlow<Float?>?
    fun getLocalImportError(): StateFlow<ModelImportError?>?
    fun startLocalModelDownload(model: LocalModel)
    fun cancelLocalModelDownload()
    suspend fun importLocalModel(source: PlatformFile): ModelImportResult
    fun cancelLocalModelImport()
    suspend fun deleteLocalModel(modelId: String)
}
