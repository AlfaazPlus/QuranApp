package com.quranapp.android.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.backup.BackupPreferences
import com.quranapp.android.reader.ReaderMode
import com.quranapp.android.reader.ReadingLayout
import com.quranapp.android.utils.app.ResourceDownloadProxy
import com.quranapp.android.utils.mediaplayer.AudioEndBehaviour
import com.quranapp.android.utils.mediaplayer.AudioOption
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.TranslUtils


@Composable
fun <T> DataStoreManager.observe(prefKey: PrefKey<T>): T = observe(prefKey.key, prefKey.default)

@Composable
fun <T> DataStoreManager.observe(
    key: androidx.datastore.preferences.core.Preferences.Key<T>,
    defaultValue: T
): T {
    val flow = remember(key) { flow(key, defaultValue) }
    return flow.collectAsStateWithLifecycle(initialValue = defaultValue).value
}

@Composable
fun ReaderPreferences.observeArabicTextEnabled(): Boolean {
    return DataStoreManager.observe(KEY_ARABIC_TEXT_ENABLED)
}


@Composable
fun ReaderPreferences.observeKeepScreenOn(): Boolean {
    return DataStoreManager.observe(KEY_KEEP_SCREEN_ON)
}


@Composable
fun ReaderPreferences.observeAutoScrollSpeed(): Float {
    return DataStoreManager.observe(KEY_AUTO_SCROLL_SPEED)
}


@Composable
fun ReaderPreferences.observeArabicTextSizeMultiplier(): Float {
    return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_ARABIC)
}


@Composable
fun ReaderPreferences.observeTranlationTextSizeMultiplier(): Float {
    return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_TRANSL)
}


@Composable
fun ReaderPreferences.observeTafsirTextSizeMultiplier(): Float {
    return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_TAFSIR)
}


@Composable
fun ReaderPreferences.observePrimaryTranslationSlug(): String {
    val saved = observeTranslations()
    return saved.firstOrNull { !TranslUtils.isTransliteration(it) }
        ?: TranslUtils.TRANSL_SLUG_DEFAULT
}


@Composable
fun ReaderPreferences.observeTranslations(): Set<String> {
    return DataStoreManager.observe(KEY_TRANSLATIONS)
}


@Composable
fun ReaderPreferences.observeQuranScript(): String {
    val s = DataStoreManager.observe(KEY_SCRIPT)

    if (!QuranScriptUtils.availableScripts().contains(s)) {
        return QuranScriptUtils.SCRIPT_DEFAULT
    }

    return s
}


@Composable
fun ReaderPreferences.observeQuranScriptVariant(): QuranScriptVariant? {
    val value = DataStoreManager.observe(KEY_SCRIPT_VARIANT)

    return QuranScriptVariant.fromValue(value)
}


@Composable
fun ReaderPreferences.observeReaderMode(): ReaderMode {
    return DataStoreManager.observe(KEY_READER_MODE).let { ReaderMode.fromValue(it) }
}


@Composable
fun ReaderPreferences.observeReadingLayout(): ReadingLayout {
    return ReadingLayout.fromValue(DataStoreManager.observe(KEY_READING_LAYOUT))
}


@Composable
fun ReaderPreferences.observeTafsirId(): String? {
    val raw = DataStoreManager.observe(KEY_TAFSIR)
    return raw.ifEmpty { null }
}


@Composable
fun ReaderPreferences.observeWbwId(): String {
    return DataStoreManager.observe(KEY_WBW)
}


@Composable
fun ReaderPreferences.observeWbwShowTranslation(): Boolean {
    return DataStoreManager.observe(KEY_WBW_SHOW_TRANSLATION)
}


@Composable
fun ReaderPreferences.observeWbwShowTransliteration(): Boolean {
    return DataStoreManager.observe(KEY_WBW_SHOW_TRANSLITERATION)
}


@Composable
fun ReaderPreferences.observeWbwTooltipShowTranslation(): Boolean {
    return DataStoreManager.observe(KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
}


@Composable
fun ReaderPreferences.observeWbwTooltipShowTransliteration(): Boolean {
    return DataStoreManager.observe(KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
}


@Composable
fun ReaderPreferences.observeWbwRecitationEnabled(): Boolean {
    return DataStoreManager.observe(KEY_WBW_RECITATION)
}


@Composable
fun ReaderPreferences.observeWbwTextSizeMultiplier(): Float {
    return DataStoreManager.observe(KEY_TEXT_SIZE_MULT_WBW)
}


@Composable
fun RecitationPreferences.observeReciterId(): String? {
    val raw = DataStoreManager.observe(KEY_RECITER, "")
    return raw.ifEmpty { null }
}


@Composable
fun RecitationPreferences.observeTranslationReciterId(): String? {
    val raw = DataStoreManager.observe(KEY_TRANSLATION_RECITER, "")
    return raw.ifEmpty { null }
}


@Composable
fun RecitationPreferences.observeSpeed(): Float {
    return DataStoreManager.observe(KEY_SPEED, RECITATION_DEFAULT_SPEED)
}


@Composable
fun RecitationPreferences.observeRepeatCount(): Int {
    return DataStoreManager.observe(
        KEY_REPEAT_COUNT, RECITATION_DEFAULT_REPEAT_COUNT
    ).coerceAtLeast(0)
}


@Composable
fun RecitationPreferences.observeAudioOption(): AudioOption {
    return DataStoreManager.observe(KEY_AUDIO_OPTION, AudioOption.DEFAULT.value).let {
        AudioOption.fromValue(it)
    }
}


@Composable
fun RecitationPreferences.observeAudioEndBehaviour(): AudioEndBehaviour {
    return DataStoreManager.observe(KEY_AUDIO_END_BEHAVIOUR, AudioEndBehaviour.DEFAULT.value)
        .let {
            AudioEndBehaviour.fromValue(it)
        }
}


@Composable
fun RecitationPreferences.observeVerseGroupSize(): Int {
    return DataStoreManager.observe(
        KEY_VERSE_GROUP_SIZE,
        RECITATION_DEFAULT_VERSE_GROUP_SIZE
    )
}


@Composable
fun AppPreferences.observeDeveloperModeEnabled(): Boolean {
    return DataStoreManager.observe(KEY_DEVELOPER_MODE)
}


@Composable
fun AppPreferences.observeResourceDownloadProxy(): ResourceDownloadProxy {
    return DataStoreManager.observe(KEY_DOWNLOAD_PROXY)
        .let { ResourceDownloadProxy.fromValue(it) }
}


@Composable
fun VersePreferences.observeVOTDReminderEnabled(): Boolean {
    return DataStoreManager.observe(KEY_VOTD_REMINDER_ENABLED)
}


@Composable
fun BackupPreferences.observeEnabled(): Boolean = DataStoreManager.observe(enabledKey)

@Composable
fun BackupPreferences.observeDocumentUri(): String = DataStoreManager.observe(documentUriKey)

@Composable
fun BackupPreferences.observeFileDeletedAlert(): Boolean =
    DataStoreManager.observe(fileDeletedAlertKey)

