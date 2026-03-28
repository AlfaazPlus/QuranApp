package com.quranapp.android.utils.mediaplayer

enum class RecitationClipKind {
    QURAN,
    TRANSLATION,
}

data class RecitationMediaItem(
    val slug: String,
    val translationSlug: String?,
    val chapterNo: Int,
    val verseNo: Int,
    val clipKind: RecitationClipKind = RecitationClipKind.QURAN,
) {
    companion object {
        fun fromTag(tag: Any?): RecitationMediaItem? = tag as? RecitationMediaItem
    }
}
