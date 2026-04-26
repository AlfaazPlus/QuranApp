@file:OptIn(ExperimentalCoroutinesApi::class)

package com.quranapp.android.compose.utils.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.alfaazplus.sunnah.ui.utils.shared_preference.PrefKey
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.tafsir.TafsirUtils
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking

object ReaderPreferences {

    private const val LEGACY_SP_READER = "sp_reader"
    private const val LEGACY_SP_TEXT_STYLE = "sp_reader_text"
    private const val LEGACY_SP_TRANSL = "sp_reader_translations"
    private const val LEGACY_SP_SCRIPT = "sp_reader_script"
    private const val LEGACY_SP_READER_STYLE = "sp_reader_style"
    private const val LEGACY_SP_TAFSIR = "sp_reader_tafsir"

    val KEY_ARABIC_TEXT_ENABLED =
        PrefKey(booleanPreferencesKey(Keys.READER_KEY_ARABIC_TEXT_ENABLED), true)

    val KEY_AUTO_SCROLL_SPEED =
        PrefKey(floatPreferencesKey(Keys.READER_KEY_AUTO_SCROLL_SPEED), 7f)

    val KEY_TEXT_SIZE_MULT_ARABIC =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT
        )

    val KEY_TEXT_SIZE_MULT_TRANSL =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT
        )

    val KEY_TEXT_SIZE_MULT_TAFSIR =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT
        )

    val KEY_TRANSLATIONS =
        PrefKey(
            stringSetPreferencesKey(TranslUtils.KEY_TRANSLATIONS),
            TranslUtils.defaultTranslationSlugs()
        )

    val KEY_SCRIPT =
        PrefKey(stringPreferencesKey(QuranScriptUtils.KEY_SCRIPT), QuranScriptUtils.SCRIPT_DEFAULT)

    val KEY_SCRIPT_VARIANT = PrefKey(stringPreferencesKey("script_variant"), "")

    val KEY_READER_MODE =
        PrefKey(stringPreferencesKey(Keys.READER_KEY_READER_MODE), ReaderMode.VerseByVerse.value)

    val KEY_TAFSIR =
        PrefKey(stringPreferencesKey(TafsirUtils.KEY_TAFSIR), "")

    val KEY_WBW =
        PrefKey(stringPreferencesKey("key.wbw"), "")

    val KEY_WBW_CONTENT_EPOCH =
        PrefKey(longPreferencesKey("reader.wbw.content_epoch"), 0L)

    val KEY_WBW_SHOW_TRANSLATION =
        PrefKey(booleanPreferencesKey("reader.wbw.show_translation"), false)

    val KEY_WBW_SHOW_TRANSLITERATION =
        PrefKey(booleanPreferencesKey("reader.wbw.show_transliteration"), false)


    val KEY_WBW_TOOLTIP_SHOW_TRANSLATION =
        PrefKey(booleanPreferencesKey("reader.wbw_tooltip.show_translation"), true)

    val KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION =
        PrefKey(booleanPreferencesKey("reader.wbw_tooltip.show_transliteration"), true)

    val KEY_WBW_RECITATION =
        PrefKey(booleanPreferencesKey("reader.wbw.recitation"), true)

    val KEY_TEXT_SIZE_MULT_WBW =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_WBW),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_WBW_DEFAULT
        )

    val KEY_LEGACY_MIGRATED =
        PrefKey(booleanPreferencesKey("reader.prefs.legacy_migrated_v1"), false)

    fun migrateFromLegacyIfNeeded(context: Context) {
        runBlocking {
            if (DataStoreManager.read(KEY_LEGACY_MIGRATED)) return@runBlocking

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
        return DataStoreManager.read(KEY_ARABIC_TEXT_ENABLED)
    }

    suspend fun setArabicTextEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_ARABIC_TEXT_ENABLED, enabled)
    }

    @Composable
    fun observeArabicTextEnabled(): Boolean {
        return DataStoreManager.observe(KEY_ARABIC_TEXT_ENABLED)
    }

    fun getAutoScrollSpeed(): Float {
        return DataStoreManager.read(KEY_AUTO_SCROLL_SPEED)
    }

    suspend fun setAutoScrollSpeed(speed: Float) {
        DataStoreManager.write(KEY_AUTO_SCROLL_SPEED, speed)
    }

    @Composable
    fun observeAutoScrollSpeed(): Float {
        return DataStoreManager.observe(KEY_AUTO_SCROLL_SPEED)
    }

    fun getArabicTextSizeMultiplier(): Float {
        return DataStoreManager.read(KEY_TEXT_SIZE_MULT_ARABIC)
    }

    suspend fun setArabicTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_ARABIC, sizeMult)
    }

    @Composable
    fun observeArabicTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_ARABIC)
    }

    fun getTranslationTextSizeMultiplier(): Float {
        return DataStoreManager.read(KEY_TEXT_SIZE_MULT_TRANSL)
    }

    suspend fun setTranslationTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_TRANSL, sizeMult)
    }

    @Composable
    fun observeTranlationTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_TRANSL)
    }

    fun getTafsirTextSizeMultiplier(): Float {
        return DataStoreManager.read(KEY_TEXT_SIZE_MULT_TAFSIR)
    }

    suspend fun setTafsirTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_TAFSIR, sizeMult)
    }

    @Composable
    fun observeTafsirTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_TAFSIR)
    }

    fun getTranslations(): Set<String> {
        return DataStoreManager.read(KEY_TRANSLATIONS)
    }

    fun primaryTranslationSlug(): String {
        val saved = getTranslations()
        return saved.firstOrNull { !TranslUtils.isTransliteration(it) }
            ?: TranslUtils.TRANSL_SLUG_DEFAULT
    }

    @Composable
    fun observePrimaryTranslationSlug(): String {
        val saved = observeTranslations()
        return saved.firstOrNull { !TranslUtils.isTransliteration(it) }
            ?: TranslUtils.TRANSL_SLUG_DEFAULT
    }

    suspend fun setTranslations(translSlugsSet: Set<String>) {
        DataStoreManager.write(KEY_TRANSLATIONS, HashSet(translSlugsSet))
    }

    @Composable
    fun observeTranslations(): Set<String> {
        return DataStoreManager.observe(KEY_TRANSLATIONS)
    }

    fun translationsFlow(): Flow<Set<String>> {
        return DataStoreManager.flow(KEY_TRANSLATIONS)
    }

    fun getQuranScript(): String {
        return QuranScriptUtils.validatePreferredScript(DataStoreManager.read(KEY_SCRIPT))
    }

    suspend fun setQuranScript(font: String?) {
        DataStoreManager.write(KEY_SCRIPT, font ?: QuranScriptUtils.SCRIPT_DEFAULT)
    }

    suspend fun setQuranScriptWithVariant(
        font: String?,
        variant: QuranScriptVariant?,
    ) {
        DataStoreManager.edit {
            this[KEY_SCRIPT.key] = font ?: QuranScriptUtils.SCRIPT_DEFAULT
            this[KEY_SCRIPT_VARIANT.key] = variant?.value ?: ""
        }
    }

    fun quranScriptFlow(): Flow<String> {
        return DataStoreManager.flow(KEY_SCRIPT)
            .mapLatest {
                QuranScriptUtils.validatePreferredScript(it)
            }
    }

    @Composable
    fun observeQuranScript(): String {
        val s = DataStoreManager.observe(KEY_SCRIPT)

        if (!QuranScriptUtils.availableScripts().contains(s)) {
            return QuranScriptUtils.SCRIPT_DEFAULT
        }

        return s
    }


    fun getQuranScriptVariant(): QuranScriptVariant? {
        return QuranScriptVariant.fromValue(DataStoreManager.read(KEY_SCRIPT_VARIANT))
    }

    suspend fun setQuranScriptVariant(variant: QuranScriptVariant?) {
        DataStoreManager.write(KEY_SCRIPT_VARIANT, variant?.value ?: "")
    }

    fun quranScriptVariantFlow(): Flow<QuranScriptVariant?> {
        return DataStoreManager.flow(KEY_SCRIPT_VARIANT)
            .mapLatest {
                QuranScriptVariant.fromValue(it)
            }
    }

    @Composable
    fun observeQuranScriptVariant(): QuranScriptVariant? {
        val value = DataStoreManager.observe(KEY_SCRIPT_VARIANT)

        return QuranScriptVariant.fromValue(value)
    }

    fun getReaderMode(): ReaderMode {
        return DataStoreManager.read(KEY_READER_MODE).let { ReaderMode.fromValue(it) }
    }

    suspend fun setReaderMode(mode: ReaderMode) {
        DataStoreManager.write(KEY_READER_MODE, mode.value)
    }

    fun readerModeFlow(): Flow<ReaderMode> {
        return DataStoreManager.flow(KEY_READER_MODE).map { ReaderMode.fromValue(it) }
    }

    @Composable
    fun observeReaderMode(): ReaderMode {
        return DataStoreManager.observe(KEY_READER_MODE).let { ReaderMode.fromValue(it) }
    }

    fun getTafsirId(): String? {
        val s = DataStoreManager.read(KEY_TAFSIR)
        return s.ifEmpty { null }
    }

    suspend fun setTafsirId(tafsirKey: String) {
        DataStoreManager.write(KEY_TAFSIR, tafsirKey)
        TafsirManager.setSavedTafsirKey(tafsirKey)
    }

    fun tafsirIdFlow(): Flow<String> {
        return DataStoreManager.flow(KEY_TAFSIR)
    }

    @Composable
    fun observeTafsirId(): String? {
        val raw = DataStoreManager.observe(KEY_TAFSIR)
        return raw.ifEmpty { null }
    }

    fun getWbwId(): String? {
        return DataStoreManager.read(KEY_WBW).takeIf { it.isNotEmpty() }
    }

    suspend fun setWbwId(id: String) {
        DataStoreManager.write(KEY_WBW, id)
    }

    fun wbwIdFlow(): Flow<String> {
        return DataStoreManager.flow(KEY_WBW)
    }

    @Composable
    fun observeWbwId(): String {
        return DataStoreManager.observe(KEY_WBW)
    }

    fun getWbwContentEpoch(): Long {
        return DataStoreManager.read(KEY_WBW_CONTENT_EPOCH)
    }

    suspend fun bumpWbwContentEpoch() {
        val next = DataStoreManager.read(KEY_WBW_CONTENT_EPOCH) + 1L
        DataStoreManager.write(KEY_WBW_CONTENT_EPOCH, next)
    }

    fun getWbwShowTranslation(): Boolean {
        return DataStoreManager.read(KEY_WBW_SHOW_TRANSLATION)
    }

    suspend fun setWbwShowTranslation(show: Boolean) {
        DataStoreManager.write(KEY_WBW_SHOW_TRANSLATION, show)
    }

    @Composable
    fun observeWbwShowTranslation(): Boolean {
        return DataStoreManager.observe(KEY_WBW_SHOW_TRANSLATION)
    }

    fun wbwShowTranslationFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_SHOW_TRANSLATION)
    }

    fun getWbwShowTransliteration(): Boolean {
        return DataStoreManager.read(KEY_WBW_SHOW_TRANSLITERATION)
    }

    suspend fun setWbwShowTransliteration(show: Boolean) {
        DataStoreManager.write(KEY_WBW_SHOW_TRANSLITERATION, show)
    }

    @Composable
    fun observeWbwShowTransliteration(): Boolean {
        return DataStoreManager.observe(KEY_WBW_SHOW_TRANSLITERATION)
    }

    fun wbwShowTransliterationFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_SHOW_TRANSLITERATION)
    }

    fun getWbwTooltipShowTranslation(): Boolean {
        return DataStoreManager.read(KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
    }

    suspend fun setWbwTooltipShowTranslation(show: Boolean) {
        DataStoreManager.write(KEY_WBW_TOOLTIP_SHOW_TRANSLATION, show)
    }

    @Composable
    fun observeWbwTooltipShowTranslation(): Boolean {
        return DataStoreManager.observe(KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
    }

    fun getWbwTooltipShowTransliteration(): Boolean {
        return DataStoreManager.read(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
    }

    suspend fun setWbwTooltipShowTransliteration(show: Boolean) {
        DataStoreManager.write(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION, show)
    }

    @Composable
    fun observeWbwTooltipShowTransliteration(): Boolean {
        return DataStoreManager.observe(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
    }


    fun getWbwRecitationEnabled(): Boolean {
        return DataStoreManager.read(KEY_WBW_RECITATION)
    }

    suspend fun setWbwRecitationEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_WBW_RECITATION, enabled)
    }

    @Composable
    fun observeWbwRecitationEnabled(): Boolean {
        return DataStoreManager.observe(KEY_WBW_RECITATION)
    }

    fun wbwRecitationEnabledFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_RECITATION)
    }

    fun getWbwTextSizeMultiplier(): Float {
        return DataStoreManager.read(KEY_TEXT_SIZE_MULT_WBW)
    }

    suspend fun setWbwTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_WBW, sizeMult)
    }

    @Composable
    fun observeWbwTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_WBW)
    }

    fun wbwTextSizeMultiplierFlow(): Flow<Float> {
        return DataStoreManager.flow(KEY_TEXT_SIZE_MULT_WBW)
    }
}
