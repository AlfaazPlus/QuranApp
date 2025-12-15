package com.quranapp.android.utils.sharedPrefs

import android.content.Context
import androidx.core.content.edit
import com.quranapp.android.reader_managers.ReaderParams
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.recitation.RecitationManager.setSavedRecitationSlug
import com.quranapp.android.utils.reader.recitation.RecitationManager.setSavedRecitationTranslationSlug
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.tafsir.TafsirUtils
import com.quranapp.android.utils.univ.Keys

/**
 * SharedPreferences utility class for Reader
 */
object SPReader {
    private const val SP_READER: String = "sp_reader"

    private const val SP_TEXT_STYLE: String = "sp_reader_text"
    private const val SP_TRANSL: String = "sp_reader_translations"
    private const val SP_RECITATION_OPTIONS: String = "sp_reader_recitation_options"
    private const val SP_TAFSIR: String = "sp_reader_tafsir"
    private const val SP_SCRIPT: String = "sp_reader_script"
    private const val SP_READER_STYLE: String = "sp_reader_style"

    @JvmStatic
    fun getArabicTextEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(SP_READER, Context.MODE_PRIVATE)
        return sp.getBoolean(Keys.READER_KEY_ARABIC_TEXT_ENABLED, true)
    }

    @JvmStatic
    fun setArabicTextEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(SP_READER, Context.MODE_PRIVATE)
        sp.edit(commit = true) {
            putBoolean(Keys.READER_KEY_ARABIC_TEXT_ENABLED, enabled)
        }
    }

    @JvmStatic
    fun getAutoScrollSpeed(context: Context): Float {
        val sp = context.getSharedPreferences(SP_READER, Context.MODE_PRIVATE)
        return sp.getFloat(Keys.READER_KEY_AUTO_SCROLL_SPEED, 7f)
    }

    @JvmStatic
    fun setAutoScrollSpeed(context: Context, speed: Float) {
        val sp = context.getSharedPreferences(SP_READER, Context.MODE_PRIVATE)
        sp.edit() {
            putFloat(Keys.READER_KEY_AUTO_SCROLL_SPEED, speed)
        }
    }

    @JvmStatic
    fun getSavedTextSizeMultArabic(context: Context): Float {
        val sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE)

        if (!sp.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC)) {
            setSavedTextSizeMultArabic(context, ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT)
        }

        return sp.getFloat(
            ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT
        )
    }

    @JvmStatic
    fun setSavedTextSizeMultArabic(context: Context, sizeMult: Float) {
        val sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE)
        sp.edit() {
            putFloat(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC, sizeMult)
        }
    }

    @JvmStatic
    fun getSavedTextSizeMultTransl(context: Context): Float {
        val sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE)

        if (!sp.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL)) {
            setSavedTextSizeMultTransl(context, ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT)
        }

        return sp.getFloat(
            ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT
        )
    }

    @JvmStatic
    fun setSavedTextSizeMultTransl(context: Context, sizeMult: Float) {
        val sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE)
        sp.edit() {
            putFloat(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL, sizeMult)
        }
    }


    fun getSavedTextSizeMultTafsir(context: Context): Float {
        val sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE)

        if (!sp.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR)) {
            setSavedTextSizeMultTafsir(context, ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT)
        }

        return sp.getFloat(
            ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT
        )
    }

    @JvmStatic
    fun setSavedTextSizeMultTafsir(context: Context, sizeMult: Float) {
        val sp = context.getSharedPreferences(SP_TEXT_STYLE, Context.MODE_PRIVATE)
        sp.edit() {
            putFloat(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR, sizeMult)
        }
    }

    @JvmStatic
    fun getSavedTranslations(context: Context): HashSet<String> {
        val sp = context.getSharedPreferences(SP_TRANSL, Context.MODE_PRIVATE)

        if (!sp.contains(TranslUtils.KEY_TRANSLATIONS)) {
            sp.edit() {
                putStringSet(TranslUtils.KEY_TRANSLATIONS, TranslUtils.defaultTranslationSlugs())
            }
        }

        if (sp.contains(TranslUtils.KEY_TRANSLATIONS)) {
            return sp.getStringSet(TranslUtils.KEY_TRANSLATIONS, HashSet())?.let { HashSet(it) }
                ?: HashSet()
        }

        return HashSet()
    }

    @JvmStatic
    fun setSavedTranslations(context: Context, translSlugsSet: Set<String>) {
        val sp = context.getSharedPreferences(SP_TRANSL, Context.MODE_PRIVATE)
        sp.edit() {
            putStringSet(TranslUtils.KEY_TRANSLATIONS, HashSet(translSlugsSet))
        }
    }

    @JvmStatic
    fun getSavedRecitationSlug(context: Context): String? {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        return sp.getString(RecitationUtils.KEY_RECITATION_RECITER, null)
    }

    @JvmStatic
    fun setSavedRecitationSlug(context: Context, recitation: String) {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        sp.edit() {
            putString(RecitationUtils.KEY_RECITATION_RECITER, recitation)
        }

        setSavedRecitationSlug(recitation)
    }

    @JvmStatic
    fun getSavedRecitationTranslationSlug(context: Context): String? {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        return sp.getString(RecitationUtils.KEY_RECITATION_TRANSLATION_RECITER, null)
    }

    @JvmStatic
    fun setSavedRecitationTranslationSlug(context: Context, slug: String) {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        sp.edit() {
            putString(RecitationUtils.KEY_RECITATION_TRANSLATION_RECITER, slug)
        }

        setSavedRecitationTranslationSlug(slug)
    }

    fun getRecitationSpeed(context: Context): Float {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)

        if (!sp.contains(RecitationUtils.KEY_RECITATION_SPEED)) {
            setRecitationSpeed(context, RecitationUtils.RECITATION_DEFAULT_SPEED)
        }

        return sp.getFloat(
            RecitationUtils.KEY_RECITATION_SPEED,
            RecitationUtils.RECITATION_DEFAULT_SPEED
        )
    }

    fun setRecitationSpeed(context: Context, speed: Float) {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        sp.edit() {
            putFloat(RecitationUtils.KEY_RECITATION_SPEED, speed)
        }
    }

    fun getRecitationRepeatVerse(context: Context): Boolean {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)

        if (!sp.contains(RecitationUtils.KEY_RECITATION_REPEAT)) {
            setRecitationRepeatVerse(context, RecitationUtils.RECITATION_DEFAULT_REPEAT)
        }

        return sp.getBoolean(
            RecitationUtils.KEY_RECITATION_REPEAT,
            RecitationUtils.RECITATION_DEFAULT_REPEAT
        )
    }

    fun setRecitationRepeatVerse(context: Context, repeatVerse: Boolean) {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        sp.edit() {
            putBoolean(RecitationUtils.KEY_RECITATION_REPEAT, repeatVerse)
        }
    }

    fun getRecitationContinueChapter(context: Context): Boolean {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)

        if (!sp.contains(RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER)) {
            setRecitationContinueChapter(
                context,
                RecitationUtils.RECITATION_DEFAULT_CONTINUE_CHAPTER
            )
        }

        return sp.getBoolean(
            RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER,
            RecitationUtils.RECITATION_DEFAULT_CONTINUE_CHAPTER
        )
    }

    fun setRecitationContinueChapter(context: Context, continueChapter: Boolean) {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        sp.edit() {
            putBoolean(RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER, continueChapter)
        }
    }

    fun getRecitationScrollSync(context: Context): Boolean {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)

        if (!sp.contains(RecitationUtils.KEY_RECITATION_SCROLL_SYNC)) {
            setRecitationScrollSync(context, RecitationUtils.RECITATION_DEFAULT_VERSE_SYNC)
        }

        return sp.getBoolean(
            RecitationUtils.KEY_RECITATION_SCROLL_SYNC,
            RecitationUtils.RECITATION_DEFAULT_VERSE_SYNC
        )
    }

    fun setRecitationScrollSync(context: Context, sync: Boolean) {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        sp.edit() {
            putBoolean(RecitationUtils.KEY_RECITATION_SCROLL_SYNC, sync)
        }
    }

    @JvmStatic
    fun getRecitationAudioOption(context: Context): Int {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)

        if (!sp.contains(RecitationUtils.KEY_RECITATION_AUDIO_OPTION)) {
            setRecitationAudioOption(context, RecitationUtils.AUDIO_OPTION_DEFAULT)
        }

        return sp.getInt(
            RecitationUtils.KEY_RECITATION_AUDIO_OPTION,
            RecitationUtils.AUDIO_OPTION_DEFAULT
        )
    }

    fun setRecitationAudioOption(context: Context, option: Int) {
        val sp = context.getSharedPreferences(SP_RECITATION_OPTIONS, Context.MODE_PRIVATE)
        sp.edit() {
            putInt(RecitationUtils.KEY_RECITATION_AUDIO_OPTION, option)
        }
    }

    @JvmStatic
    fun getSavedScript(context: Context): String {
        val sp = context.getSharedPreferences(SP_SCRIPT, Context.MODE_PRIVATE)

        if (!sp.contains(QuranScriptUtils.KEY_SCRIPT)) {
            setSavedScript(context, QuranScriptUtils.SCRIPT_DEFAULT)
        }

        return sp.getString(QuranScriptUtils.KEY_SCRIPT, QuranScriptUtils.SCRIPT_DEFAULT)!!
    }

    @JvmStatic
    fun setSavedScript(context: Context, font: String?) {
        val sp = context.getSharedPreferences(SP_SCRIPT, Context.MODE_PRIVATE)
        sp.edit() {
            putString(QuranScriptUtils.KEY_SCRIPT, font)
        }
    }

    @JvmStatic
    fun getSavedReaderStyle(context: Context): Int {
        var sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE)

        if (!sp.contains(Keys.READER_KEY_READER_STYLE)) {
            setSavedReaderStyle(context, ReaderParams.READER_STYLE_DEFAULT)
        }

        sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE)
        return sp.getInt(Keys.READER_KEY_READER_STYLE, ReaderParams.READER_STYLE_DEFAULT)
    }

    @JvmStatic
    fun setSavedReaderStyle(context: Context, readerStyle: Int) {
        val sp = context.getSharedPreferences(SP_READER_STYLE, Context.MODE_PRIVATE)
        sp.edit() {
            putInt(Keys.READER_KEY_READER_STYLE, readerStyle)
        }
    }

    @JvmStatic
    fun getSavedTafsirKey(context: Context): String? {
        val sp = context.getSharedPreferences(SP_TAFSIR, Context.MODE_PRIVATE)
        return sp.getString(TafsirUtils.KEY_TAFSIR, null)
    }

    fun setSavedTafsirKey(context: Context, tafsirKey: String) {
        val sp = context.getSharedPreferences(SP_TAFSIR, Context.MODE_PRIVATE)
        sp.edit() {
            putString(TafsirUtils.KEY_TAFSIR, tafsirKey)
        }

        TafsirManager.setSavedTafsirKey(tafsirKey)
    }
}
