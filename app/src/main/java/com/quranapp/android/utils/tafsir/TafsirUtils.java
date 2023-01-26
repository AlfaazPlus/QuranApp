package com.quranapp.android.utils.tafsir;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;

import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.univ.FileUtils;

public class TafsirUtils {
    public static final String DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "tafsirs");

    public static final String TAFSIR_SLUG_TAFSIR_IBN_KATHIR_EN = "en_tafsir_ibn_kathir";
    public static final String TAFSIR_SLUG_TAFSIR_IBN_KATHIR_UR = "ur_tafsir_ibn_kathir";

    public static final String TAFSIR_FULL_CHAPTER_FILE_NAME_FORMAT = "tafsir_full.txt";
    public static final String TAFSIR_SINGLE_FILE_NAME_FORMAT = "tafsir_verse_%d.txt";
    public static final String KEY_TAFSIR_SLUG = "tafsir_slug";

    public static int getTafsirId(String tafsirSlug) {
        switch (tafsirSlug) {
            case TAFSIR_SLUG_TAFSIR_IBN_KATHIR_EN:
                return 169;
            case TAFSIR_SLUG_TAFSIR_IBN_KATHIR_UR:
                return 160;
        }
        return -1;
    }


    public static String getTafsirName(String tafsirSlug) {
        switch (tafsirSlug) {
            case TAFSIR_SLUG_TAFSIR_IBN_KATHIR_EN:
                return "Tafsir Ibn Kathir";
            case TAFSIR_SLUG_TAFSIR_IBN_KATHIR_UR:
                return "تفسیر ابنِ کثیر";
        }
        return "";
    }

    @Nullable
    public static String prepareTafsirUrlSingleVerse(String tafsirSlug, int chapterNo, int verseNo) {
        int tafsirId = getTafsirId(tafsirSlug);
        if (tafsirId == -1) {
            return null;
        }
        String verseKey = chapterNo + ":" + verseNo;
        return "https://api.quran.com/api/v4/quran/tafsirs/" + tafsirId + "?verse_key=" + verseKey;
    }

    @Nullable
    public static String prepareTafsirUrlFullChapter(String tafsirSlug, int chapterNo) {
        int tafsirId = getTafsirId(tafsirSlug);
        if (tafsirId == -1) {
            return null;
        }
        return "https://api.quran.com/api/v4/quran/tafsirs/" + tafsirId + "?chapter_number=" + chapterNo;
    }

    @SuppressLint("DefaultLocale")
    public static String prepareTafsirFilePathSingleVerse(String tafsirSlug, int chapterNo, int verseNo) {
        final String fileName = String.format(TAFSIR_SINGLE_FILE_NAME_FORMAT, verseNo);
        return FileUtils.createPath(String.valueOf(chapterNo), tafsirSlug, fileName);
    }

    public static String prepareTafsirFilePathFullChapter(String tafsirSlug, int chapterNo) {
        return FileUtils.createPath(String.valueOf(chapterNo), tafsirSlug, TAFSIR_FULL_CHAPTER_FILE_NAME_FORMAT);
    }
}
