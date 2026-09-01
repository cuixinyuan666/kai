package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * 鼠标悬停时在按钮外侧显示提示。提示出现在按钮下方（空间不够则改到上方），
 * 不覆盖按钮；顶部保留一条透明连接带，避免鼠标移入提示时闪烁。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HoverTooltip(
    tooltip: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (tooltip.isBlank()) {
        Box(modifier = modifier) { content() }
        return
    }
    val buttonInteraction = remember { MutableInteractionSource() }
    val tooltipInteraction = remember { MutableInteractionSource() }
    val buttonHovered by buttonInteraction.collectIsHoveredAsState()
    val tooltipHovered by tooltipInteraction.collectIsHoveredAsState()
    val showTooltip = buttonHovered || tooltipHovered
    val density = LocalDensity.current
    val bridgePx = with(density) { 8.dp.roundToPx() }
    var placeBelow by remember { mutableStateOf(true) }
    val positionProvider = remember(bridgePx) {
        BelowOrAboveAnchorPositionProvider(overlapPx = bridgePx) { below ->
            placeBelow = below
        }
    }
    Box(
        modifier = modifier.hoverable(interactionSource = buttonInteraction),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (showTooltip) {
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(focusable = false, clippingEnabled = false),
            ) {
                Column(
                    modifier = Modifier
                        .hoverable(interactionSource = tooltipInteraction)
                        .widthIn(max = 280.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!placeBelow) {
                        TooltipBubble(tooltip)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )
                    if (placeBelow) {
                        TooltipBubble(tooltip)
                    }
                }
            }
        }
    }
}

@Composable
private fun TooltipBubble(tooltip: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = tooltip,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private class BelowOrAboveAnchorPositionProvider(
    private val overlapPx: Int = 10,
    private val onPlaceBelow: (Boolean) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val belowY = anchorBounds.bottom - overlapPx
        val fitsBelow = belowY + popupContentSize.height <= windowSize.height
        onPlaceBelow(fitsBelow)
        val y = if (fitsBelow) {
            belowY
        } else {
            (anchorBounds.top - popupContentSize.height + overlapPx).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}
