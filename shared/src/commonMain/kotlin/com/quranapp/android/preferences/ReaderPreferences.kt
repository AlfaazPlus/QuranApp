package com.quranapp.android.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quranapp.android.reader.ReaderMode
import com.quranapp.android.reader.ReadingLayout
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.QuranScripts
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ReaderPreferences {
    const val DEFAULT_TRANSLATION_SLUG = "en_101_sahih-international"
    private const val TRANSLITERATION_SLUG_PART = "transliteration"

    val KEY_ARABIC_TEXT_ENABLED =
        PrefKey(booleanPreferencesKey(Keys.READER_KEY_ARABIC_TEXT_ENABLED), true)

    val KEY_KEEP_SCREEN_ON =
        PrefKey(booleanPreferencesKey(Keys.READER_KEY_KEEP_SCREEN_ON), true)

    val KEY_AUTO_SCROLL_SPEED =
        PrefKey(floatPreferencesKey(Keys.READER_KEY_AUTO_SCROLL_SPEED), 7f)

    val KEY_TEXT_SIZE_MULT_ARABIC =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT,
        )

    val KEY_TEXT_SIZE_MULT_TRANSL =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT,
        )

    val KEY_TEXT_SIZE_MULT_TAFSIR =
        PrefKey(
            floatPreferencesKey(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR),
            ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT,
        )

    val KEY_TRANSLATIONS =
        PrefKey(
            stringSetPreferencesKey("key.translations"),
            hashSetOf(DEFAULT_TRANSLATION_SLUG),
        )

    val KEY_SCRIPT =
        PrefKey(stringPreferencesKey("key.script"), QuranScripts.DEFAULT)

    val KEY_SCRIPT_VARIANT = PrefKey(stringPreferencesKey("script_variant"), "")

    val KEY_READER_MODE =
        PrefKey(stringPreferencesKey(Keys.READER_KEY_READER_MODE), ReaderMode.VerseByVerse.value)

    val KEY_READING_LAYOUT =
        PrefKey(stringPreferencesKey(Keys.READER_KEY_READING_LAYOUT), ReadingLayout.Page.value)

    val KEY_TAFSIR =
        PrefKey(stringPreferencesKey("key.tafsir"), "")

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
            ReaderTextSizeUtils.TEXT_SIZE_MULT_WBW_DEFAULT,
        )

    val KEY_LEGACY_MIGRATED =
        PrefKey(booleanPreferencesKey("reader.prefs.legacy_migrated_v1"), false)

    var onTafsirSelected: ((String) -> Unit)? = null

    suspend fun getArabicTextEnabled(): Boolean {
        return DataStoreManager.readFirst(KEY_ARABIC_TEXT_ENABLED)
    }

    suspend fun setArabicTextEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_ARABIC_TEXT_ENABLED, enabled)
    }

    suspend fun getKeepScreenOn(): Boolean {
        return DataStoreManager.readFirst(KEY_KEEP_SCREEN_ON)
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        DataStoreManager.write(KEY_KEEP_SCREEN_ON, enabled)
    }

    suspend fun getAutoScrollSpeed(): Float {
        return DataStoreManager.readFirst(KEY_AUTO_SCROLL_SPEED)
    }

    suspend fun setAutoScrollSpeed(speed: Float) {
        DataStoreManager.write(KEY_AUTO_SCROLL_SPEED, speed)
    }

    suspend fun getArabicTextSizeMultiplier(): Float {
        return DataStoreManager.readFirst(KEY_TEXT_SIZE_MULT_ARABIC)
    }

    suspend fun setArabicTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_ARABIC, sizeMult)
    }

    suspend fun getTranslationTextSizeMultiplier(): Float {
        return DataStoreManager.readFirst(KEY_TEXT_SIZE_MULT_TRANSL)
    }

    suspend fun setTranslationTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_TRANSL, sizeMult)
    }

    suspend fun getTafsirTextSizeMultiplier(): Float {
        return DataStoreManager.readFirst(KEY_TEXT_SIZE_MULT_TAFSIR)
    }

    suspend fun setTafsirTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_TAFSIR, sizeMult)
    }

    suspend fun getTranslations(): Set<String> {
        return DataStoreManager.readFirst(KEY_TRANSLATIONS)
    }

    suspend fun primaryTranslationSlug(): String {
        val saved = getTranslations()
        return saved.firstOrNull { !it.contains(TRANSLITERATION_SLUG_PART) }
            ?: DEFAULT_TRANSLATION_SLUG
    }

    suspend fun setTranslations(translSlugsSet: Set<String>) {
        DataStoreManager.write(KEY_TRANSLATIONS, HashSet(translSlugsSet))
    }

    fun translationsFlow(): Flow<Set<String>> {
        return DataStoreManager.flow(KEY_TRANSLATIONS)
    }

    suspend fun getQuranScript(): String {
        return QuranScripts.coerce(DataStoreManager.readFirst(KEY_SCRIPT))
    }

    suspend fun setQuranScript(font: String?) {
        DataStoreManager.write(KEY_SCRIPT, font ?: QuranScripts.DEFAULT)
    }

    suspend fun setQuranScriptWithVariant(
        font: String?,
        variant: QuranScriptVariant?,
    ) {
        DataStoreManager.edit {
            this[KEY_SCRIPT.key] = font ?: QuranScripts.DEFAULT
            this[KEY_SCRIPT_VARIANT.key] = variant?.value ?: ""
        }
    }

    fun quranScriptFlow(): Flow<String> {
        return DataStoreManager.flow(KEY_SCRIPT).map { QuranScripts.coerce(it) }
    }

    suspend fun getQuranScriptVariant(): QuranScriptVariant? {
        return QuranScriptVariant.fromValue(DataStoreManager.readFirst(KEY_SCRIPT_VARIANT))
    }

    suspend fun setQuranScriptVariant(variant: QuranScriptVariant?) {
        DataStoreManager.write(KEY_SCRIPT_VARIANT, variant?.value ?: "")
    }

    fun quranScriptVariantFlow(): Flow<QuranScriptVariant?> {
        return DataStoreManager.flow(KEY_SCRIPT_VARIANT).map { QuranScriptVariant.fromValue(it) }
    }

    suspend fun getReaderMode(): ReaderMode {
        return ReaderMode.fromValue(DataStoreManager.readFirst(KEY_READER_MODE))
    }

    suspend fun setReaderMode(mode: ReaderMode) {
        DataStoreManager.write(KEY_READER_MODE, mode.value)
    }

    fun readerModeFlow(): Flow<ReaderMode> {
        return DataStoreManager.flow(KEY_READER_MODE).map { ReaderMode.fromValue(it) }
    }

    suspend fun getReadingLayout(): ReadingLayout {
        return ReadingLayout.fromValue(DataStoreManager.readFirst(KEY_READING_LAYOUT))
    }

    suspend fun setReadingLayout(layout: ReadingLayout) {
        DataStoreManager.write(KEY_READING_LAYOUT, layout.value)
    }

    suspend fun getTafsirId(): String? {
        val s = DataStoreManager.readFirst(KEY_TAFSIR)
        return s.ifEmpty { null }
    }

    suspend fun setTafsirId(tafsirKey: String) {
        DataStoreManager.write(KEY_TAFSIR, tafsirKey)
        onTafsirSelected?.invoke(tafsirKey)
    }

    fun tafsirIdFlow(): Flow<String> {
        return DataStoreManager.flow(KEY_TAFSIR)
    }

    suspend fun getWbwId(): String? {
        return DataStoreManager.readFirst(KEY_WBW).takeIf { it.isNotEmpty() }
    }

    suspend fun setWbwId(id: String) {
        DataStoreManager.write(KEY_WBW, id)
    }

    fun wbwIdFlow(): Flow<String> {
        return DataStoreManager.flow(KEY_WBW)
    }

    suspend fun getWbwContentEpoch(): Long {
        return DataStoreManager.readFirst(KEY_WBW_CONTENT_EPOCH)
    }

    suspend fun bumpWbwContentEpoch() {
        val next = DataStoreManager.readFirst(KEY_WBW_CONTENT_EPOCH) + 1L
        DataStoreManager.write(KEY_WBW_CONTENT_EPOCH, next)
    }

    suspend fun getWbwShowTranslation(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_SHOW_TRANSLATION)
    }

    suspend fun setWbwShowTranslation(show: Boolean) {
        DataStoreManager.write(KEY_WBW_SHOW_TRANSLATION, show)
    }

    fun wbwShowTranslationFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_SHOW_TRANSLATION)
    }

    suspend fun getWbwShowTransliteration(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_SHOW_TRANSLITERATION)
    }

    suspend fun setWbwShowTransliteration(show: Boolean) {
        DataStoreManager.write(KEY_WBW_SHOW_TRANSLITERATION, show)
    }

    fun wbwShowTransliterationFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_SHOW_TRANSLITERATION)
    }

    suspend fun getWbwTooltipShowTranslation(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
    }

    suspend fun setWbwTooltipShowTranslation(show: Boolean) {
        DataStoreManager.write(KEY_WBW_TOOLTIP_SHOW_TRANSLATION, show)
    }

    suspend fun getWbwTooltipShowTransliteration(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
    }

    suspend fun setWbwTooltipShowTransliteration(show: Boolean) {
        DataStoreManager.write(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION, show)
    }

    suspend fun getWbwRecitationEnabled(): Boolean {
        return DataStoreManager.readFirst(KEY_WBW_RECITATION)
    }

    suspend fun setWbwRecitationEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_WBW_RECITATION, enabled)
    }

    fun wbwRecitationEnabledFlow(): Flow<Boolean> {
        return DataStoreManager.flow(KEY_WBW_RECITATION)
    }

    suspend fun getWbwTextSizeMultiplier(): Float {
        return DataStoreManager.readFirst(KEY_TEXT_SIZE_MULT_WBW)
    }

    suspend fun setWbwTextSizeMultiplier(sizeMult: Float) {
        DataStoreManager.write(KEY_TEXT_SIZE_MULT_WBW, sizeMult)
    }

    fun wbwTextSizeMultiplierFlow(): Flow<Float> {
        return DataStoreManager.flow(KEY_TEXT_SIZE_MULT_WBW)
    }

    @Composable
    fun observeArabicTextEnabled(): Boolean {
        return DataStoreManager.observe(KEY_ARABIC_TEXT_ENABLED)
    }

    @Composable
    fun observeKeepScreenOn(): Boolean {
        return DataStoreManager.observe(KEY_KEEP_SCREEN_ON)
    }

    @Composable
    fun observeAutoScrollSpeed(): Float {
        return DataStoreManager.observe(KEY_AUTO_SCROLL_SPEED)
    }

    @Composable
    fun observeArabicTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_ARABIC)
    }

    @Composable
    fun observeTranlationTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_TRANSL)
    }

    @Composable
    fun observeTafsirTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_TAFSIR)
    }

    @Composable
    fun observePrimaryTranslationSlug(): String {
        val saved = observeTranslations()
        return saved.firstOrNull { !it.contains(TRANSLITERATION_SLUG_PART) }
            ?: DEFAULT_TRANSLATION_SLUG
    }

    @Composable
    fun observeTranslations(): Set<String> {
        return DataStoreManager.observe(KEY_TRANSLATIONS)
    }

    @Composable
    fun observeQuranScript(): String {
        return QuranScripts.coerce(DataStoreManager.observe(KEY_SCRIPT))
    }

    @Composable
    fun observeQuranScriptVariant(): QuranScriptVariant? {
        val value = DataStoreManager.observe(KEY_SCRIPT_VARIANT)
        return QuranScriptVariant.fromValue(value)
    }

    @Composable
    fun observeReaderMode(): ReaderMode {
        return DataStoreManager.observe(KEY_READER_MODE).let { ReaderMode.fromValue(it) }
    }

    @Composable
    fun observeReadingLayout(): ReadingLayout {
        return ReadingLayout.fromValue(DataStoreManager.observe(KEY_READING_LAYOUT))
    }

    @Composable
    fun observeTafsirId(): String? {
        val raw = DataStoreManager.observe(KEY_TAFSIR)
        return raw.ifEmpty { null }
    }

    @Composable
    fun observeWbwId(): String {
        return DataStoreManager.observe(KEY_WBW)
    }

    @Composable
    fun observeWbwShowTranslation(): Boolean {
        return DataStoreManager.observe(KEY_WBW_SHOW_TRANSLATION)
    }

    @Composable
    fun observeWbwShowTransliteration(): Boolean {
        return DataStoreManager.observe(KEY_WBW_SHOW_TRANSLITERATION)
    }

    @Composable
    fun observeWbwTooltipShowTranslation(): Boolean {
        return DataStoreManager.observe(KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
    }

    @Composable
    fun observeWbwTooltipShowTransliteration(): Boolean {
        return DataStoreManager.observe(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
    }

    @Composable
    fun observeWbwRecitationEnabled(): Boolean {
        return DataStoreManager.observe(KEY_WBW_RECITATION)
    }

    @Composable
    fun observeWbwTextSizeMultiplier(): Float {
        return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_WBW)
    }
}
