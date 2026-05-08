package com.quranapp.android.repository

import android.content.Context
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.TopicsDatabase
import com.quranapp.android.db.dao.TopicsDao
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.entities.topics.TopicFlags
import com.quranapp.android.db.relations.topics.TopicRelationshipRow
import com.quranapp.android.db.relations.topics.TopicSearchCandidateRow
import com.quranapp.android.db.relations.topics.TopicSummaryRow
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


data class TopicVersePreview(
    val chapterNo: Int,
    val verseNo: Int,
    val translation: String,
)

data class TopicSearchHit(
    val topicId: Int,
    val slug: String?,
    val title: String,
    val shortDescription: String?,
    val description: String?,
    val ayahCount: Int,
    val pathLabel: String,
    val breadcrumbIds: List<Int>,
    val preferredTree: RelationshipType,
    val score: Int,
)

/**
 * Topics not in ontology/thematic trees, assigned for display via parent-graph heuristics.
 */
data class SupplementalTopicAssignment(
    val ontologyAssigned: Set<Int>,
    val thematicAssigned: Set<Int>,
    val ontologySupplementalRootIds: List<Int>,
    val thematicSupplementalRootIds: List<Int>,
    val parentToChildren: Map<Int, List<Int>>,
    val childToParents: Map<Int, List<Int>>,
)

private class IntDisjointSet {
    private val parent = mutableMapOf<Int, Int>()

    private fun ensure(x: Int) {
        if (x !in parent) parent[x] = x
    }

    fun find(x: Int): Int {
        ensure(x)
        val p = parent.getValue(x)
        if (p != x) {
            parent[x] = find(p)
        }
        return parent.getValue(x)
    }

    fun union(a: Int, b: Int) {
        val ra = find(a)
        val rb = find(b)
        if (ra != rb) parent[ra] = rb
    }
}

private data class TopicHierarchyGraph(
    val parentsByChild: Map<Int, List<Pair<Int, RelationshipType>>>,
)

class TopicsRepository(
    private val context: Context,
    private val database: TopicsDatabase,
) {
    private val topicsDao: TopicsDao get() = database.topicsDao()

    @Volatile
    private var supplementalAssignment: SupplementalTopicAssignment? = null

    @Volatile
    private var hierarchyGraph: TopicHierarchyGraph? = null

    private val supplementalBuildMutex = Mutex()
    private val hierarchyGraphMutex = Mutex()

    companion object {
        const val SUPPLEMENTAL_ROOT_PAGE_SIZE: Int = 40
        private const val TOPIC_IDS_QUERY_CHUNK: Int = 450
        private const val TOPIC_SEARCH_CANDIDATE_LIMIT: Int = 140
        private const val TOPIC_SEARCH_MAX_PATH_DEPTH: Int = 8
    }

    private fun preferredLanguageCode(): String {
        // For now, only English is supported
        return "en"
    }

    suspend fun getOntologyRootTopics(): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        topicsDao.getRootTopics(
            flagMask = TopicFlags.ONTOLOGY.dbValue,
            parentType = RelationshipType.ONTOLOGY_PARENT,
            langCode = preferredLanguageCode(),
        )
    }

    suspend fun getThematicRootTopics(): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        topicsDao.getRootTopics(
            flagMask = TopicFlags.THEMATIC.dbValue,
            parentType = RelationshipType.THEMATIC_PARENT,
            langCode = preferredLanguageCode(),
        )
    }

    /**
     * Ensures supplemental assignment is built (used for hidden topics).
     * Safe to call multiple times.
     */
    suspend fun warmSupplementalAssignment(): SupplementalTopicAssignment =
        withContext(Dispatchers.IO) {
            getOrBuildSupplementalAssignmentLocked()
        }

    suspend fun getSupplementalRootTopicsPage(
        parentType: RelationshipType,
        offset: Int,
        limit: Int,
    ): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        val plan = getOrBuildSupplementalAssignmentLocked()

        val ids = when (parentType) {
            RelationshipType.ONTOLOGY_PARENT -> plan.ontologySupplementalRootIds
            RelationshipType.THEMATIC_PARENT -> plan.thematicSupplementalRootIds
            else -> emptyList()
        }

        val slice = ids.drop(offset.coerceAtLeast(0)).take(limit.coerceAtLeast(0))

        if (slice.isEmpty()) return@withContext emptyList()

        val rows = getTopicSummariesByIdsBatched(slice, parentType)
        val byId = rows.associateBy { it.topicId }
        val assigned = assignedSetFor(parentType, plan)

        slice.mapNotNull { byId[it] }.map { row ->
            val cnt =
                plan.parentToChildren[row.topicId]?.count { childId -> childId in assigned } ?: 0
            row.copy(childCount = cnt)
        }
    }

    suspend fun getTopicSummaryForExplorer(
        topicId: Int,
        parentType: RelationshipType,
    ): TopicSummaryRow? = withContext(Dispatchers.IO) {
        val summaries = getTopicSummariesForExplorer(listOf(topicId), parentType)

        summaries.firstOrNull()
    }

    suspend fun getTopicSummariesForExplorer(
        topicIds: List<Int>,
        parentType: RelationshipType,
    ): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        val distinct = topicIds.distinct()

        if (distinct.isEmpty()) return@withContext emptyList()

        val plan = getOrBuildSupplementalAssignmentLocked()
        val assigned = assignedSetFor(parentType, plan)
        val rows = getTopicSummariesByIdsBatched(distinct, parentType)
        val rowsById = rows.associateBy { it.topicId }

        distinct.mapNotNull { rowsById[it] }.map { base ->
            if (base.topicId !in assigned) {
                base
            } else {
                val cnt =
                    plan.parentToChildren[base.topicId]?.count { childId -> childId in assigned }
                        ?: 0

                base.copy(childCount = cnt)
            }
        }
    }

    suspend fun getTopicSummaryById(
        topicId: Int,
        parentType: RelationshipType,
    ): TopicSummaryRow? = withContext(Dispatchers.IO) {
        val lang = preferredLanguageCode()
        val base = topicsDao.getTopicById(topicId, parentType, lang) ?: return@withContext null

        val plan = getOrBuildSupplementalAssignmentLocked()
        val assigned = assignedSetFor(parentType, plan)

        if (topicId !in assigned) return@withContext base

        val cnt = plan.parentToChildren[topicId]?.count { childId -> childId in assigned } ?: 0

        base.copy(childCount = cnt)
    }

    suspend fun getChildTopicsRespectingSupplemental(
        topicId: Int,
        parentType: RelationshipType,
    ): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        val lang = preferredLanguageCode()
        val primary = topicsDao.getChildTopics(topicId, parentType, lang)

        if (primary.isNotEmpty()) return@withContext primary

        val plan = getOrBuildSupplementalAssignmentLocked()
        val assigned = assignedSetFor(parentType, plan)

        if (topicId !in assigned) return@withContext emptyList()
        val childIds = plan.parentToChildren[topicId].orEmpty().filter { it in assigned }

        if (childIds.isEmpty()) return@withContext emptyList()

        getTopicSummariesByIdsBatched(childIds, parentType)
            .map { row ->
                val cnt = plan.parentToChildren[row.topicId]?.count { cid -> cid in assigned } ?: 0

                row.copy(childCount = cnt)
            }
            .sortedBy { it.title.lowercase() }
    }

    suspend fun getBroaderCatalogChildren(
        topicId: Int,
        parentType: RelationshipType,
        excludeTopicIds: Set<Int> = emptySet(),
    ): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        val plan = getOrBuildSupplementalAssignmentLocked()
        val assigned = assignedSetFor(parentType, plan)

        if (assigned.isEmpty()) return@withContext emptyList()

        val broaderChildIds = plan.parentToChildren[topicId]
            .orEmpty()
            .filter { childId -> childId in assigned && childId !in excludeTopicIds }

        if (broaderChildIds.isEmpty()) return@withContext emptyList()

        getTopicSummariesByIdsBatched(broaderChildIds, parentType)
            .map { row ->
                val cnt = plan.parentToChildren[row.topicId]
                    ?.count { cid -> cid in assigned }
                    ?: 0

                row.copy(childCount = cnt)
            }
            .sortedBy { it.title.lowercase() }
    }

    suspend fun getTopicById(
        topicId: Int,
        tree: RelationshipType,
    ): TopicSummaryRow? = withContext(Dispatchers.IO) {
        topicsDao.getTopicById(topicId, tree, preferredLanguageCode())
    }

    suspend fun getTopicBySlug(
        slug: String,
        tree: RelationshipType,
    ): TopicSummaryRow? = withContext(Dispatchers.IO) {
        topicsDao.getTopicBySlug(slug, tree, preferredLanguageCode())
    }

    suspend fun getChildTopics(
        topicId: Int,
        tree: RelationshipType,
    ): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        topicsDao.getChildTopics(topicId, tree, preferredLanguageCode())
    }

    suspend fun getTopicVerseRefs(topicId: Int, limit: Int = 8): List<String> =
        withContext(Dispatchers.IO) {
            topicsDao.getTopicVerses(topicId, limit)
                .toVerseRefs()
        }

    suspend fun getAllTopicVerseRefs(topicId: Int): List<String> =
        withContext(Dispatchers.IO) {
            topicsDao.getAllTopicVerses(topicId).toVerseRefs()
        }

    suspend fun getTopicVersePreviews(topicId: Int, limit: Int = 5): List<TopicVersePreview> =
        withContext(Dispatchers.IO) {
            val rows = topicsDao.getTopicVerses(topicId, limit)
            if (rows.isEmpty()) return@withContext emptyList()

            val primarySlug = ReaderPreferences.primaryTranslationSlug()

            QuranTranslationFactory(context.applicationContext).use { factory ->
                rows.map { row ->
                    val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(row.ayahId)
                    TopicVersePreview(
                        chapterNo = chapterNo,
                        verseNo = verseNo,
                        translation = factory
                            .getTranslationsSingleSlugVerse(primarySlug, chapterNo, verseNo)
                            ?.text
                            .orEmpty(),
                    )
                }
            }
        }

    suspend fun getTopicRelationships(
        topicId: Int,
        tree: RelationshipType,
        limit: Int = 8,
    ): List<TopicRelationshipRow> = withContext(Dispatchers.IO) {
        topicsDao.getTopicRelationships(
            topicId = topicId,
            parentType = tree,
            langCode = preferredLanguageCode(),
            limit = limit,
        )
    }

    suspend fun searchTopicHits(
        query: String,
        limit: Int = 30,
    ): List<TopicSearchHit> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty() || limit <= 0) return@withContext emptyList()

        val candidates = topicsDao.searchTopicCandidates(
            query = normalizedQuery,
            langCode = preferredLanguageCode(),
            limit = TOPIC_SEARCH_CANDIDATE_LIMIT,
        )
        if (candidates.isEmpty()) return@withContext emptyList()

        val graph = getOrBuildHierarchyGraphLocked()

        val trailsByTopic = candidates.associate { candidate ->
            candidate.topicId to findBestAncestorTrail(
                topicId = candidate.topicId,
                graph = graph,
                maxDepth = TOPIC_SEARCH_MAX_PATH_DEPTH,
            )
        }

        val allTopicIdsNeeded = buildSet {
            candidates.forEach { add(it.topicId) }
            trailsByTopic.values.forEach { trail -> addAll(trail) }
        }

        val topicTitles = getTopicSummariesByIdsBatched(
            topicIds = allTopicIdsNeeded.toList(),
            parentType = RelationshipType.ONTOLOGY_PARENT,
        ).associate { it.topicId to it.title }

        candidates
            .asSequence()
            .map { candidate ->
                val ancestorTrail = trailsByTopic[candidate.topicId].orEmpty()
                val fullPathIds = ancestorTrail + candidate.topicId
                val pathText = fullPathIds
                    .mapNotNull { topicTitles[it] }
                    .joinToString(" » ")

                val preferredTree = inferPreferredTree(candidate, ancestorTrail, graph)
                val score = scoreTopicSearchCandidate(candidate, normalizedQuery, pathText)

                TopicSearchHit(
                    topicId = candidate.topicId,
                    slug = candidate.slug,
                    title = candidate.title,
                    shortDescription = candidate.shortDescription,
                    description = candidate.description,
                    ayahCount = candidate.ayahCount,
                    pathLabel = pathText.ifBlank { candidate.title },
                    breadcrumbIds = ancestorTrail,
                    preferredTree = preferredTree,
                    score = score,
                )
            }
            .sortedWith(
                compareByDescending<TopicSearchHit> { it.score }
                    .thenByDescending { it.ayahCount }
                    .thenBy { it.title.lowercase() }
            )
            .take(limit)
            .toList()
    }

    private suspend fun getOrBuildHierarchyGraphLocked(): TopicHierarchyGraph {
        hierarchyGraph?.let { return it }

        return hierarchyGraphMutex.withLock {
            hierarchyGraph?.let { return@withLock it }

            val edges = topicsDao.getAllHierarchyEdges()
            val parentsByChild = edges
                .groupBy { it.childTopicId }
                .mapValues { (_, rows) ->
                    rows
                        .map { row -> row.parentTopicId to row.relationshipType }
                        .distinctBy { it.first to it.second }
                        .sortedWith(
                            compareBy<Pair<Int, RelationshipType>> { relationshipOrder(it.second) }
                                .thenBy { it.first }
                        )
                }

            TopicHierarchyGraph(
                parentsByChild = parentsByChild,
            ).also { hierarchyGraph = it }
        }
    }

    private fun findBestAncestorTrail(
        topicId: Int,
        graph: TopicHierarchyGraph,
        maxDepth: Int,
    ): List<Int> {
        val paths = mutableListOf<List<Pair<Int, RelationshipType>>>()

        fun dfs(
            nodeId: Int,
            depth: Int,
            visited: MutableSet<Int>,
            path: MutableList<Pair<Int, RelationshipType>>,
        ) {
            if (depth >= maxDepth) {
                paths += path.toList()
                return
            }

            val parents = graph.parentsByChild[nodeId].orEmpty()
                .filter { (parentId, _) -> parentId !in visited }
            if (parents.isEmpty()) {
                paths += path.toList()
                return
            }

            parents.forEach { (parentId, relType) ->
                visited += parentId
                path += parentId to relType
                dfs(
                    nodeId = parentId,
                    depth = depth + 1,
                    visited = visited,
                    path = path,
                )
                path.removeAt(path.lastIndex)
                visited.remove(parentId)
            }
        }

        dfs(
            nodeId = topicId,
            depth = 0,
            visited = mutableSetOf(topicId),
            path = mutableListOf(),
        )

        val best = paths.maxWithOrNull(
            compareBy<List<Pair<Int, RelationshipType>>> { relationStrength(it) }
                .thenBy { it.size }
        ).orEmpty()

        return best.map { it.first }.reversed()
    }

    private fun inferPreferredTree(
        candidate: TopicSearchCandidateRow,
        ancestorTrail: List<Int>,
        graph: TopicHierarchyGraph,
    ): RelationshipType {
        val path = ancestorTrail + candidate.topicId
        val edgeTypes = path.windowed(size = 2, step = 1, partialWindows = false)
            .mapNotNull { (parentId, childId) ->
                graph.parentsByChild[childId]
                    ?.firstOrNull { (pId, _) -> pId == parentId }
                    ?.second
            }

        return when {
            edgeTypes.any { it == RelationshipType.ONTOLOGY_PARENT } -> RelationshipType.ONTOLOGY_PARENT
            edgeTypes.any { it == RelationshipType.THEMATIC_PARENT } -> RelationshipType.THEMATIC_PARENT
            candidate.flags?.isOntology == true -> RelationshipType.ONTOLOGY_PARENT
            candidate.flags?.isThematic == true -> RelationshipType.THEMATIC_PARENT
            else -> RelationshipType.ONTOLOGY_PARENT
        }
    }

    private fun scoreTopicSearchCandidate(
        candidate: TopicSearchCandidateRow,
        query: String,
        pathText: String,
    ): Int {
        val normalizedQuery = normalizeSearchText(query)
        val normalizedTitle = normalizeSearchText(candidate.title)
        val normalizedShort = normalizeSearchText(candidate.shortDescription.orEmpty())
        val normalizedDescription = normalizeSearchText(candidate.description.orEmpty())
        val normalizedPath = normalizeSearchText(pathText)

        val base = when {
            normalizedTitle == normalizedQuery -> 1000
            normalizedTitle.startsWith(normalizedQuery) -> 900
            Regex("\\b${Regex.escape(normalizedQuery)}\\b").containsMatchIn(normalizedTitle) -> 820
            normalizedTitle.contains(normalizedQuery) -> 760
            normalizedShort.contains(normalizedQuery) -> 540
            normalizedDescription.contains(normalizedQuery) -> 380
            else -> 120
        }

        val pathBonus = when {
            normalizedPath.startsWith(normalizedQuery) -> 55
            normalizedPath.contains(normalizedQuery) -> 35
            else -> 0
        }

        val positionBonus = normalizedTitle.indexOf(normalizedQuery)
            .takeIf { it >= 0 }
            ?.let { 40 - it.coerceAtMost(40) }
            ?: 0

        val lengthPenalty = (normalizedTitle.length - normalizedQuery.length)
            .coerceAtLeast(0)
            .coerceAtMost(80)

        val verseBonus = (candidate.ayahCount * 2).coerceAtMost(50)

        return base + pathBonus + positionBonus + verseBonus - lengthPenalty
    }

    private fun normalizeSearchText(value: String): String =
        value
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun relationStrength(path: List<Pair<Int, RelationshipType>>): Int =
        path.sumOf { (_, rel) ->
            when (rel) {
                RelationshipType.ONTOLOGY_PARENT -> 6
                RelationshipType.THEMATIC_PARENT -> 5
                RelationshipType.PARENT -> 3
                RelationshipType.RELATED -> 1
                RelationshipType.NONE -> 0
            }
        }

    private fun relationshipOrder(type: RelationshipType): Int =
        when (type) {
            RelationshipType.ONTOLOGY_PARENT -> 0
            RelationshipType.THEMATIC_PARENT -> 1
            RelationshipType.PARENT -> 2
            RelationshipType.RELATED -> 3
            RelationshipType.NONE -> 4
        }

    private fun assignedSetFor(
        parentType: RelationshipType,
        plan: SupplementalTopicAssignment,
    ): Set<Int> = when (parentType) {
        RelationshipType.ONTOLOGY_PARENT -> plan.ontologyAssigned
        RelationshipType.THEMATIC_PARENT -> plan.thematicAssigned
        else -> emptySet()
    }

    private suspend fun getOrBuildSupplementalAssignmentLocked(): SupplementalTopicAssignment {
        supplementalAssignment?.let { return it }

        return supplementalBuildMutex.withLock {
            supplementalAssignment?.let { return@withLock it }

            val visibleOntology = topicsDao.getVisibleOntologyTopicIds().toSet()
            val visibleThematic = topicsDao.getVisibleThematicTopicIds().toSet()
            val allIds = topicsDao.getAllTopicIds().toSet()
            val hidden = allIds - visibleOntology - visibleThematic
            val edges = topicsDao.getAllParentEdges()

            val uf = IntDisjointSet()

            for (e in edges) {
                uf.union(e.childTopicId, e.parentTopicId)
            }

            for (h in hidden) {
                uf.find(h)
            }

            val compTouches = mutableMapOf<Int, Pair<Boolean, Boolean>>()

            for (tid in allIds) {
                val r = uf.find(tid)
                val (o, t) = compTouches[r] ?: (false to false)
                compTouches[r] = (o || tid in visibleOntology) to (t || tid in visibleThematic)
            }

            val ontologyAssigned = mutableSetOf<Int>()
            val thematicAssigned = mutableSetOf<Int>()

            for (h in hidden) {
                val r = uf.find(h)
                val (touchO, touchT) = compTouches[r] ?: (false to false)
                when {
                    touchO && !touchT -> ontologyAssigned.add(h)
                    touchT && !touchO -> thematicAssigned.add(h)
                    else -> thematicAssigned.add(h)
                }
            }

            val parentToChildren = mutableMapOf<Int, MutableList<Int>>()
            val childToParents = mutableMapOf<Int, MutableList<Int>>()

            for (e in edges) {
                parentToChildren.getOrPut(e.parentTopicId) { mutableListOf() }.add(e.childTopicId)
                childToParents.getOrPut(e.childTopicId) { mutableListOf() }.add(e.parentTopicId)
            }

            parentToChildren.values.forEach { it.sort() }
            childToParents.values.forEach { it.sort() }

            suspend fun supplementalRootsFor(assigned: Set<Int>): List<Int> {
                val roots = assigned.filter { childId ->
                    childToParents[childId].isNullOrEmpty()
                }

                if (roots.isEmpty()) return emptyList()

                val rows = getTopicSummariesByIdsBatched(roots, RelationshipType.ONTOLOGY_PARENT)
                val order = rows.associateBy { it.topicId }

                return roots
                    .mapNotNull { order[it] }
                    .sortedBy { it.title.lowercase() }
                    .map { it.topicId }
            }

            val ontologyRoots = supplementalRootsFor(ontologyAssigned)
            val thematicRoots = supplementalRootsFor(thematicAssigned)

            SupplementalTopicAssignment(
                ontologyAssigned = ontologyAssigned,
                thematicAssigned = thematicAssigned,
                ontologySupplementalRootIds = ontologyRoots,
                thematicSupplementalRootIds = thematicRoots,
                parentToChildren = parentToChildren.mapValues { (_, v) -> v.toList() },
                childToParents = childToParents.mapValues { (_, v) -> v.toList() },
            ).also { supplementalAssignment = it }
        }
    }

    private suspend fun getTopicSummariesByIdsBatched(
        topicIds: List<Int>,
        parentType: RelationshipType,
    ): List<TopicSummaryRow> {
        val distinct = topicIds.distinct()

        if (distinct.isEmpty()) return emptyList()

        val lang = preferredLanguageCode()

        return distinct.chunked(TOPIC_IDS_QUERY_CHUNK).flatMap { chunk ->
            topicsDao.getTopicSummariesByIds(chunk, parentType, lang)
        }
    }

    private fun List<com.quranapp.android.db.relations.topics.TopicVerseRow>.toVerseRefs(): List<String> {
        return map { QuranMeta.getVerseNoFromAyahId(it.ayahId) }
            .map { (chapterNo, verseNo) -> "$chapterNo:$verseNo" }
    }
}
