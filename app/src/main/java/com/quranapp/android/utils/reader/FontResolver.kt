package com.quranapp.android.utils.reader

import android.content.Context
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.quranapp.android.utils.extensions.asFontFamily
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.univ.FileUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

private const val KFQPC_TYPEFACE_CACHE_SIZE = 24

private data class TypefaceResult(
    val typeface: Typeface?,
    val fontFamily: FontFamily,
)

private data class ScriptFontKey(
    val script: String,
    val isDark: Boolean,
)

private data class KfqpcFontKey(
    val script: String,
    val pageNo: Int,
    val isDark: Boolean,
)

class FontResolver private constructor(val context: Context) {
    // non-KFQPC: one font per script
    private val scriptFontCache = ConcurrentHashMap<ScriptFontKey, TypefaceResult>()

    // KFQPC has hundreds of page fonts, so keep only the recent reader window resident.
    private val kfqpcFontCache = object : LruCache<KfqpcFontKey, TypefaceResult>(
        KFQPC_TYPEFACE_CACHE_SIZE
    ) {}

    private val kfqpcFontCacheLock = Any()

    private val fileUtils by lazy { FileUtils.newInstance(context) }

    private fun resolveKfqpcFontFile(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): File? {
        return try {
            val fontsDir = fileUtils.getKFQPCScriptFontDir(script)
            val useDark = isDark && script.getQuranScriptFontHasDark()
            val darkFile = File(fontsDir, pageNo.toKFQPCFontFilename(true))
            val lightFile = File(fontsDir, pageNo.toKFQPCFontFilename(false))
            val oldFile = File(fontsDir, pageNo.toKFQPCFontFilenameOld())

            val primary = if (useDark) darkFile else lightFile
            val fallbackTtf = if (useDark) lightFile else null

            when {
                primary.exists() && primary.length() > 0L -> primary
                fallbackTtf != null && fallbackTtf.exists() && fallbackTtf.length() > 0L -> fallbackTtf
                oldFile.exists() && oldFile.length() > 0L -> oldFile
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadKfqpcTypeface(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): Typeface {
        return try {
            val file = resolveKfqpcFontFile(script, pageNo, isDark)
            if (file != null) Typeface.createFromFile(file) else Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    /**
     * Opens a font stream for WebView `@font-face` interception.
     * Atlas scripts use raster images in WebView instead — returns null
     */
    @Throws(IOException::class)
    fun openQuranArabicFontInputStream(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): InputStream? {
        if (script.isQuranAtlasScript()) return null

        if (script.isKFQPCScript()) {
            val file = resolveKfqpcFontFile(script, pageNo, isDark) ?: return null
            return file.inputStream()
        }

        return context.resources.openRawResource(script.getQuranScriptFontRes(isDark))
    }


    fun fontFamily(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): FontFamily {
        return resolve(script, pageNo, isDark)?.fontFamily ?: FontFamily.Default
    }

    fun typeface(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): Typeface? {
        return resolve(script, pageNo, isDark)?.typeface
    }

    private fun resolve(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): TypefaceResult? {
        if (script.isQuranAtlasScript()) return null

        return if (script.isKFQPCScript()) {
            getKfqpcFont(KfqpcFontKey(script, pageNo, isDark))
        } else {
            scriptFontCache.getOrPut(ScriptFontKey(script, isDark)) {
                loadScriptFont(script, isDark)
            }
        }
    }

    private fun loadScriptFont(
        script: String,
        isDark: Boolean,
    ): TypefaceResult {
        return context
            .getFont(script.getQuranScriptFontRes(isDark))
            .toTypefaceResult()
    }

    private fun getKfqpcFont(key: KfqpcFontKey): TypefaceResult {
        synchronized(kfqpcFontCacheLock) {
            kfqpcFontCache.get(key)?.let { return it }
        }

        val loaded = loadKfqpcTypeface(key.script, key.pageNo, key.isDark)
            .toTypefaceResult()

        synchronized(kfqpcFontCacheLock) {
            kfqpcFontCache.get(key)?.let { return it }
            kfqpcFontCache.put(key, loaded)
        }

        return loaded
    }

    private fun Typeface?.toTypefaceResult(): TypefaceResult {
        return TypefaceResult(
            typeface = this,
            fontFamily = this?.asFontFamily() ?: FontFamily.Default,
        )
    }

    fun prefetch(script: String, pages: List<Int>, isDark: Boolean) {
        if (script.isQuranAtlasScript()) return

        if (!script.isKFQPCScript()) {
            scriptFontCache.getOrPut(ScriptFontKey(script, isDark)) {
                loadScriptFont(script, isDark)
            }

            return
        }

        pages
            .distinct()
            .take(KFQPC_TYPEFACE_CACHE_SIZE)
            .forEach { pageNo ->
                getKfqpcFont(KfqpcFontKey(script, pageNo, isDark))
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
