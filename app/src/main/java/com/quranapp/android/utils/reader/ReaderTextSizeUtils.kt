package com.quranapp.android.utils.reader

import kotlin.math.max
import kotlin.math.min

object ReaderTextSizeUtils {
    const val KEY_TEXT_SIZE_MULT_ARABIC: String = "key.textsize.mult.arabic"
    const val KEY_TEXT_SIZE_MULT_TRANSL: String = "key.textsize.mult.translation"
    const val KEY_TEXT_SIZE_MULT_TAFSIR: String = "key.textsize.mult.tafsir"
    const val KEY_TEXT_SIZE_MULT_WBW: String = "key.textsize.mult.wbw"

    const val TEXT_SIZE_MIN_PROGRESS: Int = 50
    const val TEXT_SIZE_MAX_PROGRESS: Int = 200
    const val TEXT_SIZE_DEFAULT_PROGRESS: Int = 100
    const val TEXT_SIZE_MULT_AR_DEFAULT: Float = 1.0f
    const val TEXT_SIZE_MULT_TRANSL_DEFAULT: Float = 1.0f
    const val TEXT_SIZE_MULT_TAFSIR_DEFAULT: Float = 1.0f
    const val TEXT_SIZE_MULT_WBW_DEFAULT: Float = 1.0f
    
    val maxProgress: Int
        get() = TEXT_SIZE_MAX_PROGRESS - TEXT_SIZE_MIN_PROGRESS
    
    fun normalizeProgress(seekbarProgress: Int): Int {
        return TEXT_SIZE_MIN_PROGRESS + seekbarProgress
    }
    
    fun calculateMultiplier(
        progress: Int,
        min: Int = TEXT_SIZE_MIN_PROGRESS,
        max: Int = TEXT_SIZE_MAX_PROGRESS
    ): Float {
        var progress = progress
        progress = max(progress, min)
        progress = min(progress, max)

        return progress.toFloat() / 100
    }
    
    fun calculateProgressText(multiplier: Float): Int {
        return (multiplier * 100).toInt()
    }
    
    fun calculateProgress(multiplier: Float): Int {
        return (multiplier * 100).toInt() - TEXT_SIZE_MIN_PROGRESS
    }
}
