package com.quranapp.android.compose.screens.onboarding

import com.quranapp.android.compose.utils.ThemeUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.RadioItem
import kotlinx.coroutines.launch
import verticalFadingEdge

@Composable
fun OnboardingThemePage() {
    val themeMode = ThemeUtils.observeThemeMode()
    val scope = rememberCoroutineScope()

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
        }
    }
}
