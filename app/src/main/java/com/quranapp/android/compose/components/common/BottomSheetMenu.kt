package com.quranapp.android.compose.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.components.dialogs.BottomSheet


@Composable
fun BottomSheetMenu(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    icon: Int? = null,
    title: String?,
    headerArrangement: Arrangement.Horizontal = Arrangement.Center,
    items: @Composable () -> Unit,
) {
    BottomSheet(
        title = title,
        icon = icon,
        isOpen = isOpen,
        headerArrangement = headerArrangement,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        ) {
            items()
        }
    }
}

@Composable
fun BottomSheetMenuItem(
    modifier: Modifier = Modifier,
    text: String,
    icon: Int? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Text(
            text = text,
            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
        )
    }
}