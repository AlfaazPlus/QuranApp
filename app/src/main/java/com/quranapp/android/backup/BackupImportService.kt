package com.quranapp.android.backup

import android.content.Context
import com.quranapp.android.compose.components.player.dialogs.AudioEndBehaviour
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.components.reader.ReadingLayout
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.preferences.AppPreferences
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.compose.utils.preferences.VersePreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.utils.app.ResourceDownloadProxy
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import java.time.Instant

class ExportKeys {
    companion object {
        const val BOOKMARKS = "bookmarks"
        const val COLLECTIONS = "collections"
        const val SETTINGS = "settings"
    }
}

data class ImportResult(
    val counts: MergeCounts,
    val unsupportedSettings: List<String>,
    val settingsImported: Boolean,
)

class BackupImportService(private val context: Context) {
    private val schema by lazy {
        context.assets.open("schemas/backup-v2.schema.json").bufferedReader().use { it.readText() }
    }
    private val repository by lazy { DatabaseProvider.getUserRepository(context) }
    private val quran by lazy { DatabaseProvider.getQuranRepository(context) }

    suspend fun exportBytes(scopes: Map<String, Boolean>): ByteArray {
        val bookmarks = if (scopes[ExportKeys.BOOKMARKS] == true) {
            repository.getBookmarks().map { it.toBackupBookmark() }.takeIf { it.isNotEmpty() }
        } else {
            null
        }

        val collections = if (scopes[ExportKeys.COLLECTIONS] == true) {
            repository.getCollectionsForBackup().takeIf { it.isNotEmpty() }
        } else {
            null
        }

        val settings = if (scopes[ExportKeys.SETTINGS] == true) exportSettings() else null
        val text = BackupCodec.write(Instant.now(), bookmarks, settings, collections, schema)

        return BackupFiles.gzip(text)
    }

    suspend fun import(bytes: ByteArray, scopes: Map<String, Boolean>): ImportResult {
        val document = BackupCodec.read(BackupFiles.decode(bytes), schema)
        val bookmarks = if (scopes[ExportKeys.BOOKMARKS] == true) document.bookmarks else null
        val collections = if (scopes[ExportKeys.COLLECTIONS] == true) document.collections else null

        val filteredBookmarks = bookmarks?.filter { bookmark ->
            quran.isVerseValid4Chapter(bookmark.chapterNumber, bookmark.startVerseNumber) &&
                    quran.isVerseValid4Chapter(bookmark.chapterNumber, bookmark.endVerseNumber)
        }

        val filteredCollections = collections?.map { collection ->
            collection.copy(
                items = collection.items.filter { item ->
                    quran.isVerseValid4Chapter(item.chapterNumber, item.verseNumber)
                }
            )
        }

        val skipped = (bookmarks?.size ?: 0) - (filteredBookmarks?.size ?: 0) +
                (collections?.sumOf { it.items.size } ?: 0) -
                (filteredCollections?.sumOf { it.items.size } ?: 0)

        val counts = repository.applyBackup(filteredBookmarks, filteredCollections) +
                MergeCounts(skipped = skipped)

        val issues = document.settingIssues.toMutableList()

        val settingsImported = scopes[ExportKeys.SETTINGS] == true && document.settings != null
        if (settingsImported) {
            issues += importSettings(document.settings)
        }

        return ImportResult(counts, issues, settingsImported)
    }

    private suspend fun exportSettings(): JsonObject {
        val values = linkedMapOf<String, JsonElement>()
        val locale = SPAppConfigs.getLocale(context)
        values[SettingsContract.LOCALE] = JsonPrimitive(locale)
        values[SettingsContract.NUMERAL_SYSTEM] = JsonPrimitive(
            SPAppConfigs.getNumeralSystem(context) ?: SPAppConfigs.NUMERAL_SYSTEM_DEFAULT
        )
        values[SettingsContract.THEME] = JsonPrimitive(ThemeUtils.getThemeMode())
        values[SettingsContract.THEME_COLOR] = JsonPrimitive(ThemeUtils.getThemeColor())
        values[SettingsContract.DYNAMIC_COLOR] = JsonPrimitive(ThemeUtils.getDynamicColor())
        values[SettingsContract.DAILY_REMINDER] =
            JsonPrimitive(VersePreferences.getVOTDReminderEnabled())
        values[SettingsContract.DOWNLOAD_SOURCE] =
            JsonPrimitive(AppPreferences.getResourceDownloadProxy().value)
        values[SettingsContract.DEVELOPER_MODE] =
            JsonPrimitive(AppPreferences.getDeveloperModeEnabled())
        values[SettingsContract.AUTO_SCROLL_SPEED] =
            JsonPrimitive(ReaderPreferences.getAutoScrollSpeed())
        values[SettingsContract.ARABIC_ENABLED] =
            JsonPrimitive(ReaderPreferences.getArabicTextEnabled())
        values[SettingsContract.STYLE] = JsonPrimitive(ReaderPreferences.getReaderMode().value)
        values[SettingsContract.KEEP_SCREEN_ON] = JsonPrimitive(ReaderPreferences.getKeepScreenOn())
        values[SettingsContract.READING_LAYOUT] =
            JsonPrimitive(ReaderPreferences.getReadingLayout().value)
        values[SettingsContract.PLAYBACK_SPEED] = JsonPrimitive(RecitationPreferences.getSpeed())
        values[SettingsContract.REPEAT_COUNT] = JsonPrimitive(
            RecitationPreferences.getRepeatCount().coerceIn(0, 9)
        )
        values[SettingsContract.VERSE_GROUP_SIZE] = JsonPrimitive(
            RecitationPreferences.getVerseGroupSize().coerceIn(1, 10)
        )
        RecitationPreferences.getReciterId()?.takeIf { it.isNotBlank() }?.let {
            values[SettingsContract.RECITER] = JsonPrimitive(it)
        }
        RecitationPreferences.getTranslationReciterId()?.takeIf { it.isNotBlank() }?.let {
            values[SettingsContract.VOICE] = JsonPrimitive(it)
        }
        values[SettingsContract.AUDIO_MODE] =
            JsonPrimitive(RecitationPreferences.getAudioOption().value)
        values[SettingsContract.END_BEHAVIOUR] =
            JsonPrimitive(RecitationPreferences.getAudioEndBehaviour().value)
        values[SettingsContract.TAFSIR_SCALE] =
            JsonPrimitive(ReaderPreferences.getTafsirTextSizeMultiplier())
        values[SettingsContract.ARABIC_SCALE] =
            JsonPrimitive(ReaderPreferences.getArabicTextSizeMultiplier())
        values[SettingsContract.TRANSLATION_SCALE] =
            JsonPrimitive(ReaderPreferences.getTranslationTextSizeMultiplier())
        values[SettingsContract.WBW_SCALE] =
            JsonPrimitive(ReaderPreferences.getWbwTextSizeMultiplier())
        values[SettingsContract.SCRIPT] = JsonPrimitive(ReaderPreferences.getQuranScript())
        ReaderPreferences.getQuranScriptVariant()?.let {
            values[SettingsContract.SCRIPT_VARIANT] = JsonPrimitive(it.value)
        }
        ReaderPreferences.getTafsirId()?.takeIf { it.isNotBlank() }?.let {
            values[SettingsContract.TAFSIR] = JsonPrimitive(it)
        }
        values[SettingsContract.TRANSLATIONS] = buildJsonArray {
            ReaderPreferences.getTranslations().forEach { add(JsonPrimitive(it)) }
        }
        ReaderPreferences.getWbwId()?.takeIf { it.isNotBlank() }?.let {
            values[SettingsContract.WBW_SOURCE] = JsonPrimitive(it)
        }
        values[SettingsContract.WBW_TRANSLATION] =
            JsonPrimitive(ReaderPreferences.getWbwShowTranslation())
        values[SettingsContract.WBW_TRANSLITERATION] =
            JsonPrimitive(ReaderPreferences.getWbwShowTransliteration())
        values[SettingsContract.WBW_TOOLTIP_TRANSLATION] =
            JsonPrimitive(ReaderPreferences.getWbwTooltipShowTranslation())
        values[SettingsContract.WBW_TOOLTIP_TRANSLITERATION] =
            JsonPrimitive(ReaderPreferences.getWbwTooltipShowTransliteration())
        values[SettingsContract.WBW_RECITATION] =
            JsonPrimitive(ReaderPreferences.getWbwRecitationEnabled())
        return SettingsContract.nest(values)
    }

    private suspend fun importSettings(settings: JsonObject): List<String> {
        val read = SettingsContract.read(settings)

        for (setting in read.accepted) {
            apply(setting)
        }

        return read.issues
    }

    private suspend fun apply(setting: AcceptedSetting) {
        when (setting.path) {
            SettingsContract.LOCALE -> {
                SPAppConfigs.setLocale(context, (setting.value as SettingValue.Text).value)
            }

            SettingsContract.NUMERAL_SYSTEM -> {
                SPAppConfigs.setNumeralSystem(context, (setting.value as SettingValue.Text).value)
            }

            SettingsContract.DAILY_REMINDER -> {
                VersePreferences.setVOTDReminderEnabled((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.THEME -> {
                ThemeUtils.setThemeMode((setting.value as SettingValue.Text).value)
            }

            SettingsContract.THEME_COLOR -> {
                ThemeUtils.setThemeColor((setting.value as SettingValue.Text).value)
            }

            SettingsContract.DYNAMIC_COLOR -> {
                ThemeUtils.setDynamicColor((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.DOWNLOAD_SOURCE -> {
                AppPreferences.setResourceDownloadProxy(
                    ResourceDownloadProxy.entries.first { it.value == (setting.value as SettingValue.Text).value }
                )
            }

            SettingsContract.DEVELOPER_MODE -> {
                AppPreferences.setDeveloperModeEnabled((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.AUTO_SCROLL_SPEED -> {
                ReaderPreferences.setAutoScrollSpeed((setting.value as SettingValue.Num).value.toFloat())
            }

            SettingsContract.ARABIC_ENABLED -> {
                ReaderPreferences.setArabicTextEnabled((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.STYLE -> {
                ReaderPreferences.setReaderMode(
                    ReaderMode.entries.first { it.value == (setting.value as SettingValue.Text).value }
                )
            }

            SettingsContract.KEEP_SCREEN_ON -> {
                ReaderPreferences.setKeepScreenOn((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.READING_LAYOUT -> {
                ReaderPreferences.setReadingLayout(
                    ReadingLayout.entries.first { it.value == (setting.value as SettingValue.Text).value }
                )
            }

            SettingsContract.PLAYBACK_SPEED -> {
                RecitationPreferences.setSpeed((setting.value as SettingValue.Num).value.toFloat())
            }

            SettingsContract.REPEAT_COUNT -> {
                RecitationPreferences.setRepeatCount((setting.value as SettingValue.Num).value.toInt())
            }

            SettingsContract.VERSE_GROUP_SIZE -> {
                RecitationPreferences.setVerseGroupSize((setting.value as SettingValue.Num).value.toInt())
            }

            SettingsContract.RECITER -> {
                RecitationPreferences.setReciterId((setting.value as SettingValue.Text).value)
            }

            SettingsContract.VOICE -> {
                RecitationPreferences.setTranslationReciterId((setting.value as SettingValue.Text).value)
            }

            SettingsContract.AUDIO_MODE -> {
                RecitationPreferences.setAudioOption(
                    AudioOption.entries.first { it.value == (setting.value as SettingValue.Text).value }
                )
            }

            SettingsContract.END_BEHAVIOUR -> {
                RecitationPreferences.setAudioEndBehaviour(
                    AudioEndBehaviour.entries.first { it.value == (setting.value as SettingValue.Text).value }
                )
            }

            SettingsContract.TAFSIR_SCALE -> {
                ReaderPreferences.setTafsirTextSizeMultiplier((setting.value as SettingValue.Num).value.toFloat())
            }

            SettingsContract.ARABIC_SCALE -> {
                ReaderPreferences.setArabicTextSizeMultiplier((setting.value as SettingValue.Num).value.toFloat())
            }

            SettingsContract.TRANSLATION_SCALE -> {
                ReaderPreferences.setTranslationTextSizeMultiplier((setting.value as SettingValue.Num).value.toFloat())
            }

            SettingsContract.WBW_SCALE -> {
                ReaderPreferences.setWbwTextSizeMultiplier((setting.value as SettingValue.Num).value.toFloat())
            }

            SettingsContract.SCRIPT -> ReaderPreferences.setQuranScript((setting.value as SettingValue.Text).value)
            SettingsContract.SCRIPT_VARIANT -> {
                val value = (setting.value as SettingValue.Text).value
                ReaderPreferences.setQuranScriptVariant(QuranScriptVariant.fromValue(value))
            }

            SettingsContract.TAFSIR -> ReaderPreferences.setTafsirId((setting.value as SettingValue.Text).value)
            SettingsContract.TRANSLATIONS -> {
                ReaderPreferences.setTranslations((setting.value as SettingValue.Texts).value.toSet())
            }

            SettingsContract.WBW_SOURCE -> ReaderPreferences.setWbwId((setting.value as SettingValue.Text).value)
            SettingsContract.WBW_TRANSLATION -> {
                ReaderPreferences.setWbwShowTranslation((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.WBW_TRANSLITERATION -> {
                ReaderPreferences.setWbwShowTransliteration((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.WBW_TOOLTIP_TRANSLATION -> {
                ReaderPreferences.setWbwTooltipShowTranslation((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.WBW_TOOLTIP_TRANSLITERATION -> {
                ReaderPreferences.setWbwTooltipShowTransliteration((setting.value as SettingValue.Bool).value)
            }

            SettingsContract.WBW_RECITATION -> {
                ReaderPreferences.setWbwRecitationEnabled((setting.value as SettingValue.Bool).value)
            }
        }
    }

}

private fun BookmarkEntity.toBackupBookmark(): BackupBookmark {
    return BackupBookmark(
        id = stableId,
        chapterNumber = chapterNo,
        startVerseNumber = fromVerseNo,
        endVerseNumber = toVerseNo,
        note = note,
        createdAt = if (legacyCreatedAtText.isNullOrBlank()) Instant.ofEpochMilli(dateTime.time) else null,
        updatedAt = updatedAt?.let { Instant.ofEpochMilli(it.time) },
        legacyCreatedAtText = legacyCreatedAtText,
    )
}
