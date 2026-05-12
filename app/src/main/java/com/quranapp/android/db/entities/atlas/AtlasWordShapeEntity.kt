package com.quranapp.android.db.entities.atlas

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "atlas_word_shapes",
    primaryKeys = ["bundle_key", "word", "page"],
    indices = [
        Index(value = ["bundle_key"], name = "idx_atlas_word_shapes_bundle"),
    ],
)
data class AtlasWordShapeEntity(
    @ColumnInfo(name = "bundle_key")
    val bundleKey: String,
    @ColumnInfo(name = "word")
    val word: String,
    /**
     * Mushaf page (1-based) when the bundle is page-scoped; otherwise [ATLAS_PAGE_NONE] for
     * word-global shapes.
     */
    @ColumnInfo(name = "page")
    val page: Int,
    @ColumnInfo(name = "placements_json")
    val placementsJson: String,
) {
    companion object {
        /** Row key for bundles where glyph shapes are not split by Mushaf page. */
        const val ATLAS_PAGE_NONE: Int = -1
    }
}
