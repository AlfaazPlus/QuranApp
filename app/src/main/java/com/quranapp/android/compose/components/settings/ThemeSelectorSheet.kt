package com.quranapp.android.compose.components.settings

import ThemeUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.dialogs.BottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ThemeSelectorSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val themeMode = ThemeUtils.observeThemeMode()

    val items = listOf(
        Triple(
            ThemeUtils.THEME_MODE_DEFAULT,
            R.string.strLabelSystemDefault,
            R.string.strMsgThemeDefault
        ),
        Triple(ThemeUtils.THEME_MODE_DARK, R.string.strLabelThemeDark, R.string.strMsgThemeDark),
        Triple(ThemeUtils.THEME_MODE_LIGHT, R.string.strLabelThemeLight, null),
    )

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = R.drawable.dr_icon_theme,
        title = stringResource(R.string.strTitleTheme),
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            items.forEach { (theme, title, description) ->
                RadioItem(
                    title = title,
                    subtitle = description,
                    selected = themeMode == theme,
                    onClick = {
                        coroutineScope.launch {
                            ThemeUtils.setThemeMode(theme)

                            withContext(Dispatchers.Main) {
                                onDismiss()
                            }
                        }
                    }
                )
            }
        }
    }
}