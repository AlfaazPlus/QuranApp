package com.quranapp.android.utils.extensions

import android.content.Context
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.annotation.Dimension
import androidx.annotation.FontRes
import androidx.annotation.Px
import java.io.File


private fun Context.loadTypefaceFromResourceFile(@FontRes fontResId: Int): Typeface? {
    val tempFile = File(cacheDir, "font_res_$fontResId.tmp")

    return try {
        resources.openRawResource(fontResId).use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        Typeface.createFromFile(tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Context.getDisplayMetrics(): DisplayMetrics = resources.displayMetrics

@Dimension
fun Context.dp2px(@Dimension(unit = Dimension.DP) dpValue: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getDisplayMetrics())
        .toInt()
}

/**
 * @param spValue int value in SP
 * @return int value in [Px]
 */
@Dimension
fun Context.sp2px(@Dimension(unit = Dimension.SP) spValue: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, getDisplayMetrics())
        .toInt()
}
