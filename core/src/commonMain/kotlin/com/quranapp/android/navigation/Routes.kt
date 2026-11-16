package com.quranapp.android.navigation

import com.quranapp.android.utils.miscellenous.Keys

data class SingleArgRoute(private val route: String, private val argName: String) {
    operator fun invoke() = "$route/{$argName}"

    fun arg(arg: Any) = "$route/$arg"
}

data class MultiArgRoute(private val route: String, private val argNames: List<String>) {
    operator fun invoke() = "$route?${argNames.joinToString("&") { "$it={$it}" }}"

    fun args(vararg args: Any) = "$route?${
        argNames
            .zip(args)
            .joinToString("&") { "${it.first}=${it.second}" }
    }"
}

object MainRoutes {
    const val HOME = "home"
    const val LIBRARY = "library"
}

object CollectionRoutes {
    const val HOME = "home"
    val DETAIL = SingleArgRoute("detail", Keys.COLLECTION_ID)
}
