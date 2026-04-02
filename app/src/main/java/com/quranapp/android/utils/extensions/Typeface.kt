package com.quranapp.android.utils.extensions

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

fun Typeface.asFontFamily(): FontFamily {
    return FontFamily(this)
}