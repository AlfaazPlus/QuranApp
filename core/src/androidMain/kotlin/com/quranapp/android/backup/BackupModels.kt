package com.quranapp.android.backup

import kotlinx.serialization.json.JsonObject
import java.time.Instant

const val BACKUP_SCHEMA = "https://schema.alfaazplus.com/quran/backup-v2.schema.json"
const val BACKUP_FORMAT_VERSION = 2

enum class BackupReadFailure {
    UNREADABLE,
    INVALID,
    UNSUPPORTED_VERSION,
    TOO_LARGE,
}

class BackupReadException(val failure: BackupReadFailure) : Exception()

data class BackupBookmark(
    val id: String?,
    val chapterNumber: Int,
    val startVerseNumber: Int,
    val endVerseNumber: Int,
    val note: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val legacyCreatedAtText: String?,
)

data class BackupCollectionItem(
    val chapterNumber: Int,
    val verseNumber: Int,
    val note: String?,
    val position: Int,
    val addedAt: Instant?,
)

data class BackupCollection(
    val id: String?,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val color: String?,
    val sortIndex: Int?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val timestampsTrusted: Boolean,
    val items: List<BackupCollectionItem>,
)

data class BackupDocument(
    val bookmarks: List<BackupBookmark>?,
    val settings: JsonObject?,
    val collections: List<BackupCollection>?,
    val settingIssues: List<String> = emptyList(),
)

data class MergeCounts(
    val inserted: Int = 0,
    val updated: Int = 0,
    val unchanged: Int = 0,
    val skipped: Int = 0,
    val conflicts: Int = 0,
) {
    operator fun plus(other: MergeCounts) = MergeCounts(
        inserted + other.inserted,
        updated + other.updated,
        unchanged + other.unchanged,
        skipped + other.skipped,
        conflicts + other.conflicts,
    )
}

enum class BookmarkChange { UPDATE, UNCHANGED, CONFLICT }

/**
 * Newest-wins only when both modification times are known.
 * Identical content is unchanged. Unknown times never overwrite local content.
 */
fun bookmarkChange(
    localUpdatedAt: Instant?,
    incomingUpdatedAt: Instant?,
    sameContent: Boolean,
): BookmarkChange {
    if (localUpdatedAt != null && incomingUpdatedAt != null) {
        val comparison = incomingUpdatedAt.compareTo(localUpdatedAt)
        if (comparison > 0) return if (sameContent) BookmarkChange.UNCHANGED else BookmarkChange.UPDATE
        if (comparison < 0) return BookmarkChange.UNCHANGED
        return if (sameContent) BookmarkChange.UNCHANGED else BookmarkChange.CONFLICT
    }
    return if (sameContent) BookmarkChange.UNCHANGED else BookmarkChange.CONFLICT
}
