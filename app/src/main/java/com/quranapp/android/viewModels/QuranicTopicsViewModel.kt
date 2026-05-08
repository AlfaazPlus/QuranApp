package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.relations.topics.TopicRelationshipRow
import com.quranapp.android.db.relations.topics.TopicSummaryRow
import com.quranapp.android.repository.TopicVersePreview
import com.quranapp.android.repository.TopicSearchHit
import com.quranapp.android.repository.TopicsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_TOPIC_DETAIL_CACHE_ENTRIES = 64

enum class TopicsTree(
    val routeName: String,
    val parentType: RelationshipType,
) {
    Ontology("ontology", RelationshipType.ONTOLOGY_PARENT),
    Thematic("thematic", RelationshipType.THEMATIC_PARENT);

    companion object {
        fun fromRouteName(value: String?): TopicsTree =
            entries.firstOrNull { it.routeName == value } ?: Ontology
    }
}

data class TopicNode(
    val id: Int,
    val slug: String,
    val title: String,
    val type: String,
    val imageUrl: String?,
    val icon: String?,
    val shortDescription: String?,
    val description: String?,
    val verseCount: Int,
    val childCount: Int,
    val relatedCount: Int,
) {
    val isLeaf: Boolean get() = childCount == 0
}

data class TopicRelationship(
    val type: RelationshipType,
    val topic: TopicNode,
)

data class TopicsUiState(
    val isLoadingRoots: Boolean = true,
    val ontologyRoots: List<TopicNode> = emptyList(),
    val thematicRoots: List<TopicNode> = emptyList(),

    val ontologyPrimaryRootCount: Int = 0,
    val ontologySupplementalTotal: Int = 0,
    val ontologySupplementalLoaded: Int = 0,
    val hasMoreOntologySupplemental: Boolean = false,
    val isLoadingMoreOntologySupplemental: Boolean = false,

    val thematicPrimaryRootCount: Int = 0,
    val thematicSupplementalTotal: Int = 0,
    val thematicSupplementalLoaded: Int = 0,
    val hasMoreThematicSupplemental: Boolean = false,
    val isLoadingMoreThematicSupplemental: Boolean = false,

    val topicDetails: Map<String, TopicDetailUiState> = emptyMap(),
    val error: String? = null,
)

data class TopicDetailUiState(
    val isLoading: Boolean = false,
    val topic: TopicNode? = null,
    val childTopics: List<TopicNode> = emptyList(),
    val broaderCatalogChildren: List<TopicNode> = emptyList(),
    val verseRefs: List<String> = emptyList(),
    val versePreviews: List<TopicVersePreview> = emptyList(),
    val breadcrumbs: List<TopicNode> = emptyList(),
    val relationships: List<TopicRelationship> = emptyList(),
    val error: String? = null,
)

private data class TopicLoadResult(
    val topic: TopicNode?,
    val children: List<TopicNode>,
    val broaderCatalogChildren: List<TopicNode>,
    val verseRefs: List<String>,
    val versePreviews: List<TopicVersePreview>,
    val breadcrumbs: List<TopicNode>,
    val relationships: List<TopicRelationship>,
)

private data class RootsLoadResult(
    val ontologyPrimary: List<TopicNode>,
    val ontologySupplementalTotal: Int,
    val ontologySupplementalFirstPage: List<TopicNode>,

    val thematicPrimary: List<TopicNode>,
    val thematicSupplementalTotal: Int,
    val thematicSupplementalFirstPage: List<TopicNode>,
) {
    val ontologySupplementalLoaded: Int get() = ontologySupplementalFirstPage.size
    val thematicSupplementalLoaded: Int get() = thematicSupplementalFirstPage.size
}

class QuranicTopicsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DatabaseProvider.getTopicsRepository(application)
    private val inFlightTopicLoads = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    init {
        loadRoots()
    }

    fun loadRoots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoots = true, error = null) }

            runCatching {
                val assign = repository.warmSupplementalAssignment()

                val ontologyPrimary = repository.getOntologyRootTopics().toTopicNodes()
                val thematicPrimary = repository.getThematicRootTopics().toTopicNodes()

                val pageSize = TopicsRepository.SUPPLEMENTAL_ROOT_PAGE_SIZE

                val ontologyExtra = repository
                    .getSupplementalRootTopicsPage(
                        TopicsTree.Ontology.parentType,
                        offset = 0,
                        limit = pageSize,
                    )
                    .toTopicNodes()

                val thematicExtra = repository
                    .getSupplementalRootTopicsPage(
                        TopicsTree.Thematic.parentType,
                        offset = 0,
                        limit = pageSize,
                    )
                    .toTopicNodes()

                RootsLoadResult(
                    ontologyPrimary = ontologyPrimary,
                    thematicPrimary = thematicPrimary,
                    ontologySupplementalTotal = assign.ontologySupplementalRootIds.size,
                    thematicSupplementalTotal = assign.thematicSupplementalRootIds.size,
                    ontologySupplementalFirstPage = ontologyExtra,
                    thematicSupplementalFirstPage = thematicExtra,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isLoadingRoots = false,
                        ontologyPrimaryRootCount = result.ontologyPrimary.size,
                        thematicPrimaryRootCount = result.thematicPrimary.size,
                        ontologySupplementalTotal = result.ontologySupplementalTotal,
                        thematicSupplementalTotal = result.thematicSupplementalTotal,
                        ontologySupplementalLoaded = result.ontologySupplementalFirstPage.size,
                        thematicSupplementalLoaded = result.thematicSupplementalFirstPage.size,
                        hasMoreOntologySupplemental = result.ontologySupplementalLoaded < result.ontologySupplementalTotal,
                        hasMoreThematicSupplemental = result.thematicSupplementalLoaded < result.thematicSupplementalTotal,
                        ontologyRoots = result.ontologyPrimary + result.ontologySupplementalFirstPage,
                        thematicRoots = result.thematicPrimary + result.thematicSupplementalFirstPage,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingRoots = false,
                        error = throwable.localizedMessage ?: throwable.javaClass.simpleName,
                    )
                }
            }
        }
    }

    fun loadMoreSupplementalRoots(tree: TopicsTree) {
        viewModelScope.launch {
            val snap = _uiState.value
            when (tree) {
                TopicsTree.Ontology -> {
                    if (!snap.hasMoreOntologySupplemental || snap.isLoadingMoreOntologySupplemental) return@launch

                    _uiState.update { it.copy(isLoadingMoreOntologySupplemental = true) }

                    runCatching {
                        val pageSize = TopicsRepository.SUPPLEMENTAL_ROOT_PAGE_SIZE
                        val offset = snap.ontologySupplementalLoaded

                        repository.getSupplementalRootTopicsPage(
                            TopicsTree.Ontology.parentType,
                            offset = offset,
                            limit = pageSize,
                        ).toTopicNodes()
                    }.onSuccess { more ->
                        _uiState.update {
                            val newLoaded = it.ontologySupplementalLoaded + more.size

                            it.copy(
                                ontologyRoots = it.ontologyRoots + more,
                                ontologySupplementalLoaded = newLoaded,
                                hasMoreOntologySupplemental = newLoaded < it.ontologySupplementalTotal,
                                isLoadingMoreOntologySupplemental = false,
                            )
                        }
                    }.onFailure {
                        _uiState.update { it.copy(isLoadingMoreOntologySupplemental = false) }
                    }
                }

                TopicsTree.Thematic -> {
                    if (!snap.hasMoreThematicSupplemental || snap.isLoadingMoreThematicSupplemental) return@launch

                    _uiState.update { it.copy(isLoadingMoreThematicSupplemental = true) }

                    runCatching {
                        val pageSize = TopicsRepository.SUPPLEMENTAL_ROOT_PAGE_SIZE
                        val offset = snap.thematicSupplementalLoaded

                        repository.getSupplementalRootTopicsPage(
                            TopicsTree.Thematic.parentType,
                            offset = offset,
                            limit = pageSize,
                        ).toTopicNodes()
                    }.onSuccess { more ->
                        _uiState.update {
                            val newLoaded = it.thematicSupplementalLoaded + more.size

                            it.copy(
                                thematicRoots = it.thematicRoots + more,
                                thematicSupplementalLoaded = newLoaded,
                                hasMoreThematicSupplemental = newLoaded < it.thematicSupplementalTotal,
                                isLoadingMoreThematicSupplemental = false,
                            )
                        }
                    }.onFailure {
                        _uiState.update { it.copy(isLoadingMoreThematicSupplemental = false) }
                    }
                }
            }
        }
    }

    fun loadTopic(topicId: Int, tree: TopicsTree, breadcrumbIds: List<Int> = emptyList()) {
        val detailKey = buildTopicDetailKey(tree, topicId, breadcrumbIds)

        val cached = _uiState.value.topicDetails[detailKey]

        if (cached?.topic != null) return

        val shouldLoad = synchronized(inFlightTopicLoads) {
            inFlightTopicLoads.add(detailKey)
        }

        if (!shouldLoad) return

        viewModelScope.launch {
            _uiState.update {
                val currentDetails = it.topicDetails[detailKey] ?: TopicDetailUiState()

                val nextDetails = currentDetails.copy(
                    isLoading = true,
                    error = null,
                )

                it.copy(
                    topicDetails = putDetailWithCap(
                        current = it.topicDetails,
                        key = detailKey,
                        value = nextDetails,
                    )
                )
            }

            runCatching {
                repository.warmSupplementalAssignment()

                val topic = repository.getTopicSummaryForExplorer(topicId, tree.parentType)
                    ?.toTopicNode()

                if (topic == null) {
                    TopicLoadResult(
                        topic = null,
                        children = emptyList(),
                        broaderCatalogChildren = emptyList(),
                        verseRefs = emptyList(),
                        versePreviews = emptyList(),
                        breadcrumbs = emptyList(),
                        relationships = emptyList(),
                    )
                } else {
                    coroutineScope {
                        val normalizedBreadcrumbIds = breadcrumbIds
                            .distinct()
                            .filter { it != topicId }

                        val childrenDeferred = async {
                            repository.getChildTopicsRespectingSupplemental(
                                topic.id,
                                tree.parentType
                            ).toTopicNodes()
                        }

                        val breadcrumbsDeferred = async {
                            repository.getTopicSummariesForExplorer(
                                topicIds = normalizedBreadcrumbIds,
                                parentType = tree.parentType,
                            ).toTopicNodes()
                        }

                        val verseRefsDeferred = async {
                            repository.getAllTopicVerseRefs(topic.id)
                        }

                        val versePreviewsDeferred = async {
                            repository.getTopicVersePreviews(topic.id)
                        }

                        val relationshipsDeferred = async {
                            repository.getTopicRelationships(topic.id, tree.parentType)
                                .toTopicRelationships()
                        }

                        val children = childrenDeferred.await()
                        val breadcrumbs = breadcrumbsDeferred.await()

                        val broaderCatalogChildren = repository.getBroaderCatalogChildren(
                            topicId = topic.id,
                            parentType = tree.parentType,
                            excludeTopicIds = buildSet {
                                add(topic.id)
                                addAll(children.map { child -> child.id })
                                addAll(breadcrumbs.map { breadcrumb -> breadcrumb.id })
                            },
                        ).toTopicNodes()

                        val relationships = relationshipsDeferred.await()
                            .distinctBy { it.topic.id }
                            .filterNot { relationship ->
                                relationship.topic.id == topic.id ||
                                        children.any { child -> child.id == relationship.topic.id } ||
                                        broaderCatalogChildren.any { child -> child.id == relationship.topic.id } ||
                                        breadcrumbs.any { breadcrumb -> breadcrumb.id == relationship.topic.id }
                            }

                        TopicLoadResult(
                            topic = topic,
                            children = children,
                            broaderCatalogChildren = broaderCatalogChildren,
                            verseRefs = verseRefsDeferred.await(),
                            versePreviews = versePreviewsDeferred.await(),
                            breadcrumbs = breadcrumbs,
                            relationships = relationships,
                        )
                    }
                }
            }.onSuccess { result ->
                _uiState.update {
                    val nextDetails = TopicDetailUiState(
                        isLoading = false,
                        topic = result.topic,
                        childTopics = result.children,
                        broaderCatalogChildren = result.broaderCatalogChildren,
                        verseRefs = result.verseRefs,
                        versePreviews = result.versePreviews,
                        breadcrumbs = result.breadcrumbs,
                        relationships = result.relationships,
                    )

                    it.copy(
                        topicDetails = putDetailWithCap(
                            current = it.topicDetails,
                            key = detailKey,
                            value = nextDetails,
                        ),
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    val currentDetails = it.topicDetails[detailKey] ?: TopicDetailUiState()

                    val nextDetails = currentDetails.copy(
                        isLoading = false,
                        error = throwable.localizedMessage ?: throwable.javaClass.simpleName,
                    )

                    it.copy(
                        topicDetails = putDetailWithCap(
                            current = it.topicDetails,
                            key = detailKey,
                            value = nextDetails,
                        ),
                    )
                }
            }.also {
                synchronized(inFlightTopicLoads) {
                    inFlightTopicLoads.remove(detailKey)
                }
            }
        }
    }

    suspend fun searchTopicsForTree(
        query: String,
        tree: TopicsTree,
        limit: Int = 60,
    ): List<TopicSearchHit> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()

        val preferred = when (tree) {
            TopicsTree.Ontology -> RelationshipType.ONTOLOGY_PARENT
            TopicsTree.Thematic -> RelationshipType.THEMATIC_PARENT
        }

        val hits = repository.searchTopicHits(
            query = normalized,
            limit = limit,
        )

        val matchingTree = hits.filter { it.preferredTree == preferred }

        return if (matchingTree.isNotEmpty()) {
            matchingTree
        } else {
            hits
        }
    }
}

private fun List<TopicSummaryRow>.toTopicNodes(): List<TopicNode> =
    map { it.toTopicNode() }

private fun TopicSummaryRow.toTopicNode(): TopicNode =
    TopicNode(
        id = topicId,
        slug = slug.orEmpty(),
        title = title,
        type = type,
        imageUrl = imageUrl,
        icon = icon,
        shortDescription = shortDescription,
        description = description,
        verseCount = ayahCount,
        childCount = childCount,
        relatedCount = relatedCount,
    )

private fun List<TopicRelationshipRow>.toTopicRelationships(): List<TopicRelationship> =
    map {
        TopicRelationship(
            type = it.relationshipType,
            topic = TopicNode(
                id = it.topicId,
                slug = it.slug.orEmpty(),
                title = it.title,
                type = it.type,
                imageUrl = it.imageUrl,
                icon = it.icon,
                shortDescription = it.shortDescription,
                description = it.description,
                verseCount = it.ayahCount,
                childCount = it.childCount,
                relatedCount = it.relatedCount,
            ),
        )
    }

private fun putDetailWithCap(
    current: Map<String, TopicDetailUiState>,
    key: String,
    value: TopicDetailUiState,
): Map<String, TopicDetailUiState> {
    val mutable = LinkedHashMap(current)

    mutable.remove(key)
    mutable[key] = value

    while (mutable.size > MAX_TOPIC_DETAIL_CACHE_ENTRIES) {
        val oldestKey = mutable.keys.firstOrNull() ?: break

        mutable.remove(oldestKey)
    }

    return mutable
}

fun buildTopicDetailKey(
    tree: TopicsTree,
    topicId: Int,
    breadcrumbIds: List<Int>,
): String {
    val normalizedTrail = breadcrumbIds
        .distinct()
        .filter { it != topicId }
        .joinToString(",")

    return "${tree.routeName}|$topicId|$normalizedTrail"
}
