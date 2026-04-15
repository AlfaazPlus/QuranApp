package com.quranapp.android.compose.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun RadioItem(
    modifier: Modifier = Modifier,
    title: Int? = null,
    titleStr: String? = null,
    subtitle: Int? = null,
    subtitleStr: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        ListItemContent(
            title = title,
            titleStr = titleStr,
            subtitle = subtitle,
            subtitleStr = subtitleStr,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp),
            enabled = enabled,
            selected = selected,
            onClick = onClick,
        )
    }
}