package com.quranapp.android.db.entities.user

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import java.util.Date

@Entity(
    tableName = "user_collection_items",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["collection_id", "chapter_no", "verse_no"],
            unique = true,
            name = "index_user_collection_items_collection_id_chapter_no_verse_no",
        ),
        Index(
            value = ["collection_id"],
            name = "index_user_collection_items_collection_id",
        ),
    ],
)
data class CollectionItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "collection_id")
    val collectionId: Long,

    @ColumnInfo(name = "chapter_no")
    val chapterNo: Int,

    @ColumnInfo(name = "verse_no")
    val verseNo: Int,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "sort_index")
    val sortIndex: Int,

    @ColumnInfo(name = "added_at")
    val addedAt: Date = Date(),
)
