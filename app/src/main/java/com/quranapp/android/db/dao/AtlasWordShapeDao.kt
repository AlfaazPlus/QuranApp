package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranapp.android.db.entities.atlas.AtlasBundleEntity
import com.quranapp.android.db.entities.atlas.AtlasWordShapeEntity

@Dao
interface AtlasWordShapeDao {
    @Query("SELECT * FROM atlas_bundles WHERE bundle_key = :bundleKey LIMIT 1")
    suspend fun getBundleByKey(bundleKey: String): AtlasBundleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBundle(row: AtlasBundleEntity)

    @Query("SELECT COUNT(*) FROM atlas_word_shapes WHERE bundle_key = :bundleKey")
    suspend fun countShapesForBundle(bundleKey: String): Long

    @Query(
        """
        SELECT * FROM atlas_word_shapes
        WHERE bundle_key = :bundleKey AND word = :word AND page = :page
        LIMIT 1
        """,
    )
    suspend fun getShape(bundleKey: String, word: String, page: Int): AtlasWordShapeEntity?

    @Query(
        """
        SELECT * FROM atlas_word_shapes
        WHERE bundle_key = :bundleKey AND page = :page AND word IN (:words)
        """,
    )
    suspend fun getShapesForWords(bundleKey: String, words: List<String>, page: Int): List<AtlasWordShapeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShapes(rows: List<AtlasWordShapeEntity>)

    @Query("DELETE FROM atlas_word_shapes WHERE bundle_key = :bundleKey")
    suspend fun deleteShapesForBundle(bundleKey: String)
}
