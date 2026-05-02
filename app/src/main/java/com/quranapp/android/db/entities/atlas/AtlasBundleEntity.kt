package com.quranapp.android.db.entities.atlas

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "atlas_bundles",
    primaryKeys = ["bundle_key"],
)
data class AtlasBundleEntity(
    @ColumnInfo(name = "bundle_key")
    val bundleKey: String,
    @ColumnInfo(name = "meta_json")
    val metaJson: String,
    @ColumnInfo(name = "layer_json")
    val layerJson: String,
    @ColumnInfo(name = "image_png")
    val imagePng: ByteArray,
)
