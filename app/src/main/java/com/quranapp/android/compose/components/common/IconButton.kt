package com.quranapp.android.compose.components.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp


@Composable
fun IconButton(
    painter: Painter,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = tint,
        disabledContainerColor = Color.Transparent
    ),
    shape: RoundedCornerShape = RoundedCornerShape(100),
    enabled: Boolean = true,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(
            if (small) 36.dp else 48.dp,
        ),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        enabled = enabled,
        colors = colors,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(
                if (small) 20.dp else 24.dp,
            ),
        )
    }
}