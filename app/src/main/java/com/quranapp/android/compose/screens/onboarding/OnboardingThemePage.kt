package com.quranapp.android.compose.screens.onboarding

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.common.SwitchItem
import com.quranapp.android.compose.components.settings.ListItemCategoryLabel
import com.quranapp.android.compose.components.settings.SettingsThemeItem
import com.quranapp.android.compose.screens.settings.themeColorItems
import com.quranapp.android.compose.utils.ThemeUtils
import kotlinx.coroutines.launch
import verticalFadingEdge

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
            R.string.strLabelSystemDefault,
            R.string.strMsgThemeDefault,
        ),
        Triple(
            ThemeUtils.THEME_MODE_DARK,
            R.string.strLabelThemeDark,
            R.string.strMsgThemeDark,
        ),
        Triple(
            ThemeUtils.THEME_MODE_LIGHT,
            R.string.strLabelThemeLight,
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
                        title = R.string.dynamic_color,
                        subtitle = R.string.dynamic_color_description,
                        checked = isDynamicColor,
                    ) {
                        scope.launch {
                            ThemeUtils.setDynamicColor(it)
                        }
                    }
                }

                if (!dynamicColorSupported || !isDynamicColor) {
                    ListItemCategoryLabel(title = stringResource(R.string.theme_colors))

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
