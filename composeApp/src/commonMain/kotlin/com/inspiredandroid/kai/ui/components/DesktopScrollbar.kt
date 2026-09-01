package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
expect fun VerticalScrollbarForList(
    listState: LazyListState,
    modifier: Modifier = Modifier,
)

@Composable
expect fun VerticalScrollbarForScroll(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
)

@Composable
expect fun VerticalScrollbarForGrid(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
)

@Composable
expect fun HorizontalScrollbarForScroll(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
)

@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(end = 12.dp)
                .then(contentModifier),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
        VerticalScrollbarForScroll(
            scrollState = scrollState,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
