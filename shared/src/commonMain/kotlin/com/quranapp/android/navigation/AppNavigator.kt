package com.quranapp.android.navigation

import com.quranapp.android.utils.reader.ReaderLaunchParams

sealed class AppRoute {
    data class Reader(val params: ReaderLaunchParams) : AppRoute()
    data class Screen(
        val screen: String,
        val extras: Map<String, Any?> = emptyMap(),
    ) : AppRoute()
}

/** Platform-agnostic navigation contract. Register the platform impl via [PlatformServices]. */
interface AppNavigator {
    fun navigate(route: AppRoute)
}
