package com.quranapp.android.utils.chapterInfo;

import android.annotation.SuppressLint;

import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.univ.FileUtils;

public class ChapterInfoUtils {
    public static final String DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "chapters_info");

    public static final String CHAPTER_INFO_FILE_NAME_FORMAT = "chapter_info_%d-%s.txt";

    public static String prepareChapterInfoUrl(String lang, int chapterNo) {
        return "https://api.quran.com/api/v4/chapters/" + chapterNo + "/info?language=" + lang;
    }


    @SuppressLint("DefaultLocale")
    public static String prepareChapterInfoFilePath(String lang, int chapterNo) {
        final String fileName = String.format(CHAPTER_INFO_FILE_NAME_FORMAT, chapterNo, lang);
        return FileUtils.createPath(String.valueOf(chapterNo), fileName);
    }
}
