package com.quranapp.android.utils.chapterInfo

import com.quranapp.android.utils.app.AppFiles
import okio.Path

object ChapterInfoUtils {
    const val DIR_NAME = "chapters_info"

    fun file(langCode: String, chapterNo: Int): Path {
        val fileName = "chapter_info_$chapterNo-$langCode.json"
        return AppFiles.savedFile(DIR_NAME, chapterNo.toString(), fileName)
    }
}
