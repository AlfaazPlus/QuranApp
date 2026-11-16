package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.utils.quran.parser.ExclusiveVersesParser

object QuranExclusiveVerses {
    suspend fun get(context: Context, dataset: ExclusiveVersesDataset): List<ExclusiveVerse> =
        ExclusiveVersesParser.parseFromAssets(context, dataset.assetKey)
}
