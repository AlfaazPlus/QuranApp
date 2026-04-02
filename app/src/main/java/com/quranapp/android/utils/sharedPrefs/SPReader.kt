package com.quranapp.android.utils.sharedPrefs

import android.content.Context
import androidx.core.content.edit
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.recitation.RecitationManager.setSavedRecitationSlug
import com.quranapp.android.utils.reader.recitation.RecitationManager.setSavedRecitationTranslationSlug
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import kotlinx.coroutines.runBlocking

/**
 * SharedPreferences utility class for Reader
 */
@Deprecated("Use ReaderPreferences (DataStore)")
object SPReader {
    private const val SP_RECITATION_OPTIONS: String = "sp_reader_recitation_options"

    @JvmStatic
    fun getArabicTextEnabled(context: Context): Boolean {
        return ReaderPreferences.getArabicTextEnabled()
    }

    @JvmStatic
    fun setArabicTextEnabled(context: Context, enabled: Boolean) {
        runBlocking { ReaderPreferences.setArabicTextEnabled(enabled) }
    }

    @JvmStatic
    fun getAutoScrollSpeed(context: Context): Float {
        return ReaderPreferences.getAutoScrollSpeed()
    }

    @JvmStatic
    fun setAutoScrollSpeed(context: Context, speed: Float) {
        runBlocking { ReaderPreferences.setAutoScrollSpeed(speed) }
    }

    @JvmStatic
    fun getSavedTextSizeMultArabic(context: Context): Float {
        return ReaderPreferences.getArabicTextSizeMultiplier()
    }

    @JvmStatic
    fun setSavedTextSizeMultArabic(context: Context, sizeMult: Float) {
        runBlocking { ReaderPreferences.setArabicTextSizeMultiplier(sizeMult) }
    }

    @JvmStatic
    fun getSavedTextSizeMultTransl(context: Context): Float {
        return ReaderPreferences.getTranslationTextSizeMultiplier()
    }

    @JvmStatic
    fun setSavedTextSizeMultTransl(context: Context, sizeMult: Float) {
        runBlocking { ReaderPreferences.setTranslationTextSizeMultiplier(sizeMult) }
    }

    fun getSavedTextSizeMultTafsir(context: Context): Float {
        return ReaderPreferences.getTafsirTextSizeMultiplier()
    }

    @JvmStatic
    fun setSavedTextSizeMultTafsir(context: Context, sizeMult: Float) {
        runBlocking { ReaderPreferences.setTafsirTextSizeMultiplier(sizeMult) }
    }

    @JvmStatic
    fun getSavedTranslations(context: Context): HashSet<String> {
        return ReaderPreferences.getTranslations()
    }

    @JvmStatic
    fun setSavedTranslations(context: Context, translSlugsSet: Set<String>) {
        runBlocking { ReaderPreferences.setTranslations(translSlugsSet) }
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
        return ReaderPreferences.getQuranScript()
    }

    @JvmStatic
    fun setSavedScript(context: Context, font: String?) {
        runBlocking { ReaderPreferences.setQuranScript(font) }
    }

    @JvmStatic
    fun getSavedReaderStyle(context: Context): Int {
        return ReaderPreferences.getReaderStyle()
    }

    @JvmStatic
    fun setSavedReaderStyle(context: Context, readerStyle: Int) {
        runBlocking { ReaderPreferences.setReaderStyle(readerStyle) }
    }

    @JvmStatic
    fun getSavedTafsirKey(context: Context): String? {
        return ReaderPreferences.getTafsirId()
    }

    fun setSavedTafsirKey(context: Context, tafsirKey: String) {
        runBlocking { ReaderPreferences.setTafsirId(tafsirKey) }
    }
}
