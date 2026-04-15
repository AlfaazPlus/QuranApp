package com.quranapp.android.compose.screens.settings

import android.content.Intent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quranapp.android.compose.navigation.LocalSettingsNavHostController
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.utils.univ.Keys


val enterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(durationMillis = 100)
)
val exitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(durationMillis = 100)
)
val popEnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(durationMillis = 100)
)
val popExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(durationMillis = 100)
)

private fun NavGraphBuilder.route(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition },
        content = content
    )
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

    CompositionLocalProvider(LocalSettingsNavHostController provides navController) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
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
                route(SettingRoutes.RECITATION_DOWNLOAD) { RecitationDownloadScreen() }
                route(SettingRoutes.APP_LOGS) {
                    AppLogsScreen()
                }
            }
        }
    }
}