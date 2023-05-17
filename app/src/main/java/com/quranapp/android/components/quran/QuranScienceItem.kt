package com.quranapp.android.components.quran

import java.io.Serializable

data class QuranScienceItem(
    val title: String,
    val referencesCount: Int,
    val path: String,
    val drawableRes: Int,
) : Serializable