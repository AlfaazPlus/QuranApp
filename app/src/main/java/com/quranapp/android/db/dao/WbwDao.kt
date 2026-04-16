package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.entities.quran.SurahLocalizationEntity
import com.quranapp.android.db.relations.SurahWithLocalizations
import kotlinx.coroutines.flow.Flow

@Dao
interface WbwDao {
}