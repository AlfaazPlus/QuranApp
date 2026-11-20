package com.quranapp.android.models

import com.quranapp.android.api.safeString
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.utils.miscellenous.DateUtils
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

private object CollectionJsonKeys {
    const val NAME = "name"
    const val DESCRIPTION = "description"
    const val ICON_KEY = "icon_key"
    const val COLOR = "color"
    const val CREATED_AT = "created_at"
    const val UPDATED_AT = "updated_at"
    const val VERSES = "verses"
    const val CHAPTER_NO = "chapter_no"
    const val VERSE_NO = "verse_no"
    const val NOTE = "note"
    const val ADDED_AT = "added_at"
}

data class CollectionExportModel(
    val name: String,
    val description: String?,
    val iconKey: String?,
    val color: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val verses: List<Verse>,
) {
    data class Verse(
        val chapterNo: Int,
        val verseNo: Int,
        val note: String?,
        val addedAt: Instant,
    )

    companion object {
        fun fromEntities(
            collection: CollectionEntity,
            items: List<CollectionItemEntity>,
        ): CollectionExportModel {
            return CollectionExportModel(
                name = collection.name,
                description = collection.description,
                iconKey = collection.icon_key,
                color = collection.color,
                createdAt = Instant.fromEpochMilliseconds(collection.createdAt),
                updatedAt = Instant.fromEpochMilliseconds(collection.updatedAt),
                verses = items.map { item ->
                    Verse(
                        chapterNo = item.chapterNo,
                        verseNo = item.verseNo,
                        note = item.note,
                        addedAt = Instant.fromEpochMilliseconds(item.addedAt),
                    )
                },
            )
        }

        private fun parseVerse(obj: JsonObject): Verse? {
            val chapterNo = obj.intOrNull(CollectionJsonKeys.CHAPTER_NO) ?: return null
            val verseNo = obj.intOrNull(CollectionJsonKeys.VERSE_NO) ?: return null

            return Verse(
                chapterNo = chapterNo,
                verseNo = verseNo,
                note = obj.safeString(CollectionJsonKeys.NOTE),
                addedAt = parseDate(obj.safeString(CollectionJsonKeys.ADDED_AT)),
            )
        }

        private fun JsonObject.intOrNull(key: String): Int? {
            return try {
                this[key]?.jsonPrimitive?.intOrNull
            } catch (_: Exception) {
                null
            }
        }

        private fun formatDate(date: Instant): String {
            return DateUtils.format(date, DateUtils.SYSTEM)
        }

        private fun parseDate(value: String?): Instant {
            if (value.isNullOrBlank()) return DateUtils.now()

            return DateUtils.parse(value, DateUtils.SYSTEM)?.let { it }
                ?: DateUtils.now()
        }
    }
}
