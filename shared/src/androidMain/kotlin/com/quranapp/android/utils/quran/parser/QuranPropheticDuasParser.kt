package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.honorificMuhammad
import com.quranapp.android.generated.resources.honorificProphet
import com.quranapp.android.locale.appPlatformLocale
import com.quranapp.android.models.PropheticDuaProphet
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareChapterText
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareChaptersList
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareVersesList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

object QuranPropheticDuasParser {
    private data class CachedPropheticDuas(
        val localeTag: String,
        val quranPropheticDua: List<PropheticDuaProphet>,
    )

    private val cacheLock = Any()
    private var cached: CachedPropheticDuas? = null
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parsed strings and chapter labels depend on [appPlatformLocale]. Cached per locale tag.
     */
    suspend fun parsePropheticDuas(context: Context): List<PropheticDuaProphet> {
        val localeTag = appPlatformLocale().toLanguageTag()

        synchronized(cacheLock) {
            cached?.takeIf { it.localeTag == localeTag }?.let {
                return it.quranPropheticDua
            }
        }

        val parsed = parseDuasInternal(context)

        synchronized(cacheLock) {
            cached = CachedPropheticDuas(localeTag, parsed)
        }

        return parsed
    }

    private suspend fun parseDuasInternal(context: Context): List<PropheticDuaProphet> {
        val repository = DatabaseProvider.getQuranRepository(context)
        val catalog = json.decodeFromString<PropheticDuaFile>(
            PlatformServices.assets.readAssetText("quran_prophetic_duas.json"),
        )
        val prophetList = ArrayList<PropheticDuaProphet>(catalog.prophets.size)

        val honorificMuhammad = getString(Res.string.honorificMuhammad)
        val honorificProphet = getString(Res.string.honorificProphet)

        for (entry in catalog.prophets) {
            val prophet = PropheticDuaProphet(
                order = entry.order,
                name = getString(prophetName(entry.name)),
                honorific = if (entry.order == 25) honorificMuhammad else honorificProphet,
                icon = prophetIcon(entry.drawable),
                thumbnail = entry.thumbnail?.let {
                    "ghraw://AlfaazPlus/QuranAppInventory/master/images/$it"
                },
            )

            val references = entry.references.trim()
            if (references.isNotEmpty()) {
                prophet.references = references
                prophet.verses = prepareVersesList(references, true)
                prophet.chapters = prepareChaptersList(prophet.verses)
                prophet.inChapters = prepareChapterText(context, repository, prophet.chapters, 2)
            }

            prophetList.add(prophet)
        }

        prophetList.sortBy { it.order }

        return prophetList
    }
}

@Serializable
private data class PropheticDuaFile(
    val prophets: List<PropheticDuaEntry> = emptyList(),
)

@Serializable
private data class PropheticDuaEntry(
    val name: String,
    val order: Int = -1,
    val thumbnail: String? = null,
    val drawable: String = "",
    val references: String = "",
)
