package com.quranapp.android.utils.reader.atlas

import android.content.Context
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.sync.Mutex
import java.io.File

object AtlasManager {
    private const val DIR_NAME = "atlas"

    private val ROOT_DIR_PATH: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        DIR_NAME
    )

    private val lock = Mutex()

    private fun getRootDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, ROOT_DIR_PATH)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getTempDownloadFile(context: Context, id: String): File {
        return File(getRootDir(context), "${id}.tmp")
    }
}
