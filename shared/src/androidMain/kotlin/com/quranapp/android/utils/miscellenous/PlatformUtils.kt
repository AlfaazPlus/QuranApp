package com.quranapp.android.utils.miscellenous

import android.content.res.Resources

actual object PlatformUtils {
    private val metrics get() = Resources.getSystem().displayMetrics

    actual val density: Float get() = metrics.density

    actual val screenWidthPx: Int get() = metrics.widthPixels

    actual val screenHeightPx: Int get() = metrics.heightPixels
}
