package com.quranapp.android.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room3.withWriteTransaction
import com.quranapp.android.backup.BackupBookmark
import com.quranapp.android.backup.BackupCollection
import com.quranapp.android.backup.BackupCollectionItem
import com.quranapp.android.backup.BookmarkChange
import com.quranapp.android.backup.MergeCounts
import com.quranapp.android.backup.bookmarkChange
import com.quranapp.android.components.bookmark.CollectionExportModel
import com.quranapp.android.db.UserDatabase
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.db.entities.user.CollectionWithCount
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import com.quranapp.android.db.epochNow
import com.quranapp.android.db.newStableId
import com.quranapp.android.utils.miscellenous.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking
import java.time.Instant

class UserRepository(
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
    ): BookmarkMessage {
        if (isBookmarked(chapterNo, verseRange)) {
            return BookmarkMessage.AlreadyAdded
        }

        val now = epochNow()
        val entity = BookmarkEntity(
            chapterNo = chapterNo,
            fromVerseNo = verseRange.first,
            toVerseNo = verseRange.last,
            note = note,
            dateTime = now,
            updatedAt = now,
        )

        val rowId = bookmarkDao.insert(entity)
        return if (rowId != -1L) BookmarkMessage.Added else BookmarkMessage.AddFailed
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
        bookmarkDao.updateBookmark(existing.copy(note = note, updatedAt = epochNow()))
    }

    suspend fun removeFromBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): BookmarkMessage {
        val rowsAffected = bookmarkDao.removeBookmark(chapterNo, fromVerse, toVerse)
        return if (rowsAffected >= 1) BookmarkMessage.Removed else BookmarkMessage.RemoveFailed
    }

    fun removeFromBookmarkBlocking(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): BookmarkMessage = runBlocking {
        removeFromBookmark(chapterNo, fromVerse, toVerse)
    }

    suspend fun removeBookmarksBulk(
        ids: LongArray,
    ): BookmarkMessage {
        val rowsAffected = bookmarkDao.removeBookmarksBulk(ids.toList())
        return if (rowsAffected >= 1) BookmarkMessage.Removed else BookmarkMessage.RemoveFailed
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
        val nonMainEntity = if (entity.isMain) entity.copy(isMain = false) else entity
        val latest = readHistoryDao.getLatestNonMain()
        if (latest != null && latest.pageNo != null && latest.hasSameMushafPage(nonMainEntity)) {
            readHistoryDao.updateVersePosition(
                id = latest.id,
                fromVerseNo = nonMainEntity.fromVerseNo,
                toVerseNo = nonMainEntity.toVerseNo,
                datetime = nonMainEntity.datetime,
            )
            return latest.id
        }

        readHistoryDao.deleteDuplicate(
            readType = nonMainEntity.readType,
            readerMode = nonMainEntity.readerMode,
            divisionNo = nonMainEntity.divisionNo,
            chapterNo = nonMainEntity.chapterNo,
            fromVerseNo = nonMainEntity.fromVerseNo,
            toVerseNo = nonMainEntity.toVerseNo,
        )

        val id = readHistoryDao.insert(nonMainEntity)
        readHistoryDao.trimToSize(HISTORY_LIMIT)

        return id
    }

    suspend fun updateMainHistory(entity: ReadHistoryEntity): Boolean {
        return readHistoryDao.updateMainSession(
            id = entity.id,
            readType = entity.readType,
            readerMode = entity.readerMode,
            divisionNo = entity.divisionNo,
            chapterNo = entity.chapterNo,
            fromVerseNo = entity.fromVerseNo,
            toVerseNo = entity.toVerseNo,
            mushafCode = entity.mushafCode,
            mushafVariant = entity.mushafVariant,
            pageNo = entity.pageNo,
            datetime = entity.datetime,
        ) > 0
    }

    suspend fun setMainHistory(id: Long) {
        database.withWriteTransaction {
            readHistoryDao.clearMain()
            readHistoryDao.setMain(id)
        }
    }

    fun getMainHistoryFlow(): Flow<ReadHistoryEntity?> {
        return readHistoryDao.getMainFlow()
    }

    private fun ReadHistoryEntity.hasSameMushafPage(other: ReadHistoryEntity): Boolean =
        readType == other.readType &&
                readerMode == other.readerMode &&
                divisionNo == other.divisionNo &&
                chapterNo == other.chapterNo &&
                mushafCode == other.mushafCode &&
                mushafVariant == other.mushafVariant &&
                pageNo == other.pageNo

    fun getHistoriesFlow(limit: Int): Flow<List<ReadHistoryEntity>> {
        return readHistoryDao.getFlow(limit)
    }

    fun getHistoriesPaginated(): Flow<PagingData<ReadHistoryEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { readHistoryDao.getNonMainPaginated() }
        ).flow
    }

    suspend fun deleteHistory(id: Long) {
        readHistoryDao.deleteById(id)
    }

    suspend fun deleteAllHistories() {
        readHistoryDao.deleteAllNonMain()
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

    suspend fun getCollectionsForBackup(): List<BackupCollection> {
        return collectionDao.getAll().map { collection ->
            val items = collectionDao.getItems(collection.id)
            BackupCollection(
                id = collection.stableId,
                name = collection.name,
                description = collection.description,
                iconKey = collection.icon_key,
                color = collection.color,
                sortIndex = collection.sortIndex,
                createdAt = Instant.ofEpochMilli(collection.createdAt),
                updatedAt = Instant.ofEpochMilli(collection.updatedAt),
                timestampsTrusted = true,
                items = items.mapIndexed { index, item ->
                    BackupCollectionItem(
                        chapterNumber = item.chapterNo,
                        verseNumber = item.verseNo,
                        note = item.note,
                        position = index,
                        addedAt = Instant.ofEpochMilli(item.addedAt),
                    )
                },
            )
        }
    }

    suspend fun applyBackup(
        bookmarks: List<BackupBookmark>?,
        collections: List<BackupCollection>?,
    ): MergeCounts {
        return database.withWriteTransaction {
            val bookmarkCounts = bookmarks?.let { mergeBookmarks(it) } ?: MergeCounts()
            val collectionCounts = collections?.let { mergeCollections(it) } ?: MergeCounts()
            bookmarkCounts + collectionCounts
        }
    }

    private suspend fun mergeBookmarks(bookmarks: List<BackupBookmark>): MergeCounts {
        var counts = MergeCounts()
        for (incoming in bookmarks) {
            if (
                incoming.chapterNumber < 1 ||
                incoming.startVerseNumber < 1 ||
                incoming.endVerseNumber < incoming.startVerseNumber
            ) {
                counts = counts.copy(skipped = counts.skipped + 1)
                continue
            }
            val note = cleanNote(incoming.note)
            val byId =
                incoming.id?.takeIf { it.isNotBlank() }?.let { bookmarkDao.getByStableId(it) }
            val byRange = bookmarkDao.getBookmark(
                incoming.chapterNumber,
                incoming.startVerseNumber,
                incoming.endVerseNumber,
            )
            val local = byId ?: byRange
            if (local == null) {
                bookmarkDao.insertNew(
                    BookmarkEntity(
                        chapterNo = incoming.chapterNumber,
                        fromVerseNo = incoming.startVerseNumber,
                        toVerseNo = incoming.endVerseNumber,
                        note = note,
                        dateTime = incoming.createdAt?.toEpochMilli()
                            ?: legacyDate(incoming.legacyCreatedAtText),
                        stableId = incoming.id?.takeIf { it.isNotBlank() } ?: newStableId(),
                        updatedAt = incoming.updatedAt?.toEpochMilli(),
                        legacyCreatedAtText = incoming.legacyCreatedAtText,
                    )
                )
                counts = counts.copy(inserted = counts.inserted + 1)
                continue
            }
            val same = local.chapterNo == incoming.chapterNumber &&
                    local.fromVerseNo == incoming.startVerseNumber &&
                    local.toVerseNo == incoming.endVerseNumber &&
                    cleanNote(local.note) == note
            val rangeOwner = byRange
            val blocked = rangeOwner != null && rangeOwner.id != local.id
            val change = if (blocked) {
                BookmarkChange.CONFLICT
            } else {
                bookmarkChange(
                    localUpdatedAt = local.updatedAt?.let { Instant.ofEpochMilli(it) },
                    incomingUpdatedAt = incoming.updatedAt,
                    sameContent = same,
                )
            }
            when (change) {
                BookmarkChange.UNCHANGED -> counts = counts.copy(unchanged = counts.unchanged + 1)
                BookmarkChange.CONFLICT -> counts = counts.copy(conflicts = counts.conflicts + 1)
                BookmarkChange.UPDATE -> {
                    bookmarkDao.updateBookmark(
                        local.copy(
                            chapterNo = incoming.chapterNumber,
                            fromVerseNo = incoming.startVerseNumber,
                            toVerseNo = incoming.endVerseNumber,
                            note = note,
                            updatedAt = incoming.updatedAt?.toEpochMilli(),
                            legacyCreatedAtText = incoming.legacyCreatedAtText
                                ?: local.legacyCreatedAtText,
                        )
                    )
                    counts = counts.copy(updated = counts.updated + 1)
                }
            }
        }
        return counts
    }

    private suspend fun mergeCollections(collections: List<BackupCollection>): MergeCounts {
        var counts = MergeCounts()
        for (incoming in collections) {
            val name = incoming.name.trim()
            if (name.isEmpty()) {
                counts = counts.copy(skipped = counts.skipped + 1)
                continue
            }
            val byId =
                incoming.id?.takeIf { it.isNotBlank() }?.let { collectionDao.getByStableId(it) }
            val local = byId ?: collectionDao.getByName(name)
            if (local == null) {
                val createdAt = incoming.createdAt?.toEpochMilli() ?: epochNow()
                val collectionId = collectionDao.createCollection(
                    CollectionEntity(
                        name = name,
                        description = cleanNote(incoming.description),
                        icon_key = cleanNote(incoming.iconKey),
                        color = cleanNote(incoming.color),
                        sortIndex = incoming.sortIndex,
                        createdAt = createdAt,
                        updatedAt = incoming.updatedAt?.toEpochMilli() ?: createdAt,
                        stableId = incoming.id?.takeIf { it.isNotBlank() } ?: newStableId(),
                    )
                )
                insertCollectionItems(collectionId, incoming.items)
                counts = counts.copy(inserted = counts.inserted + 1)
                continue
            }

            val incomingMillis = incoming.updatedAt?.toEpochMilli()
            val localMillis = local.updatedAt
            val newer =
                incoming.timestampsTrusted && incomingMillis != null && incomingMillis > localMillis
            val older =
                incoming.timestampsTrusted && incomingMillis != null && incomingMillis < localMillis
            val sameTime =
                incoming.timestampsTrusted && incomingMillis != null && incomingMillis == localMillis
            var changed = false
            var conflict = false
            if (newer) {
                val updated = local.copy(
                    name = name,
                    description = cleanNote(incoming.description),
                    icon_key = cleanNote(incoming.iconKey),
                    color = cleanNote(incoming.color),
                    sortIndex = incoming.sortIndex ?: local.sortIndex,
                    updatedAt = checkNotNull(incomingMillis),
                )
                if (updated != local) {
                    collectionDao.updateCollection(updated)
                    changed = true
                }
            } else if (!older && !sameTime) {
                val description = mergeText(local.description, incoming.description)
                val icon = preferExisting(local.icon_key, incoming.iconKey)
                val color = preferExisting(local.color, incoming.color)
                if (description != local.description || icon != local.icon_key || color != local.color) {
                    collectionDao.updateCollection(
                        local.copy(description = description, icon_key = icon, color = color)
                    )
                    changed = true
                }
            } else if (sameTime && collectionContentDiffers(local, incoming)) {
                conflict = true
            }

            val itemResult = mergeCollectionItems(
                collectionId = local.id,
                items = incoming.items,
                replaceNotes = newer,
                mergeNotes = !incoming.timestampsTrusted,
            )
            changed = changed || itemResult.changed
            conflict = conflict || itemResult.conflicts > 0
            if (changed && !newer) {
                collectionDao.touch(local.id, System.currentTimeMillis())
            }
            counts = when {
                conflict && !changed -> counts.copy(conflicts = counts.conflicts + 1)
                changed && conflict -> counts.copy(
                    updated = counts.updated + 1,
                    conflicts = counts.conflicts + 1,
                )

                changed -> counts.copy(updated = counts.updated + 1)
                conflict -> counts.copy(conflicts = counts.conflicts + 1)
                else -> counts.copy(unchanged = counts.unchanged + 1)
            }
        }
        return counts
    }

    private suspend fun insertCollectionItems(
        collectionId: Long,
        items: List<BackupCollectionItem>,
    ) {
        items.forEachIndexed { index, item ->
            collectionDao.insertItem(
                CollectionItemEntity(
                    collectionId = collectionId,
                    chapterNo = item.chapterNumber,
                    verseNo = item.verseNumber,
                    note = cleanNote(item.note),
                    sortIndex = item.position.takeIf { it >= 0 } ?: index,
                    addedAt = item.addedAt?.toEpochMilli() ?: epochNow(),
                )
            )
        }
    }

    private suspend fun mergeCollectionItems(
        collectionId: Long,
        items: List<BackupCollectionItem>,
        replaceNotes: Boolean,
        mergeNotes: Boolean,
    ): ItemMerge {
        val existing =
            collectionDao.getItems(collectionId).associateBy { it.chapterNo to it.verseNo }
                .toMutableMap()
        var maxSort = collectionDao.maxItemSort(collectionId) ?: -1
        var changed = false
        var conflicts = 0
        for (item in items) {
            val key = item.chapterNumber to item.verseNumber
            val local = existing[key]
            val note = cleanNote(item.note)
            if (local == null) {
                maxSort += 1
                val rowId = collectionDao.insertItem(
                    CollectionItemEntity(
                        collectionId = collectionId,
                        chapterNo = item.chapterNumber,
                        verseNo = item.verseNumber,
                        note = note,
                        sortIndex = maxSort,
                        addedAt = item.addedAt?.toEpochMilli() ?: epochNow(),
                    )
                )
                if (rowId != -1L) changed = true
            } else if (replaceNotes && cleanNote(local.note) != note) {
                collectionDao.updateItem(local.copy(note = note))
                changed = true
            } else if (mergeNotes) {
                val merged = mergeText(local.note, note)
                if (merged != local.note) {
                    collectionDao.updateItem(local.copy(note = merged))
                    changed = true
                }
            } else if (cleanNote(local.note) != note && note != null) {
                conflicts += 1
            }
        }
        return ItemMerge(changed, conflicts)
    }

    private fun collectionContentDiffers(
        local: CollectionEntity,
        incoming: BackupCollection
    ): Boolean {
        return local.name != incoming.name.trim() ||
                cleanNote(local.description) != cleanNote(incoming.description) ||
                cleanNote(local.icon_key) != cleanNote(incoming.iconKey) ||
                cleanNote(local.color) != cleanNote(incoming.color)
    }

    private fun legacyDate(value: String?): Long {
        if (value.isNullOrBlank()) return epochNow()
        return DateUtils.toDate(value, DateUtils.DATETIME_FORMAT_SYSTEM)?.time ?: epochNow()
    }

    private fun cleanNote(value: String?) = value?.trim()?.ifBlank { null }

    private data class ItemMerge(val changed: Boolean, val conflicts: Int)

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
            val now = epochNow()
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
