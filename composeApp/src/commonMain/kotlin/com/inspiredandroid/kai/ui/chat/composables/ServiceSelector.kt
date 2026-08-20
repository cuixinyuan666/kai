package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.inspiredandroid.kai.data.benchmarkScoreColor
import com.inspiredandroid.kai.data.ServiceEntry
import com.inspiredandroid.kai.data.ServiceModelOption
import com.inspiredandroid.kai.ui.handCursor
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ic_arrow_drop_down
import kai.composeapp.generated.resources.ic_crown
import kai.composeapp.generated.resources.ic_vip
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.roundToInt

/**
 * Chat model switcher rendered as a two-level (multi-level) dropdown.
 *
 * Level 1 — the 总类: each configured service (e.g. OpenCode).
 * Level 2 — the 分支: every model branch available under that service
 *           (e.g. opencode-hy3, opencode-deepseek v4), shown when the
 *           总类 is expanded. Picking a branch switches to that service and
 *           selects the model in one tap.
 */
@Composable
internal fun ServiceSelector(
    services: ImmutableList<ServiceEntry>,
    onSelectService: (String) -> Unit,
    onSelectModel: (String, String) -> Unit,
    modelBenchmarks: Map<String, Double> = emptyMap(),
) {
    if (services.isEmpty()) return

    val current = services.first()
    var expanded by remember { mutableStateOf(false) }
    var reversed by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .clickable { expanded = true }
                .handCursor(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vectorResource(current.icon),
                contentDescription = current.serviceName,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            val spacingPx = with(LocalDensity.current) { 8.dp.roundToPx() }
            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false),
                popupPositionProvider = remember(spacingPx) { AnchorAbovePositionProvider(spacingPx) },
            ) {
                BoxWithConstraints {
                    val maxMenuHeight = maxHeight - 24.dp
                    // 默认全部折叠：用户点击总类后才展开分支（原为自动展开全部）。
                    var expandedIds by remember(services) {
                        mutableStateOf(emptySet<String>())
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 240.dp)
                                .heightIn(max = maxMenuHeight)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { reversed = !reversed }) {
                                    Text(if (reversed) "Z-A" else "A-Z")
                                }
                            }
                            val sortedServices = services.sortedBy { it.serviceName }
                            val displayServices = if (reversed) sortedServices.asReversed() else sortedServices
                            displayServices.forEach { entry ->
                                val isCurrent = entry.instanceId == current.instanceId
                                val hasBranches = entry.modelOptions.isNotEmpty()
                                ServiceHeaderItem(
                                    entry = entry,
                                    isCurrent = isCurrent,
                                    isExpanded = entry.instanceId in expandedIds,
                                    onClick = {
                                        if (hasBranches) {
                                            expandedIds = if (entry.instanceId in expandedIds) {
                                                expandedIds - entry.instanceId
                                            } else {
                                                expandedIds + entry.instanceId
                                            }
                                        } else {
                                            expanded = false
                                            onSelectService(entry.instanceId)
                                        }
                                    },
                                )
                                if (entry.instanceId in expandedIds && hasBranches) {
                                    entry.modelOptions.sortedBy { it.label }.forEach { option ->
                                    ModelBranchItem(
                                        option = option,
                                        isSelected = entry.modelId == option.id,
                                        showCrown = (entry.serviceId == "opencode" || entry.serviceId == "opencode-terminal") && option.id.endsWith("-free"),
                                        score = modelBenchmarks["${entry.serviceId}::${option.id}"],
                                        onClick = {
                                                expanded = false
                                                onSelectModel(entry.instanceId, option.id)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceHeaderItem(
    entry: ServiceEntry,
    isCurrent: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val rowBackground = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val textColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subTextColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .handCursor()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = vectorResource(entry.icon),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = textColor,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.serviceName,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            if (entry.modelId.isNotEmpty()) {
                Text(
                    text = entry.modelId,
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor,
                )
            }
        }
        if (entry.serviceId == "opencode" || entry.serviceId == "opencode-terminal") {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_vip),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
        }
        if (entry.modelOptions.isNotEmpty()) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isExpanded) 180f else 0f),
                tint = textColor,
            )
        }
    }
}

@Composable
private fun ModelBranchItem(
    option: ServiceModelOption,
    isSelected: Boolean,
    showCrown: Boolean = false,
    score: Double? = null,
    onClick: () -> Unit,
) {
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .handCursor()
            .padding(start = 42.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                ),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (score != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = score.roundToInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(benchmarkScoreColor(score)),
            )
        }
        if (showCrown) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = vectorResource(Res.drawable.ic_crown),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private class AnchorAbovePositionProvider(
    private val verticalSpacing: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val above = anchorBounds.top - popupContentSize.height - verticalSpacing
        val y = if (above >= 0) {
            above
        } else {
            val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
            (anchorBounds.bottom + verticalSpacing).coerceAtMost(maxY)
        }
        return IntOffset(x, y)
    }
}
