package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp

@Composable
private fun scrollbarStyle(): ScrollbarStyle {
    val onSurface = MaterialTheme.colorScheme.onSurface
    return remember(onSurface) {
        ScrollbarStyle(
            minimalHeight = 48.dp,
            thickness = 12.dp,
            shape = RoundedCornerShape(4.dp),
            hoverDurationMillis = 300,
            unhoverColor = onSurface.copy(alpha = 0.45f),
            hoverColor = onSurface.copy(alpha = 0.75f),
        )
    }
}

@Composable
actual fun VerticalScrollbarForList(
    listState: LazyListState,
    modifier: Modifier,
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(listState),
        modifier = modifier.fillMaxHeight().width(16.dp).pointerHoverIcon(PointerIcon.Hand),
        style = scrollbarStyle(),
    )
}

@Composable
actual fun VerticalScrollbarForScroll(
    scrollState: ScrollState,
    modifier: Modifier,
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .pointerHoverIcon(PointerIcon.Hand),
        style = scrollbarStyle(),
    )
}

@Composable
actual fun VerticalScrollbarForGrid(
    gridState: LazyGridState,
    modifier: Modifier,
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(gridState),
        modifier = modifier.fillMaxHeight().width(16.dp).pointerHoverIcon(PointerIcon.Hand),
        style = scrollbarStyle(),
    )
}

@Composable
actual fun HorizontalScrollbarForScroll(
    scrollState: ScrollState,
    modifier: Modifier,
) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier.fillMaxWidth().height(16.dp).pointerHoverIcon(PointerIcon.Hand),
        style = scrollbarStyle(),
    )
}
