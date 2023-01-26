package com.peacedesign.android.utils.kotlin_utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.Dimension
import androidx.annotation.Px

object DimenKT {
    @Dimension
    fun dp2px(ctx: Context, @Dimension(unit = Dimension.DP) dpValue: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, ctx.getDisplayMetrics())
            .toInt()
    }

    @Dimension
    fun px2dp(ctx: Context, @Dimension pxValue: Float): Float {
        return pxValue / ctx.getDisplayMetrics().density
    }

    /**
     * @param spValue int value in SP
     * @return int value in [Px]
     * @see .px2sp
     */
    @Dimension
    fun sp2px(ctx: Context, @Dimension(unit = Dimension.SP) spValue: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, ctx.getDisplayMetrics())
            .toInt()
    }

    /**
     * @param pxValue float value in [Px]
     * @return float value in SP
     * @see .sp2px
     */
    fun px2sp(ctx: Context, @Dimension pxValue: Float): Float {
        return pxValue / ctx.getDisplayMetrics().scaledDensity
    }

    /**
     * Normalize angle to between -170 and 180;
     *
     * @param angle The angle to be normalized
     * @return Returns a normalized angle between -170 and 180;
     */
    fun normalizeAngle(angle: Int): Int {
        var nAngle = angle
        while (nAngle <= -180) nAngle += 360
        while (nAngle > 180) nAngle -= 360
        return nAngle
    }

    /**
     * Calculate a dimension relative to the other dimension
     *
     * @param sampleDimen  The dimension value that is to be considered.
     * @param actualDimen1 The dimension which is considered.
     * @param actualDimen2 The dimension which is to founded out relative to actualDimen1
     * @return Returns the dimension for ACTUAL_DIMEN_2 relative to ACTUAL_DIMEN_1
     */
    fun calcRelDimen(sampleDimen: Float, actualDimen1: Float, actualDimen2: Float): Float {
        return actualDimen2 * (sampleDimen / actualDimen1)
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            try {
                result = context.resources.getDimensionPixelSize(resourceId)
            } catch (ignored: Exception) {
            }
        }
        return result
    }

    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            try {
                result = context.resources.getDimensionPixelSize(resourceId)
            } catch (ignored: Exception) {
            }
        }
        return result
    }

    /**
     * Get width of the window.
     *
     * @return Returns the window width in [Px] .
     */
    @Px
    @Dimension
    fun getWindowWidth(ctx: Context): Int {
        return ctx.getDisplayMetrics().widthPixels
    }

    /**
     * Get width of the window.
     *
     * @return Returns the window height in [Px] .
     */
    fun getWindowHeight(ctx: Context): Int {
        return ctx.getDisplayMetrics().heightPixels
    }

    fun getLocationOnScreen(v: View): IntArray {
        val locs = intArrayOf(0, 0)
        v.getLocationOnScreen(locs)
        return locs
    }

    /**
     * Get x position to center of a view.
     *
     * @param view View to be centered.
     * @return Returns x position to be scrolled to.
     */
    fun getScrollXCenter(view: View, relWidth: Int): Int {
        return view.left + view.width / 2 - relWidth / 2
    }

    fun calcNoOfColumns(context: Context, columnWidthDp: Float): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return (screenWidthDp / columnWidthDp + 0.5).toInt()
    }

    fun createBorderWidthsForBG(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): IntArray {
        return intArrayOf(left, top, right, bottom)
    }

    fun createRadiiForBGInDP(ctx: Context, radiusDP: Float): FloatArray {
        return createRadiiForBG(dp2px(ctx, radiusDP).toFloat())
    }

    fun createRadiiForBG(radius: Float): FloatArray {
        return floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
    }

    fun createRadiiForBGInDP(
        ctx: Context,
        topLeftDP: Float = 0F,
        topRightDP: Float = 0F,
        bottomRightDP: Float = 0F,
        bottomLeftDP: Float = 0F,
    ): FloatArray {
        var mTopLeftDP = topLeftDP
        var nTopRightDP = topRightDP
        var nBottomRightDP = bottomRightDP
        var nBottomLeftDP = bottomLeftDP
        mTopLeftDP = dp2px(ctx, mTopLeftDP).toFloat()
        nTopRightDP = dp2px(ctx, nTopRightDP).toFloat()
        nBottomRightDP = dp2px(ctx, nBottomRightDP).toFloat()
        nBottomLeftDP = dp2px(ctx, nBottomLeftDP).toFloat()
        return createRadiiForBG(mTopLeftDP, nTopRightDP, nBottomRightDP, nBottomLeftDP)
    }

    fun createRadiiForBG(topLeft: Float = 0F, topRight: Float = 0F, bottomRight: Float = 0F, bottomLeft: Float = 0F): FloatArray {
        return floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)
    }
}