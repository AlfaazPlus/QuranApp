package com.quranapp.android.db.entities.user

import androidx.compose.runtime.Immutable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "user_bookmarks",
    indices = [
        Index(value = ["stable_id"], unique = true, name = "index_user_bookmarks_stable_id"),
        Index(
            value = ["chapter_no", "from_verse_no", "to_verse_no"],
            unique = true,
            name = "index_user_bookmarks_chapter_no_from_verse_no_to_verse_no",
        ),
    ],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "chapter_no")
    val chapterNo: Int,

    @ColumnInfo(name = "from_verse_no")
    val fromVerseNo: Int,

    @ColumnInfo(name = "to_verse_no")
    val toVerseNo: Int,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "date")
    val dateTime: Date = Date(),

    @ColumnInfo(name = "stable_id", defaultValue = "''")
    val stableId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date? = null,

    @ColumnInfo(name = "legacy_created_at_text")
    val legacyCreatedAtText: String? = null,
)


@Immutable
data class BookmarkKey(
    val chapterNo: Int,
    val fromVerse: Int,
    val toVerse: Int
)
