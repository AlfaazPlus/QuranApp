package com.quranapp.android.utils.reader;

import androidx.annotation.DimenRes;

import com.quranapp.android.R;

public class ScriptUtils {
    public static final String KEY_SCRIPT = "key.script";

    public static final String SCRIPT_INDO_PAK = "indopak";
    public static final String SCRIPT_UTHMANI = "uthmani";

    public static final String SCRIPT_DEFAULT = SCRIPT_INDO_PAK;

    public static String[] availableScriptSlugs() {
        return new String[]{SCRIPT_INDO_PAK, SCRIPT_UTHMANI};
    }

    public static String getScriptName(String slug) {
        switch (slug) {
            case SCRIPT_INDO_PAK:
                return "IndoPak";
            case SCRIPT_UTHMANI:
                return "Uthmani";
        }
        return "";
    }

    public static int getScriptPreviewRes(String slug) {
        switch (slug) {
            case SCRIPT_INDO_PAK:
                return R.string.strScriptPreviewIndopak;
            case SCRIPT_UTHMANI:
                return R.string.strScriptPreviewUthmani;
        }

        return 0;
    }

    @DimenRes
    public static int getScriptTextSizeSmall2Res(String slug) {
        switch (slug) {
            case SCRIPT_INDO_PAK:
                return R.dimen.dmnReaderTextSizeArIndoPakSmall2;
            case SCRIPT_UTHMANI:
                return R.dimen.dmnReaderTextSizeArUthmaniSmall2;
        }

        return 0;
    }

    public static int getScriptFontRes(String slug) {
        switch (slug) {
            case SCRIPT_INDO_PAK:
                return R.font.pdms;
            case SCRIPT_UTHMANI:
                return R.font.me_quran_4_uthmani_text;
        }

        return 0;
    }

    public static int getScriptFontDimenRes(String slug) {
        switch (slug) {
            case SCRIPT_INDO_PAK:
                return R.dimen.dmnReaderTextSizeArIndoPakMedium;
            case SCRIPT_UTHMANI:
                return R.dimen.dmnReaderTextSizeArUthmaniMedium;
        }

        return 0;
    }

    public static String getScriptResPath(String slug) {
        switch (slug) {
            case SCRIPT_INDO_PAK:
                return "scripts/script_indopak.json";
            case SCRIPT_UTHMANI:
                return "scripts/script_uthmani.json";
        }

        return "";
    }

    public static boolean isPremium(String slug) {
        return SCRIPT_UTHMANI.equals(slug);
    }
}
