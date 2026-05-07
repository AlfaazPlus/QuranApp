package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.relations.topics.TopicRelationshipRow
import com.quranapp.android.db.relations.topics.TopicSummaryRow
import com.quranapp.android.repository.TopicVersePreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuranicTopicsTree(
    val routeName: String,
    val parentType: RelationshipType,
) {
    Ontology("ontology", RelationshipType.ONTOLOGY_PARENT),
    Thematic("thematic", RelationshipType.THEMATIC_PARENT);

    companion object {
        fun fromRouteName(value: String?): QuranicTopicsTree =
            entries.firstOrNull { it.routeName == value } ?: Ontology
    }
}

data class QuranicTopicNode(
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

data class QuranicTopicRelationship(
    val type: RelationshipType,
    val topic: QuranicTopicNode,
)

data class QuranicTopicsUiState(
    val isLoadingRoots: Boolean = true,
    val isLoadingTopic: Boolean = false,
    val ontologyRoots: List<QuranicTopicNode> = emptyList(),
    val thematicRoots: List<QuranicTopicNode> = emptyList(),
    val currentTopic: QuranicTopicNode? = null,
    val childTopics: List<QuranicTopicNode> = emptyList(),
    val verseRefs: List<String> = emptyList(),
    val versePreviews: List<TopicVersePreview> = emptyList(),
    val breadcrumbs: List<QuranicTopicNode> = emptyList(),
    val relationships: List<QuranicTopicRelationship> = emptyList(),
    val error: String? = null,
)

class QuranicTopicsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DatabaseProvider.getTopicsRepository(application)

    private val _uiState = MutableStateFlow(QuranicTopicsUiState())
    val uiState: StateFlow<QuranicTopicsUiState> = _uiState.asStateFlow()

    init {
        loadRoots()
    }

    fun loadRoots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoots = true, error = null) }
            runCatching {
                repository.getOntologyRootTopics().toTopicNodes() to
                    repository.getThematicRootTopics().toTopicNodes()
            }.onSuccess { (ontology, thematic) ->
                _uiState.update {
                    it.copy(
                        isLoadingRoots = false,
                        ontologyRoots = ontology,
                        thematicRoots = thematic,
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

    fun loadTopic(topicId: Int, tree: QuranicTopicsTree, breadcrumbIds: List<Int> = emptyList()) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingTopic = true,
                    currentTopic = null,
                    childTopics = emptyList(),
                    verseRefs = emptyList(),
                    versePreviews = emptyList(),
                    breadcrumbs = emptyList(),
                    relationships = emptyList(),
                    error = null,
                )
            }

            runCatching {
                val topic = repository.getTopicById(topicId, tree.parentType)?.toTopicNode()
                val children = topic?.let {
                    repository.getChildTopics(it.id, tree.parentType).toTopicNodes()
                }.orEmpty()
                val breadcrumbs = breadcrumbIds
                    .distinct()
                    .filter { it != topicId }
                    .mapNotNull { repository.getTopicById(it, tree.parentType)?.toTopicNode() }

                val verseRefs = topic?.let { repository.getAllTopicVerseRefs(it.id) }.orEmpty()
                val versePreviews = topic?.let { repository.getTopicVersePreviews(it.id) }.orEmpty()
                val relationships = topic?.let {
                    repository.getTopicRelationships(it.id, tree.parentType).toTopicRelationships()
                }.orEmpty()
                    .distinctBy { it.topic.id }
                    .filterNot { relationship ->
                        relationship.topic.id == topic?.id ||
                            children.any { child -> child.id == relationship.topic.id } ||
                            breadcrumbs.any { breadcrumb -> breadcrumb.id == relationship.topic.id }
                    }

                TopicLoadResult(topic, children, verseRefs, versePreviews, breadcrumbs, relationships)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isLoadingTopic = false,
                        currentTopic = result.topic,
                        childTopics = result.children,
                        verseRefs = result.verseRefs,
                        versePreviews = result.versePreviews,
                        breadcrumbs = result.breadcrumbs,
                        relationships = result.relationships,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingTopic = false,
                        error = throwable.localizedMessage ?: throwable.javaClass.simpleName,
                    )
                }
            }
        }
    }

    private data class TopicLoadResult(
        val topic: QuranicTopicNode?,
        val children: List<QuranicTopicNode>,
        val verseRefs: List<String>,
        val versePreviews: List<TopicVersePreview>,
        val breadcrumbs: List<QuranicTopicNode>,
        val relationships: List<QuranicTopicRelationship>,
    )
}

private fun List<TopicSummaryRow>.toTopicNodes(): List<QuranicTopicNode> =
    map { it.toTopicNode() }

private fun TopicSummaryRow.toTopicNode(): QuranicTopicNode =
    QuranicTopicNode(
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

private fun List<TopicRelationshipRow>.toTopicRelationships(): List<QuranicTopicRelationship> =
    map {
        QuranicTopicRelationship(
            type = it.relationshipType,
            topic = QuranicTopicNode(
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
