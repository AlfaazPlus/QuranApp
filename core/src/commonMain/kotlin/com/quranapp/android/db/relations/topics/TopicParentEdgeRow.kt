package com.quranapp.android.db.relations.topics

/**
 * A `parent` relationship: [childTopicId] is the child, [parentTopicId] is the parent
 * (matches [relationships] row: src_topic_id = child, tgt_topic_id = parent).
 */
data class TopicParentEdgeRow(
    val childTopicId: Int,
    val parentTopicId: Int,
)
