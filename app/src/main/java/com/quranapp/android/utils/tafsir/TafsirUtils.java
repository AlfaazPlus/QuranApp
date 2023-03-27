package com.quranapp.android.utils.tafsir;

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;

import com.quranapp.android.components.tafsir.TafsirModel;
import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.reader.tafsir.TafsirManager;
import com.quranapp.android.utils.univ.FileUtils;

public class TafsirUtils {
    public static final String DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "tafsirs");

    public static final String AVAILABLE_TAFSIRS_FILENAME = "available_tafsirs.json";
    public static final String TAFSIR_SINGLE_FILE_NAME_FORMAT = "tafsir_verse_%d.txt";
    public static final String KEY_TAFSIR = "key.tafsir";

    @Nullable
    public static String getTafsirName(String key) {
        if (key == null) {
            return null;
        }

        TafsirModel model = TafsirManager.getModel(key);
        if (model == null) {
            return null;
        }

        return model.getName();
    }

    @Nullable
    public static String prepareTafsirUrlSingleVerse(String tafsirSlug, int chapterNo, int verseNo) {
        /*int tafsirId = getTafsirId(tafsirSlug);
        if (tafsirId == -1) {
            return null;
        }*/
        String verseKey = chapterNo + ":" + verseNo;
        return "https://api.quran.com/api/v4/quran/tafsirs/" + "tafsirId" + "?verse_key=" + verseKey;
    }

    @SuppressLint("DefaultLocale")
    public static String prepareTafsirFilePathSingleVerse(String tafsirSlug, int chapterNo, int verseNo) {
        final String fileName = String.format(TAFSIR_SINGLE_FILE_NAME_FORMAT, verseNo);
        return FileUtils.createPath(String.valueOf(chapterNo), tafsirSlug, fileName);
    }
}
