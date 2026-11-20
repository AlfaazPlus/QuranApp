package com.quranapp.android.utils.reader

import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.reader.ReaderMode

enum class ReadType(val value: String) {
    Chapter("chapter"),
    Juz("juz"),
    Hizb("hizb");

    companion object {
        fun fromValue(value: String?): ReadType =
            entries.find { it.value == value } ?: Chapter

        fun fromLegacyInt(type: Int): ReadType = when (type) {
            0x5 -> Juz
            else -> Chapter
        }
    }
}

sealed class ReaderIntentData {
    open val initialVerse: ChapterVersePair? get() = null

    data class FullChapter(
        val chapterNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class FullJuz(
        val juzNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class FullHizb(
        val hizbNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()

    data class MushafPage(
        val mushafCode: String,
        val mushafVariant: QuranScriptVariant?,
        val pageNo: Int,
        override val initialVerse: ChapterVersePair? = null,
    ) : ReaderIntentData()
}

data class ReaderLaunchParams(
    val data: ReaderIntentData,
    val readerMode: ReaderMode? = null,
    val slugs: Set<String>? = null,
    val mainHistoryId: Long? = null,
) {
    fun toInitSignature(): String {
        val slugsPart = slugs?.toList()?.sorted()?.joinToString(",") ?: "-"
        val modePart = readerMode?.value ?: "-"
        val mainPart = mainHistoryId?.toString() ?: "-"
        val dataPart = when (val d = data) {
            is ReaderIntentData.FullChapter -> "chapter:$d"
            is ReaderIntentData.FullJuz -> "juz:$d"
            is ReaderIntentData.FullHizb -> "hizb:$d"
            is ReaderIntentData.MushafPage -> "mushaf:$d"
        }
        return "$dataPart|mode:$modePart|slugs:$slugsPart|main:$mainPart"
    }

    companion object {
        internal const val KEY_READ_TYPE = "reader.read_type"
        internal const val KEY_CHAPTER_NO = "reader.chapter_no"
        internal const val KEY_JUZ_NO = "reader.juz_no"
        internal const val KEY_HIZB_NO = "reader.hizb_no"
        internal const val KEY_MUSHAF_CODE = "reader.mushaf_code"
        internal const val KEY_MUSHAF_VARIANT = "reader.mushaf_variant"
        internal const val KEY_RESTORE_PAGE = "reader.restore_page"
        internal const val KEY_INITIAL_VERSE_CHAPTER = "reader.initial_verse_chapter"
        internal const val KEY_INITIAL_VERSE_NO = "reader.initial_verse_no"
        internal const val KEY_READER_MODE = "reader.mode"
        internal const val KEY_TRANSL_SLUGS = "reader.translation_slugs"
        internal const val KEY_MAIN_HISTORY_ID = "reader.main_history_id"

        /** Exposed for external intent builders (deep links, app intents). */
        const val EXTERNAL_KEY_READER_MODE = KEY_READER_MODE
        const val EXTERNAL_KEY_TRANSL_SLUGS = KEY_TRANSL_SLUGS
    }
}
