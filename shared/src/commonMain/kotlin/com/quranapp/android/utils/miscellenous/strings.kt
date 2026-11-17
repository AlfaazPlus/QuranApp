package com.quranapp.android.utils.miscellenous

const val DASH: String = "–"
const val VERTICAL_BAR: String = "│"
const val HYPHEN: String = "—"

fun Char.isRTL(): Boolean {
    val d = Character.getDirectionality(this)
    return d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
}
