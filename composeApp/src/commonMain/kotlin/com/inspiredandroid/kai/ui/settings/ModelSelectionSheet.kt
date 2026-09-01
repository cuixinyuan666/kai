package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.benchmarkScoreColor
import com.inspiredandroid.kai.formatContextWindow
import com.inspiredandroid.kai.formatReleaseDate
import com.inspiredandroid.kai.ui.KaiOutlinedTextField
import com.inspiredandroid.kai.ui.components.KaiSearchField
import com.inspiredandroid.kai.ui.components.ModelPairChips
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForList
import com.inspiredandroid.kai.ui.handCursor
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ic_arrow_drop_down
import kai.composeapp.generated.resources.ic_crown
import kai.composeapp.generated.resources.model_filter_free
import kai.composeapp.generated.resources.model_free_badge
import kai.composeapp.generated.resources.model_free_empty
import kai.composeapp.generated.resources.model_sort_context
import kai.composeapp.generated.resources.model_sort_date
import kai.composeapp.generated.resources.model_sort_score
import kai.composeapp.generated.resources.model_sort_name
import kai.composeapp.generated.resources.settings_model_label
import kai.composeapp.generated.resources.settings_model_search
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelSelection(
    currentSelectedModel: SettingsModel?,
    models: ImmutableList<SettingsModel>,
    serviceName: String = "",
    onClick: (String) -> Unit,
    modelBenchmarks: Map<String, Double> = emptyMap(),
    serviceId: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    // Provider list only — synthetic manual entries stay out of the picker grid.
    val pickerModels = remember(models) { models.filter { !it.isManualEntry } }
    if (pickerModels.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            KaiOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = currentSelectedModel?.let { " " } ?: "",
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(
                        stringResource(Res.string.settings_model_label),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                trailingIcon = {
                    Icon(
                        modifier = Modifier.handCursor(),
                        imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                },
            )
            currentSelectedModel?.let { selected ->
                val child = selected.displayName ?: selected.id
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 16.dp, end = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ModelPairChips(
                        parent = serviceName.ifBlank { child },
                        child = child.takeIf { serviceName.isNotBlank() && it != serviceName },
                        compact = true,
                    )
                }
            }
            // Transparent overlay to capture clicks reliably on all platforms
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .handCursor()
                    .clickable { expanded = true },
            )
        }
        if (expanded) {
            ModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onDismissRequest = {
                    expanded = false
                },
            ) {
                var searchQuery by remember { mutableStateOf("") }
                val hasFreeModels = remember(pickerModels) { pickerModels.any { it.isFreeTier } }
                var freeFilterOnly by remember { mutableStateOf(false) }
                // Reset free filter when the service has no free models.
                LaunchedEffect(hasFreeModels) {
                    if (!hasFreeModels) freeFilterOnly = false
                }
                val filteredModels = remember(pickerModels, searchQuery, freeFilterOnly) {
                    pickerModels.filter { model ->
                        val matchesFree = !freeFilterOnly || model.isFreeTier
                        val matchesSearch = searchQuery.isBlank() ||
                            model.id.contains(searchQuery, ignoreCase = true) ||
                            model.subtitle.contains(searchQuery, ignoreCase = true) ||
                            model.displayName?.contains(searchQuery, ignoreCase = true) == true
                        matchesFree && matchesSearch
                    }
                }
                if (pickerModels.size > 6) {
                    KaiSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = stringResource(Res.string.settings_model_search),
                    )
                }
                var sortOption by remember { mutableStateOf(ModelSortOption.Name) }
                val sortedModels = remember(filteredModels, sortOption, searchQuery) {
                    val base = filteredModels.sortedWith(sortOption.comparator)
                    if (searchQuery.isBlank()) {
                        base
                    } else {
                        base.sortedBy { model ->
                            val name = model.displayName ?: model.id
                            when {
                                name.contains(searchQuery, ignoreCase = true) -> 0
                                model.subtitle.contains(searchQuery, ignoreCase = true) -> 1
                                else -> 2
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModelSortOption.entries.forEach { option ->
                        FilterChip(
                            selected = sortOption == option,
                            onClick = { sortOption = option },
                            label = { Text(stringResource(option.labelRes)) },
                            modifier = Modifier.handCursor(),
                        )
                    }
                    if (hasFreeModels) {
                        FilterChip(
                            selected = freeFilterOnly,
                            onClick = { freeFilterOnly = !freeFilterOnly },
                            label = { Text(stringResource(Res.string.model_filter_free)) },
                            modifier = Modifier.handCursor(),
                        )
                    }
                }
                val listState = rememberLazyListState()
                LaunchedEffect(sortOption, freeFilterOnly) {
                    listState.requestScrollToItem(0)
                }
                if (sortedModels.isEmpty() && freeFilterOnly) {
                    Text(
                        text = stringResource(Res.string.model_free_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    // Multi-level presentation: the service is the 总类 (category header),
                    // each model is a 分支 listed beneath it (indented, selectable).
                    Box {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (serviceName.isNotBlank()) {
                                item {
                                    Text(
                                        text = serviceName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            items(sortedModels, key = { it.id }) { model ->
                                ModelCard(
                                    model = model,
                                    parentName = serviceName,
                                    isSelected = currentSelectedModel?.id == model.id,
                                    showCrown = (serviceName == "OpenCode API" || serviceName == "OpenCode 终端" ||
                                        serviceName == "opencode api" || serviceName == "opencode terminal") &&
                                        (model.id.endsWith("-free") || model.isFreeTier),
                                    score = if (serviceId.isBlank()) modelBenchmarks[model.id] else modelBenchmarks["$serviceId::${model.id}"],
                                    onClick = {
                                        onClick(model.id)
                                        expanded = false
                                    },
                                )
                            }
                        }
                        VerticalScrollbarForList(
                            listState = listState,
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

private enum class ModelSortOption(
    val labelRes: StringResource,
    val comparator: Comparator<SettingsModel>,
) {
    Name(Res.string.model_sort_name, compareBy<SettingsModel> { it.id }),
    Date(Res.string.model_sort_date, compareByDescending<SettingsModel> { it.releaseDate }.thenBy { it.id }),
    Score(Res.string.model_sort_score, compareByDescending<SettingsModel> { it.arenaScore }.thenBy { it.id }),
    Ctx(Res.string.model_sort_context, compareByDescending<SettingsModel> { it.contextWindow }.thenBy { it.id }),
}

@Composable
private fun ModelCard(
    model: SettingsModel,
    parentName: String,
    isSelected: Boolean,
    showCrown: Boolean = false,
    score: Double? = null,
    onClick: () -> Unit,
) {
    val displayName = model.displayName?.takeIf { it.isNotBlank() && it != model.id }
    val title = displayName ?: model.id
    val secondary = if (displayName == null && model.subtitle.isNotBlank()) model.subtitle else null
    val contextText = model.contextWindow?.let { formatContextWindow(it) }
    val releaseText = model.releaseDate?.let { formatReleaseDate(it) }
    val detailText = listOfNotNull(releaseText, model.parameterCount, contextText)
        .joinToString("  ·  ").ifEmpty { null }

    val secondaryColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.handCursor().clip(CardDefaults.shape).clickable { onClick() },
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val child = title
                ModelPairChips(
                    parent = parentName.ifBlank { child },
                    child = child.takeIf { parentName.isNotBlank() && it != parentName },
                    modifier = Modifier.weight(1f),
                )
                if (model.isFreeTier) {
                    Spacer(Modifier.width(8.dp))
                    FreeTierBadge(isSelected = isSelected)
                }
                if (showCrown) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_crown),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (score != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = score.roundToInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(benchmarkScoreColor(score)),
                    )
                }
                model.arenaScore?.let { arena ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$arena",
                        style = MaterialTheme.typography.labelSmall,
                        color = arenaScoreColor(arena),
                    )
                }
            }
            secondary?.let {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                )
            }
            detailText?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor,
                )
            }
        }
    }
}

private fun arenaScoreColor(score: Int): Color = when {
    score >= 1400 -> Color(0xFF2E7D32)
    score >= 1350 -> Color(0xFF558B2F)
    score >= 1300 -> Color(0xFF9E9D24)
    score >= 1250 -> Color(0xFFF9A825)
    else -> Color(0xFFEF6C00)
}

@Composable
private fun FreeTierBadge(isSelected: Boolean) {
    val background = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        Color(0xFF1B5E20)
    }
    val content = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.White
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = background,
    ) {
        Text(
            text = stringResource(Res.string.model_free_badge),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
