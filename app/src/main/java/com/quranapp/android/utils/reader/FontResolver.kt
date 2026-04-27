package com.quranapp.android.utils.reader

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.quranapp.android.utils.extensions.asFontFamily
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.univ.FileUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FontResolver private constructor(val context: Context) {
    // non-KFQPC: one font per script
    private val scriptFontCache = ConcurrentHashMap<String, FontFamily>()

    // KFQPC: one font per (script, pageNo, dark/light resolution for UI)
    private val kfqpcFontCache = ConcurrentHashMap<Triple<String, Int, Boolean>, FontFamily>()

    private val fileUtils by lazy { FileUtils.newInstance(context) }

    fun getKfqpcTypeface(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): Typeface = loadKfqpcTypeface(script, pageNo, isDark)

    private fun loadKfqpcTypeface(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): Typeface {
        return try {
            val fontsDir = fileUtils.getKFQPCScriptFontDir(script)
            val useDark = isDark && script.getQuranScriptFontHasDark()
            val darkFile = File(fontsDir, pageNo.toKFQPCFontFilename(true))
            val lightFile = File(fontsDir, pageNo.toKFQPCFontFilename(false))
            val oldFile = File(fontsDir, pageNo.toKFQPCFontFilenameOld())

            val primary = if (useDark) darkFile else lightFile
            val fallbackTtf = if (useDark) lightFile else null

            when {
                primary.exists() && primary.length() > 0L ->
                    Typeface.createFromFile(primary)

                fallbackTtf != null && fallbackTtf.exists() && fallbackTtf.length() > 0L ->
                    Typeface.createFromFile(fallbackTtf)

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
        isDark: Boolean,
    ): FontFamily {
        return if (script.isKFQPCScript()) {
            kfqpcFontCache.getOrPut(Triple(script, pageNo, isDark)) {
                loadKfqpcTypeface(script, pageNo, isDark).asFontFamily()
            }
        } else {
            scriptFontCache.getOrPut(script) {
                context
                    .getFont(script.getQuranScriptFontRes(isDark))
                    ?.asFontFamily()
                    ?: FontFamily.Default
            }
        }
    }

    fun prefetch(script: String, pages: List<Int>, isDark: Boolean) {
        if (!script.isKFQPCScript()) {
            scriptFontCache.getOrPut(script) {
                context
                    .getFont(script.getQuranScriptFontRes(isDark))
                    ?.asFontFamily()
                    ?: FontFamily.Default
            }
            return
        }

        pages
            .distinct()
            .forEach { pageNo ->
                kfqpcFontCache.getOrPut(Triple(script, pageNo, isDark)) {
                    loadKfqpcTypeface(script, pageNo, isDark).asFontFamily()
                }
            }
    }

    companion object {
        @Volatile
        private var instance: FontResolver? = null

        @JvmStatic
        fun getInstance(context: Context): FontResolver {
            return instance ?: synchronized(this) {
                instance ?: FontResolver(context).also {
                    instance = it
                }
            }
        }

        @Composable
        fun remember(): FontResolver? {
            val context = LocalContext.current

            val state = produceState<FontResolver?>(initialValue = null, context) {
                value = getInstance(context)
            }

            return state.value
        }
    }
}