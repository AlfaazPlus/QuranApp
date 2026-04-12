package com.quranapp.android.compose.components.common

import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Chip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = label,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colorScheme.primary,
            selectedLabelColor = colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = colorScheme.outlineVariant,
        ),
    )
}