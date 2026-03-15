package com.quranapp.android.compose.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun StatusBar(
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    val insets = WindowInsets.statusBars

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                insets
                    .asPaddingValues()
                    .calculateTopPadding()
            )
            .background(backgroundColor)
    )
}