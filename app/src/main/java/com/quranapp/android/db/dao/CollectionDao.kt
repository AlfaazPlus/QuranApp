package com.quranapp.android.db.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.db.entities.user.CollectionWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun createCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM user_collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("SELECT * FROM user_collections WHERE id = :id")
    suspend fun getCollection(id: Long): CollectionEntity?

    @Query(
        """
        SELECT user_collections.*,
            (
                SELECT COUNT(*) FROM user_collection_items
                WHERE collection_id = user_collections.id
            ) AS verse_count
        FROM user_collections
        WHERE id = :id
        """
    )
    fun observeWithCount(id: Long): Flow<CollectionWithCount?>

    @Query(
        """
        SELECT user_collections.*,
            (
                SELECT COUNT(*) FROM user_collection_items
                WHERE collection_id = user_collections.id
            ) AS verse_count
        FROM user_collections
        ORDER BY updated_at DESC, id DESC
        LIMIT :limit
        """
    )
    fun getFlow(limit: Int): Flow<List<CollectionWithCount>>

    @Query(
        """
        SELECT user_collections.*,
            (
                SELECT COUNT(*) FROM user_collection_items
                WHERE collection_id = user_collections.id
            ) AS verse_count
        FROM user_collections
        WHERE :query = ''
           OR name LIKE '%' || :query || '%' COLLATE NOCASE
           OR COALESCE(description, '') LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY updated_at DESC, id DESC
        """
    )
    fun getAllPaginated(query: String): PagingSource<Int, CollectionWithCount>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: CollectionItemEntity): Long

    @Update
    suspend fun updateItem(item: CollectionItemEntity)

    @Query("SELECT * FROM user_collection_items WHERE id = :id")
    suspend fun getItem(id: Long): CollectionItemEntity?

    @Query(
        """
        SELECT * FROM user_collection_items
        WHERE collection_id = :collectionId
        ORDER BY sort_index ASC, id ASC
        """
    )
    fun getItemsPaginated(collectionId: Long): PagingSource<Int, CollectionItemEntity>

    @Query("SELECT MAX(sort_index) FROM user_collection_items WHERE collection_id = :collectionId")
    suspend fun maxItemSort(collectionId: Long): Int?

    @Query(
        """
        DELETE FROM user_collection_items
        WHERE collection_id = :collectionId
          AND chapter_no = :chapterNo
          AND verse_no = :verseNo
        """
    )
    suspend fun deleteItem(collectionId: Long, chapterNo: Int, verseNo: Int)

    @Query("DELETE FROM user_collection_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE user_collections SET updated_at = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Query(
        """
        SELECT collection_id FROM user_collection_items
        WHERE chapter_no = :chapterNo AND verse_no = :verseNo
        """
    )
    fun observeCollectionIdsForVerse(chapterNo: Int, verseNo: Int): Flow<List<Long>>

    @Query(
        """
        SELECT collection_id FROM user_collection_items
        WHERE chapter_no = :chapterNo AND verse_no = :verseNo
        """
    )
    suspend fun collectionIdsForVerse(chapterNo: Int, verseNo: Int): List<Long>
}
