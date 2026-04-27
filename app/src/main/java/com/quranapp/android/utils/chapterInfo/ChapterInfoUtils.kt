package com.quranapp.android.utils.chapterInfo

import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils

object ChapterInfoUtils {
    @JvmField
    val DIR_NAME: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "chapters_info",
    )

    private const val CHAPTER_INFO_FILE_NAME_FORMAT = "chapter_info_%d-%s.json"

    @JvmStatic
    fun prepareChapterInfoFilePath(lang: String, chapterNo: Int): String {
        val fileName = StringUtils.formatInvariant(CHAPTER_INFO_FILE_NAME_FORMAT, chapterNo, lang)
        return FileUtils.createPath(chapterNo.toString(), fileName)
    }
}
