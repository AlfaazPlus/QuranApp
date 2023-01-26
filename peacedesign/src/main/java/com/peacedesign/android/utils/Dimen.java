package com.peacedesign.android.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

public abstract class Dimen {
    @NonNull
    public static DisplayMetrics getDisplayMetrics(Context ctx) {
        return ctx.getResources().getDisplayMetrics();
    }

    @Dimension
    public static int dp2px(Context ctx, @Dimension(unit = Dimension.DP) float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getDisplayMetrics(ctx));
    }

    @Dimension
    public static float px2dp(Context ctx, @Dimension float pxValue) {
        return pxValue / getDisplayMetrics(ctx).density;
    }

    /**
     * @param spValue int value in SP
     * @return int value in {@link Px}
     * @see #px2sp(Context, float)
     */
    @Dimension
    public static int sp2px(Context ctx, @Dimension(unit = Dimension.SP) float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, getDisplayMetrics(ctx));
    }

    /**
     * @param pxValue float value in {@link Px}
     * @return float value in SP
     * @see #sp2px(Context, float)
     */
    public static float px2sp(Context ctx, @Dimension float pxValue) {
        return pxValue / getDisplayMetrics(ctx).scaledDensity;
    }

    /**
     * Normalize angle to between -170 and 180;
     *
     * @param angle The angle to be normalized
     * @return Returns a normalized angle between -170 and 180;
     */
    public static int normalizeAngle(int angle) {
        while (angle <= -180) angle += 360;
        while (angle > 180) angle -= 360;
        return angle;
    }

    /**
     * Calculate a dimension relative to the other dimension
     *
     * @param sampleDimen  The dimension value that is to be considered.
     * @param actualDimen1 The dimension which is considered.
     * @param actualDimen2 The dimension which is to founded out relative to actualDimen1
     * @return Returns the dimension for ACTUAL_DIMEN_2 relative to ACTUAL_DIMEN_1
     */
    public static float calcRelDimen(float sampleDimen, float actualDimen1, float actualDimen2) {
        return actualDimen2 * (sampleDimen / actualDimen1);
    }

    public static int getStatusBarHeight(@NonNull Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            try {
                result = context.getResources().getDimensionPixelSize(resourceId);
            } catch (Exception ignored) {}
        }
        return result;
    }

    public static int getNavigationBarHeight(@NonNull Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            try {
                result = context.getResources().getDimensionPixelSize(resourceId);
            } catch (Exception ignored) {}
        }
        return result;
    }

    /**
     * Get width of the window.
     *
     * @return Returns the window width in {@link Px} .
     */
    @Px
    @Dimension
    public static int getWindowWidth(Context ctx) {
        return getDisplayMetrics(ctx).widthPixels;
    }

    /**
     * Get width of the window.
     *
     * @return Returns the window height in {@link Px} .
     */
    public static int getWindowHeight(Context ctx) {
        return getDisplayMetrics(ctx).heightPixels;
    }

    @NonNull
    public static int[] getLocationOnScreen(@NonNull View v) {
        int[] locs = {0, 0};
        v.getLocationOnScreen(locs);
        return locs;
    }

    /**
     * Get x position to center of a view.
     *
     * @param view View to be centered.
     * @return Returns x position to be scrolled to.
     */
    public static int getScrollXCenter(@NonNull final View view, int relWidth) {
        return (view.getLeft() + view.getWidth() / 2) - (relWidth / 2);
    }

    public static int calcNoOfColumns(@NonNull Context context, float columnWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (screenWidthDp / columnWidthDp + 0.5);
    }

    @NonNull
    public static int[] createBorderWidthsForBG(int left, int top, int right, int bottom) {
        return new int[]{left, top, right, bottom};
    }

    @NonNull
    public static float[] createRadiiForBGInDP(Context ctx, float radiusDP) {
        return createRadiiForBG(dp2px(ctx, radiusDP));
    }

    @NonNull
    public static float[] createRadiiForBG(float radius) {
        return new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
    }

    @NonNull
    public static float[] createRadiiForBGInDP(Context ctx, float topLeftDP, float topRightDP, float bottomRightDP, float bottomLeftDP) {
        topLeftDP = dp2px(ctx, topLeftDP);
        topRightDP = dp2px(ctx, topRightDP);
        bottomRightDP = dp2px(ctx, bottomRightDP);
        bottomLeftDP = dp2px(ctx, bottomLeftDP);
        return createRadiiForBG(topLeftDP, topRightDP, bottomRightDP, bottomLeftDP);
    }

    @NonNull
    public static float[] createRadiiForBG(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        return new float[]{topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft};
    }
}
