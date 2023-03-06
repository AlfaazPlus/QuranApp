package com.quranapp.android.utils.extensions

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
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.peacedesign.R

fun Context.getPackageNameRelease(): String {
    return packageName.replace(".debug", "")
}

fun Context.isRTL() = getBoolean(R.bool.isRTL)

fun Context.getBoolean(@BoolRes boolResId: Int): Boolean = resources.getBoolean(boolResId)

fun Context.getStringArray(@ArrayRes arrayResId: Int): Array<String?> = resources.getStringArray(
    arrayResId
)

fun Context.getIntArray(@ArrayRes arrayResId: Int): IntArray = resources.getIntArray(arrayResId)

fun Context.getTypedArray(@ArrayRes arrayResId: Int): TypedArray = resources.obtainTypedArray(
    arrayResId
)

@ColorInt
fun Context.color(@ColorRes colorResId: Int): Int = ContextCompat.getColor(this, colorResId)

@ColorInt
fun Context.obtainPrimaryColor(): Int {
    return ContextCompat.getColor(this, R.color.colorPrimary)
}

@ColorInt
fun Context.obtainWindowBackgroundColor(): Int {
    val attributes = this.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))

    @ColorInt val backgroundColor = attributes.getColor(0, 0)
    attributes.recycle()
    return backgroundColor
}

fun Context.colorStateList(@ColorRes colorResId: Int): ColorStateList? =
    ContextCompat.getColorStateList(this, colorResId)

fun Context.getFont(@FontRes fontResId: Int): Typeface? {
    return try {
        ResourcesCompat.getFont(this, fontResId)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Context.drawable(@DrawableRes drawableResId: Int): Drawable {
    return AppCompatResources.getDrawable(this, drawableResId)!!
}

fun Context.getDimension(@DimenRes dimenResId: Int): Int = getDimenPx(dimenResId)

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
fun Context.getDimenPx(@DimenRes dimenRes: Int): Int {
    return resources.getDimensionPixelSize(dimenRes)
}

@Dimension(unit = Dimension.SP)
fun Context.getDimenSp(@DimenRes dimenRes: Int): Float {
    return px2sp(getDimenPx(dimenRes).toFloat())
}

fun Context.getFraction(@FractionRes dimenRes: Int): Float {
    return resources.getFraction(dimenRes, 1, 1)
}

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

fun Context.getWindowHeight(): Int {
    return getDisplayMetrics().heightPixels
}

fun Context.getWindowWidth(): Int {
    return getDisplayMetrics().widthPixels
}
