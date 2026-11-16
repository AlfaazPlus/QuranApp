package com.quranapp.android.db.entities.user

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "user_collections",
    indices = [
        Index(value = ["stable_id"], unique = true, name = "index_user_collections_stable_id"),
    ],
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "icon_key")
    val icon_key: String? = null,

    @ColumnInfo(name = "color")
    val color: String? = null,

    @ColumnInfo(name = "sort_index")
    val sortIndex: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date(),

    @ColumnInfo(name = "stable_id", defaultValue = "''")
    val stableId: String = UUID.randomUUID().toString(),
)

data class CollectionWithCount(
    @Embedded
    val collection: CollectionEntity,
    @ColumnInfo(name = "verse_count")
    val verseCount: Int,
)
