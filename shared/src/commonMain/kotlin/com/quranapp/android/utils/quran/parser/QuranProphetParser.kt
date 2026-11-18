package com.quranapp.android.utils.quran.parser

import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.honorificMuhammad
import com.quranapp.android.generated.resources.honorificProphet
import com.quranapp.android.generated.resources.prophetAdam
import com.quranapp.android.generated.resources.prophetAlyasaa
import com.quranapp.android.generated.resources.prophetAyyub
import com.quranapp.android.generated.resources.prophetDawud
import com.quranapp.android.generated.resources.prophetDhulkifl
import com.quranapp.android.generated.resources.prophetHarun
import com.quranapp.android.generated.resources.prophetHud
import com.quranapp.android.generated.resources.prophetIbrahim
import com.quranapp.android.generated.resources.prophetIdrees
import com.quranapp.android.generated.resources.prophetIliyas
import com.quranapp.android.generated.resources.prophetIsa
import com.quranapp.android.generated.resources.prophetIshaq
import com.quranapp.android.generated.resources.prophetIsmail
import com.quranapp.android.generated.resources.prophetLut
import com.quranapp.android.generated.resources.prophetMuhammad
import com.quranapp.android.generated.resources.prophetMusa
import com.quranapp.android.generated.resources.prophetNuh
import com.quranapp.android.generated.resources.prophetSaleh
import com.quranapp.android.generated.resources.prophetShuaib
import com.quranapp.android.generated.resources.prophetSulayman
import com.quranapp.android.generated.resources.prophetYahya
import com.quranapp.android.generated.resources.prophetYaqub
import com.quranapp.android.generated.resources.prophetYunus
import com.quranapp.android.generated.resources.prophetYusuf
import com.quranapp.android.generated.resources.prophetZakariyya
import com.quranapp.android.generated.resources.prophet_adam
import com.quranapp.android.generated.resources.prophet_al_yasaa
import com.quranapp.android.generated.resources.prophet_ayyub
import com.quranapp.android.generated.resources.prophet_dawud
import com.quranapp.android.generated.resources.prophet_dhul_kifl
import com.quranapp.android.generated.resources.prophet_harun
import com.quranapp.android.generated.resources.prophet_hud
import com.quranapp.android.generated.resources.prophet_ibrahim
import com.quranapp.android.generated.resources.prophet_idrees
import com.quranapp.android.generated.resources.prophet_iliyas
import com.quranapp.android.generated.resources.prophet_isa
import com.quranapp.android.generated.resources.prophet_ishaq
import com.quranapp.android.generated.resources.prophet_ismail
import com.quranapp.android.generated.resources.prophet_lut
import com.quranapp.android.generated.resources.prophet_muhammad
import com.quranapp.android.generated.resources.prophet_musa
import com.quranapp.android.generated.resources.prophet_nuh
import com.quranapp.android.generated.resources.prophet_saleh
import com.quranapp.android.generated.resources.prophet_shuaib
import com.quranapp.android.generated.resources.prophet_sulayman
import com.quranapp.android.generated.resources.prophet_yahya
import com.quranapp.android.generated.resources.prophet_yaqub
import com.quranapp.android.generated.resources.prophet_yunus
import com.quranapp.android.generated.resources.prophet_yusuf
import com.quranapp.android.generated.resources.prophet_zakariyya
import com.quranapp.android.locale.appLocale
import com.quranapp.android.models.Prophet
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareChapterText
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareChaptersList
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareVersesList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

object QuranProphetParser {

    private data class CachedProphets(
        val localeTag: String,
        val quranProphet: List<Prophet>,
    )

    private val cacheLock = Any()
    private var cached: CachedProphets? = null
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parsed strings and chapter labels depend on [com.quranapp.android.locale.appPlatformLocale]. Cached per locale tag.
     */
    suspend fun parseProphets(): List<Prophet> {
        val localeTag = appLocale().languageTag

        synchronized(cacheLock) {
            cached?.takeIf { it.localeTag == localeTag }?.let {
                return it.quranProphet
            }
        }

        val parsed = parseProphetsInternal()

        synchronized(cacheLock) {
            cached = CachedProphets(localeTag, parsed)
        }

        return parsed
    }

    private suspend fun parseProphetsInternal(): List<Prophet> {
        val repository = DatabaseProvider.getQuranRepository()
        val catalog = json.decodeFromString<ProphetCatalogFile>(
            PlatformServices.assets.readAssetText("quran_prophets.json"),
        )
        val prophetList = ArrayList<Prophet>(catalog.prophets.size)

        val honorificMuhammad = getString(Res.string.honorificMuhammad)
        val honorificProphet = getString(Res.string.honorificProphet)

        for (entry in catalog.prophets) {
            val prophet = Prophet(
                order = entry.order,
                nameAr = entry.nameAr,
                nameEn = entry.nameEn,
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
                prophet.inChapters = prepareChapterText( repository, prophet.chapters, 1)
            }

            prophetList.add(prophet)
        }

        prophetList.sortBy { it.order }

        return prophetList
    }
}

fun prophetName(key: String): StringResource = when (key) {
    "prophetAdam" -> Res.string.prophetAdam
    "prophetAlyasaa" -> Res.string.prophetAlyasaa
    "prophetAyyub" -> Res.string.prophetAyyub
    "prophetDawud" -> Res.string.prophetDawud
    "prophetDhulkifl" -> Res.string.prophetDhulkifl
    "prophetHarun" -> Res.string.prophetHarun
    "prophetHud" -> Res.string.prophetHud
    "prophetIbrahim" -> Res.string.prophetIbrahim
    "prophetIdrees" -> Res.string.prophetIdrees
    "prophetIliyas" -> Res.string.prophetIliyas
    "prophetIsa" -> Res.string.prophetIsa
    "prophetIshaq" -> Res.string.prophetIshaq
    "prophetIsmail" -> Res.string.prophetIsmail
    "prophetLut" -> Res.string.prophetLut
    "prophetMuhammad" -> Res.string.prophetMuhammad
    "prophetMusa" -> Res.string.prophetMusa
    "prophetNuh" -> Res.string.prophetNuh
    "prophetSaleh" -> Res.string.prophetSaleh
    "prophetShuaib" -> Res.string.prophetShuaib
    "prophetSulayman" -> Res.string.prophetSulayman
    "prophetYahya" -> Res.string.prophetYahya
    "prophetYaqub" -> Res.string.prophetYaqub
    "prophetYunus" -> Res.string.prophetYunus
    "prophetYusuf" -> Res.string.prophetYusuf
    "prophetZakariyya" -> Res.string.prophetZakariyya
    else -> error("Unknown prophet $key")
}

fun prophetIcon(key: String): DrawableResource = when (key) {
    "prophet_adam" -> Res.drawable.prophet_adam
    "prophet_al_yasaa" -> Res.drawable.prophet_al_yasaa
    "prophet_ayyub" -> Res.drawable.prophet_ayyub
    "prophet_dawud" -> Res.drawable.prophet_dawud
    "prophet_dhul_kifl" -> Res.drawable.prophet_dhul_kifl
    "prophet_harun" -> Res.drawable.prophet_harun
    "prophet_hud" -> Res.drawable.prophet_hud
    "prophet_ibrahim" -> Res.drawable.prophet_ibrahim
    "prophet_idrees" -> Res.drawable.prophet_idrees
    "prophet_iliyas" -> Res.drawable.prophet_iliyas
    "prophet_isa" -> Res.drawable.prophet_isa
    "prophet_ishaq" -> Res.drawable.prophet_ishaq
    "prophet_ismail" -> Res.drawable.prophet_ismail
    "prophet_lut" -> Res.drawable.prophet_lut
    "prophet_muhammad" -> Res.drawable.prophet_muhammad
    "prophet_musa" -> Res.drawable.prophet_musa
    "prophet_nuh" -> Res.drawable.prophet_nuh
    "prophet_saleh" -> Res.drawable.prophet_saleh
    "prophet_shuaib" -> Res.drawable.prophet_shuaib
    "prophet_sulayman" -> Res.drawable.prophet_sulayman
    "prophet_yahya" -> Res.drawable.prophet_yahya
    "prophet_yaqub" -> Res.drawable.prophet_yaqub
    "prophet_yunus" -> Res.drawable.prophet_yunus
    "prophet_yusuf" -> Res.drawable.prophet_yusuf
    "prophet_zakariyya" -> Res.drawable.prophet_zakariyya
    else -> error("Unknown prophet icon $key")
}

@Serializable
private data class ProphetCatalogFile(
    val prophets: List<ProphetCatalogEntry> = emptyList(),
)

@Serializable
private data class ProphetCatalogEntry(
    val name: String,
    val nameAr: String = "",
    val nameEn: String = "",
    val order: Int = -1,
    val thumbnail: String? = null,
    val drawable: String = "",
    val references: String = "",
)
