package com.quranapp.android.utils.reader

import android.content.Context
import androidx.annotation.DimenRes
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.utils.Logger
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
    const val SCRIPT_KFQPC_V1 = "kfqpc_v1"
    const val SCRIPT_KFQPC_V2 = "kfqpc_v2"
    const val SCRIPT_NOOREHUDA = "noorehuda"

    const val PREVIEW_TEXT_UTHMANI = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ ١"
    const val PREVIEW_TEXT_KFQPC_V1 = "ﭑ ﭒ ﭓ ﭔ ﭕ"
    const val PREVIEW_TEXT_KFQPC_V2 = "ﱰ ﱱ ﱲ ﱳ ﱴ"
    const val PREVIEW_TEXT_NOOREHUDA = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِیْمِ ﴿﴾"

    const val SCRIPT_DEFAULT = SCRIPT_NOOREHUDA

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
        SCRIPT_NOOREHUDA,
        SCRIPT_UTHMANI,
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
        QuranScriptUtils.SCRIPT_UTHMANI -> QuranScriptUtils.UTHMANI_SCRIPT_NAMES
        QuranScriptUtils.SCRIPT_KFQPC_V1 -> QuranScriptUtils.KFQPC_SCRIPT_NAMES(1)
        QuranScriptUtils.SCRIPT_KFQPC_V2 -> QuranScriptUtils.KFQPC_SCRIPT_NAMES(2)
        else -> QuranScriptUtils.NOOREHUDA_SCRIPT_NAMES
    }

    return mapToQuery[Locale.getDefault().toLanguageTag()] ?: mapToQuery["en"]!!
}

fun String.getScriptPreviewText(): String = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI -> QuranScriptUtils.PREVIEW_TEXT_UTHMANI
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> QuranScriptUtils.PREVIEW_TEXT_KFQPC_V1
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> QuranScriptUtils.PREVIEW_TEXT_KFQPC_V2
    else -> QuranScriptUtils.PREVIEW_TEXT_NOOREHUDA
}

@DimenRes
fun String.getQuranScriptVerseTextSizeSmallRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI -> R.dimen.dmnReaderTextSizeArUthmaniSmall
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArQpcV1Small
    QuranScriptUtils.SCRIPT_KFQPC_V2
        -> R.dimen.dmnReaderTextSizeArQpcV2Small

    else -> R.dimen.dmnReaderTextSizeArNoorehudaSmall
}

fun String.getQuranScriptVerseTextSizeWidgetSP(): Float = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI -> 21f
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> 20f
    QuranScriptUtils.SCRIPT_KFQPC_V2
        -> 15f

    else -> 21f
}

@DimenRes
fun String.getQuranScriptVerseTextSizeMediumRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI -> R.dimen.dmnReaderTextSizeArUthmaniMedium
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArQpcV1Medium
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> R.dimen.dmnReaderTextSizeArQpcV2Medium

    else -> R.dimen.dmnReaderTextSizeArNoorehudaMedium
}

fun String.getQuranScriptFontRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI -> R.font.uthmanic_hafs
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.font.qpc_v1_page_1
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> R.font.qpc_v2_page_604
    else -> R.font.noorehuda_quranapp_v2
}

fun String.getQuranScriptResPath(): String = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI -> "scripts/script_uthmani_hafs.json"
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> "scripts/script_qpc_v1.json"
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> "scripts/script_qpc_v2.json"
    else -> "scripts/script_noorehuda.json"
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
