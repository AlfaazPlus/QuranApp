package com.quranapp.android.db.entities.topics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relationships",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["src_topic_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["tgt_topic_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(name = "idx_relationships_source", value = ["src_topic_id"]),
        Index(name = "idx_relationships_target", value = ["tgt_topic_id"]),
        Index(name = "idx_relationships_type", value = ["type"]),
        Index(name = "idx_relationships_source_type", value = ["src_topic_id", "type"]),
    ],
)
data class RelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int? = null,
    @ColumnInfo(name = "src_topic_id")
    val srcTopicId: Int,
    @ColumnInfo(name = "tgt_topic_id")
    val tgtTopicId: Int,
    @ColumnInfo(name = "type")
    val type: RelationshipType,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int? = 0,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String?,
)

enum class RelationshipType(val dbValue: String) {
    NONE("none"),
    PARENT("parent"),
    RELATED("related"),
    THEMATIC_PARENT("thematic_parent"),
    ONTOLOGY_PARENT("ontology_parent");

    companion object {
        fun fromDbValue(value: String): RelationshipType =
            entries.firstOrNull { it.dbValue == value }
                ?: RelationshipType.NONE
    }
}
