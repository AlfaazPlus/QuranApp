package com.quranapp.android.compose.components.homepage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R


@Composable
fun HomeSectionHeader(
    icon: Int,
    title: Int,
    iconTint: Color? = colorScheme.primary,
    onViewAllClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconTint != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = stringResource(title),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
            style = typography.titleSmall,
            color = colorScheme.onSurface,
        )

        if (onViewAllClick != null) {
            TextButton(
                onClick = onViewAllClick,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.primary.copy(0.1f)
                ),
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = shapes.small
            ) {
                Text(
                    text = stringResource(R.string.strLabelViewAll),
                    style = typography.labelSmall
                )
            }
        }
    }
}