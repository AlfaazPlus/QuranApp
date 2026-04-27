/*
 * (c) Faisal Khan. Created on 3/11/2021.
 */
package com.quranapp.android.utils.univ

object Keys {
    // Reader keys used by ActivityTafsir, ActivityChapInfo, ChapterInfoCard, etc.
    const val READER_KEY_CHAPTER_NO = "reader.chapter_no"
    const val READER_KEY_VERSE_NO = "reader.verse_no"

    // Reader keys used by settings, translation fragments, and other non-reader screens
    const val READER_KEY_READER_MODE = "reader.mode"
    const val READER_KEY_READ_TYPE = "reader.read_type"
    const val READER_KEY_TRANSL_SLUGS = "reader.translation_slugs"
    const val READER_KEY_SAVE_TRANSL_CHANGES = "reader.save_translation_changes"
    const val READER_KEY_SETTING_IS_FROM_READER = "reader.setting_is_from_reader"
    const val READER_KEY_ARABIC_TEXT_ENABLED = "reader.arabic_text_enabled"
    const val READER_KEY_AUTO_SCROLL_SPEED = "reader.auto_scroll_speed"

    // Keys still referenced by old ActivityReader.java — remove when that class is deleted
    @Deprecated("Used only by legacy ActivityReader")
    const val READER_KEY_READER_STYLE = "reader.style"

    @Deprecated("Used only by legacy ActivityReader")
    const val READER_KEY_JUZ_NO = "reader.juz_no"

    @Deprecated("Used only by legacy ActivityReader")
    const val READER_KEY_VERSES = "reader.verses"

    @Deprecated("Replaced by ReaderIntentData.initialVerse; used only by legacy ActivityReader")
    const val READER_KEY_PENDING_SCROLL = "reader.pending_scroll"

    const val KEY_ACTIVITY_RESUMED_FROM_NOTIFICATION = "key.resumeFromPlayerNotification"

    const val KEY_VOTD_DATE = "votd_date"
    const val KEY_VOTD_CHAPTER_NO = "votd_chapter_no"
    const val KEY_VOTD_VERSE_NO = "votd_verse_no"
    const val KEY_VOTD_REMINDER_ENABLED = "votd_reminder_enabled"

    const val KEY_EXTRA_TITLE = "title"

    const val KEY_EXCLUSIVE_VERSES_KIND = "exclusive_verses.kind"

    const val FAVOURITE_CHAPTERS = "favourite_chapters"

    const val NAV_DESTINATION = "nav_destination"
    const val SHOW_READER_SETTINGS_ONLY = "reader_settings_only"
}
