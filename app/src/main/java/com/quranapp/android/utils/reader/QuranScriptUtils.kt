package com.quranapp.android.utils.reader

import android.content.Context
import androidx.annotation.DimenRes
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.FileUtils
import java.io.File

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

    const val SCRIPT_INDO_PAK = "indopak"
    const val SCRIPT_UTHMANI = "uthmani"
    const val SCRIPT_KFQPC_V1 = "kfqpc_v1"

    const val SCRIPT_DEFAULT = SCRIPT_INDO_PAK

    fun availableScriptSlugs(): Array<String> = arrayOf(
        SCRIPT_INDO_PAK,
        SCRIPT_UTHMANI,
        SCRIPT_KFQPC_V1
    )

    fun verifyKFQPCScriptDownloaded(ctx: Context, kfqpcScriptSlug: String): Boolean {
        return FileUtils.newInstance(ctx).getScriptFile(kfqpcScriptSlug).length() > 0
    }

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
            if (File(kfqpcScriptFontDir, pageNo.toKFQPCFontFilename()).length() > 0L) {
                downloaded++
            }
        }

        return Triple(totalPages, downloaded, totalPages - downloaded)
    }
}

fun String.isKFQPCScript(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> true
    else -> false
}

fun String.getQuranScriptName(): String = when (this) {
    QuranScriptUtils.SCRIPT_INDO_PAK -> "IndoPak"
    QuranScriptUtils.SCRIPT_UTHMANI -> "Uthmani"
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> "King Fahd Complex V1"
    else -> ""
}

fun String.getScriptPreviewRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_INDO_PAK -> R.string.strScriptPreviewIndopak
    QuranScriptUtils.SCRIPT_UTHMANI -> R.string.strScriptPreviewUthmani
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.string.strScriptPreviewKFQPC_V1
    else -> 0
}

@DimenRes
fun String.getQuranScriptVerseTextSizeSmallRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_INDO_PAK -> R.dimen.dmnReaderTextSizeArIndoPakSmall
    QuranScriptUtils.SCRIPT_UTHMANI -> R.dimen.dmnReaderTextSizeArUthmaniSmall
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArKFQPCSmall
    else -> 0
}

@DimenRes
fun String.getQuranScriptVerseTextSizeMediumRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_INDO_PAK -> R.dimen.dmnReaderTextSizeArIndoPakMedium
    QuranScriptUtils.SCRIPT_UTHMANI -> R.dimen.dmnReaderTextSizeArUthmaniMedium
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArKFQPCMedium
    else -> 0
}

@DimenRes
fun String.getQuranScriptSerialTextSizeSmallRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_INDO_PAK, QuranScriptUtils.SCRIPT_UTHMANI -> R.dimen.dmnReaderTextSizeArIndoPakSmall
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArKFQPCSmall
    else -> 0
}

@DimenRes
fun String.getQuranScriptSerialTextSizeMediumRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_INDO_PAK, QuranScriptUtils.SCRIPT_UTHMANI -> R.dimen.dmnReaderTextSizeArIndoPakMedium
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.dimen.dmnReaderTextSizeArKFQPCMedium
    else -> 0
}

fun String.getQuranScriptFontRes(): Int = when (this) {
    QuranScriptUtils.SCRIPT_INDO_PAK -> R.font.pdms
    QuranScriptUtils.SCRIPT_UTHMANI -> R.font.me_quran_4_uthmani_text
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> R.font.qpc_page_1
    else -> 0
}

fun String.getQuranScriptResPath(): String = when (this) {
    QuranScriptUtils.SCRIPT_INDO_PAK -> "scripts/script_indopak.json"
    QuranScriptUtils.SCRIPT_UTHMANI -> "scripts/script_uthmani.json"
    else -> ""
}

fun Int.toKFQPCFontFilename(): String {
    return "qpc_page_$this.TTF"
}
