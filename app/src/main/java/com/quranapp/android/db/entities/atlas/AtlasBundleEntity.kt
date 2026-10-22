package com.quranapp.android.db.entities.atlas

import androidx.room3.ColumnInfo
import androidx.room3.Entity

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
)
