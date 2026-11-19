package com.quranapp.android.db.bookmark

import android.content.Context
import androidx.core.content.edit
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.db.readHistory.ReadHistoryMigration
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.miscellenous.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UserDataMigrationManager(
    private val context: Context,
) {

    companion object {
        private const val PREF_NAME = "user_data_migration"
        private const val KEY_BOOKMARKS_MIGRATED = "bookmarks_migrated"
        private const val KEY_READ_HISTORY_MIGRATED = "read_history_migrated"
    }

    private fun migrateBookmarksIfNeeded() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val alreadyMigrated = prefs.getBoolean(KEY_BOOKMARKS_MIGRATED, false)

            if (alreadyMigrated) {
                Logger.d("Bookmarks - already migrated!")
                return@launch
            }

            val oldHelper = BookmarkDbHelper()
            val oldBookmarks = oldHelper.getBookmarks()

            if (oldBookmarks.isNotEmpty()) {
                Logger.d("Bookmarks - running migration")

                val userDatabase = DatabaseProvider.getUserDatabase()
                val dao = userDatabase.bookmarkDao()

                val existing = dao.getBookmarks()
                if (existing.isEmpty()) {
                    dao.insertAll(oldBookmarks.map {
                        BookmarkEntity(
                            chapterNo = it.chapterNo,
                            fromVerseNo = it.fromVerseNo,
                            toVerseNo = it.toVerseNo,
                            dateTime = it.date?.let {
                                DateUtils.parse(it, DateUtils.SYSTEM)?.toEpochMilliseconds()
                            } ?: System.currentTimeMillis(),
                            note = it.note
                        )
                    })
                }
            }

            prefs.edit(commit = true) {
                putBoolean(KEY_BOOKMARKS_MIGRATED, true)
            }
        }
    }

    private fun migrateReadHistoryIfNeeded() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            if (prefs.getBoolean(KEY_READ_HISTORY_MIGRATED, false)) {
                Logger.d("ReadHistory - already migrated!")
                return@launch
            }

            Logger.d("ReadHistory - running migration")
            val userDatabase = DatabaseProvider.getUserDatabase()
            ReadHistoryMigration.migrateIfNeeded(context, userDatabase)

            prefs.edit(commit = true) {
                putBoolean(KEY_READ_HISTORY_MIGRATED, true)
            }
        }
    }

    fun migrate() {
        migrateBookmarksIfNeeded()
        migrateReadHistoryIfNeeded()
    }
}
