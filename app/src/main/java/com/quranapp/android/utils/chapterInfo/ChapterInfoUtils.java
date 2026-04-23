package com.quranapp.android.utils.chapterInfo;

import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.StringUtils;

public class ChapterInfoUtils {
    public static final String DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "chapters_info");

    public static final String CHAPTER_INFO_FILE_NAME_FORMAT = "chapter_info_%d-%s.txt";

    public static String prepareChapterInfoUrl(String lang, int chapterNo) {
        return "https://api.quran.com/api/v4/chapters/" + chapterNo + "/info?language=" + lang;
    }

    public static String prepareChapterInfoFilePath(String lang, int chapterNo) {
        final String fileName = StringUtils.formatInvariant(CHAPTER_INFO_FILE_NAME_FORMAT, chapterNo, lang);
        return FileUtils.createPath(String.valueOf(chapterNo), fileName);
    }
}
