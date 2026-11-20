package com.quranapp.android.platform.shortcut

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.continue_reading_2
import com.quranapp.android.generated.resources.strTitleVOTD
import com.quranapp.android.utils.quran.QuranMeta
import org.jetbrains.compose.resources.getString

/**
 * Platform-agnostic interface for publishing and removing application shortcuts.
 */
interface ShortcutManager {
    fun pushShortcut(shortcut: AppShortcut)
    fun removeShortcuts(ids: List<String>)
    fun removeShortcut(id: String) = removeShortcuts(listOf(id))
    fun removeAllShortcuts()
    fun getDynamicShortcuts(): List<AppShortcut> = emptyList()
    fun isDynamicShortcutsSupported(): Boolean
    fun disableShortcuts(ids: List<String>, disabledMessage: String? = null) {}
    fun enableShortcuts(ids: List<String>) {}

    suspend fun pushVotdShortcut(chapterNo: Int, verseNo: Int)
    suspend fun pushLastVersesShortcut(
        readType: String,
        readerMode: String?,
        chapterNo: Int,
        fromVerseNo: Int,
        divisionNo: Int? = null,
    )
}

/**
 * Common base implementation of [ShortcutManager] containing cross-platform
 * business logic for shortcut creation, string resolution, and payload preparation.
 */
abstract class BaseShortcutManager : ShortcutManager {

    override fun removeShortcut(id: String) {
        removeShortcuts(listOf(id))
    }

    override fun getDynamicShortcuts(): List<AppShortcut> = emptyList()

    override fun disableShortcuts(ids: List<String>, disabledMessage: String?) {}

    override fun enableShortcuts(ids: List<String>) {}

    override suspend fun pushVotdShortcut(chapterNo: Int, verseNo: Int) {
        val label = try {
            getString(Res.string.strTitleVOTD)
        } catch (_: Exception) {
            "Verse of the Day"
        }

        val shortcut = AppShortcut(
            id = ShortcutConstants.ID_VOTD,
            shortLabel = label,
            longLabel = label,
            icon = ShortcutIcon.Resource(name = "dr_ic_shortcut_votd"),
            intent = ShortcutIntent.Activity(
                destination = ShortcutConstants.DESTINATION_READER,
                action = "android.intent.action.VIEW",
                extras = mapOf(
                    "chapterNo" to chapterNo,
                    "verseNo" to verseNo,
                    "read_type" to "chapter",
                    "chapter_no" to chapterNo,
                    "initial_verse_chapter" to chapterNo,
                    "initial_verse_no" to verseNo,
                ),
            ),
        )

        pushShortcut(shortcut)
    }

    override suspend fun pushLastVersesShortcut(
        readType: String,
        readerMode: String?,
        chapterNo: Int,
        fromVerseNo: Int,
        divisionNo: Int?,
    ) {
        val isValid = when (readType.lowercase()) {
            "chapter" -> QuranMeta.isChapterValid(chapterNo)
            "juz" -> QuranMeta.isJuzValid(divisionNo)
            "hizb" -> QuranMeta.isHizbValid(divisionNo)
            else -> QuranMeta.isChapterValid(chapterNo)
        }

        if (!isValid) {
            removeShortcut(ShortcutConstants.ID_LAST_VERSES)
            return
        }

        val label = try {
            getString(Res.string.continue_reading_2)
        } catch (_: Exception) {
            "Continue Reading"
        }

        val extras = mutableMapOf<String, Any?>(
            "chapterNo" to chapterNo,
            "verseNo" to fromVerseNo,
            "read_type" to readType,
            "chapter_no" to chapterNo,
            "initial_verse_chapter" to chapterNo,
            "initial_verse_no" to fromVerseNo,
        )

        if (!readerMode.isNullOrEmpty()) {
            extras["reader_mode"] = readerMode
        }

        if (divisionNo != null && divisionNo > 0) {
            extras["division_no"] = divisionNo
            when (readType.lowercase()) {
                "juz" -> extras["juzNo"] = divisionNo
                "hizb" -> extras["hizbNo"] = divisionNo
            }
        }

        val shortcut = AppShortcut(
            id = ShortcutConstants.ID_LAST_VERSES,
            shortLabel = label,
            longLabel = label,
            icon = ShortcutIcon.Resource(name = "icon_launcher"),
            intent = ShortcutIntent.Activity(
                destination = ShortcutConstants.DESTINATION_READER,
                action = "android.intent.action.VIEW",
                extras = extras,
            ),
        )

        pushShortcut(shortcut)
    }
}
