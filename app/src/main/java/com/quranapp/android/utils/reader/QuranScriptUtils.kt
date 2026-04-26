package com.quranapp.android.utils.reader

import android.content.Context
import androidx.annotation.DimenRes
import androidx.compose.runtime.Composable
import com.quranapp.android.R
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.reader.QuranScriptUtils.SCRIPT_NAMES
import com.quranapp.android.utils.reader.QuranScriptUtils.VARIANT_NAMES
import com.quranapp.android.utils.univ.FileUtils
import java.io.File
import java.util.Locale

object QuranScriptUtils {
    val FONTS_DIR_NAME: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "script_fonts"
    )
    val SCRIPT_DIR_NAME: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "scripts"
    )

    const val KEY_SCRIPT = "key.script"

    const val SCRIPT_UTHMANI = "uthmani"

    const val SCRIPT_DK_INDOPAK = "dk_indopak"
    const val SCRIPT_KFQPC_V1 = "kfqpc_v1"
    const val SCRIPT_KFQPC_V2 = "kfqpc_v2"
    const val SCRIPT_KFQPC_V4 = "kfqpc_v4_tajweed"

    const val PREVIEW_TEXT_DK_INDOPAK = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ \u06DD"
    const val PREVIEW_TEXT_UTHMANI = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ ١"
    const val PREVIEW_TEXT_KFQPC_V1 = "ﭑ ﭒ ﭓ ﭔ ﭕ"
    const val PREVIEW_TEXT_KFQPC_V2 = "ﱰ ﱱ ﱲ ﱳ ﱴ"
    const val PREVIEW_TEXT_KFQPC_V4 = "ﱗ ﱘ ﱙ ﱚ"

    const val SCRIPT_DEFAULT = SCRIPT_UTHMANI

    fun SCRIPT_NAMES(scriptCode: String): Map<String, String> {
        return when (scriptCode) {
            SCRIPT_DK_INDOPAK -> mapOf(
                "en" to "IndoPak (Beta)",
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

    fun validatePreferredScript(scriptCode: String): String {
        if (!QuranScriptUtils.availableScripts().contains(scriptCode)) {
            return QuranScriptUtils.SCRIPT_DEFAULT
        }

        return scriptCode
    }

    data class DownloadedFontsInfo(
        val totalFonts: Int,
        val downloaded: Int,
        val remaining: Int
    )

    /**
     * Get the summary of the KFQPC font download.
     * @return A triple containing counts of the total pages, total downloaded, and the remaining.
     */
    fun getKFQPCFontDownloadedCount(ctx: Context, kfqpcScriptSlug: String): DownloadedFontsInfo {
        val fileUtils = FileUtils.newInstance(ctx)
        val kfqpcScriptFontDir = fileUtils.getKFQPCScriptFontDir(kfqpcScriptSlug)
        val totalPages = 604

        var downloaded = 0

        for (pageNo in 1..totalPages) {
            val lightFile = File(kfqpcScriptFontDir, pageNo.toKFQPCFontFilename(false))

            val darkFile = if (kfqpcScriptSlug.getQuranScriptFontHasDark()) {
                File(kfqpcScriptFontDir, pageNo.toKFQPCFontFilename(true))
            } else {
                null
            }

            val fontFileOld = File(kfqpcScriptFontDir, pageNo.toKFQPCFontFilenameOld())

            if (
                lightFile.length() > 0L ||
                (darkFile != null && darkFile.length() > 0L) ||
                fontFileOld.length() > 0L
            ) {
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

/**
 * When true, page mode draws a thin frame around the mushaf text block and horizontal rules between
 * lines.
 */
fun String.mushafShowsRuledPageDecoration(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> true
    else -> false
}

fun String.getQuranScriptName(): String {
    val mapToQuery = SCRIPT_NAMES(this)
    return appFallbackLanguageCodes()
        .firstNotNullOfOrNull { mapToQuery[it] }
        ?: ""
}

fun String.getScriptPreviewText(): String = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> QuranScriptUtils.PREVIEW_TEXT_DK_INDOPAK
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> QuranScriptUtils.PREVIEW_TEXT_KFQPC_V1
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> QuranScriptUtils.PREVIEW_TEXT_KFQPC_V2
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> QuranScriptUtils.PREVIEW_TEXT_KFQPC_V4
    else -> QuranScriptUtils.PREVIEW_TEXT_UTHMANI
}

@DimenRes
fun String.getQuranScriptVerseTextSizeSmallRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> R.dimen.dmnReaderTextSizeArIndoPakSmall
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArQpcV1Small
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> R.dimen.dmnReaderTextSizeArQpcV2Small

    else -> R.dimen.dmnReaderTextSizeArUthmaniSmall
}

fun String.getQuranScriptVerseTextSizeWidgetSP(): Float = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> 21f
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> 20f
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> 15f

    else -> 21f
}

@DimenRes
fun String.getQuranScriptVerseTextSizeMediumRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> R.dimen.dmnReaderTextSizeArIndoPakMedium
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArQpcV1Medium
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> R.dimen.dmnReaderTextSizeArQpcV2Medium

    else -> R.dimen.dmnReaderTextSizeArUthmaniMedium
}

fun String.getQuranScriptFontRes(isDark: Boolean): Int = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> R.font.digital_khatt_indopak
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.font.qpc_v1_page_1
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> R.font.qpc_v2_page_604
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> if (isDark) R.font.qpc_v4_page_001_dark else R.font.qpc_v4_page_001
    else -> R.font.uthmanic_hafs
}

/**
 * Download size -> Uncompressed size
 */
fun String.getQuranScriptFontPackSizeMb(): Pair<Int, Int> = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> Pair(52, 90)
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> Pair(129, 200)
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> Pair(132, 320)
    else -> Pair(0, 0)
}

fun String.getQuranScriptFontHasDark(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> true
    else -> false
}

fun String.toQuranMushafId(
    variant: QuranScriptVariant?
): Int = when (this) {
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

fun String.toDbScriptCode(): String = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> QuranScriptUtils.SCRIPT_KFQPC_V2
    else -> this
}

@Composable
fun rememberQuranMushafId(): Int {
    val scriptCode = ReaderPreferences.observeQuranScript()
    val scriptVariant = ReaderPreferences.observeQuranScriptVariant()

    return scriptCode.toQuranMushafId(scriptVariant)
}


fun QuranScriptVariant.getQuranScriptVariantName(): String {
    val mapToQuery = VARIANT_NAMES[this] ?: return ""
    return appFallbackLanguageCodes()
        .firstNotNullOfOrNull { mapToQuery[it] }
        ?: ""
}

fun Int.toKFQPCFontFilename(isDark: Boolean): String {
    val format = if (isDark) "qpc_page_%03d_dark.ttf" else "qpc_page_%03d.ttf"
    return format.format(Locale.ENGLISH, this)
}

/**
 * For backward compatibility with old KFQPC font filenames.
 */
fun Int.toKFQPCFontFilenameOld(): String {
    return "qpc_page_%03d.TTF".format(Locale.ENGLISH, this)
}

enum class QuranScriptVariant(val value: String) {
    INDOPAK_16("indopak_16"),
    INDOPAK_15("indopak_15");

    companion object {
        fun fromValue(value: String?): QuranScriptVariant? {
            return values().find { it.value == value }
        }
    }
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