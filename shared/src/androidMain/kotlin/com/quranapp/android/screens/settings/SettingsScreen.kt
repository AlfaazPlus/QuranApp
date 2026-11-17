package com.quranapp.android.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quranapp.android.extensions.predictivePopEnterTransition
import com.quranapp.android.extensions.predictivePopExitTransition
import com.quranapp.android.extensions.route
import com.quranapp.android.navigation.SettingRoutes
import com.quranapp.android.utils.miscellenous.Keys


val LocalSettingsNavController = compositionLocalOf<NavHostController> {
    error("NavHostController is not provided")
}

@Composable
fun SettingsScreen(intent: Intent?, isNewIntent: Boolean) {
    val navController = rememberNavController()

    LaunchedEffect(intent, isNewIntent) {
        if (!isNewIntent) return@LaunchedEffect

        val startDestination = intent?.getStringExtra(Keys.NAV_DESTINATION)

        if (startDestination != null) {
            navController.navigate(startDestination) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val startDestination = intent?.getStringExtra(Keys.NAV_DESTINATION)
        ?: SettingRoutes.MAIN.arg(false)

    CompositionLocalProvider(LocalSettingsNavController provides navController) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = startDestination,
                predictivePopEnterTransition = predictivePopEnterTransition,
                predictivePopExitTransition = predictivePopExitTransition,
            ) {
                route(
                    SettingRoutes.MAIN(), arguments = listOf(
                        navArgument(Keys.SHOW_READER_SETTINGS_ONLY) { type = NavType.BoolType },
                    )
                ) { rsEntry ->
                    SettingsMainScreen(
                        rsEntry.arguments?.getBoolean(Keys.SHOW_READER_SETTINGS_ONLY) ?: false
                    )
                }
                route(SettingRoutes.LANGUAGE) { LanguageSelectionScreen() }
                route(SettingRoutes.THEME) { SettingsThemeScreen() }
                route(SettingRoutes.TRANSLATIONS) { TranslationSelectionScreen() }
                route(SettingRoutes.TRANSLATIONS_DOWNLOAD) { TranslationDownloadScreen() }
                route(SettingRoutes.TAFSIR) { TafsirSelectionScreen() }
                route(SettingRoutes.SCRIPT) { ScriptsScreen() }
                route(SettingRoutes.WWB) { SettingsWbwScreen() }
                route(SettingRoutes.RECITATION_DOWNLOAD) { RecitationDownloadScreen() }
                route(SettingRoutes.ADVANCED_SETTINGS) { AdvancedSettingsScreen() }
                route(SettingRoutes.APP_LOGS) { AppLogsScreen() }
            }
        }
    }
}
