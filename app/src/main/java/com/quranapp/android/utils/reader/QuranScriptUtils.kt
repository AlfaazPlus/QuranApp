package com.quranapp.android.utils.reader

import android.content.Context
import androidx.annotation.DimenRes
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.utils.app.AppUtils
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

    const val PREVIEW_TEXT_INDOPAK = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِیْمِ "
    const val PREVIEW_TEXT_UTHMANI = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ ١"
    const val PREVIEW_TEXT_KFQPC_V1 = "ﭑ ﭒ ﭓ ﭔ ﭕ"
    const val PREVIEW_TEXT_KFQPC_V2 = "ﱰ ﱱ ﱲ ﱳ ﱴ"
    const val PREVIEW_TEXT_NOOREHUDA = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِیْمِ ﴿﴾"

    const val SCRIPT_DEFAULT = SCRIPT_UTHMANI

    val INDO_PAK_SCRIPT_NAMES = mapOf(
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

    val UTHMANI_SCRIPT_NAMES = mapOf(
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

    val KFQPC_SCRIPT_NAMES = { version: Int ->
        mapOf(
            "en" to "King Fahd Complex V${version}",
            "ar" to "مجمع الملك فهد الإصدار ${version}",
            "bn" to "কিং ফাহাদ কমপ্লেক্স V${version}",
            "ckb" to "لێکدراوی پاشا فەهد v${version}",
            "de" to "König Fahd Komplex V${version}",
            "es" to "Rey Fahd Complex V${version}",
            "fa" to "مجتمع شاه فهد V${version}",
            "fr" to "Complexe Roi Fahad V${version}",
            "gu" to "કિંગ ફહદ કોમ્પ્લેક્સ V${version}",
            "hi" to "राजा फहद कॉम्प्लेक्स v${version}",
            "in" to "Kompleks Raja Fahad V${version}",
            "it" to "Complesso di Re Fahad V${version}",
            "ml" to "കിംഗ് ഫഹദ് സമുച്ചയം v${version}",
            "pt" to "Complexo King Fahad V${version}",
            "tr" to "Kral Fehd Kompleksi V${version}",
            "ur" to "کنگ فہد کمپلیکس V${version}",
        )
    }

    val NOOREHUDA_SCRIPT_NAMES = mapOf(
        "en" to "Noorehuda",
        "ar" to "نورلهدا",
        "bn" to "নূর উল হুদা",
        "ckb" to "نورلهدا",
        "de" to "Noorehuda",
        "es" to "Noorehuda",
        "fa" to "نورلهدا",
        "fr" to "Noorehuda",
        "gu" to "નૂર એ હુદા",
        "hi" to "नूर ए हुदा",
        "in" to "Noorehuda",
        "it" to "Noorehuda",
        "ml" to "നൂർ ഇ ഹുദാ",
        "pt" to "Noorehuda",
        "tr" to "Noorehuda",
        "ur" to "نورلهدا",
    )

    fun availableScriptSlugs(): Array<String> = arrayOf(
//        SCRIPT_NOOREHUDA,
        SCRIPT_UTHMANI,
        SCRIPT_DK_INDOPAK,
        SCRIPT_KFQPC_V1,
        SCRIPT_KFQPC_V2
    )

    /**
     * Get the summary of the KFQPC font download.
     * @return A triple containing counts of the total pages, total downloaded, and the remaining.
     */
    fun getKFQPCFontDownloadedCount(ctx: Context, kfqpcScriptSlug: String): Triple<Int, Int, Int> {
        val fileUtils = FileUtils.newInstance(ctx)
        val kfqpcScriptFontDir = fileUtils.getKFQPCScriptFontDir(kfqpcScriptSlug)
        val totalPages = QuranMeta.totalPages()

        var downloaded = 0

        for (pageNo in 1..totalPages) {
            val fontFile = File(kfqpcScriptFontDir, pageNo.toKFQPCFontFilename())
            val fontFileOld = File(kfqpcScriptFontDir, pageNo.toKFQPCFontFilenameOld())

            if (fontFile.length() > 0L || fontFileOld.length() > 0L) {
                downloaded++
            }
        }

        return Triple(totalPages, downloaded, totalPages - downloaded)
    }
}

fun String.isKFQPCScript(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V1,
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> true

    else -> false
}

fun String.getQuranScriptName(): String {
    val mapToQuery: Map<String, String> = when (this) {
//        QuranScriptUtils.SCRIPT_NOOREHUDA -> QuranScriptUtils.NOOREHUDA_SCRIPT_NAMES
        QuranScriptUtils.SCRIPT_DK_INDOPAK -> QuranScriptUtils.INDO_PAK_SCRIPT_NAMES
        QuranScriptUtils.SCRIPT_KFQPC_V1 -> QuranScriptUtils.KFQPC_SCRIPT_NAMES(1)
        QuranScriptUtils.SCRIPT_KFQPC_V2 -> QuranScriptUtils.KFQPC_SCRIPT_NAMES(2)
        else -> QuranScriptUtils.UTHMANI_SCRIPT_NAMES
    }

    return mapToQuery[Locale.getDefault().toLanguageTag()] ?: mapToQuery["en"]!!
}

fun String.getScriptPreviewText(): String = when (this) {
//    QuranScriptUtils.SCRIPT_NOOREHUDA -> QuranScriptUtils.PREVIEW_TEXT_NOOREHUDA
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> QuranScriptUtils.PREVIEW_TEXT_INDOPAK
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> QuranScriptUtils.PREVIEW_TEXT_KFQPC_V1
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> QuranScriptUtils.PREVIEW_TEXT_KFQPC_V2
    else -> QuranScriptUtils.PREVIEW_TEXT_UTHMANI
}

@DimenRes
fun String.getQuranScriptVerseTextSizeSmallRes(): Int = when (this) {
//    QuranScriptUtils.SCRIPT_NOOREHUDA -> R.dimen.dmnReaderTextSizeArNoorehudaSmall
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> R.dimen.dmnReaderTextSizeArIndoPakSmall
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArQpcV1Small
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> R.dimen.dmnReaderTextSizeArQpcV2Small
    else -> R.dimen.dmnReaderTextSizeArUthmaniSmall
}

fun String.getQuranScriptVerseTextSizeWidgetSP(): Float = when (this) {
//    QuranScriptUtils.SCRIPT_NOOREHUDA -> 21f
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> 21f
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> 20f
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> 15f
    else -> 21f
}

@DimenRes
fun String.getQuranScriptVerseTextSizeMediumRes(): Int = when (this) {
//    QuranScriptUtils.SCRIPT_NOOREHUDA -> R.dimen.dmnReaderTextSizeArNoorehudaMedium
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> R.dimen.dmnReaderTextSizeArIndoPakMedium
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArQpcV1Medium
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> R.dimen.dmnReaderTextSizeArQpcV2Medium
    else -> R.dimen.dmnReaderTextSizeArUthmaniMedium
}

fun String.getQuranScriptFontRes(): Int = when (this) {
//    QuranScriptUtils.SCRIPT_NOOREHUDA -> R.font.noorehuda_quranapp_v2
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> R.font.digital_khatt_indopak
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.font.qpc_v1_page_1
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> R.font.qpc_v2_page_604
    else -> R.font.uthmanic_hafs
}

fun String.getQuranScriptResPath(): String = when (this) {
//    QuranScriptUtils.SCRIPT_NOOREHUDA -> "scripts/script_noorehuda.json" // fixme convert to wbw
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> "scripts/script_dk_indopak.json"
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> "scripts/script_qpc_v1.json"
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> "scripts/script_qpc_v2.json"
    else -> "scripts/script_uthmani.json"
}

fun String.getQuranMushafId(
    variant: QuranScriptVariant?
): Int = when (this) {
//    QuranScriptUtils.SCRIPT_NOOREHUDA,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> {
        when (variant) {
            // QuranScriptVariant.INDOPAK_13 -> 2
            QuranScriptVariant.INDOPAK_15 -> 3
            else -> 4
        }
    }

    QuranScriptUtils.SCRIPT_KFQPC_V1,
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_UTHMANI -> 1

    else -> 0
}

fun Int.toKFQPCFontFilename(): String {
    return "qpc_page_%03d.ttf".format(Locale.ENGLISH, this)
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
        fun fromValue(value: String): QuranScriptVariant? {
            return values().find { it.value == value }
        }
    }
}