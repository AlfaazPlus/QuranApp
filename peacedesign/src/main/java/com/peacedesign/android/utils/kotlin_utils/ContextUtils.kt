package com.peacedesign.android.utils.kotlin_utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.annotation.*
import androidx.core.content.ContextCompat
import com.peacedesign.android.utils.ResUtils

fun Context.getPackageNameRelease(): String {
    return packageName.replace(".debug", "")
}

fun Context.getBoolean(@BoolRes boolResId: Int): Boolean = resources.getBoolean(boolResId)

fun Context.getStringArray(@ArrayRes arrayResId: Int): Array<String?> = resources.getStringArray(arrayResId)

fun Context.getIntArray(@ArrayRes arrayResId: Int): IntArray = resources.getIntArray(arrayResId)

fun Context.getTypedArray(@ArrayRes arrayResId: Int): TypedArray = resources.obtainTypedArray(arrayResId)

@ColorInt
fun Context.color(@ColorRes colorResId: Int): Int = ContextCompat.getColor(this, colorResId)

fun Context.colorStateList(@ColorRes colorResId: Int): ColorStateList? = ContextCompat.getColorStateList(this, colorResId)

fun Context.getFont(@FontRes fontResId: Int): Typeface? = ResUtils.getFont(this, fontResId)

fun Context.drawable(@DrawableRes drawableResId: Int): Drawable? = ResUtils.getDrawable(this, drawableResId)

fun Context.getDimension(@DimenRes dimenResId: Int): Int = ResUtils.getDimenPx(this, dimenResId)

fun Context.copyToClipboard(text: CharSequence): Boolean {
    val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
    clipboard?.let {
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
        return true
    }
    return false
}

fun Context.getDisplayMetrics(): DisplayMetrics = resources.displayMetrics

@Dimension
fun Context.dp2px(@Dimension(unit = Dimension.DP) dpValue: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getDisplayMetrics()).toInt()
}

@Dimension
fun Context.px2dp(@Dimension pxValue: Float): Float = pxValue / getDisplayMetrics().density

/**
 * @param spValue int value in SP
 * @return int value in [Px]
 * @see [px2sp]
 */
@Dimension
fun Context.sp2px(@Dimension(unit = Dimension.SP) spValue: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, getDisplayMetrics()).toInt()
}

/**
 * @param pxValue float value in [Px]
 * @return float value in SP
 * @see [sp2px]
 */
fun Context.px2sp(@Dimension pxValue: Float): Float = pxValue / getDisplayMetrics().scaledDensity
