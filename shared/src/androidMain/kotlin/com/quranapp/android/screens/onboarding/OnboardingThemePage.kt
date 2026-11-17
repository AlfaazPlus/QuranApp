package com.quranapp.android.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.RadioItem
import com.quranapp.android.components.common.SwitchItem
import com.quranapp.android.components.settings.ListItemCategoryLabel
import com.quranapp.android.components.settings.SettingsThemeItem
import com.quranapp.android.extensions.verticalFadingEdge
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dynamic_color
import com.quranapp.android.generated.resources.dynamic_color_description
import com.quranapp.android.generated.resources.strLabelSystemDefault
import com.quranapp.android.generated.resources.strLabelThemeDark
import com.quranapp.android.generated.resources.strLabelThemeLight
import com.quranapp.android.generated.resources.strMsgThemeDark
import com.quranapp.android.generated.resources.strMsgThemeDefault
import com.quranapp.android.generated.resources.theme_colors
import com.quranapp.android.screens.settings.themeColorItems
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.isDynamicColorSupported
import com.quranapp.android.theme.observeDarkTheme
import com.quranapp.android.theme.observeIsDynamicColor
import com.quranapp.android.theme.observeThemeColor
import com.quranapp.android.theme.observeThemeMode
import com.quranapp.android.theme.resolveThemeModeForDelegate
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingThemePage() {
    val themeMode = ThemeUtils.observeThemeMode()
    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val themeColor = ThemeUtils.observeThemeColor()
    val isDynamicColor = ThemeUtils.observeIsDynamicColor()
    val scope = rememberCoroutineScope()
    val themeItems = themeColorItems()

    val items = listOf(
        Triple(
            ThemeUtils.THEME_MODE_DEFAULT,
            Res.string.strLabelSystemDefault,
            Res.string.strMsgThemeDefault,
        ),
        Triple(
            ThemeUtils.THEME_MODE_DARK,
            Res.string.strLabelThemeDark,
            Res.string.strMsgThemeDark,
        ),
        Triple(
            ThemeUtils.THEME_MODE_LIGHT,
            Res.string.strLabelThemeLight,
            null,
        ),
    )

    val scrollState = rememberScrollState()
    val dynamicColorSupported = ThemeUtils.isDynamicColorSupported()

    Box(
        Modifier.verticalFadingEdge(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(8.dp),
        ) {
            items.forEach { (mode, title, description) ->
                RadioItem(
                    title = title,
                    subtitle = description,
                    selected = themeMode == mode,
                    onClick = {
                        scope.launch {
                            ThemeUtils.setThemeMode(mode)
                            AppCompatDelegate.setDefaultNightMode(
                                ThemeUtils.resolveThemeModeForDelegate(mode),
                            )
                        }
                    },
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (dynamicColorSupported) {
                    SwitchItem(
                        title = Res.string.dynamic_color,
                        subtitle = Res.string.dynamic_color_description,
                        checked = isDynamicColor,
                    ) {
                        scope.launch {
                            ThemeUtils.setDynamicColor(it)
                        }
                    }
                }

                if (!dynamicColorSupported || !isDynamicColor) {
                    ListItemCategoryLabel(title = stringResource(Res.string.theme_colors))

                    themeItems.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    SettingsThemeItem(
                                        themeItem = item,
                                        isDarkTheme = isDarkTheme,
                                        currentThemeColor = themeColor,
                                    ) {
                                        scope.launch {
                                            ThemeUtils.setThemeColor(it)
                                        }
                                    }
                                }
                            }

                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
