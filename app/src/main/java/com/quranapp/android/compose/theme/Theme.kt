package com.quranapp.android.compose.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.alfaazplus.sunnah.ui.theme.getAppTypography
import com.quranapp.android.compose.utils.LocalAppLocale
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.appLocaleFlow


@Composable
fun QuranAppTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current

    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val colorScheme = ThemeUtils.observeColorScheme(context, isDarkTheme)
    val appLocale by appLocaleFlow.collectAsState()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window


            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppLocale provides appLocale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getAppTypography(),
            content = content
        )
    }

}

