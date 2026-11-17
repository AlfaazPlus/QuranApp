package com.quranapp.android.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidColorSpace

fun Color.toAndroidColor(): android.graphics.Color =
    android.graphics.Color.valueOf(red, green, blue, alpha, colorSpace.toAndroidColorSpace())
