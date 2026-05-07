package com.quranapp.android.repository

import android.content.Context
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.db.TopicsDatabase
import com.quranapp.android.db.dao.TopicsDao
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.entities.topics.TopicFlags
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TopicsRepository(
    private val context: Context,
    private val database: TopicsDatabase,
    private val quranRepo: QuranRepository,
) {
}
