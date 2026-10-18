/*
 * --------------------------------------------------------------------------
 * Project: SunnahApp
 * Created by Faisal Khan (http://github.com/faisalcodes) on July 06, 2023
 *
 * Copyright (c) 2023 All rights reserved.
 * --------------------------------------------------------------------------
 */


package com.quranapp.android.utils.univ

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

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
}
