package com.quranapp.android.compose.utils.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.reader_managers.ReaderParams
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.tafsir.TafsirUtils
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.runBlocking

/**
 * Reader settings stored in DataStore via [DataStoreManager].
 * Legacy SharedPreferences (`sp_reader`, `sp_reader_text`, etc.) are migrated once on startup.
 */
object ReaderPreferences {

    private const val LEGACY_SP_READER = "sp_reader"
    private const val LEGACY_SP_TEXT_STYLE = "sp_reader_text"
    private const val LEGACY_SP_TRANSL = "sp_reader_translations"
    private const val LEGACY_SP_SCRIPT = "sp_reader_script"
    private const val LEGACY_SP_READER_STYLE = "sp_reader_style"
    private const val LEGACY_SP_TAFSIR = "sp_reader_tafsir"

    private val KEY_LEGACY_MIGRATED = booleanPreferencesKey("reader.prefs.legacy_migrated_v1")

    private val KEY_ARABIC_TEXT_ENABLED =
        booleanPreferencesKey(Keys.READER_KEY_ARABIC_TEXT_ENABLED)
    private val KEY_AUTO_SCROLL_SPEED = floatPreferencesKey(Keys.READER_KEY_AUTO_SCROLL_SPEED)
    private val KEY_TEXT_SIZE_MULT_ARABIC =
        floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC)
    private val KEY_TEXT_SIZE_MULT_TRANSL =
        floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL)
    private val KEY_TEXT_SIZE_MULT_TAFSIR =
        floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR)
    private val KEY_TRANSLATIONS = stringSetPreferencesKey(TranslUtils.KEY_TRANSLATIONS)
    private val KEY_SCRIPT = stringPreferencesKey(QuranScriptUtils.KEY_SCRIPT)
    private val KEY_READER_STYLE = intPreferencesKey(Keys.READER_KEY_READER_STYLE)
    private val KEY_TAFSIR = stringPreferencesKey(TafsirUtils.KEY_TAFSIR)

    fun migrateFromLegacyIfNeeded(context: Context) {
        runBlocking {
            if (DataStoreManager.read(KEY_LEGACY_MIGRATED, false)) return@runBlocking

            val appCtx = context.applicationContext

            val spReader = appCtx.getSharedPreferences(LEGACY_SP_READER, Context.MODE_PRIVATE)
            if (spReader.contains(Keys.READER_KEY_ARABIC_TEXT_ENABLED)) {
                DataStoreManager.write(
                    KEY_ARABIC_TEXT_ENABLED,
                    spReader.getBoolean(Keys.READER_KEY_ARABIC_TEXT_ENABLED, true)
                )
            }
            if (spReader.contains(Keys.READER_KEY_AUTO_SCROLL_SPEED)) {
                DataStoreManager.write(
                    KEY_AUTO_SCROLL_SPEED,
                    spReader.getFloat(Keys.READER_KEY_AUTO_SCROLL_SPEED, 7f)
                )
            }

            val spTextStyle =
                appCtx.getSharedPreferences(LEGACY_SP_TEXT_STYLE, Context.MODE_PRIVATE)
            if (spTextStyle.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC)) {
                DataStoreManager.write(
                    KEY_TEXT_SIZE_MULT_ARABIC,
                    spTextStyle.getFloat(
                        ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC,
                        ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT
                    )
                )
            }
            if (spTextStyle.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL)) {
                DataStoreManager.write(
                    KEY_TEXT_SIZE_MULT_TRANSL,
                    spTextStyle.getFloat(
                        ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL,
                        ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT
                    )
                )
            }
            if (spTextStyle.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR)) {
                DataStoreManager.write(
                    KEY_TEXT_SIZE_MULT_TAFSIR,
                    spTextStyle.getFloat(
                        ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR,
                        ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT
                    )
                )
            }

            val spTransl = appCtx.getSharedPreferences(LEGACY_SP_TRANSL, Context.MODE_PRIVATE)
            if (spTransl.contains(TranslUtils.KEY_TRANSLATIONS)) {
                val legacy = spTransl.getStringSet(TranslUtils.KEY_TRANSLATIONS, null)
                if (legacy != null) {
                    DataStoreManager.write(KEY_TRANSLATIONS, HashSet(legacy))
                }
            }

            val spScript = appCtx.getSharedPreferences(LEGACY_SP_SCRIPT, Context.MODE_PRIVATE)
            if (spScript.contains(QuranScriptUtils.KEY_SCRIPT)) {
                val script = spScript.getString(QuranScriptUtils.KEY_SCRIPT, null)
                DataStoreManager.write(KEY_SCRIPT, script ?: QuranScriptUtils.SCRIPT_DEFAULT)
            }

            val spReaderStyle =
                appCtx.getSharedPreferences(LEGACY_SP_READER_STYLE, Context.MODE_PRIVATE)
            if (spReaderStyle.contains(Keys.READER_KEY_READER_STYLE)) {
                DataStoreManager.write(
                    KEY_READER_STYLE,
                    spReaderStyle.getInt(
                        Keys.READER_KEY_READER_STYLE,
                        ReaderParams.READER_STYLE_DEFAULT
                    )
                )
            }

            val spTafsir = appCtx.getSharedPreferences(LEGACY_SP_TAFSIR, Context.MODE_PRIVATE)
            if (spTafsir.contains(TafsirUtils.KEY_TAFSIR)) {
                val key = spTafsir.getString(TafsirUtils.KEY_TAFSIR, null)
                if (key != null) {
                    DataStoreManager.write(KEY_TAFSIR, key)
                }
            }

            DataStoreManager.write(KEY_LEGACY_MIGRATED, true)
        }
    }

    fun getArabicTextEnabled(): Boolean {
        return DataStoreManager.read(KEY_ARABIC_TEXT_ENABLED, true)
    }

    suspend fun setArabicTextEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_ARABIC_TEXT_ENABLED, enabled)
    }

    @Composable
    fun observeArabicTextEnabled(): Boolean {
        return DataStoreManager.observe(KEY_ARABIC_TEXT_ENABLED, true)
    }

    fun getAutoScrollSpeed(): Float {
        return DataStoreManager.read(KEY_AUTO_SCROLL_SPEED, 7f)
    }

    suspend fun setAutoScrollSpeed(speed: Float) {
        DataStoreManager.write(KEY_AUTO_SCROLL_SPEED, speed)
    }

    @Composable
    fun observeAutoScrollSpeed(): Float {
        return DataStoreManager.observe(KEY_AUTO_SCROLL_SPEED, 7f)
    }

    fun getArabicTextSizeMultiplier(): Float {
        return DataStoreManager.read(
            KEY_TEXT_SIZE_MULT_ARABIC,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT
        )
    }

    suspend fun setArabicTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_ARABIC, sizeMult)
    }

    @Composable
    fun observeArabicTextSizeMultiplier(): Float {
        return DataStoreManager.observe(
            KEY_TEXT_SIZE_MULT_ARABIC,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT
        )
    }

    fun getTranslationTextSizeMultiplier(): Float {
        return DataStoreManager.read(
            KEY_TEXT_SIZE_MULT_TRANSL,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT
        )
    }

    suspend fun setTranslationTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_TRANSL, sizeMult)
    }

    @Composable
    fun observeTranlationTextSizeMultiplier(): Float {
        return DataStoreManager.observe(
            KEY_TEXT_SIZE_MULT_TRANSL,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT
        )
    }

    fun getTafsirTextSizeMultiplier(): Float {
        return DataStoreManager.read(
            KEY_TEXT_SIZE_MULT_TAFSIR,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT
        )
    }

    suspend fun setTafsirTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_TAFSIR, sizeMult)
    }

    @Composable
    fun observeTafsirTextSizeMultiplier(): Float {
        return DataStoreManager.observe(
            KEY_TEXT_SIZE_MULT_TAFSIR,
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT
        )
    }

    fun getTranslations(): HashSet<String> {
        val raw = DataStoreManager.read(KEY_TRANSLATIONS, emptySet())
        if (raw.isEmpty()) {
            return TranslUtils.defaultTranslationSlugs()
        }
        return HashSet(raw)
    }

    suspend fun setTranslations(translSlugsSet: Set<String>) {
        DataStoreManager.write(KEY_TRANSLATIONS, HashSet(translSlugsSet))
    }

    @Composable
    fun observeTranslations(): HashSet<String> {
        val raw = DataStoreManager.observe(KEY_TRANSLATIONS, emptySet())
        return if (raw.isEmpty()) TranslUtils.defaultTranslationSlugs() else HashSet(raw)
    }

    fun getQuranScript(): String {
        val s = DataStoreManager.read(KEY_SCRIPT, QuranScriptUtils.SCRIPT_DEFAULT)

        if (!QuranScriptUtils.availableScriptSlugs().contains(s)) {
            return QuranScriptUtils.SCRIPT_DEFAULT
        }

        return s
    }

    suspend fun setQuranScript(font: String?) {
        DataStoreManager.write(KEY_SCRIPT, font ?: QuranScriptUtils.SCRIPT_DEFAULT)
    }

    @Composable
    fun observeQuranScript(): String {
        val s = DataStoreManager.observe(KEY_SCRIPT, QuranScriptUtils.SCRIPT_DEFAULT)

        if (!QuranScriptUtils.availableScriptSlugs().contains(s)) {
            return QuranScriptUtils.SCRIPT_DEFAULT
        }

        return s
    }

    fun getReaderStyle(): Int {
        return DataStoreManager.read(KEY_READER_STYLE, ReaderParams.READER_STYLE_DEFAULT)
    }

    suspend fun setReaderStyle(readerStyle: Int) {
        DataStoreManager.write(KEY_READER_STYLE, readerStyle)
    }

    @Composable
    fun observeReaderStyle(): Int {
        return DataStoreManager.observe(KEY_READER_STYLE, ReaderParams.READER_STYLE_DEFAULT)
    }

    fun getTafsirId(): String? {
        val s = DataStoreManager.read(KEY_TAFSIR, "")
        return s.ifEmpty { null }
    }

    suspend fun setTafsirId(tafsirKey: String) {
        DataStoreManager.write(KEY_TAFSIR, tafsirKey)
        TafsirManager.setSavedTafsirKey(tafsirKey)
    }

    @Composable
    fun observeTafsirId(): String? {
        val raw = DataStoreManager.observe(KEY_TAFSIR, "")
        return raw.ifEmpty { null }
    }
}
