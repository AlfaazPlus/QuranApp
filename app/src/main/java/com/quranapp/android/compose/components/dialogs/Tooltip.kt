package com.quranapp.android.compose.components.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class SimpleTooltipPosition {
    Above,
    Below,
    Start,
    End,
    Left,
    Right,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTooltip(
    text: String,
    position: SimpleTooltipPosition = SimpleTooltipPosition.Below,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (text.isEmpty() || !enabled) {
        content()
        return
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = when (position) {
                SimpleTooltipPosition.Above -> TooltipAnchorPosition.Above
                SimpleTooltipPosition.Below -> TooltipAnchorPosition.Below
                SimpleTooltipPosition.Start -> TooltipAnchorPosition.Start
                SimpleTooltipPosition.End -> TooltipAnchorPosition.End
                SimpleTooltipPosition.Left -> TooltipAnchorPosition.Left
                SimpleTooltipPosition.Right -> TooltipAnchorPosition.Right
            },
            12.dp
        ),
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