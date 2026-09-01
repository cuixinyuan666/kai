package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.components.CuiBranding
import com.inspiredandroid.kai.ui.handCursor
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.kai_build_open
import org.jetbrains.compose.resources.stringResource

private val TerminalGreenOnDark = Color(0xFF16C60C)
private val TerminalGreenOnLight = Color(0xFF13A10E)

@Composable
internal fun EmptyState(
    modifier: Modifier,
    isUsingSharedKey: Boolean,
    onOpenKaiBuild: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CuiBranding()
        if (onOpenKaiBuild != null) {
            Spacer(Modifier.height(16.dp))
            val terminalGreen = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                TerminalGreenOnDark
            } else {
                TerminalGreenOnLight
            }
            OutlinedButton(
                onClick = onOpenKaiBuild,
                modifier = Modifier.handCursor(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = terminalGreen),
                border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.6f)),
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(stringResource(Res.string.kai_build_open))
            }
        }
    }
}
