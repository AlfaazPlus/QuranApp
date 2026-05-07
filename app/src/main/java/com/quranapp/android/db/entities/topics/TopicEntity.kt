package com.quranapp.android.db.entities.topics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    indices = [
        Index(name = "idx_topics_type", value = ["type"]),
        Index(value = ["slug"], unique = true),
    ],
)
data class TopicEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int?,
    @ColumnInfo(name = "slug")
    val slug: String?,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
    @ColumnInfo(name = "icon")
    val icon: String?,
    @ColumnInfo(name = "flags", defaultValue = "0")
    val flags: TopicFlags? = TopicFlags.NONE,
    @ColumnInfo(name = "created_at")
    val createdAt: Long?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long?,
)


enum class TopicFlags(val dbValue: Int) {
    NONE(0),
    THEMATIC(1),
    ONTOLOGY(2),
    THEMATIC_AND_ONTOLOGY(3);

    val isThematic: Boolean
        get() = (dbValue and THEMATIC.dbValue) != 0

    val isOntology: Boolean
        get() = (dbValue and ONTOLOGY.dbValue) != 0

    companion object {
        fun fromDbValue(value: Int): TopicFlags =
            entries.firstOrNull { it.dbValue == value }
                ?: TopicFlags.NONE
    }
}
