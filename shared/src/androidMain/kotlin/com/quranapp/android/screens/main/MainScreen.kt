package com.quranapp.android.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quranapp.android.components.MainBottomNavigationBar
import com.quranapp.android.components.mainBottomNavigationOuterHeight
import com.quranapp.android.components.miscellenous.LastCrashReportDialog
import com.quranapp.android.components.player.RecitationPlayerSheet
import com.quranapp.android.extensions.enterTransition
import com.quranapp.android.extensions.exitTransition
import com.quranapp.android.extensions.popEnterTransition
import com.quranapp.android.extensions.popExitTransition
import com.quranapp.android.extensions.predictivePopEnterTransition
import com.quranapp.android.extensions.predictivePopExitTransition
import com.quranapp.android.navigation.MainRoutes

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    LastCrashReportDialog()

    Box(Modifier.fillMaxSize()) {
        Column {
            NavHost(
                navController = navController,
                startDestination = MainRoutes.HOME,
                modifier = Modifier.weight(1f),
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition },
                predictivePopEnterTransition = predictivePopEnterTransition,
                predictivePopExitTransition = predictivePopExitTransition,
            ) {
                composable(MainRoutes.HOME) { HomeScreen() }
                composable(MainRoutes.LIBRARY) { LibraryScreen() }
            }

            MainBottomNavigationBar(navController)
        }

        RecitationPlayerSheet(
            collapsedBottomInset = mainBottomNavigationOuterHeight(),
        )
    }
}
