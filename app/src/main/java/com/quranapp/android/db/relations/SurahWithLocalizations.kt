package com.quranapp.android.db.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.entities.quran.SurahLocalizationEntity
import com.quranapp.android.db.interfaces.SurahMethods

data class SurahWithLocalizations(
    @Embedded
    val surah: SurahEntity,

    @Relation(
        parentColumns = ["surah_no"],
        entityColumns = ["surah_no"],
    )
    val localizations: List<SurahLocalizationEntity>,
) : SurahMethods by surah {
    fun getCurrentName(): String {
        return appFallbackLanguageCodes()
            .firstNotNullOfOrNull { code ->
                localizations.firstOrNull { it.langCode == code && !it.name.isNullOrBlank() }
            }?.name.orEmpty()
    }

    fun getCurrentMeaning(): String {
        return appFallbackLanguageCodes()
            .firstNotNullOfOrNull { code ->
                localizations.firstOrNull { it.langCode == code && !it.meaning.isNullOrBlank() }
            }?.meaning.orEmpty()
    }
}
