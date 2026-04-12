package com.quranapp.android.db.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.entities.quran.SurahLocalizationEntity
import com.quranapp.android.compose.utils.appLocale
import com.quranapp.android.db.interfaces.SurahMethods

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
        val locale = appLocale()
        val tag = locale.toLanguageTag()
        val lang = locale.language
        val localization = localizations.firstOrNull { it.langCode == tag }
            ?: localizations.firstOrNull { it.langCode == lang }
            ?: localizations.firstOrNull { it.langCode == "en" }

        return localization?.name ?: ""
    }

    fun getCurrentMeaning(): String {
        val locale = appLocale()
        val tag = locale.toLanguageTag()
        val lang = locale.language
        val localization = localizations.firstOrNull { it.langCode == tag }
            ?: localizations.firstOrNull { it.langCode == lang }
            ?: localizations.firstOrNull { it.langCode == "en" }

        return localization?.meaning ?: ""
    }
}