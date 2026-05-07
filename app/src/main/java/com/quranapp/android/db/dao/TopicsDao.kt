package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.relations.topics.TopicRelationshipRow
import com.quranapp.android.db.relations.topics.TopicSummaryRow
import com.quranapp.android.db.relations.topics.TopicVerseRow

@Dao
interface TopicsDao {
    @Query(
        """
        SELECT
            t.id AS topicId,
            t.slug AS slug,
            t.type AS type,
            t.image_url AS imageUrl,
            t.icon AS icon,
            t.flags AS flags,
            COALESCE(loc.title, en.title, ar.title, t.slug, '') AS title,
            ar.title AS arabicTitle,
            COALESCE(loc.short_description, en.short_description) AS shortDescription,
            COALESCE(loc.description, en.description) AS description,
            (SELECT COUNT(*) FROM topic_ayahs ta WHERE ta.topic_id = t.id) AS ayahCount,
            (
                SELECT COUNT(*)
                FROM relationships child_rel
                WHERE child_rel.tgt_topic_id = t.id
                    AND child_rel.type = :parentType
            ) AS childCount,
            (
                SELECT COUNT(*)
                FROM relationships related_rel
                WHERE related_rel.type = 'related'
                    AND (related_rel.src_topic_id = t.id OR related_rel.tgt_topic_id = t.id)
            ) AS relatedCount
        FROM topics t
        LEFT JOIN topic_localizations loc
            ON loc.topic_id = t.id AND loc.lang_code = :langCode
        LEFT JOIN topic_localizations en
            ON en.topic_id = t.id AND en.lang_code = 'en'
        LEFT JOIN topic_localizations ar
            ON ar.topic_id = t.id AND ar.lang_code = 'ar'
        WHERE (COALESCE(t.flags, 0) & :flagMask) != 0
            AND NOT EXISTS (
                SELECT 1
                FROM relationships parent_rel
                WHERE parent_rel.src_topic_id = t.id
                    AND parent_rel.type = :parentType
            )
        ORDER BY LOWER(COALESCE(loc.title, en.title, ar.title, t.slug, ''))
        """
    )
    suspend fun getRootTopics(
        flagMask: Int,
        parentType: RelationshipType,
        langCode: String,
    ): List<TopicSummaryRow>

    @Query(
        """
        SELECT
            t.id AS topicId,
            t.slug AS slug,
            t.type AS type,
            t.image_url AS imageUrl,
            t.icon AS icon,
            t.flags AS flags,
            COALESCE(loc.title, en.title, ar.title, t.slug, '') AS title,
            ar.title AS arabicTitle,
            COALESCE(loc.short_description, en.short_description) AS shortDescription,
            COALESCE(loc.description, en.description) AS description,
            (SELECT COUNT(*) FROM topic_ayahs ta WHERE ta.topic_id = t.id) AS ayahCount,
            (
                SELECT COUNT(*)
                FROM relationships child_rel
                WHERE child_rel.tgt_topic_id = t.id
                    AND child_rel.type = :parentType
            ) AS childCount,
            (
                SELECT COUNT(*)
                FROM relationships related_rel
                WHERE related_rel.type = 'related'
                    AND (related_rel.src_topic_id = t.id OR related_rel.tgt_topic_id = t.id)
            ) AS relatedCount
        FROM topics t
        LEFT JOIN topic_localizations loc
            ON loc.topic_id = t.id AND loc.lang_code = :langCode
        LEFT JOIN topic_localizations en
            ON en.topic_id = t.id AND en.lang_code = 'en'
        LEFT JOIN topic_localizations ar
            ON ar.topic_id = t.id AND ar.lang_code = 'ar'
        WHERE t.id = :topicId
        LIMIT 1
        """
    )
    suspend fun getTopicById(
        topicId: Int,
        parentType: RelationshipType,
        langCode: String,
    ): TopicSummaryRow?

    @Query(
        """
        SELECT
            t.id AS topicId,
            t.slug AS slug,
            t.type AS type,
            t.image_url AS imageUrl,
            t.icon AS icon,
            t.flags AS flags,
            COALESCE(loc.title, en.title, ar.title, t.slug, '') AS title,
            ar.title AS arabicTitle,
            COALESCE(loc.short_description, en.short_description) AS shortDescription,
            COALESCE(loc.description, en.description) AS description,
            (SELECT COUNT(*) FROM topic_ayahs ta WHERE ta.topic_id = t.id) AS ayahCount,
            (
                SELECT COUNT(*)
                FROM relationships child_rel
                WHERE child_rel.tgt_topic_id = t.id
                    AND child_rel.type = :parentType
            ) AS childCount,
            (
                SELECT COUNT(*)
                FROM relationships related_rel
                WHERE related_rel.type = 'related'
                    AND (related_rel.src_topic_id = t.id OR related_rel.tgt_topic_id = t.id)
            ) AS relatedCount
        FROM topics t
        LEFT JOIN topic_localizations loc
            ON loc.topic_id = t.id AND loc.lang_code = :langCode
        LEFT JOIN topic_localizations en
            ON en.topic_id = t.id AND en.lang_code = 'en'
        LEFT JOIN topic_localizations ar
            ON ar.topic_id = t.id AND ar.lang_code = 'ar'
        WHERE t.slug = :slug
        LIMIT 1
        """
    )
    suspend fun getTopicBySlug(
        slug: String,
        parentType: RelationshipType,
        langCode: String,
    ): TopicSummaryRow?

    @Query(
        """
        SELECT
            t.id AS topicId,
            t.slug AS slug,
            t.type AS type,
            t.image_url AS imageUrl,
            t.icon AS icon,
            t.flags AS flags,
            COALESCE(loc.title, en.title, ar.title, t.slug, '') AS title,
            ar.title AS arabicTitle,
            COALESCE(loc.short_description, en.short_description) AS shortDescription,
            COALESCE(loc.description, en.description) AS description,
            (SELECT COUNT(*) FROM topic_ayahs ta WHERE ta.topic_id = t.id) AS ayahCount,
            (
                SELECT COUNT(*)
                FROM relationships child_rel
                WHERE child_rel.tgt_topic_id = t.id
                    AND child_rel.type = :parentType
            ) AS childCount,
            (
                SELECT COUNT(*)
                FROM relationships related_rel
                WHERE related_rel.type = 'related'
                    AND (related_rel.src_topic_id = t.id OR related_rel.tgt_topic_id = t.id)
            ) AS relatedCount
        FROM relationships r
        INNER JOIN topics t
            ON t.id = r.src_topic_id
        LEFT JOIN topic_localizations loc
            ON loc.topic_id = t.id AND loc.lang_code = :langCode
        LEFT JOIN topic_localizations en
            ON en.topic_id = t.id AND en.lang_code = 'en'
        LEFT JOIN topic_localizations ar
            ON ar.topic_id = t.id AND ar.lang_code = 'ar'
        WHERE r.tgt_topic_id = :parentTopicId
            AND r.type = :parentType
        ORDER BY r.sort_order, LOWER(COALESCE(loc.title, en.title, ar.title, t.slug, ''))
        """
    )
    suspend fun getChildTopics(
        parentTopicId: Int,
        parentType: RelationshipType,
        langCode: String,
    ): List<TopicSummaryRow>

    @Query(
        """
        SELECT ayah_id AS ayahId
        FROM topic_ayahs
        WHERE topic_id = :topicId
        ORDER BY ayah_id
        LIMIT :limit
        """
    )
    suspend fun getTopicVerses(topicId: Int, limit: Int): List<TopicVerseRow>

    @Query(
        """
        SELECT ayah_id AS ayahId
        FROM topic_ayahs
        WHERE topic_id = :topicId
        ORDER BY ayah_id
        """
    )
    suspend fun getAllTopicVerses(topicId: Int): List<TopicVerseRow>

    @Query(
        """
        SELECT
            r.type AS relationshipType,
            t.id AS topicId,
            t.slug AS slug,
            t.type AS type,
            t.image_url AS imageUrl,
            t.icon AS icon,
            t.flags AS flags,
            COALESCE(loc.title, en.title, ar.title, t.slug, '') AS title,
            ar.title AS arabicTitle,
            COALESCE(loc.short_description, en.short_description) AS shortDescription,
            COALESCE(loc.description, en.description) AS description,
            (SELECT COUNT(*) FROM topic_ayahs ta WHERE ta.topic_id = t.id) AS ayahCount,
            (
                SELECT COUNT(*)
                FROM relationships child_rel
                WHERE child_rel.tgt_topic_id = t.id
                    AND child_rel.type = :parentType
            ) AS childCount,
            (
                SELECT COUNT(*)
                FROM relationships related_rel
                WHERE related_rel.type = 'related'
                    AND (related_rel.src_topic_id = t.id OR related_rel.tgt_topic_id = t.id)
            ) AS relatedCount
        FROM relationships r
        INNER JOIN topics t
            ON t.id = CASE
                WHEN r.src_topic_id = :topicId THEN r.tgt_topic_id
                ELSE r.src_topic_id
            END
        LEFT JOIN topic_localizations loc
            ON loc.topic_id = t.id AND loc.lang_code = :langCode
        LEFT JOIN topic_localizations en
            ON en.topic_id = t.id AND en.lang_code = 'en'
        LEFT JOIN topic_localizations ar
            ON ar.topic_id = t.id AND ar.lang_code = 'ar'
        WHERE (r.src_topic_id = :topicId OR r.tgt_topic_id = :topicId)
            AND r.type = 'related'
        ORDER BY
            r.sort_order,
            LOWER(COALESCE(loc.title, en.title, ar.title, t.slug, ''))
        LIMIT :limit
        """
    )
    suspend fun getTopicRelationships(
        topicId: Int,
        parentType: RelationshipType,
        langCode: String,
        limit: Int,
    ): List<TopicRelationshipRow>
}
