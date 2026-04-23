package com.quranapp.android.compose.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SwitchItem(
    modifier: Modifier = Modifier,
    title: Int,
    subtitle: Int? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled) {
                if (enabled) onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        ListItemContent(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f)
        )
        Switch(
            modifier = Modifier
                .padding(start = 16.dp)
                .height(24.dp),
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}