package com.quranapp.android.components.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.RadioItem
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_theme
import com.quranapp.android.generated.resources.strLabelSystemDefault
import com.quranapp.android.generated.resources.strLabelThemeDark
import com.quranapp.android.generated.resources.strLabelThemeLight
import com.quranapp.android.generated.resources.strMsgThemeDark
import com.quranapp.android.generated.resources.strMsgThemeDefault
import com.quranapp.android.generated.resources.strTitleTheme
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.observeThemeMode
import com.quranapp.android.theme.resolveThemeModeForDelegate
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ThemeSelectorSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val themeMode = ThemeUtils.observeThemeMode()

    val items = listOf(
        Triple(
            ThemeUtils.THEME_MODE_DEFAULT,
            Res.string.strLabelSystemDefault,
            Res.string.strMsgThemeDefault
        ),
        Triple(
            ThemeUtils.THEME_MODE_DARK,
            Res.string.strLabelThemeDark,
            Res.string.strMsgThemeDark
        ),
        Triple(ThemeUtils.THEME_MODE_LIGHT, Res.string.strLabelThemeLight, null),
    )

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_theme,
        title = stringResource(Res.string.strTitleTheme),
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
                        onDismiss()

                        coroutineScope.launch {
                            ThemeUtils.setThemeMode(theme)
                            AppCompatDelegate.setDefaultNightMode(
                                ThemeUtils.resolveThemeModeForDelegate(
                                    theme
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}
