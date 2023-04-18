package com.peacedesign.android.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.Arrays;

public abstract class DrawableUtils {

    @ColorInt
    public static int getColorFromDrawable(@NonNull Drawable drawable) {
        if (drawable instanceof ColorDrawable) {
            return ((ColorDrawable) drawable).getColor();
        }
        return -1;
    }

    private static float[] makeRadii(float radius) {
        float[] cornerRadii = new float[8];
        Arrays.fill(cornerRadii, radius);

        return cornerRadii;
    }

    @NonNull
    public static Drawable createBackground(@ColorInt int bgColor, @Dimension float cornerRadius) {
        return createBackground(bgColor, makeRadii(cornerRadius));
    }

    @NonNull
    public static Drawable createBackground(@ColorInt int bgColor, @Size(8) @Nullable float[] cornerRadii) {
        return createBackground(bgColor, bgColor, cornerRadii);
    }

    @NonNull
    public static Drawable createBackground(@ColorInt int bgColor, @ColorInt int hoverColor, @Dimension float cornerRadius) {
        return createBackground(bgColor, hoverColor, makeRadii(cornerRadius));
    }

    @NonNull
    public static Drawable createBackground(@ColorInt int bgColor, @ColorInt int hoverColor, @Size(8) @Nullable float[] cornerRadii) {
        int[][] states = {
                new int[]{-android.R.attr.state_pressed}, // not pressed
                new int[]{android.R.attr.state_pressed}, // pressed
        };
        int[] colors = {bgColor, hoverColor};
        return createBackground(new ColorStateList(states, colors), cornerRadii);
    }

    @NonNull
    public static Drawable createBackground(@NonNull ColorStateList stateList, @Dimension float cornerRadius) {
        return createBackground(stateList, makeRadii(cornerRadius));
    }

    @NonNull
    public static Drawable createBackground(@NonNull ColorStateList stateList, @Size(8) @Nullable float[] cornerRadii) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(stateList);
        if (cornerRadii != null) drawable.setCornerRadii(cornerRadii);
        return drawable;
    }

    @NonNull
    public static Drawable createBackgroundStroked(@ColorInt int bgColor, @ColorInt int borderColor, @Dimension int borderWidth, @Dimension float cornerRadius) {
        float[] cornerRadii = new float[8];
        Arrays.fill(cornerRadii, cornerRadius);

        int[] borderWidths = new int[4];
        Arrays.fill(borderWidths, borderWidth);
        return createBackgroundStroked(bgColor, borderColor, borderWidths, cornerRadii);
    }

    /**
     * @param bgColor      bgColor int, not resId
     * @param borderColor  borderColor int, not resId
     * @param borderWidths inter array: {left, top, right, bottom}, specified in pixels
     * @param cornerRadii  an array of length >= 8 containing 4 pairs of X and Y radius for each corner, specified in pixels
     * @return created background
     */
    @NonNull
    public static Drawable createBackgroundStroked(@ColorInt int bgColor, @ColorInt int borderColor,
                                                   @Nullable @Size(4) int[] borderWidths, @Size(8) @Nullable float[] cornerRadii) {
        if (borderWidths != null && borderWidths.length < 4) {
            throw new IllegalArgumentException("borderWidths[] length must be >= 4");
        }

        GradientDrawable main = new GradientDrawable();
        main.setColor(bgColor);
        main.setShape(GradientDrawable.RECTANGLE);
        if (cornerRadii != null) main.setCornerRadii(cornerRadii);

        GradientDrawable bgForBorder = new GradientDrawable();
        bgForBorder.setColor(borderColor);
        bgForBorder.setShape(GradientDrawable.RECTANGLE);
        if (cornerRadii != null) bgForBorder.setCornerRadii(cornerRadii);

        Drawable[] layers = {bgForBorder, main};
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        if (borderWidths != null) {
            layerDrawable.setLayerInset(1, borderWidths[0], borderWidths[1], borderWidths[2], borderWidths[3]);
        }

        return DrawableCompat.wrap(layerDrawable);
    }


    @NonNull
    public static RippleDrawable createRippleDrawable(@ColorInt int normalColor, @Dimension int radius) {
        return createRippleDrawable(normalColor, 0xFF050505, radius);
    }

    @NonNull
    public static RippleDrawable createRippleDrawable(@ColorInt int normalColor, @ColorInt int rippleColor, @Dimension int radius) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), createBackground(normalColor, radius), null);
    }

    public static Drawable rotate(@NonNull Context ctx, @NonNull Drawable drawable, float angle) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.save();
        canvas.rotate(angle, bitmap.getWidth() >> 1, bitmap.getHeight() >> 1);
        Drawable mutate = drawable.mutate();
        mutate.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        mutate.draw(canvas);
        canvas.restore();

        return new BitmapDrawable(ctx.getResources(), bitmap);
    }
}
