package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.db.entities.user.CollectionWithCount
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = DatabaseProvider.getUserRepository(application)
    private val collectionItemFlows = mutableMapOf<Long, Flow<PagingData<CollectionItemEntity>>>()


    val allCollections = collections("")

    fun collections(query: String): Flow<PagingData<CollectionWithCount>> = repo.getCollectionsPaginated(query)

    val recentCollections = repo.getCollectionsFlow(4)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    fun observeCollection(id: Long) = repo.observeCollection(id)

    fun collectionItems(id: Long) = collectionItemFlows.getOrPut(id) {
        repo.getCollectionItems(id).cachedIn(viewModelScope)
    }

    fun addCollection(collection: CollectionEntity) {
        viewModelScope.launch { repo.addCollection(collection) }
    }

    fun updateCollection(collection: CollectionEntity) {
        viewModelScope.launch { repo.updateCollection(collection) }
    }

    suspend fun deleteCollection(id: Long) {
        withContext(NonCancellable) { repo.deleteCollection(id) }
    }

    fun updateItemNote(itemId: Long, note: String?) {
        viewModelScope.launch { repo.updateItemNote(itemId, note) }
    }

    fun removeItem(itemId: Long) {
        viewModelScope.launch { repo.removeItem(itemId) }
    }
}
