package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.components.quran.Quran
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.subcomponents.Chapter
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.getQuranScriptResPath
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicReference

private const val KEY_CHAPTER_LIST = "suras"
private const val KEY_VERSE_LIST = "ayas"
private const val KEY_ARABIC_TEXT = "text"
private const val KEY_END_TEXT = "end"
private const val KEY_ID = "id"
private const val KEY_NUMBER = "index"
private const val KEY_PAGE_NUMBER = "page"

class QuranParser(private val ctx: Context) {
    fun parse(
        scriptKey: String,
        quranMeta: QuranMeta?,
        quranRef: AtomicReference<Quran>,
        postRunnable: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                initQuranParse(scriptKey, quranMeta, quranRef)
            } catch (e: Exception) {
                Log.saveError(e, "QuranParser.parse")
            }

            withContext(Dispatchers.Main) {
                postRunnable()
            }
        }
    }

    private fun initQuranParse(
        scriptKey: String,
        quranMeta: QuranMeta?,
        quranRef: AtomicReference<Quran>
    ) {
        val quranStringContent: String = if (scriptKey.isKFQPCScript()) {
            val fileUtils = FileUtils.newInstance(ctx)
            val scriptFile = fileUtils.getScriptFile(scriptKey)

            if (scriptFile.length() > 0) {
                scriptFile.readText()
            } else {
                SPReader.setSavedScript(ctx, QuranScriptUtils.SCRIPT_DEFAULT)
                StringUtils.readInputStream(
                    ctx.assets.open(QuranScriptUtils.SCRIPT_DEFAULT.getQuranScriptResPath())
                )
            }
        } else {
            StringUtils.readInputStream(ctx.assets.open(scriptKey.getQuranScriptResPath()))
        }

        val quranElement = JsonHelper.json.parseToJsonElement(quranStringContent)

        val parsedQuran = resolveQuranData(scriptKey, quranMeta, quranElement.jsonObject)

        quranRef.set(parsedQuran)
    }

    @Throws(java.lang.Exception::class)
    private fun resolveQuranData(script: String, quranMeta: QuranMeta?, quranObject: JsonObject): Quran {
        val chapters = mutableMapOf<Int, Chapter>()

        quranObject[KEY_CHAPTER_LIST]!!.jsonArray.forEach {
            val chapterObj = it.jsonObject

            val chapterNo = chapterObj[KEY_NUMBER]!!.jsonPrimitive.int
            val chapter = Chapter().apply {
                if (quranMeta != null) {
                    setChapterNumber(chapterNo, quranMeta)
                }
            }

            chapter.verses = readVerses(chapterObj, chapterNo)

            chapters[chapterNo] = chapter
        }

        return Quran(script, chapters)
    }

    private fun readVerses(chapterObj: JsonObject, chapterNo: Int): ArrayList<Verse> {
        val verses = ArrayList<Verse>()

        chapterObj[KEY_VERSE_LIST]!!.jsonArray.forEach {
            val verseObj = it.jsonObject

            verses.add(
                Verse(
                    verseObj[KEY_ID]!!.jsonPrimitive.int,
                    chapterNo,
                    verseObj[KEY_NUMBER]!!.jsonPrimitive.int,
                    verseObj[KEY_PAGE_NUMBER]?.jsonPrimitive?.int ?: -1,
                    verseObj[KEY_ARABIC_TEXT]!!.jsonPrimitive.content,
                    verseObj[KEY_END_TEXT]?.jsonPrimitive?.content ?: ""
                )
            )
        }

        return verses
    }
}
