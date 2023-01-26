package com.peacedesign.android.utils;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import java.util.Random;

public abstract class ColorUtils {
    @ColorInt
    public static final int NO_COLOR = -19;
    @ColorInt
    public static final int SUCCESS = 0xFF28A745;
    @ColorInt
    public static final int SUCCESS_DARK = 0xFF007E33;
    @ColorInt
    public static final int DANGER = 0xFFDC3545;
    @ColorInt
    public static final int DANGER_DARK = 0xFFCC0000;
    @ColorInt
    public static final int WARNING = 0xFFD67200;
    @ColorInt
    public static final int WARNING_DARK = 0xFFB86200;
    @ColorInt
    public static final int INFO = 0xFF33E5B5;
    @ColorInt
    public static final int INFO_DARK = 0xFF0099CC;
    @ColorInt
    public static final int LINK = 0xFF007BFF;
    @ColorInt
    public static final int COMFORT_WHITE = 0xFFE3E3E3;

    @NonNull
    public static String colorIntToHex(int color) {
        return colorIntToHex(color, false);
    }

    @NonNull
    public static String colorIntToHex(int color, boolean withoutHash) {
        String hex = String.format("#%06X", (0xFFFFFF & color));
        return withoutHash ? hex.substring(1) : hex;
    }

    @NonNull
    public static String colorHex3To6(@NonNull String hexCode3Chars) {
        if (hexCode3Chars.length() < 3) return "";
        if (hexCode3Chars.length() > 3) hexCode3Chars = hexCode3Chars.substring(0, 3);

        StringBuilder hexCode6Chars = new StringBuilder();
        char[] chars = hexCode3Chars.toCharArray();
        for (char ch : chars) hexCode6Chars.append(ch).append(ch);
        return hexCode6Chars.toString();
    }

    @ColorInt
    public static int createAlphaColor(int color, float alpha) {
        return (int) (0xFF * alpha) << 24 | color & 0xFFFFFF;
    }

    @ColorInt
    public static int generateRandomLightColor() {
        final Random mRandom = new Random(System.currentTimeMillis());
        // This is the base color which will be mixed with the generated one
        final int baseColor = Color.WHITE;

        final int baseRed = Color.red(baseColor);
        final int baseGreen = Color.green(baseColor);
        final int baseBlue = Color.blue(baseColor);

        final int red = (baseRed + mRandom.nextInt(256)) / 2;
        final int green = (baseGreen + mRandom.nextInt(256)) / 2;
        final int blue = (baseBlue + mRandom.nextInt(256)) / 2;

        return Color.rgb(red, green, blue);
    }

    /**
     * @param color  Color to darken
     * @param factor Factor for darkening
     * @return Returns darkened color
     */

    @ColorInt
    public static int darken(@ColorInt int color, @FloatRange(from = 0, to = 1) float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }
}
