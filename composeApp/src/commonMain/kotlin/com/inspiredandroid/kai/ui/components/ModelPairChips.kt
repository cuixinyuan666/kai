package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

fun splitModelPairLabel(label: String): Pair<String, String?> {
    val idx = label.indexOf(" / ")
    return if (idx >= 0) {
        label.substring(0, idx).trim() to label.substring(idx + 3).trim().ifBlank { null }
    } else {
        label.trim() to null
    }
}

@Composable
fun ModelPairChipsFromLabel(
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val (parent, child) = splitModelPairLabel(label)
    ModelPairChips(parent = parent, child = child, modifier = modifier, compact = compact)
}

@Composable
fun ModelPairChips(
    parent: String,
    child: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(6.dp)
    val padH = if (compact) 6.dp else 8.dp
    val padV = if (compact) 2.dp else 4.dp
    val style = if (compact) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.labelMedium
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                text = parent,
                style = style,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = padH, vertical = padV),
            )
        }
        if (!child.isNullOrBlank()) {
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape),
            ) {
                Text(
                    text = child,
                    style = style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = padH, vertical = padV),
                )
            }
        }
    }
}
