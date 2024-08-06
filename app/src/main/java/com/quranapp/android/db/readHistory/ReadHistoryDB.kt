package com.quranapp.android.db.readHistory

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quranapp.android.db.dao.ReadHistoryDao
import com.quranapp.android.db.entities.ReadHistory

@Database(
    entities = [ReadHistory::class],
    version = 1
)
abstract class ReadHistoryDB : RoomDatabase() {

    abstract fun readHistoryDao(): ReadHistoryDao
}