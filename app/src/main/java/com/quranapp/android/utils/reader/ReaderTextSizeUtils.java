package com.quranapp.android.utils.reader;

public class ReaderTextSizeUtils {
    public static final String KEY_TEXT_SIZE_MULT_ARABIC = "key.textsize.mult.arabic";
    public static final String KEY_TEXT_SIZE_MULT_TRANSL = "key.textsize.mult.translation";
    public static final String KEY_TEXT_SIZE_MULT_TAFSIR = "key.textsize.mult.tafsir";

    public static final int TEXT_SIZE_MIN_PROGRESS = 50;
    public static final int TEXT_SIZE_MAX_PROGRESS = 200;
    public static final int TEXT_SIZE_DEFAULT_PROGRESS = 100;
    public static final float TEXT_SIZE_MULT_AR_DEFAULT = 1.0f;
    public static final float TEXT_SIZE_MULT_TRANSL_DEFAULT = 1.0f;
    public static final float TEXT_SIZE_MULT_TAFSIR_DEFAULT = 1.0f;

    public static int getMaxProgress() {
        return TEXT_SIZE_MAX_PROGRESS - TEXT_SIZE_MIN_PROGRESS;
    }

    public static int normalizeProgress(int seekbarProgress) {
        return TEXT_SIZE_MIN_PROGRESS + seekbarProgress;
    }

    public static float calculateMultiplier(int progress) {
        progress = Math.max(progress, TEXT_SIZE_MIN_PROGRESS);
        progress = Math.min(progress, TEXT_SIZE_MAX_PROGRESS);

        return (float) progress / 100;
    }

    public static int calculateProgressText(float multiplier) {
        return (int) (multiplier * 100);
    }

    public static int calculateProgress(float multiplier) {
        return (int) (multiplier * 100) - TEXT_SIZE_MIN_PROGRESS;
    }
}
