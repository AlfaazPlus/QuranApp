package com.quranapp.android.utils.tafsir

import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.importing.ImportException
import com.quranapp.android.utils.importing.ImportFailure
import com.quranapp.android.utils.importing.ImportManifest
import com.quranapp.android.utils.translation.isCustomImportSlug
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString

object CustomTafsirImporter {
    suspend fun import(manifest: ImportManifest, pickedFile: PlatformFile) {
        try {
            val existing = TafsirManager.getModel(manifest.slug)

            if (existing != null && !isCustomImportSlug(existing.key)) {
                throw ImportException(ImportFailure.RESERVED)
            }

            val repository = DatabaseProvider.getQuranRepository()
            val models = parseTafsirImport(
                pickedFile.readString(),
                manifest.slug
            ) { chapterNo, verseNo ->
                repository.isVerseValid4Chapter(chapterNo, verseNo)
            }

            val info = manifest.toInfo()
            val dbHelper = QuranTafsirDBHelper()

            try {
                dbHelper.deleteTafsirData(manifest.slug)
                dbHelper.storeTafsirs(models, "1", System.currentTimeMillis())
                dbHelper.storeTafsirInfo(info)
            } finally {
                dbHelper.close()
            }

            TafsirManager.add(info)
        } catch (error: ImportException) {
            throw error
        } catch (error: Exception) {
            Logger.saveError(error, "CustomTafsirImporter")
            throw ImportException(ImportFailure.UNREADABLE)
        }
    }

    private fun ImportManifest.toInfo(): TafsirInfoModel {
        return TafsirInfoModel(
            key = slug,
            name = book,
            author = author,
            langCode = langCode,
            langName = langName,
        ).apply {
            isDownloaded = true
        }
    }
}
