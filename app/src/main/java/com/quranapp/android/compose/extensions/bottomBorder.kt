package com.quranapp.android.compose.extensions

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.theme.alpha

@Composable
fun Modifier.bottomBorder(width: Dp = 1.dp, color: Color = colorScheme.outline.alpha(0.3f)) = this.then(
    Modifier.drawBehind {
        val strokeWidth = width.toPx()

        drawLine(
            color = color,
            start = Offset(0f, size.height - strokeWidth / 2),
            end = Offset(size.width, size.height - strokeWidth / 2),
            strokeWidth = strokeWidth
        )
    }
)