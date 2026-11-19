package com.quranapp.android.utils.miscellenous

expect object PlatformUtils {
    val density: Float
    val screenWidthPx: Int
    val screenHeightPx: Int
}

val PlatformUtils.screenWidthDp: Float
    get() = screenWidthPx / density

val PlatformUtils.screenHeightDp: Float
    get() = screenHeightPx / density

fun Float.dpToPx(): Float = this * PlatformUtils.density

fun Float.pxToDp(): Float = this / PlatformUtils.density
