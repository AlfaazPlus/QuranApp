package com.quranapp.android.compose.components.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListItemCategoryLabel(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp),
        color = MaterialTheme.colorScheme.primary,
    )
}
