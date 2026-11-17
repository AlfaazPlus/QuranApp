package com.quranapp.android.db.relations.topics

import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.entities.topics.TopicFlags

data class TopicSummaryRow(
    val topicId: Int,
    val slug: String?,
    val type: String,
    val imageUrl: String?,
    val icon: String?,
    val flags: TopicFlags?,
    val title: String,
    val shortDescription: String?,
    val description: String?,
    val ayahCount: Int,
    val childCount: Int,
    val relatedCount: Int,
)

data class TopicSearchCandidateRow(
    val topicId: Int,
    val slug: String?,
    val type: String,
    val imageUrl: String?,
    val icon: String?,
    val flags: TopicFlags?,
    val title: String,
    val shortDescription: String?,
    val description: String?,
    val ayahCount: Int,
)

data class TopicVerseRow(
    val ayahId: Int,
)

data class TopicRelationshipRow(
    val relationshipType: RelationshipType,
    val topicId: Int,
    val slug: String?,
    val type: String,
    val imageUrl: String?,
    val icon: String?,
    val flags: TopicFlags?,
    val title: String,
    val shortDescription: String?,
    val description: String?,
    val ayahCount: Int,
    val childCount: Int,
    val relatedCount: Int,
)

data class TopicHierarchyEdgeRow(
    val childTopicId: Int,
    val parentTopicId: Int,
    val relationshipType: RelationshipType,
)
