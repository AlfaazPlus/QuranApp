package com.quranapp.android.compose.theme

import ThemeUtilsV2
import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.alfaazplus.sunnah.ui.theme.Typography


@Composable
fun QuranAppTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current

    val isDarkTheme = ThemeUtilsV2.isDarkTheme()
    val colorScheme = ThemeUtilsV2.getColorScheme(context, isDarkTheme)

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window


            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )

}

