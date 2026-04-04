package com.quranapp.android.utils.reader

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import com.quranapp.android.utils.extensions.asFontFamily
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.univ.FileUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FontResolver(val context: Context) {
    // non-KFQPC: one font per script
    private val scriptFontCache = ConcurrentHashMap<String, FontFamily>()

    // KFQPC: one font per (script, pageNo)
    private val kfqpcFontCache = ConcurrentHashMap<Pair<String, Int>, FontFamily>()

    private val fileUtils by lazy { FileUtils.newInstance(context) }

    private fun loadKfqpcTypeface(
        script: String,
        pageNo: Int,
    ): Typeface {
        return try {
            val fontsDir = fileUtils.getKFQPCScriptFontDir(script)

            val newFile = File(fontsDir, pageNo.toKFQPCFontFilename())
            val oldFile = File(fontsDir, pageNo.toKFQPCFontFilenameOld())

            when {
                newFile.exists() && newFile.length() > 0L ->
                    Typeface.createFromFile(newFile)

                oldFile.exists() && oldFile.length() > 0L ->
                    Typeface.createFromFile(oldFile)

                else -> Typeface.DEFAULT
            }
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }


    fun fontFamily(
        script: String,
        pageNo: Int,
    ): FontFamily {
        return if (script.isKFQPCScript()) {
            kfqpcFontCache.getOrPut(script to pageNo) {
                loadKfqpcTypeface(script, pageNo).asFontFamily()
            }
        } else {
            scriptFontCache.getOrPut(script) {
                context.getFont(script.getQuranScriptFontRes())!!.asFontFamily()
            }
        }
    }

    fun prefetch(script: String, pages: List<Int>) {
        if (!script.isKFQPCScript()) {
            scriptFontCache.getOrPut(script) {
                context.getFont(script.getQuranScriptFontRes())?.asFontFamily()
            }
            return
        }

        pages
            .distinct()
            .forEach { pageNo ->
                kfqpcFontCache.getOrPut(script to pageNo) {
                    loadKfqpcTypeface(script, pageNo).asFontFamily()
                }
            }
    }
}