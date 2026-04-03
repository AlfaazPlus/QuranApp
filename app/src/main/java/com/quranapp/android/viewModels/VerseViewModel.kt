package com.quranapp.android.viewModels

import android.app.Application
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.asFontFamily
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.getQuranScriptFontRes
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.toKFQPCFontFilename
import com.quranapp.android.utils.reader.toKFQPCFontFilenameOld
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class VerseViewModel(application: Application) : AndroidViewModel(application) {
    private val fileUtils by lazy { FileUtils.newInstance(application) }
    val translationFactory by lazy {
        QuranTranslationFactory(application)
    }

    val controller = RecitationController.getInstance(application)

    // non-KFQPC: one font per script
    private val scriptFontCache = ConcurrentHashMap<String, FontFamily>()

    // KFQPC: one font per (script, pageNo)
    private val kfqpcFontCache = ConcurrentHashMap<Pair<String, Int>, FontFamily>()


    init {
        controller.connect()
    }

    override fun onCleared() {
        controller.disconnect()
        translationFactory.close()
        super.onCleared()
    }

    fun fontFamily(
        script: String,
        verse: Verse,
    ): FontFamily {
        Log.d(script, scriptFontCache)
        return if (script.isKFQPCScript()) {
            kfqpcFontCache.getOrPut(script to verse.pageNo) {
                loadKfqpcTypeface(script, verse.pageNo).asFontFamily()
            }
        } else {
            scriptFontCache.getOrPut(script) {
                application.getFont(script.getQuranScriptFontRes())!!.asFontFamily()
            }
        }
    }

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

    /**
     * Optional warm-up for visible/nearby verses.
     * Call once per page/chunk, not inside row composition.
     */
    fun prefetch(script: String, verses: List<Verse>) {
        if (!script.isKFQPCScript()) {
            scriptFontCache.getOrPut(script) {
                application.getFont(script.getQuranScriptFontRes())?.asFontFamily()
            }
            return
        }

        verses
            .asSequence()
            .map { it.pageNo }
            .distinct()
            .forEach { pageNo ->
                kfqpcFontCache.getOrPut(script to pageNo) {
                    loadKfqpcTypeface(script, pageNo).asFontFamily()
                }
            }
    }
}
