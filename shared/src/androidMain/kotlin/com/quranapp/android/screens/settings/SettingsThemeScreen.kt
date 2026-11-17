package com.quranapp.android.screens.settings

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.SwitchItem
import com.quranapp.android.components.settings.ListItemCategoryLabel
import com.quranapp.android.components.settings.SettingsItem
import com.quranapp.android.components.settings.SettingsThemeItem
import com.quranapp.android.components.settings.ThemeSelectorSheet
import com.quranapp.android.extensions.fullWidthColumn
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dynamic_color
import com.quranapp.android.generated.resources.dynamic_color_description
import com.quranapp.android.generated.resources.strTitleTheme
import com.quranapp.android.generated.resources.theme_amoled
import com.quranapp.android.generated.resources.theme_blue
import com.quranapp.android.generated.resources.theme_colors
import com.quranapp.android.generated.resources.theme_default
import com.quranapp.android.generated.resources.theme_mode
import com.quranapp.android.generated.resources.theme_mono
import com.quranapp.android.generated.resources.theme_purple
import com.quranapp.android.generated.resources.theme_red
import com.quranapp.android.generated.resources.theme_violet
import com.quranapp.android.generated.resources.theme_yellow
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.colors.BaseColors
import com.quranapp.android.theme.colors.ThemeAmoledColors
import com.quranapp.android.theme.colors.ThemeBlueColors
import com.quranapp.android.theme.colors.ThemeDefaultColors
import com.quranapp.android.theme.colors.ThemeMonoColors
import com.quranapp.android.theme.colors.ThemePurpleColors
import com.quranapp.android.theme.colors.ThemeRedColors
import com.quranapp.android.theme.colors.ThemeVioletColors
import com.quranapp.android.theme.colors.ThemeYellowColors
import com.quranapp.android.theme.isDynamicColorSupported
import com.quranapp.android.theme.observeDarkTheme
import com.quranapp.android.theme.observeIsDynamicColor
import com.quranapp.android.theme.observeThemeColor
import com.quranapp.android.theme.observeThemeMode
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class ThemeItem(
    val id: String,
    val title: StringResource,
    val color: BaseColors,
)

fun themeColorItems(): List<ThemeItem> {
    return listOf(
        ThemeItem(ThemeUtils.THEME_COLOR_DEFAULT, Res.string.theme_default, ThemeDefaultColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_AMOLED, Res.string.theme_amoled, ThemeAmoledColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_BLUE, Res.string.theme_blue, ThemeBlueColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_RED, Res.string.theme_red, ThemeRedColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_PURPLE, Res.string.theme_purple, ThemePurpleColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_VIOLET, Res.string.theme_violet, ThemeVioletColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_YELLOW, Res.string.theme_yellow, ThemeYellowColors()),
        ThemeItem(ThemeUtils.THEME_COLOR_MONO, Res.string.theme_mono, ThemeMonoColors()),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsThemeScreen() {
    val themeItems = themeColorItems()

    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val themeMode = ThemeUtils.observeThemeMode()
    val themeColor = ThemeUtils.observeThemeColor()
    val isDynamicColor = ThemeUtils.observeIsDynamicColor()
    var showThemeBottomSheet by rememberSaveable { mutableStateOf(false) }

    val span = 2

    Scaffold(
        topBar = { AppBar(title = stringResource(Res.string.strTitleTheme)) },
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
                    title = Res.string.theme_mode,
                    subtitle = ThemeUtils.resolveThemeModeLabel(themeMode),
                ) { showThemeBottomSheet = true }
            }

            if (ThemeUtils.isDynamicColorSupported()) {
                fullWidthColumn(span) {
                    SwitchItem(
                        title = Res.string.dynamic_color,
                        subtitle = Res.string.dynamic_color_description,
                        checked = isDynamicColor,
                    ) {
                        coroutineScope.launch {
                            ThemeUtils.setDynamicColor(it)
                        }
                    }
                }
            }

            if (!ThemeUtils.isDynamicColorSupported() || !isDynamicColor) {
                fullWidthColumn(span) { ListItemCategoryLabel(title = stringResource(Res.string.theme_colors)) }
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
