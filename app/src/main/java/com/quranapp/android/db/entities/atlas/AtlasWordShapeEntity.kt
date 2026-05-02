package com.quranapp.android.db.entities.atlas

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "atlas_word_shapes",
    primaryKeys = ["bundle_key", "word"],
    indices = [
        Index(value = ["bundle_key"], name = "idx_atlas_word_shapes_bundle"),
    ],
)
data class AtlasWordShapeEntity(
    @ColumnInfo(name = "bundle_key")
    val bundleKey: String,
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "placements_json")
    val placementsJson: String,
)
