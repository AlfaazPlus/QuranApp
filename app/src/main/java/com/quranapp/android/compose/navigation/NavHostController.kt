package com.quranapp.android.compose.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

val LocalSettingsNavHostController = compositionLocalOf<NavHostController> {
    error("NavHostController is not provided")
}