package com.quranapp.android.compose.screens.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quranapp.android.compose.components.MainBottomNavigationBar
import com.quranapp.android.compose.components.mainBottomNavigationOuterHeight
import com.quranapp.android.compose.components.player.RecitationPlayerSheet
import com.quranapp.android.utils.univ.MainRoutes


val enterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(durationMillis = 100),
)
val exitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(durationMillis = 100),
)
val popEnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(durationMillis = 100),
)
val popExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(durationMillis = 100),
)

val predictivePopEnterTransition:
    AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition =
    { _ -> popEnterTransition }

val predictivePopExitTransition:
    AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition =
    { _ -> popExitTransition }

@Composable
fun MainScreen() {
    val navController = rememberNavController()

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
