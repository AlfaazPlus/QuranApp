package com.quranapp.android.repository

import android.content.Context
import android.widget.Toast
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room3.withWriteTransaction
import com.quranapp.android.R
import com.quranapp.android.components.bookmark.CollectionExportModel
import com.quranapp.android.db.UserDatabase
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.db.entities.user.CollectionWithCount
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking
import java.util.Date

class UserRepository(
    private val context: Context,
    private val database: UserDatabase
) {
    private val bookmarkDao get() = database.bookmarkDao()
    private val readHistoryDao get() = database.readHistoryDao()
    private val collectionDao get() = database.collectionDao()

    companion object {
        private const val HISTORY_LIMIT = 40
    }

    suspend fun addMultipleBookmarks(bookmarks: List<BookmarkEntity>) {
        bookmarkDao.insertAll(bookmarks)
    }

    suspend fun addToBookmark(
        chapterNo: Int,
        verseRange: IntRange,
        note: String?,
    ) {
        if (isBookmarked(chapterNo, verseRange)) {
            Toast.makeText(context, R.string.strMsgBookmarkAddedAlready, Toast.LENGTH_SHORT).show()
            return
        }

        val entity = BookmarkEntity(
            chapterNo = chapterNo,
            fromVerseNo = verseRange.first,
            toVerseNo = verseRange.last,
            note = note
        )

        val rowId = bookmarkDao.insert(entity)
        val inserted = rowId != -1L

        val msg = if (inserted) {
            R.string.strMsgBookmarkAdded
        } else {
            R.string.strMsgBookmarkAddFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun addToBookmarkBlocking(
        chapterNo: Int,
        verseRange: IntRange,
        note: String?,
    ) = runBlocking {
        addToBookmark(chapterNo, verseRange, note)
    }

    suspend fun updateBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        note: String?,
    ) {
        val existing = bookmarkDao.getBookmark(chapterNo, fromVerse, toVerse) ?: return
        bookmarkDao.updateBookmark(existing.copy(note = note))
    }

    suspend fun removeFromBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): Boolean {
        val rowsAffected = bookmarkDao.removeBookmark(chapterNo, fromVerse, toVerse)
        val deleted = rowsAffected >= 1

        val msg = if (deleted) {
            R.string.strMsgBookmarkRemoved
        } else {
            R.string.strMsgBookmarkRemoveFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

        return deleted
    }

    fun removeFromBookmarkBlocking(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): Boolean = runBlocking {
        removeFromBookmark(chapterNo, fromVerse, toVerse)
    }

    suspend fun removeBookmarksBulk(
        ids: LongArray,
    ): Boolean {
        val rowsAffected = bookmarkDao.removeBookmarksBulk(ids.toList())
        val deleted = rowsAffected >= 1

        val msg = if (deleted) {
            R.string.strMsgBookmarkRemoved
        } else {
            R.string.strMsgBookmarkRemoveFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

        return deleted
    }

    suspend fun isBookmarked(
        chapterNo: Int,
        verseRange: IntRange
    ): Boolean {
        return bookmarkDao.countBookmark(chapterNo, verseRange.first, verseRange.last) > 0
    }

    fun isBookmarkedBlocking(
        chapterNo: Int,
        verseRange: IntRange
    ): Boolean = runBlocking {
        isBookmarked(chapterNo, verseRange)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isBookmarkedFlow(
        chapterNo: Int,
        verseRange: IntRange
    ): Flow<Boolean> {
        return bookmarkDao.countBookmarkFlow(chapterNo, verseRange.first, verseRange.last)
            .mapLatest { it > 0 }
    }

    suspend fun removeAllBookmarks() {
        bookmarkDao.removeAllBookmarks()
    }

    suspend fun getBookmark(
        chapNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): BookmarkEntity? {
        return bookmarkDao.getBookmark(chapNo, fromVerse, toVerse)
    }

    suspend fun getBookmarks(): ArrayList<BookmarkEntity> {
        return ArrayList(bookmarkDao.getBookmarks())
    }

    fun getBookmarksFlow(): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksFlow()
    }

    fun getBookmarksFlow(limit: Int): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getFlow(limit)
    }

    fun getBookmarksPagingFlow(
        pageSize: Int = 20
    ): Flow<PagingData<BookmarkEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { bookmarkDao.getBookmarksPaginated() }
        ).flow
    }

    fun getBookmarkFlow(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Flow<BookmarkEntity?> {
        return bookmarkDao.getBookmarkFlow(chapterNo, fromVerse, toVerse)
    }

    // ── Read History ──
    suspend fun saveReadHistory(entity: ReadHistoryEntity): Long {
        readHistoryDao.deleteDuplicate(
            readType = entity.readType,
            readerMode = entity.readerMode,
            divisionNo = entity.divisionNo,
            chapterNo = entity.chapterNo,
            fromVerseNo = entity.fromVerseNo,
            toVerseNo = entity.toVerseNo,
        )

        val id = readHistoryDao.insert(entity)
        readHistoryDao.trimToSize(HISTORY_LIMIT)

        return id
    }

    fun getHistoriesFlow(limit: Int): Flow<List<ReadHistoryEntity>> {
        return readHistoryDao.getFlow(limit)
    }

    fun getHistoriesPaginated(): Flow<PagingData<ReadHistoryEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { readHistoryDao.getAllPaginated() }
        ).flow
    }

    suspend fun deleteHistory(id: Long) {
        readHistoryDao.deleteById(id)
    }

    suspend fun deleteAllHistories() {
        readHistoryDao.deleteAll()
    }

    fun getCollectionsFlow(limit: Int): Flow<List<CollectionWithCount>> {
        return collectionDao.getFlow(limit)
    }

    fun getCollectionsPaginated(query: String = ""): Flow<PagingData<CollectionWithCount>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { collectionDao.getAllPaginated(query.trim()) }
        ).flow
    }


    suspend fun getCollection(id: Long): CollectionEntity? {
        return collectionDao.getCollection(id)
    }

    fun observeCollection(id: Long): Flow<CollectionWithCount?> {
        return collectionDao.observeWithCount(id)
    }

    fun getCollectionItems(collectionId: Long): Flow<PagingData<CollectionItemEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { collectionDao.getItemsPaginated(collectionId) }
        ).flow
    }

    fun observeCollectionIdsForVerse(chapterNo: Int, verseNo: Int): Flow<List<Long>> {
        return collectionDao.observeCollectionIdsForVerse(chapterNo, verseNo)
    }

    suspend fun addCollection(collection: CollectionEntity) {
        collectionDao.createCollection(collection)
    }

    suspend fun updateCollection(collection: CollectionEntity) {
        collectionDao.updateCollection(collection)
    }

    suspend fun deleteCollection(id: Long) {
        collectionDao.deleteCollection(id)
    }

    suspend fun getCollectionsForExport(): List<CollectionExportModel> {
        return collectionDao.getAll().map { collection ->
            CollectionExportModel.fromEntities(
                collection,
                collectionDao.getItems(collection.id),
            )
        }
    }

    suspend fun importCollections(collections: List<CollectionExportModel>) {
        database.withWriteTransaction {
            for (incoming in collections) {
                val name = incoming.name.trim()
                if (name.isEmpty()) continue

                val existing = collectionDao.getByName(name)
                val collectionId = if (existing == null) {
                    collectionDao.createCollection(
                        CollectionEntity(
                            name = name,
                            description = incoming.description?.trim()?.ifBlank { null },
                            icon_key = incoming.iconKey?.trim()?.ifBlank { null },
                            color = incoming.color?.trim()?.ifBlank { null },
                            createdAt = incoming.createdAt,
                            updatedAt = incoming.updatedAt,
                        )
                    )
                } else {
                    existing.id
                }

                var changed = existing == null
                if (existing != null) {
                    val description = mergeText(existing.description, incoming.description)
                    val icon = preferExisting(existing.icon_key, incoming.iconKey)
                    val color = preferExisting(existing.color, incoming.color)
                    if (
                        description != existing.description ||
                        icon != existing.icon_key ||
                        color != existing.color
                    ) {
                        collectionDao.updateCollection(
                            existing.copy(
                                description = description,
                                icon_key = icon,
                                color = color,
                            )
                        )
                        changed = true
                    }
                }

                val items = collectionDao.getItems(collectionId)
                    .associateBy { it.chapterNo to it.verseNo }
                    .toMutableMap()
                var maxSort = collectionDao.maxItemSort(collectionId) ?: -1

                for (verse in incoming.verses) {
                    val key = verse.chapterNo to verse.verseNo
                    val item = items[key]
                    if (item == null) {
                        maxSort += 1
                        val rowId = collectionDao.insertItem(
                            CollectionItemEntity(
                                collectionId = collectionId,
                                chapterNo = verse.chapterNo,
                                verseNo = verse.verseNo,
                                note = verse.note?.trim()?.ifBlank { null },
                                sortIndex = maxSort,
                                addedAt = verse.addedAt,
                            )
                        )
                        if (rowId != -1L) {
                            items[key] = CollectionItemEntity(
                                id = rowId,
                                collectionId = collectionId,
                                chapterNo = verse.chapterNo,
                                verseNo = verse.verseNo,
                                note = verse.note?.trim()?.ifBlank { null },
                                sortIndex = maxSort,
                                addedAt = verse.addedAt,
                            )
                            changed = true
                        }
                    } else {
                        val note = mergeText(item.note, verse.note)
                        if (note != item.note) {
                            val updated = item.copy(note = note)
                            collectionDao.updateItem(updated)
                            items[key] = updated
                            changed = true
                        }
                    }
                }

                if (changed) {
                    collectionDao.touch(collectionId, System.currentTimeMillis())
                }
            }
        }
    }

    private fun preferExisting(local: String?, incoming: String?): String? {
        if (!local.isNullOrBlank()) return local
        return incoming?.trim()?.ifBlank { null }
    }

    private fun mergeText(local: String?, incoming: String?): String? {
        if (local.isNullOrBlank() && incoming.isNullOrBlank()) return null
        if (local.isNullOrBlank()) return incoming!!.trim()
        if (incoming.isNullOrBlank()) return local
        val incomingTrimmed = incoming.trim()
        if (local.contains(incomingTrimmed)) return local
        return local.trimEnd() + "\n\n" + incomingTrimmed
    }


    suspend fun updateItemNote(itemId: Long, note: String?) {
        val item = collectionDao.getItem(itemId) ?: return
        val cleaned = note?.trim()?.ifBlank { null }
        collectionDao.updateItem(item.copy(note = cleaned))
        collectionDao.touch(item.collectionId, System.currentTimeMillis())
    }

    suspend fun removeItem(itemId: Long) {
        val item = collectionDao.getItem(itemId) ?: return
        collectionDao.deleteItemById(itemId)
        collectionDao.touch(item.collectionId, System.currentTimeMillis())
    }

    suspend fun applyVerseMembership(
        chapterNo: Int,
        verseNo: Int,
        selectedIds: Set<Long>,
    ) {
        database.withWriteTransaction {
            val current = collectionDao.collectionIdsForVerse(chapterNo, verseNo).toSet()
            val toAdd = selectedIds - current
            val toRemove = current - selectedIds
            val now = Date()
            val touched = mutableSetOf<Long>()

            for (collectionId in toAdd) {
                val rowId = collectionDao.insertItem(
                    CollectionItemEntity(
                        collectionId = collectionId,
                        chapterNo = chapterNo,
                        verseNo = verseNo,
                        note = null,
                        sortIndex = (collectionDao.maxItemSort(collectionId) ?: -1) + 1,
                        addedAt = now,
                    )
                )
                if (rowId != -1L) touched += collectionId
            }

            for (collectionId in toRemove) {
                collectionDao.deleteItem(collectionId, chapterNo, verseNo)
                touched += collectionId
            }

            val time = System.currentTimeMillis()
            touched.forEach { collectionDao.touch(it, time) }
        }
    }
}
