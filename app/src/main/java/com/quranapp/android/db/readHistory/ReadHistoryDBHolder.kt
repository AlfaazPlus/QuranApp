package com.quranapp.android.db.readHistory

import android.content.Context
import androidx.room.Room


object ReadHistoryDBHolder {
    private const val DB_NAME = "read.history.db"
    lateinit var instance: ReadHistoryDB

    fun init(context: Context) {
        instance = Room
            .databaseBuilder(context, ReadHistoryDB::class.java, DB_NAME)
            .build()
    }
}