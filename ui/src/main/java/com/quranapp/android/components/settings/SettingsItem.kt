package com.quranapp.android.components.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.ListItem
import com.quranapp.android.core.R

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    title: Int,
    subtitle: Int? = null,
    subtitleStr: String? = null,
    icon: Int? = null,
    iconImage: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        modifier = modifier,
        leading = {
            if (icon != null) SettingsItemIcon(
                icon = icon,
                contentDescription = stringResource(title)
            )
            else if (iconImage != null) iconImage()
        },
        trailing = {
            SettingsItemArrow()
        },
        title = title,
        subtitle = subtitle,
        subtitleStr = subtitleStr,
        onClick = onClick
    )
}

@Composable
fun SettingsItemIcon(
    icon: Int,
    contentDescription: String
) {
    Icon(
        modifier = Modifier.size(24.dp),
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
    )
}

@Composable
fun SettingsItemArrow() {
    Icon(
        painter = painterResource(id = R.drawable.dr_icon_chevron_right),
        contentDescription = null,
        modifier = Modifier.padding(start = 15.dp)
    )
}
