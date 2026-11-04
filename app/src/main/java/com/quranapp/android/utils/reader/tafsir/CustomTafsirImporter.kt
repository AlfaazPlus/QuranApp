package com.quranapp.android.utils.reader.tafsir

import android.content.Context
import android.net.Uri
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.importing.ImportException
import com.quranapp.android.utils.reader.importing.ImportFailure
import com.quranapp.android.utils.reader.importing.ImportManifest
import com.quranapp.android.utils.reader.importing.readImportText
import com.quranapp.android.utils.reader.translation.isCustomImportSlug

object CustomTafsirImporter {
    suspend fun import(context: Context, manifest: ImportManifest, uri: Uri) {
        try {
            val existing = TafsirManager.getModel(manifest.slug)
            if (existing != null && !isCustomImportSlug(existing.key)) {
                throw ImportException(ImportFailure.RESERVED)
            }

            val repository = DatabaseProvider.getQuranRepository(context)
            val models = parseTafsirImport(
                readImportText(context, uri),
                manifest.slug
            ) { chapterNo, verseNo ->
                repository.isVerseValid4Chapter(chapterNo, verseNo)
            }

            val info = manifest.toInfo()
            val dbHelper = QuranTafsirDBHelper(context)

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
            Log.saveError(error, "CustomTafsirImporter")
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
