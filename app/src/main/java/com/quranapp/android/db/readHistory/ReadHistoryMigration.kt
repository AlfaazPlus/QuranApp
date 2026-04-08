package com.quranapp.android.db.readHistory

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.db.UserDatabase
import com.quranapp.android.db.entities.ReadHistoryEntity
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.ReadType
import java.io.File

object ReadHistoryMigration {

    private const val OLD_DB_NAME = "ReadHistory.db"

    suspend fun migrateIfNeeded(context: Context, userDb: UserDatabase) {
        val dbFile = context.getDatabasePath(OLD_DB_NAME)
        if (!dbFile.exists()) return

        try {
            val entries = readOldEntries(dbFile)
            if (entries.isNotEmpty()) {
                val dao = userDb.readHistoryDao()
                for (entity in entries) {
                    dao.insert(entity)
                }
            }
        } catch (e: Exception) {
            Log.saveError(e, "ReadHistoryMigration")
        }

        deleteOldDatabase(context)
    }

    private fun readOldEntries(dbFile: File): List<ReadHistoryEntity> {
        val entries = mutableListOf<ReadHistoryEntity>()
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)

        try {
            val cursor = db.query(
                "ReadHistory", null, null, null,
                null, null, "_id DESC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    val legacyReadType = it.getInt(it.getColumnIndexOrThrow("ReadType"))
                    val legacyStyle = it.getInt(it.getColumnIndexOrThrow("ReaderStyle"))
                    val juzNo = it.getInt(it.getColumnIndexOrThrow("JuzNumber"))
                    val chapterNo = it.getInt(it.getColumnIndexOrThrow("ChapterNumber"))
                    val fromVerseNo = it.getInt(it.getColumnIndexOrThrow("FromVerseNumber"))
                    val toVerseNo = it.getInt(it.getColumnIndexOrThrow("ToVerseNumber"))
                    val dateStr = it.getString(it.getColumnIndexOrThrow("Date"))

                    val readType = ReadType.fromLegacyInt(legacyReadType)
                    val readerMode = ReaderMode.fromLegacyStyleInt(legacyStyle)

                    entries += ReadHistoryEntity(
                        readType = readType.value,
                        readerMode = readerMode.value,
                        divisionNo = if (readType == ReadType.Juz) juzNo else 0,
                        chapterNo = chapterNo,
                        fromVerseNo = fromVerseNo,
                        toVerseNo = toVerseNo,
                        mushafId = 0,
                        datetime = parseLegacyDate(dateStr),
                    )
                }
            }
        } finally {
            db.close()
        }

        return entries
    }

    private fun parseLegacyDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            @Suppress("DEPRECATION")
            java.util.Date.parse(dateStr)
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun deleteOldDatabase(context: Context) {
        try {
            context.deleteDatabase(OLD_DB_NAME)
        } catch (e: Exception) {
            Log.saveError(e, "ReadHistoryMigration.deleteOldDb")
        }
    }
}
