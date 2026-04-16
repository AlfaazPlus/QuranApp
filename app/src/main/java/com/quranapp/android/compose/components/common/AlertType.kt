package com.quranapp.android.compose.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.theme.alpha

sealed class AlertType {
    data object Info : AlertType()
    data object Error : AlertType()
}

@Composable
private fun getBackgroundColorForAlertType(type: AlertType): Color {
    return when (type) {
        AlertType.Info -> colorScheme.surface
        AlertType.Error -> colorScheme.errorContainer
    }
}

@Composable
private fun getColorForAlertType(type: AlertType): Color {
    return when (type) {
        AlertType.Info -> colorScheme.onSurface
        AlertType.Error -> colorScheme.onErrorContainer
    }
}

@Composable
fun AlertCard(
    modifier: Modifier = Modifier,
    type: AlertType = AlertType.Info,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val shape = shapes.large

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = shape,
                spotColor = Color.Black.alpha(0.2f),
            ),
        color = getBackgroundColorForAlertType(type),
        contentColor = getColorForAlertType(type),
        shape = shape,
        border = BorderStroke(1.dp, getColorForAlertType(type).copy(alpha = 0.1f)),
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) { content() }
    }
}