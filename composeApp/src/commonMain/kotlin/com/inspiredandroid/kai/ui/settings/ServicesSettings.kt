@file:OptIn(ExperimentalMaterial3Api::class)

package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.inspiredandroid.kai.data.ModelBenchmark
import com.inspiredandroid.kai.data.Service
import com.inspiredandroid.kai.data.benchmarkScoreColor
import com.inspiredandroid.kai.formatFileSize
import com.inspiredandroid.kai.inference.DevicePerformance
import com.inspiredandroid.kai.inference.DownloadError
import com.inspiredandroid.kai.inference.LocalModel
import com.inspiredandroid.kai.inference.ModelImportError
import com.inspiredandroid.kai.inference.calculateDevicePerformance
import com.inspiredandroid.kai.inference.estimateGpuMemoryMb
import com.inspiredandroid.kai.network.dtos.SponsorsResponseDto
import com.inspiredandroid.kai.ui.KaiClearableTextField
import com.inspiredandroid.kai.ui.components.KaiSlider
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForScroll
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.icons.DragIndicator
import com.inspiredandroid.kai.ui.kaiAdaptiveCardBorder
import com.inspiredandroid.kai.ui.kaiAdaptiveCardColors
import com.inspiredandroid.kai.ui.kaiAdaptiveCardSurface
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ic_arrow_drop_down
import kai.composeapp.generated.resources.ic_vip
import kai.composeapp.generated.resources.litert_cancel
import kai.composeapp.generated.resources.litert_context_size
import kai.composeapp.generated.resources.litert_download
import kai.composeapp.generated.resources.litert_error_checksum_mismatch
import kai.composeapp.generated.resources.litert_error_download_incomplete
import kai.composeapp.generated.resources.litert_error_import_failed
import kai.composeapp.generated.resources.litert_error_import_invalid
import kai.composeapp.generated.resources.litert_error_import_too_small
import kai.composeapp.generated.resources.litert_error_network
import kai.composeapp.generated.resources.litert_error_not_enough_disk_space
import kai.composeapp.generated.resources.litert_free_space
import kai.composeapp.generated.resources.litert_import
import kai.composeapp.generated.resources.litert_import_description
import kai.composeapp.generated.resources.litert_imported
import kai.composeapp.generated.resources.litert_importing
import kai.composeapp.generated.resources.litert_on_device_description
import kai.composeapp.generated.resources.litert_performance_good
import kai.composeapp.generated.resources.litert_performance_ok
import kai.composeapp.generated.resources.litert_performance_poor
import kai.composeapp.generated.resources.litert_recommended
import kai.composeapp.generated.resources.litert_tool_support
import kai.composeapp.generated.resources.settings_add_service
import kai.composeapp.generated.resources.settings_api_key_label
import kai.composeapp.generated.resources.settings_api_key_optional_label
import kai.composeapp.generated.resources.settings_base_url_label
import kai.composeapp.generated.resources.settings_become_sponsor
import kai.composeapp.generated.resources.settings_business_partnerships
import kai.composeapp.generated.resources.settings_business_partnerships_description
import kai.composeapp.generated.resources.settings_contact_sponsorship
import kai.composeapp.generated.resources.settings_custom_model_hint
import kai.composeapp.generated.resources.settings_custom_model_label
import kai.composeapp.generated.resources.settings_free_fallback
import kai.composeapp.generated.resources.settings_free_tier_description
import kai.composeapp.generated.resources.settings_free_tier_title
import kai.composeapp.generated.resources.settings_model_label
import kai.composeapp.generated.resources.settings_open_app_settings
import kai.composeapp.generated.resources.settings_openai_compatible_or_other_service
import kai.composeapp.generated.resources.settings_openai_compatible_providers
import kai.composeapp.generated.resources.settings_openai_compatible_setup_ollama
import kai.composeapp.generated.resources.settings_remove_service
import kai.composeapp.generated.resources.settings_reorder_content_description
import kai.composeapp.generated.resources.settings_sign_in_copy_api_key_from
import kai.composeapp.generated.resources.settings_sponsors
import kai.composeapp.generated.resources.settings_status_checking
import kai.composeapp.generated.resources.settings_status_connected
import kai.composeapp.generated.resources.settings_status_error
import kai.composeapp.generated.resources.settings_status_error_connection_failed
import kai.composeapp.generated.resources.settings_status_error_invalid_key
import kai.composeapp.generated.resources.settings_status_error_local_network
import kai.composeapp.generated.resources.settings_status_error_quota_exhausted
import kai.composeapp.generated.resources.settings_status_error_rate_limited
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import sh.calvin.reorderable.ReorderableColumn
import kotlin.math.roundToInt

@Composable
internal fun FreeSettings() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = kaiAdaptiveCardColors(),
        border = kaiAdaptiveCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "APP-FREE",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.settings_free_tier_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SponsorList(
    title: String,
    sponsors: ImmutableList<SponsorsResponseDto.Sponsor>,
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sponsors.forEach { sponsor ->
            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { uriHandler.openUri("https://github.com/${sponsor.username}") }
                    .handCursor()
                    .padding(4.dp),
            ) {
                coil3.compose.AsyncImage(
                    model = sponsor.avatar,
                    contentDescription = sponsor.username,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sponsor.username,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun ServicesContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    var showAddServiceSheet by remember { mutableStateOf(false) }

    // 反序切换：服务与子模型按 Z-A 显示
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = { actions.onToggleServiceSortReversed(!uiState.serviceSortReversed) }) {
            Text(if (uiState.serviceSortReversed) "Z-A" else "A-Z")
        }
    }

    // 一键测试所有大模型
    val freeEntry = uiState.configuredServices.firstOrNull { it.instanceId == "free" }
    val reorderableEntries = uiState.configuredServices.filter { it.instanceId != "free" }

    if (freeEntry != null) {
        ConfiguredServiceCardContent(
            entry = freeEntry,
            isExpanded = uiState.expandedServiceId == freeEntry.instanceId,
            onExpand = { actions.onExpandService(if (uiState.expandedServiceId == freeEntry.instanceId) null else freeEntry.instanceId) },
            onChangeApiKey = { },
            onChangeBaseUrl = { },
            onSelectModel = { modelId -> actions.onSelectModel(freeEntry.instanceId, modelId) },
            onRemove = { },
            localAvailableModels = uiState.localAvailableModels,
            localImportedModels = uiState.localImportedModels,
            totalDeviceMemoryBytes = uiState.totalDeviceMemoryBytes,
            localFreeSpaceBytes = uiState.localFreeSpaceBytes,
            localDownloadingModelId = uiState.localDownloadingModelId,
            localDownloadProgress = uiState.localDownloadProgress,
            localDownloadError = uiState.localDownloadError,
            localImportingFileName = uiState.localImportingFileName,
            localImportProgress = uiState.localImportProgress,
            localImportError = uiState.localImportError,
            onDownloadLocalModel = actions.onDownloadLocalModel,
            onCancelLocalModelDownload = actions.onCancelLocalModelDownload,
            onImportLocalModel = actions.onImportLocalModel,
            onCancelLocalModelImport = actions.onCancelLocalModelImport,
            onDeleteLocalModel = actions.onDeleteLocalModel,
            onChangeModelContextTokens = actions.onChangeModelContextTokens,
            modelContextTokens = uiState.modelContextTokens,
            onOpenAppPermissionSettings = actions.onOpenAppPermissionSettings,
            onRecheckLocalNetworkPermission = { actions.onRecheckLocalNetworkPermission(freeEntry.instanceId) },
            modelBenchmarks = uiState.modelBenchmarks.associate { it.modelKey to it.totalScore },
            openCodeTerminalThinking = uiState.openCodeTerminalThinking,
            openCodeTerminalMode = uiState.openCodeTerminalMode,
            onToggleOpenCodeTerminalThinking = actions.onToggleOpenCodeTerminalThinking,
            onChangeOpenCodeTerminalMode = actions.onChangeOpenCodeTerminalMode,
        )
        Spacer(Modifier.height(8.dp))
    }

    ModelBenchmarkCard(
        benchmarks = uiState.modelBenchmarks,
        isRunning = uiState.isBenchmarkRunning,
        progress = uiState.benchmarkProgress,
        currentLabel = uiState.benchmarkCurrentLabel,
        done = uiState.benchmarkDone,
        summary = uiState.benchmarkSummary,
        onRun = actions.onRunModelBenchmarks,
        onCancel = actions.onCancelModelBenchmarks,
        onClear = actions.onClearModelBenchmarks,
    )

    Spacer(Modifier.height(8.dp))

    // Configured services list
    val entries = reorderableEntries
    ReorderableColumn(
        list = entries,
        onSettle = { fromIndex, toIndex ->
            val ids = entries.map { it.instanceId }.toMutableList()
            ids.add(toIndex, ids.removeAt(fromIndex))
            actions.onReorderServices(ids)
        },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { _, entry, isDragging ->
        key(entry.instanceId) {
            ReorderableItem {
                ConfiguredServiceCardContent(
                    entry = entry,
                    isExpanded = uiState.expandedServiceId == entry.instanceId,
                    onExpand = { actions.onExpandService(if (uiState.expandedServiceId == entry.instanceId) null else entry.instanceId) },
                    onChangeApiKey = { apiKey -> actions.onChangeApiKey(entry.instanceId, apiKey) },
                    onChangeBaseUrl = { baseUrl -> actions.onChangeBaseUrl(entry.instanceId, baseUrl) },
                    onSelectModel = { modelId -> actions.onSelectModel(entry.instanceId, modelId) },
                    onToggleUseCustomModel = { use -> actions.onToggleUseCustomModel(entry.instanceId, use) },
                    onChangeCustomModelId = { id -> actions.onChangeCustomModelId(entry.instanceId, id) },
                    onRemove = { actions.onRemoveService(entry.instanceId) },
                    isDragging = isDragging,
                    dragHandleModifier = if (entries.size >= 2) Modifier.draggableHandle() else null,
                    localAvailableModels = uiState.localAvailableModels,
                    localImportedModels = uiState.localImportedModels,
                    totalDeviceMemoryBytes = uiState.totalDeviceMemoryBytes,
                    localFreeSpaceBytes = uiState.localFreeSpaceBytes,
                    localDownloadingModelId = uiState.localDownloadingModelId,
                    localDownloadProgress = uiState.localDownloadProgress,
                    localDownloadError = uiState.localDownloadError,
                    localImportingFileName = uiState.localImportingFileName,
                    localImportProgress = uiState.localImportProgress,
                    localImportError = uiState.localImportError,
                    onDownloadLocalModel = actions.onDownloadLocalModel,
                    onCancelLocalModelDownload = actions.onCancelLocalModelDownload,
                    onImportLocalModel = actions.onImportLocalModel,
                    onCancelLocalModelImport = actions.onCancelLocalModelImport,
                    onDeleteLocalModel = actions.onDeleteLocalModel,
                    onChangeModelContextTokens = actions.onChangeModelContextTokens,
                    modelContextTokens = uiState.modelContextTokens,
                    onOpenAppPermissionSettings = actions.onOpenAppPermissionSettings,
                    onRecheckLocalNetworkPermission = { actions.onRecheckLocalNetworkPermission(entry.instanceId) },
                    modelBenchmarks = uiState.modelBenchmarks.associate { it.modelKey to it.totalScore },
                    openCodeTerminalThinking = uiState.openCodeTerminalThinking,
                    openCodeTerminalMode = uiState.openCodeTerminalMode,
                    onToggleOpenCodeTerminalThinking = actions.onToggleOpenCodeTerminalThinking,
                    onChangeOpenCodeTerminalMode = actions.onChangeOpenCodeTerminalMode,
                )
            }
        }
    }

    if (uiState.availableServicesToAdd.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showAddServiceSheet = true }, modifier = Modifier.handCursor()) {
            Text(stringResource(Res.string.settings_add_service))
        }
    }

    // Add service bottom sheet
    if (showAddServiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddServiceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            val addServiceScrollState = rememberScrollState()
            Box {
                Column(modifier = Modifier.verticalScroll(addServiceScrollState).padding(16.dp)) {
                    val services = uiState.availableServicesToAdd
                    services.forEachIndexed { index, service ->
                        val isFirst = index == 0
                        val isLast = index == services.lastIndex
                        val itemShape = RoundedCornerShape(
                            topStart = if (isFirst) 12.dp else 0.dp,
                            topEnd = if (isFirst) 12.dp else 0.dp,
                            bottomStart = if (isLast) 12.dp else 0.dp,
                            bottomEnd = if (isLast) 12.dp else 0.dp,
                        )
                        val isSpecial = service.isOnDevice || service is Service.OpenAICompatible || service is Service.AtlasCloud
                        Surface(
                            onClick = {
                                actions.onAddService(service)
                                showAddServiceSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().handCursor(),
                            shape = itemShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .then(
                                            if (isSpecial) {
                                                Modifier.background(
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    shape = RoundedCornerShape(8.dp),
                                                )
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = vectorResource(service.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = service.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                if (service is Service.OpenCode || service is Service.OpenCodeTerminal) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = vectorResource(Res.drawable.ic_vip),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                VerticalScrollbarForScroll(
                    scrollState = addServiceScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        }
    }

    // 协作模式设置已移至独立的「协作」tab（与「通用」同级），见 SettingsScreen。
}

@Composable
private fun ConfiguredServiceCardContent(
    entry: ConfiguredServiceEntry,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onChangeApiKey: (String) -> Unit,
    onChangeBaseUrl: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onToggleUseCustomModel: (Boolean) -> Unit = {},
    onChangeCustomModelId: (String) -> Unit = {},
    onRemove: () -> Unit,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier? = null,
    localAvailableModels: ImmutableList<LocalModel> = persistentListOf(),
    localImportedModels: ImmutableList<LocalModel> = persistentListOf(),
    totalDeviceMemoryBytes: Long = Long.MAX_VALUE,
    localFreeSpaceBytes: Long = 0L,
    localDownloadingModelId: String? = null,
    localDownloadProgress: Float? = null,
    localDownloadError: DownloadError? = null,
    localImportingFileName: String? = null,
    localImportProgress: Float? = null,
    localImportError: ModelImportError? = null,
    onDownloadLocalModel: (LocalModel) -> Unit = {},
    onCancelLocalModelDownload: () -> Unit = {},
    onImportLocalModel: (PlatformFile) -> Unit = {},
    onCancelLocalModelImport: () -> Unit = {},
    onDeleteLocalModel: (String) -> Unit = {},
    onChangeModelContextTokens: (String, Int) -> Unit = { _, _ -> },
    modelContextTokens: ImmutableMap<String, Int> = persistentMapOf(),
    onOpenAppPermissionSettings: () -> Unit = {},
    onRecheckLocalNetworkPermission: () -> Unit = {},
    modelBenchmarks: Map<String, Double> = emptyMap(),
    openCodeTerminalThinking: Boolean = false,
    openCodeTerminalMode: String = "build",
    onToggleOpenCodeTerminalThinking: (Boolean) -> Unit = {},
    onChangeOpenCodeTerminalMode: (String) -> Unit = {},
) {
    // Clear a stale denied status when the user returns from granting the permission in
    // system settings; the recheck never re-prompts, so this is a no-op while still denied.
    if (entry.connectionStatus == ConnectionStatus.ErrorLocalNetworkDenied) {
        LifecycleResumeEffect(entry.instanceId) {
            onRecheckLocalNetworkPermission()
            onPauseOrDispose { }
        }
    }
    Column(
        modifier = Modifier
            .kaiAdaptiveCardSurface()
            .fillMaxWidth()
            .clickable { onExpand() }
            .handCursor(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Drag handle
                if (dragHandleModifier != null) {
                    Icon(
                        imageVector = Icons.Rounded.DragIndicator,
                        contentDescription = stringResource(Res.string.settings_reorder_content_description),
                        modifier = dragHandleModifier.handCursor(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                }

                // Connection status dot
                val dotColor = when (entry.connectionStatus) {
                    ConnectionStatus.Connected -> StatusColorConnected
                    ConnectionStatus.Checking -> StatusColorChecking
                    ConnectionStatus.Unknown -> StatusColorUnknown
                    else -> StatusColorError
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )

                Spacer(Modifier.width(12.dp))

                // Service name and model
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.service.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val displayModelId = when {
                        entry.useCustomModel && entry.customModelId.isNotBlank() -> entry.customModelId
                        entry.selectedModel != null -> entry.selectedModel.id
                        else -> null
                    }
                    if (displayModelId != null) {
                        Text(
                            text = displayModelId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val bmScore = entry.selectedModel?.let { modelBenchmarks["${entry.service.id}::${it.id}"] }
                        if (bmScore != null) {
                            Text(
                                text = "  ·  " + bmScore.roundToInt() + "分",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(benchmarkScoreColor(bmScore)),
                            )
                        }
                    }
                }

                if (entry.service is Service.OpenCode || entry.service is Service.OpenCodeTerminal) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_vip),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Expand/collapse chevron
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Expanded content
        if (isExpanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                if (entry.service.isOnDevice) {
                    LiteRTSettings(
                        selectedModel = entry.selectedModel,
                        downloadedModels = entry.models,
                        availableModels = localAvailableModels,
                        importedModels = localImportedModels,
                        totalDeviceMemoryBytes = totalDeviceMemoryBytes,
                        freeSpaceBytes = localFreeSpaceBytes,
                        downloadingModelId = localDownloadingModelId,
                        downloadProgress = localDownloadProgress,
                        downloadError = localDownloadError,
                        importingFileName = localImportingFileName,
                        importProgress = localImportProgress,
                        importError = localImportError,
                        onSelectModel = onSelectModel,
                        onDownloadModel = onDownloadLocalModel,
                        onCancelDownload = onCancelLocalModelDownload,
                        onImportModel = onImportLocalModel,
                        onCancelImport = onCancelLocalModelImport,
                        onDeleteModel = onDeleteLocalModel,
                        onChangeModelContextTokens = onChangeModelContextTokens,
                        modelContextTokens = modelContextTokens,
                    )
                } else if (entry.service == Service.Free) {
                    Text(
                        text = stringResource(Res.string.settings_free_tier_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (entry.models.isNotEmpty()) {
                        ModelSelection(
                            currentSelectedModel = entry.selectedModel,
                            models = entry.models,
                            serviceName = entry.service.displayName,
                            onClick = onSelectModel,
                            modelBenchmarks = modelBenchmarks,
                            serviceId = entry.service.id,
                        )
                    }
                } else if (entry.service is Service.OpenAICompatible) {
                    OpenAICompatibleSettings(
                        baseUrl = entry.baseUrl,
                        onChangeBaseUrl = onChangeBaseUrl,
                        apiKey = entry.apiKey,
                        onChangeApiKey = onChangeApiKey,
                        selectedModel = entry.selectedModel,
                        models = entry.models,
                        onSelectModel = onSelectModel,
                        serviceName = entry.service.displayName,
                        useCustomModel = entry.useCustomModel,
                        customModelId = entry.customModelId,
                        onToggleUseCustomModel = onToggleUseCustomModel,
                        onChangeCustomModelId = onChangeCustomModelId,
                        connectionStatus = entry.connectionStatus,
                        onOpenAppPermissionSettings = onOpenAppPermissionSettings,
                        modelBenchmarks = modelBenchmarks,
                        serviceId = entry.service.id,
                    )
                } else {
                    ServiceSettings(
                        apiKey = entry.apiKey,
                        onChangeApiKey = onChangeApiKey,
                        apiKeyUrl = entry.service.apiKeyUrl ?: "",
                        apiKeyUrlDisplay = entry.service.apiKeyUrlDisplay ?: "",
                        selectedModel = entry.selectedModel,
                        models = entry.models,
                        onSelectModel = onSelectModel,
                        serviceName = entry.service.displayName,
                        connectionStatus = entry.connectionStatus,
                        onOpenAppPermissionSettings = onOpenAppPermissionSettings,
                        modelBenchmarks = modelBenchmarks,
                        serviceId = entry.service.id,
                        openCodeTerminalThinking = openCodeTerminalThinking,
                        openCodeTerminalMode = openCodeTerminalMode,
                        onToggleOpenCodeTerminalThinking = onToggleOpenCodeTerminalThinking,
                        onChangeOpenCodeTerminalMode = onChangeOpenCodeTerminalMode,
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (entry.service != Service.Free) {
                    // Remove action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onRemove,
                            modifier = Modifier.handCursor(),
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_remove_service),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceSettings(
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    apiKeyUrl: String,
    apiKeyUrlDisplay: String,
    selectedModel: SettingsModel?,
    models: ImmutableList<SettingsModel>,
    onSelectModel: (String) -> Unit,
    connectionStatus: ConnectionStatus,
    serviceName: String = "",
    testTag: String? = null,
    onOpenAppPermissionSettings: () -> Unit = {},
    modelBenchmarks: Map<String, Double> = emptyMap(),
    serviceId: String = "",
    openCodeTerminalThinking: Boolean = false,
    openCodeTerminalMode: String = "build",
    onToggleOpenCodeTerminalThinking: (Boolean) -> Unit = {},
    onChangeOpenCodeTerminalMode: (String) -> Unit = {},
) {
    ApiKeyField(
        apiKey = apiKey,
        onChangeApiKey = onChangeApiKey,
        labelText = stringResource(Res.string.settings_api_key_label),
        testTag = testTag,
    )

    if (serviceId == "opencode-terminal") {
        Spacer(Modifier.height(10.dp))
        OpenCodeTerminalControls(
            thinking = openCodeTerminalThinking,
            mode = openCodeTerminalMode,
            onToggleThinking = onToggleOpenCodeTerminalThinking,
            onChangeMode = onChangeOpenCodeTerminalMode,
        )
    }

    Spacer(Modifier.height(8.dp))

    ConnectionStatusIndicator(connectionStatus, onOpenAppPermissionSettings)

    Spacer(Modifier.height(8.dp))

    val linkColor = MaterialTheme.colorScheme.primary

    val copyApiKeyPromptString = stringResource(Res.string.settings_sign_in_copy_api_key_from)
    val annotatedString = remember(apiKeyUrl, apiKeyUrlDisplay) {
        buildAnnotatedString {
            append(copyApiKeyPromptString)
            append(" ")
            withLink(LinkAnnotation.Url(url = apiKeyUrl)) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append(apiKeyUrlDisplay)
                }
            }
        }
    }
    Text(
        annotatedString,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(Modifier.height(16.dp))

    if (connectionStatus == ConnectionStatus.Connected || models.isNotEmpty()) {
        ModelSelection(selectedModel, models, serviceName, onSelectModel, modelBenchmarks, serviceId)
    }
}

@Composable
private fun OpenAICompatibleSettings(
    baseUrl: String,
    onChangeBaseUrl: (String) -> Unit,
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    selectedModel: SettingsModel?,
    models: ImmutableList<SettingsModel>,
    onSelectModel: (String) -> Unit,
    useCustomModel: Boolean,
    customModelId: String,
    onToggleUseCustomModel: (Boolean) -> Unit,
    onChangeCustomModelId: (String) -> Unit,
    connectionStatus: ConnectionStatus,
    serviceName: String = "",
    onOpenAppPermissionSettings: () -> Unit = {},
    modelBenchmarks: Map<String, Double> = emptyMap(),
    serviceId: String = "",
) {
    KaiClearableTextField(
        value = baseUrl,
        onValueChange = onChangeBaseUrl,
        label = {
            Text(
                stringResource(Res.string.settings_base_url_label),
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        singleLine = true,
    )
    if (baseUrl.isNotBlank()) {
        Text(
            text = "${baseUrl.trimEnd('/')}${Service.OpenAICompatible.chatUrl}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
        )
    }

    Spacer(Modifier.height(8.dp))

    ApiKeyField(
        apiKey = apiKey,
        onChangeApiKey = onChangeApiKey,
        labelText = stringResource(Res.string.settings_api_key_optional_label),
        singleLine = true,
    )

    Spacer(Modifier.height(8.dp))

    ConnectionStatusIndicator(connectionStatus, onOpenAppPermissionSettings)

    Spacer(Modifier.height(8.dp))

    val linkColor = MaterialTheme.colorScheme.primary
    val setupOllamaText = stringResource(Res.string.settings_openai_compatible_setup_ollama)
    val orOtherServiceText = stringResource(Res.string.settings_openai_compatible_or_other_service)
    val providersText = stringResource(Res.string.settings_openai_compatible_providers)
    val annotatedString = remember(setupOllamaText, orOtherServiceText, providersText, linkColor) {
        buildAnnotatedString {
            append(setupOllamaText)
            append(" ")
            withLink(LinkAnnotation.Url(url = "https://github.com/ollama/ollama")) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append("github.com/ollama/ollama")
                }
            }
            append(" ")
            append(orOtherServiceText)
            append(" ")
            withLink(LinkAnnotation.Url(url = "https://docs.litellm.ai/docs/providers")) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append(providersText)
                }
            }
        }
    }
    Text(
        annotatedString,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(Modifier.height(16.dp))

    if (connectionStatus == ConnectionStatus.Connected || models.isNotEmpty()) {
        ModelSelection(selectedModel, models, serviceName, onSelectModel, modelBenchmarks, serviceId)
        Spacer(Modifier.height(8.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleUseCustomModel(!useCustomModel) }
            .handCursor(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = useCustomModel,
            onCheckedChange = onToggleUseCustomModel,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.settings_custom_model_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    if (useCustomModel) {
        Spacer(Modifier.height(8.dp))
        KaiClearableTextField(
            value = customModelId,
            onValueChange = onChangeCustomModelId,
            label = {
                Text(
                    stringResource(Res.string.settings_model_label),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            singleLine = true,
        )
        Text(
            text = stringResource(Res.string.settings_custom_model_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
        )
    }
}

@Composable
private fun LiteRTSettings(
    selectedModel: SettingsModel?,
    downloadedModels: ImmutableList<SettingsModel>,
    availableModels: ImmutableList<LocalModel>,
    importedModels: ImmutableList<LocalModel>,
    totalDeviceMemoryBytes: Long,
    freeSpaceBytes: Long,
    downloadingModelId: String?,
    downloadProgress: Float?,
    downloadError: DownloadError?,
    importingFileName: String?,
    importProgress: Float?,
    importError: ModelImportError?,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (LocalModel) -> Unit,
    onCancelDownload: () -> Unit,
    onImportModel: (PlatformFile) -> Unit,
    onCancelImport: () -> Unit,
    onDeleteModel: (String) -> Unit,
    onChangeModelContextTokens: (String, Int) -> Unit,
    modelContextTokens: ImmutableMap<String, Int>,
) {
    val downloadedIds = remember(downloadedModels) { downloadedModels.map { it.id }.toSet() }
    val isBusy = downloadingModelId != null || importingFileName != null
    val isPreview = LocalInspectionMode.current

    val filePickerLauncher = if (!isPreview) {
        rememberFilePickerLauncher(
            type = FileKitType.File(extensions = listOf("litertlm")),
        ) { file ->
            if (file != null) onImportModel(file)
        }
    } else {
        null
    }

    Text(
        text = stringResource(Res.string.litert_on_device_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(4.dp))

    Text(
        text = stringResource(Res.string.litert_tool_support),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(Res.string.litert_import_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { filePickerLauncher?.launch() },
        modifier = Modifier.handCursor(),
        enabled = !isBusy && filePickerLauncher != null,
    ) {
        Text(stringResource(Res.string.litert_import))
    }

    if (importingFileName != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.litert_importing) + " $importingFileName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (importProgress != null) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { importProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${(importProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onCancelImport,
                    modifier = Modifier.handCursor(),
                ) {
                    Text(
                        text = stringResource(Res.string.litert_cancel),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }

    if (importError != null && importingFileName == null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (importError) {
                    ModelImportError.INVALID_EXTENSION -> Res.string.litert_error_import_invalid
                    ModelImportError.NOT_ENOUGH_DISK_SPACE -> Res.string.litert_error_not_enough_disk_space
                    ModelImportError.FILE_TOO_SMALL -> Res.string.litert_error_import_too_small
                    ModelImportError.COPY_FAILED -> Res.string.litert_error_import_failed
                    ModelImportError.CANCELLED -> Res.string.litert_error_import_failed
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    Spacer(Modifier.height(12.dp))

    availableModels.forEach { model ->
        LocalModelCard(
            model = model,
            isDownloaded = model.id in downloadedIds,
            isSelected = selectedModel?.id == model.id,
            isDownloading = downloadingModelId == model.id,
            downloadProgress = downloadProgress,
            isBusy = isBusy,
            totalDeviceMemoryBytes = totalDeviceMemoryBytes,
            modelContextTokens = modelContextTokens,
            onSelectModel = onSelectModel,
            onDownloadModel = onDownloadModel,
            onCancelDownload = onCancelDownload,
            onDeleteModel = onDeleteModel,
            onChangeModelContextTokens = onChangeModelContextTokens,
            showImportedBadge = false,
        )
    }

    if (importedModels.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.litert_imported),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        importedModels.forEach { model ->
            LocalModelCard(
                model = model,
                isDownloaded = true,
                isSelected = selectedModel?.id == model.id,
                isDownloading = false,
                downloadProgress = null,
                isBusy = isBusy,
                totalDeviceMemoryBytes = totalDeviceMemoryBytes,
                modelContextTokens = modelContextTokens,
                onSelectModel = onSelectModel,
                onDownloadModel = onDownloadModel,
                onCancelDownload = onCancelDownload,
                onDeleteModel = onDeleteModel,
                onChangeModelContextTokens = onChangeModelContextTokens,
                showImportedBadge = true,
            )
        }
    }

    if (downloadError != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (downloadError) {
                    DownloadError.NOT_ENOUGH_DISK_SPACE -> Res.string.litert_error_not_enough_disk_space
                    DownloadError.NETWORK_ERROR -> Res.string.litert_error_network
                    DownloadError.DOWNLOAD_INCOMPLETE -> Res.string.litert_error_download_incomplete
                    DownloadError.CHECKSUM_MISMATCH -> Res.string.litert_error_checksum_mismatch
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    Spacer(Modifier.height(8.dp))

    Text(
        text = stringResource(Res.string.litert_free_space, formatFileSize(freeSpaceBytes)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LocalModelCard(
    model: LocalModel,
    isDownloaded: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    isBusy: Boolean,
    totalDeviceMemoryBytes: Long,
    modelContextTokens: ImmutableMap<String, Int>,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (LocalModel) -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: (String) -> Unit,
    onChangeModelContextTokens: (String, Int) -> Unit,
    showImportedBadge: Boolean,
) {
    val steps = (model.maxContextTokens - model.defaultContextTokens) / 1024
    val storedContextTokens = modelContextTokens[model.id] ?: model.defaultContextTokens
    var contextSliderValue by remember(storedContextTokens) {
        mutableStateOf(((storedContextTokens - model.defaultContextTokens) / 1024).toFloat())
    }
    val contextTokens = model.defaultContextTokens + (contextSliderValue.roundToInt() * 1024)
    val estimatedMemoryMb = estimateGpuMemoryMb(model, contextTokens)
    val performance = calculateDevicePerformance(totalDeviceMemoryBytes, estimatedMemoryMb)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (isSelected) 3.dp else 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isDownloaded) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectModel(model.id) },
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            model.isRecommended ->
                                "${model.displayName} (${stringResource(Res.string.litert_recommended)})"

                            showImportedBadge ->
                                "${model.displayName} (${stringResource(Res.string.litert_imported)})"

                            else -> model.displayName
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatFileSize(model.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        DevicePerformanceLabel(performance)
                    }
                }
                if (isDownloaded) {
                    IconButton(
                        onClick = { onDeleteModel(model.id) },
                        modifier = Modifier.handCursor(),
                        enabled = !isBusy,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (!isDownloading) {
                    TextButton(
                        onClick = { onDownloadModel(model) },
                        modifier = Modifier.handCursor(),
                        enabled = !isBusy,
                    ) {
                        Text(stringResource(Res.string.litert_download))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.litert_context_size, "${contextTokens / 1024}K"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KaiSlider(
                value = contextSliderValue,
                onValueChange = { contextSliderValue = it },
                onValueChangeFinished = {
                    onChangeModelContextTokens(model.id, contextTokens)
                },
                valueRange = 0f..steps.toFloat().coerceAtLeast(0f),
                steps = (steps - 1).coerceAtLeast(0),
            )
            if (isDownloading && downloadProgress != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onCancelDownload,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = stringResource(Res.string.litert_cancel),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicePerformanceLabel(performance: DevicePerformance) {
    when (performance) {
        DevicePerformance.GOOD -> Text(
            text = stringResource(Res.string.litert_performance_good),
            style = MaterialTheme.typography.labelSmall,
            color = StatusColorConnected,
        )

        DevicePerformance.OK -> Text(
            text = stringResource(Res.string.litert_performance_ok),
            style = MaterialTheme.typography.labelSmall,
            color = StatusColorChecking,
        )

        DevicePerformance.POOR -> Text(
            text = stringResource(Res.string.litert_performance_poor),
            style = MaterialTheme.typography.labelSmall,
            color = StatusColorError,
        )
    }
}

@Composable
private fun ConnectionStatusIndicator(status: ConnectionStatus, onOpenAppPermissionSettings: () -> Unit = {}) {
    when (status) {
        ConnectionStatus.Unknown -> return

        ConnectionStatus.Checking -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_checking),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ConnectionStatus.Connected -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_connected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        ConnectionStatus.ErrorQuotaExhausted -> {
            val warningColor = Color(0xFFFF9800)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = warningColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_error_quota_exhausted),
                    style = MaterialTheme.typography.bodySmall,
                    color = warningColor,
                )
            }
        }

        ConnectionStatus.ErrorInvalidKey,
        ConnectionStatus.ErrorRateLimited,
        ConnectionStatus.ErrorConnectionFailed,
        ConnectionStatus.ErrorLocalNetworkDenied,
        ConnectionStatus.Error,
        -> {
            val errorMessage = when (status) {
                ConnectionStatus.ErrorInvalidKey -> stringResource(Res.string.settings_status_error_invalid_key)
                ConnectionStatus.ErrorRateLimited -> stringResource(Res.string.settings_status_error_rate_limited)
                ConnectionStatus.ErrorConnectionFailed -> stringResource(Res.string.settings_status_error_connection_failed)
                ConnectionStatus.ErrorLocalNetworkDenied -> stringResource(Res.string.settings_status_error_local_network)
                else -> stringResource(Res.string.settings_status_error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (status == ConnectionStatus.ErrorLocalNetworkDenied) {
                TextButton(
                    onClick = onOpenAppPermissionSettings,
                    modifier = Modifier.handCursor(),
                ) {
                    Text(stringResource(Res.string.settings_open_app_settings))
                }
            }
        }
    }
}

@Composable
private fun OpenCodeTerminalControls(
    thinking: Boolean,
    mode: String,
    onToggleThinking: (Boolean) -> Unit,
    onChangeMode: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "opencode 终端调节",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Thinking（思考）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = thinking,
                onCheckedChange = onToggleThinking,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Agent 模式",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onChangeMode(if (mode == "plan") "build" else "plan") },
                modifier = Modifier.handCursor(),
            ) {
                Text(if (mode == "plan") "plan（只规划）" else "build（执行）")
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    labelText: String,
    testTag: String? = null,
    singleLine: Boolean = false,
) {
    KaiClearableTextField(
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
        value = apiKey,
        onValueChange = onChangeApiKey,
        label = {
            Text(
                labelText,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        singleLine = singleLine,
    )
}

@Composable
private fun BenchmarkRow(bm: ModelBenchmark) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = bm.modelLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = buildString {
                append(bm.totalScore.roundToInt())
                if (bm.isUserScore && bm.note != null) append(" · ${bm.note}")
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(benchmarkScoreColor(bm.totalScore)),
        )
    }
}

@Composable
private fun ModelBenchmarkCard(
    benchmarks: ImmutableList<ModelBenchmark>,
    isRunning: Boolean,
    progress: Float,
    currentLabel: String,
    done: Boolean,
    summary: String = "",
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = kaiAdaptiveCardColors(),
        border = kaiAdaptiveCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "模型测试",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "一键统一测试所有已配置模型（并行发起，每模型限时 60 秒）：简单提问后按 完成度/速度/响应速度/字数 加权打分（0-100），分数越绿越好。结束后给出结果统计。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            when {
                isRunning -> {
                    Text(
                        text = currentLabel.ifBlank { "测试中…" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onCancel, modifier = Modifier.handCursor()) {
                        Text("取消")
                    }
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onRun, modifier = Modifier.handCursor()) {
                            Text(if (done) "重新测试" else "测试所有模型")
                        }
                        if (benchmarks.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = onClear, modifier = Modifier.handCursor()) {
                                Text("清除结果")
                            }
                        }
                    }
                    if (done && benchmarks.isEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "没有可测试的模型（请先添加服务并填入 API Key）。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (benchmarks.isNotEmpty()) {
                        var sortReverse by remember { mutableStateOf(false) }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(thickness = 0.5.dp)
                        if (done && summary.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { sortReverse = !sortReverse }, modifier = Modifier.handCursor()) {
                                Text(if (sortReverse) "字母反序" else "字母正序")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        val sorted = remember(benchmarks, sortReverse) {
                            val list = benchmarks.sortedBy { it.modelLabel.lowercase() }
                            if (sortReverse) list.reversed() else list
                        }
                        val zeroScores = sorted.filter { it.totalScore <= 0.0 }
                        val nonZero = sorted.filter { it.totalScore > 0.0 }
                        val grouped = nonZero.groupBy { it.serviceId.ifBlank { it.modelLabel.substringBefore('/') } }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            grouped.forEach { (group, items) ->
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                items.forEach { bm -> BenchmarkRow(bm) }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                            if (zeroScores.isNotEmpty()) {
                                Text(
                                    text = "零分模型",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                zeroScores.forEach { bm -> BenchmarkRow(bm) }
                            }
                        }
                    }
                }
            }
        }
    }
}
