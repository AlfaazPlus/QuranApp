package com.quranapp.android.components.bookmark

import com.quranapp.android.api.safeJsonArray
import com.quranapp.android.api.safeString
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.utils.univ.DateUtils
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

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
    val createdAt: Date,
    val updatedAt: Date,
    val verses: List<Verse>,
) {
    data class Verse(
        val chapterNo: Int,
        val verseNo: Int,
        val note: String?,
        val addedAt: Date,
    ) {
        fun toJsonObject(): JSONObject {
            return JSONObject().apply {
                put(CollectionJsonKeys.CHAPTER_NO, chapterNo)
                put(CollectionJsonKeys.VERSE_NO, verseNo)
                if (!note.isNullOrBlank()) put(CollectionJsonKeys.NOTE, note)
                put(
                    CollectionJsonKeys.ADDED_AT,
                    DateUtils.format(addedAt, DateUtils.DATETIME_FORMAT_SYSTEM),
                )
            }
        }
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put(CollectionJsonKeys.NAME, name)
            if (!description.isNullOrBlank()) put(CollectionJsonKeys.DESCRIPTION, description)
            if (!iconKey.isNullOrBlank()) put(CollectionJsonKeys.ICON_KEY, iconKey)
            if (!color.isNullOrBlank()) put(CollectionJsonKeys.COLOR, color)
            put(CollectionJsonKeys.CREATED_AT, formatDate(createdAt))
            put(CollectionJsonKeys.UPDATED_AT, formatDate(updatedAt))

            val versesArray = JSONArray()
            for (verse in verses) {
                versesArray.put(verse.toJsonObject())
            }
            put(CollectionJsonKeys.VERSES, versesArray)
        }
    }

    companion object {
        fun toJson(collections: List<CollectionExportModel>): JSONArray {
            val jsonArray = JSONArray()
            for (collection in collections) {
                jsonArray.put(collection.toJsonObject())
            }
            return jsonArray
        }

        fun fromJson(collections: JsonArray?): List<CollectionExportModel> {
            if (collections == null) return emptyList()

            val result = ArrayList<CollectionExportModel>()
            for (element in collections) {
                try {
                    val obj = element.jsonObject
                    val name = obj.safeString(CollectionJsonKeys.NAME)?.trim().orEmpty()
                    if (name.isEmpty()) continue

                    val verses = obj.safeJsonArray(CollectionJsonKeys.VERSES)
                        ?.mapNotNull { verseElement ->
                            try {
                                parseVerse(verseElement.jsonObject)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        .orEmpty()

                    result.add(
                        CollectionExportModel(
                            name = name,
                            description = obj.safeString(CollectionJsonKeys.DESCRIPTION),
                            iconKey = obj.safeString(CollectionJsonKeys.ICON_KEY),
                            color = obj.safeString(CollectionJsonKeys.COLOR),
                            createdAt = parseDate(obj.safeString(CollectionJsonKeys.CREATED_AT)),
                            updatedAt = parseDate(obj.safeString(CollectionJsonKeys.UPDATED_AT)),
                            verses = verses,
                        )
                    )
                } catch (_: Exception) {
                    // A malformed collection does not fail the rest of the file.
                }
            }
            return result
        }

        fun fromEntities(
            collection: CollectionEntity,
            items: List<CollectionItemEntity>,
        ): CollectionExportModel {
            return CollectionExportModel(
                name = collection.name,
                description = collection.description,
                iconKey = collection.icon_key,
                color = collection.color,
                createdAt = collection.createdAt,
                updatedAt = collection.updatedAt,
                verses = items.map { item ->
                    Verse(
                        chapterNo = item.chapterNo,
                        verseNo = item.verseNo,
                        note = item.note,
                        addedAt = item.addedAt,
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

        private fun formatDate(date: Date): String {
            return DateUtils.format(date, DateUtils.DATETIME_FORMAT_SYSTEM)
        }

        private fun parseDate(value: String?): Date {
            if (value.isNullOrBlank()) return Date()
            return DateUtils.toDate(value, DateUtils.DATETIME_FORMAT_SYSTEM) ?: Date()
        }
    }
}
