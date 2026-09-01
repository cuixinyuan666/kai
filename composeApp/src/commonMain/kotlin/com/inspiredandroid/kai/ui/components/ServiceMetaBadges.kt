package com.inspiredandroid.kai.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.Service

@Composable
fun ServiceMetaBadges(service: Service, modifier: Modifier = Modifier) {
    val labels = buildList {
        if (service.noNeedKey) add("NO NEED KEY")
        service.sourceLabel?.let { add(it) }
        if (service is Service.OpenCode) add("API")
        if (service is Service.OpenCodeTerminal) add("终端")
    }
    if (labels.isEmpty()) return
    Row(
        modifier = modifier.padding(start = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEach { label ->
            val isKeyless = label == "NO NEED KEY"
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isKeyless) Color(0xFF1B5E20) else MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isKeyless) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
