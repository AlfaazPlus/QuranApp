package com.quranapp.android.utils.reader

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.preview_dk_indopak
import com.quranapp.android.generated.resources.preview_kfqpc_v1
import com.quranapp.android.generated.resources.preview_kfqpc_v2
import com.quranapp.android.generated.resources.preview_kfqpc_v4_tajweed
import com.quranapp.android.generated.resources.preview_kfqpc_v4_tajweed_dark
import com.quranapp.android.generated.resources.preview_uthmani_hafs
import com.quranapp.android.generated.resources.uthmanic_hafs
import com.quranapp.android.locale.appFallbackLanguageCodes
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.miscellenous.toFormatted
import com.quranapp.android.utils.reader.QuranScriptUtils.SCRIPT_NAMES
import com.quranapp.android.utils.reader.QuranScriptUtils.VARIANT_NAMES
import okio.FileSystem
import okio.Path
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.FontResource

object QuranScriptUtils {
    private const val FONTS_DIR_NAME = "script_fonts"
    private const val SCRIPTS_DIR_NAME = "scripts"

    fun fontDir(): Path = AppFiles.savedDir(FONTS_DIR_NAME)

    fun kfqpcFontDir(scriptSlug: String): Path = AppFiles.savedDir(FONTS_DIR_NAME, scriptSlug)

    fun scriptFile(scriptKey: String): Path {
        return AppFiles.savedFile(SCRIPTS_DIR_NAME, "$scriptKey.json")
    }

    const val KEY_SCRIPT = "key.script"

    const val SCRIPT_UTHMANI = QuranScripts.UTHMANI
    const val SCRIPT_DK_INDOPAK = QuranScripts.DK_INDOPAK
    const val SCRIPT_NASTALEEQ_INDOPAK = QuranScripts.NASTALEEQ_INDOPAK
    const val SCRIPT_KFQPC_V1 = QuranScripts.KFQPC_V1
    const val SCRIPT_KFQPC_V2 = QuranScripts.KFQPC_V2
    const val SCRIPT_KFQPC_V4 = QuranScripts.KFQPC_V4

    const val SCRIPT_DEFAULT = QuranScripts.DEFAULT

    fun SCRIPT_NAMES(scriptCode: String): Map<String, String> {
        return when (scriptCode) {
            SCRIPT_NASTALEEQ_INDOPAK,
            SCRIPT_DK_INDOPAK -> mapOf(
                "en" to "IndoPak",
                "ar" to "نستعليق",
                "bn" to "ইন্দোপাক",
                "ckb" to "هیندوپاک",
                "de" to "IndoPak",
                "es" to "IndoPak",
                "fa" to "هند پاک",
                "fr" to "IndoPak",
                "gu" to "ઈન્ડોપાક",
                "hi" to "इंडो पाक",
                "in" to "IndoPak",
                "it" to "IndoPak",
                "ml" to "ഇൻഡോപാക്",
                "pt" to "IndoPak",
                "tr" to "Hint Paketi",
                "ur" to "انڈو پاک",
            )

            SCRIPT_UTHMANI -> mapOf(
                "en" to "Uthmani Hafs",
                "ar" to "العثماني حفص",
                "bn" to "উসমানী হাফস",
                "ckb" to "حەفسی عوسمانی",
                "de" to "Uthmani Hafs",
                "es" to "Uthmani Hafs",
                "fa" to "عثمانی حفص",
                "fr" to "Uthmani Hafs",
                "gu" to "ઉથમાની હાફ્સ",
                "hi" to "उशमानी हफ्स",
                "in" to "Utsmani Hafs",
                "it" to "Uthmani Hafs",
                "ml" to "ഓട്ടോമൻ ഹാഫുകൾ",
                "pt" to "Uthmani Hafs",
                "tr" to "Osmanca Hafs",
                "ur" to "عثمانی حفص",
            )

            SCRIPT_KFQPC_V1,
            SCRIPT_KFQPC_V2,
            SCRIPT_KFQPC_V4 -> {
                val suffix = when {
                    scriptCode == SCRIPT_KFQPC_V1 -> "V1"
                    scriptCode == SCRIPT_KFQPC_V2 -> "V2"
                    scriptCode == SCRIPT_KFQPC_V4 -> "V4 (Tajweed)"
                    else -> ""
                }

                mapOf(
                    "en" to "King Fahd Complex $suffix",
                    "ar" to "مجمع الملك فهد الإصدار $suffix",
                    "bn" to "কিং ফাহাদ কমপ্লেক্স $suffix",
                    "ckb" to "لێکدراوی پاشا فەهد v$suffix",
                    "de" to "König Fahd Komplex $suffix",
                    "es" to "Rey Fahd Complex $suffix",
                    "fa" to "مجتمع شاه فهد $suffix",
                    "fr" to "Complexe Roi Fahad $suffix",
                    "gu" to "કિંગ ફહદ કોમ્પ્લેક્સ $suffix",
                    "hi" to "राजा फहद कॉम्प्लेक्स $suffix",
                    "in" to "Kompleks Raja Fahad $suffix",
                    "it" to "Complesso di Re Fahad $suffix",
                    "ml" to "കിംഗ് ഫഹദ് സമുച്ചയം $suffix",
                    "pt" to "Complexo King Fahad $suffix",
                    "tr" to "Kral Fehd Kompleksi $suffix",
                    "ur" to "کنگ فہد کمپلیکس $suffix",
                )
            }

            else -> emptyMap()
        }
    }

    val VARIANT_NAMES: Map<QuranScriptVariant, Map<String, String>> = mapOf(
        QuranScriptVariant.INDOPAK_15 to mapOf(
            "en" to "Indopak 15 lines"
        ),
        QuranScriptVariant.INDOPAK_16 to mapOf(
            "en" to "Indopak 16 lines"
        )
    )

    fun availableScripts() = mapOf(
        SCRIPT_UTHMANI to listOf(),
        SCRIPT_KFQPC_V1 to listOf(),
        SCRIPT_KFQPC_V2 to listOf(),
        SCRIPT_KFQPC_V4 to listOf(),
        SCRIPT_DK_INDOPAK to listOf(
            QuranScriptVariant.INDOPAK_15,
            QuranScriptVariant.INDOPAK_16
        ),
    )

    fun validatePreferredScript(scriptCode: String): String = QuranScripts.coerce(scriptCode)

    data class DownloadedFontsInfo(
        val totalFonts: Int,
        val downloaded: Int,
        val remaining: Int
    )

    /**
     * Get the summary of the KFQPC font download.
     * @return A triple containing counts of the total pages, total downloaded, and the remaining.
     */
    fun getKFQPCFontDownloadedCount(kfqpcScriptSlug: String): DownloadedFontsInfo {
        val kfqpcScriptFontDir = kfqpcFontDir(kfqpcScriptSlug)
        val totalPages = 604
        val hasDark = kfqpcScriptSlug.getQuranScriptFontHasDark()

        var downloaded = 0

        for (pageNo in 1..totalPages) {
            val lightFile = kfqpcScriptFontDir / pageNo.toKFQPCFontFilename(false)
            val darkFile = if (hasDark) {
                kfqpcScriptFontDir / pageNo.toKFQPCFontFilename(true)
            } else {
                null
            }
            val fontFileOld = kfqpcScriptFontDir / pageNo.toKFQPCFontFilenameOld()

            if (lightFile.hasBytes() || darkFile.hasBytes() || fontFileOld.hasBytes()) {
                downloaded++
            }
        }

        return DownloadedFontsInfo(totalPages, downloaded, totalPages - downloaded)
    }
}

fun String.isKFQPCScript(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V1,
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> true

    else -> false
}

fun String.isQuranAtlasScript(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> true

    else -> false
}

fun String.isPrebuiltAtlas(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI -> true
    else -> false
}

fun String.toAtlasBundleDownloadKey(): String = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> "dk_indopak_v2"
    else -> this
}

/**
 * When true, page mode draws a thin frame around the mushaf text block and horizontal rules between
 * lines.
 */
fun String.mushafShowsRuledPageDecoration(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_NASTALEEQ_INDOPAK,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> true

    else -> false
}

fun String.getQuranScriptName(): String {
    val mapToQuery = SCRIPT_NAMES(this)

    return appFallbackLanguageCodes()
        .firstNotNullOfOrNull { mapToQuery[it] }
        ?: ""
}

fun String.getQuranScriptVerseTextSizeSmallSp(): Float = when (this) {
    QuranScriptUtils.SCRIPT_NASTALEEQ_INDOPAK,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> 23f

    QuranScriptUtils.SCRIPT_KFQPC_V1 -> 24f
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> 17f

    else -> 23f
}

fun String.getQuranScriptVerseTextSizeMediumSp(): Float = when (this) {
    QuranScriptUtils.SCRIPT_NASTALEEQ_INDOPAK,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> 28f

    QuranScriptUtils.SCRIPT_KFQPC_V1 -> 30f
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> 22f

    else -> 28f
}

fun String.getQuranScriptFontRes(isDark: Boolean): FontResource {
    return Res.font.uthmanic_hafs
}

fun String.getQuranScriptPreview(isDark: Boolean): DrawableResource = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> Res.drawable.preview_dk_indopak
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> Res.drawable.preview_kfqpc_v1
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> Res.drawable.preview_kfqpc_v2
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> if (isDark) Res.drawable.preview_kfqpc_v4_tajweed_dark
    else Res.drawable.preview_kfqpc_v4_tajweed

    else -> Res.drawable.preview_uthmani_hafs
}

/**
 * In KBs
 * Download size -> Uncompressed size
 */
fun String.getQuranScriptFontPackSizeMb(): Pair<Int, Int> = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> Pair(1213, 1213)
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> Pair(52000, 90000)
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> Pair(129000, 200000)
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> Pair(132000, 320000)
    else -> Pair(0, 0)
}

fun String.getQuranScriptFontHasDark(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> true
    else -> false
}

fun String.toQuranMushafId(
    variant: QuranScriptVariant?
): Int = when (this) {
    QuranScriptUtils.SCRIPT_NASTALEEQ_INDOPAK,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> {
        when (variant) {
            QuranScriptVariant.INDOPAK_15 -> 3
            else -> 4
        }
    }

    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4,
    QuranScriptUtils.SCRIPT_UTHMANI -> 1

    QuranScriptUtils.SCRIPT_KFQPC_V1 -> 5

    else -> 0
}

fun QuranScriptVariant.getQuranScriptVariantName(): String {
    val mapToQuery = VARIANT_NAMES[this] ?: return ""

    return appFallbackLanguageCodes()
        .firstNotNullOfOrNull { mapToQuery[it] }
        ?: ""
}

fun Int.toKFQPCFontFilename(isDark: Boolean): String {
    val format = if (isDark) "qpc_page_%03d_dark.ttf" else "qpc_page_%03d.ttf"
    return format.toFormatted(this)
}

/**
 * For backward compatibility with old KFQPC font filenames.
 */
fun Int.toKFQPCFontFilenameOld(): String {
    return "qpc_page_%03d.TTF".toFormatted(this)
}

private fun Path?.hasBytes(): Boolean {
    if (this == null) return false
    val size = FileSystem.SYSTEM.metadataOrNull(this)?.size ?: return false
    return size > 0L
}

data class QuranScript(
    val scriptCode: String,
    var variant: QuranScriptVariant?
) {
    fun toMushafId(): Int {
        return scriptCode.toQuranMushafId(variant)
    }

    companion object {
        fun fromRawValues(
            script: String,
            variant: String?
        ): QuranScript {
            return QuranScript(
                script,
                QuranScriptVariant.fromValue(variant)
            )
        }
    }
}
