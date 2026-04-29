package com.quranapp.android.compose.screens.settings

import ThemeUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alfaazplus.sunnah.ui.theme.colors.BaseColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeBlueColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeDefaultColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeMonoColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemePurpleColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeRedColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeVioletColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeYellowColors
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.SwitchItem
import com.quranapp.android.compose.components.settings.ListItemCategoryLabel
import com.quranapp.android.compose.components.settings.SettingsItem
import com.quranapp.android.compose.components.settings.SettingsThemeItem
import com.quranapp.android.compose.components.settings.ThemeSelectorSheet
import com.quranapp.android.compose.extensions.fullWidthColumn
import kotlinx.coroutines.launch

data class ThemeItem(
    val id: String,
    val title: Int,
    val color: BaseColors
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsThemeScreen() {
    val themeItems = listOf(
        ThemeItem(ThemeUtils.THEME_COLOR_DEFAULT, R.string.theme_default, ThemeDefaultColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_BLUE, R.string.theme_blue, ThemeBlueColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_RED, R.string.theme_red, ThemeRedColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_PURPLE, R.string.theme_purple, ThemePurpleColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_VIOLET, R.string.theme_violet, ThemeVioletColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_YELLOW, R.string.theme_yellow, ThemeYellowColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_MONO, R.string.theme_mono, ThemeMonoColors()),
    )

    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val themeMode = ThemeUtils.observeThemeMode()
    val themeColor = ThemeUtils.observeThemeColor()
    val isDynamicColor = ThemeUtils.observeIsDynamicColor()
    var showThemeBottomSheet by rememberSaveable { mutableStateOf(false) }

    val span = 2

    Scaffold(
        topBar = { AppBar(title = stringResource(R.string.strTitleTheme)) },
    ) { paddingValues ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(span),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 16.dp,
                bottom = 150.dp
            ),
        ) {
            fullWidthColumn(span) {
                SettingsItem(
                    title = R.string.theme_mode,
                    subtitle = ThemeUtils.resolveThemeModeLabel(themeMode),
                ) { showThemeBottomSheet = true }
            }

            if (ThemeUtils.isDynamicColorSupported()) {
                fullWidthColumn(span) {
                    SwitchItem(
                        title = R.string.dynamic_color,
                        subtitle = R.string.dynamic_color_description,
                        checked = isDynamicColor,
                    ) {
                        coroutineScope.launch {
                            ThemeUtils.setDynamicColor(it)
                        }
                    }
                }
            }

            if (!ThemeUtils.isDynamicColorSupported() || !isDynamicColor) {
                fullWidthColumn(span) { ListItemCategoryLabel(title = stringResource(R.string.theme_colors)) }
                items(themeItems.size) { index ->
                    SettingsThemeItem(
                        themeItem = themeItems[index],
                        isDarkTheme = isDarkTheme,
                        currentThemeColor = themeColor,
                    ) {
                        coroutineScope.launch {
                            ThemeUtils.setThemeColor(it)
                        }
                    }
                }
            }
        }

        ThemeSelectorSheet(showThemeBottomSheet) { showThemeBottomSheet = false }
    }
}
