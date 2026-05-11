package com.quranapp.android.compose.theme.colors

import androidx.compose.material3.ColorScheme

abstract class BaseColors {
    abstract fun lightColors(): ColorScheme
    abstract fun darkColors(): ColorScheme
}