package com.quranapp.android.db.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.entities.quran.SurahLocalizationEntity
import com.quranapp.android.db.interfaces.SurahMethods
import java.util.Locale

data class SurahWithLocalizations(
    @Embedded
    val surah: SurahEntity,

    @Relation(
        parentColumn = "surah_no",
        entityColumn = "surah_no"
    )
    val localizations: List<SurahLocalizationEntity>
) : SurahMethods by surah {
    fun getCurrentName(): String {
        val langCode = Locale.getDefault().language
        val localization = localizations.firstOrNull { it.langCode == langCode }
            ?: localizations.firstOrNull { it.langCode == "en" }

        return localization?.name ?: ""
    }

    fun getCurrentMeaning(): String {
        val langCode = Locale.getDefault().language
        val localization = localizations.firstOrNull { it.langCode == langCode }
            ?: localizations.firstOrNull { it.langCode == "en" }

        return localization?.meaning ?: ""
    }
}