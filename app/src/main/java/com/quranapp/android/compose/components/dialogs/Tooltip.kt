package com.quranapp.android.compose.components.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTooltip(
    text: String,
    content: @Composable () -> Unit
) {

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(12.dp),
        tooltip = {
            PlainTooltip {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        state = rememberTooltipState(),
        content = content,
    )
}