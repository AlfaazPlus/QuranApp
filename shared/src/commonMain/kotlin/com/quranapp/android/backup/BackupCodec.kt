package com.quranapp.android.backup

import com.quranapp.android.models.BookmarkModel
import com.quranapp.android.models.CollectionExportModel
import com.quranapp.android.utils.miscellenous.isGzip
import io.github.optimumcode.json.schema.FormatBehavior
import io.github.optimumcode.json.schema.JsonSchemaLoader
import io.github.optimumcode.json.schema.SchemaOption
import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okio.Buffer
import okio.GzipSink
import okio.GzipSource
import okio.Source
import okio.buffer
import kotlin.time.Instant

private val json = Json { prettyPrint = false }

object BackupFiles {
    const val MAX_COMPRESSED_BYTES = 8 * 1024 * 1024
    const val MAX_PLAIN_BYTES = 32 * 1024 * 1024

    /**
     * Reads a source fully, rejecting it as soon as [maxBytes] is exceeded.
     * The source remains owned by the caller and is not closed here.
     */
    fun readLimited(input: Source, maxBytes: Int = MAX_PLAIN_BYTES): ByteArray {
        if (maxBytes < 0) throw BackupReadException(BackupReadFailure.TOO_LARGE)

        val source = input.buffer()
        val output = Buffer()
        var total = 0L
        val limit = maxBytes.toLong()

        while (true) {
            // Read at most one byte beyond the limit so oversized inputs are detected
            // without buffering the entire source.
            val read = source.read(output, minOf(8192L, limit - total + 1L))
            if (read == -1L) break
            if (read == 0L) continue

            total += read
            if (total > limit) throw BackupReadException(BackupReadFailure.TOO_LARGE)
        }

        return output.readByteArray()
    }

    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) throw BackupReadException(BackupReadFailure.UNREADABLE)

        val plain = if (bytes.isGzip()) {
            if (bytes.size > MAX_COMPRESSED_BYTES) {
                throw BackupReadException(BackupReadFailure.TOO_LARGE)
            }

            gunzip(bytes)
        } else {
            if (bytes.size > MAX_PLAIN_BYTES) {
                throw BackupReadException(BackupReadFailure.TOO_LARGE)
            }

            bytes
        }

        return utf8(plain)
    }

    fun gzip(text: String): ByteArray {
        val output = Buffer()

        GzipSink(output).buffer().use { sink ->
            sink.writeUtf8(text)
        }

        return output.readByteArray()
    }

    private fun gunzip(bytes: ByteArray): ByteArray {
        return try {
            GzipSource(Buffer().write(bytes)).use { source ->
                readLimited(source, MAX_PLAIN_BYTES)
            }
        } catch (e: BackupReadException) {
            throw e
        } catch (_: Exception) {
            throw BackupReadException(BackupReadFailure.UNREADABLE)
        }
    }

    private fun utf8(bytes: ByteArray): String = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        throw BackupReadException(BackupReadFailure.UNREADABLE)
    }
}

object BackupCodec {
    fun read(text: String, @Suppress("UNUSED_PARAMETER") schema: String): BackupDocument {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (_: Exception) {
            throw BackupReadException(BackupReadFailure.INVALID)
        }

        return when (versionOf(root)) {
            1 -> readV1(root)
            else -> readV2(root)
        }
    }

    fun write(
        exportedAt: Instant,
        bookmarks: List<BackupBookmark>?,
        settings: JsonObject?,
        collections: List<BackupCollection>?,
        schema: String,
    ): String {
        val document = buildJsonObject {
            put($$"$schema", BACKUP_SCHEMA)
            put("formatVersion", BACKUP_FORMAT_VERSION)
            put("exportedAt", formatInstant(exportedAt))
            put("data", buildJsonObject {
                if (bookmarks != null) put("bookmarks", bookmarks.toBookmarkArray())
                if (settings != null) put("settings", settings)
                if (collections != null) put("collections", collections.toCollectionArray())
            })
        }

        // Validate the JsonObject directly; don't serialize and parse it again.
        BackupSchema.assertValid(schema, document)

        return json.encodeToString(JsonObject.serializer(), document)
    }

    private fun versionOf(root: JsonObject): Int {
        val formatVersion = root["formatVersion"]?.intOrNull()
            ?: root["formatVersion"]?.contentOrNull()?.toIntOrNull()

        if (formatVersion != null) {
            if (formatVersion >= BACKUP_FORMAT_VERSION) return 2
            if (formatVersion == 1) return 1
            throw BackupReadException(BackupReadFailure.UNSUPPORTED_VERSION)
        }

        if (root["data"] is JsonObject) return 2

        val legacy = root["version"]?.intOrNull() ?: root["version"]?.contentOrNull()?.toIntOrNull()
        if (legacy != null && legacy != 1) {
            throw BackupReadException(BackupReadFailure.UNSUPPORTED_VERSION)
        }

        val known = root.containsKey("bookmarks") ||
                root.containsKey("settings") ||
                root.containsKey("collections") ||
                legacy != null

        if (!known) throw BackupReadException(BackupReadFailure.INVALID)

        return 1
    }

    private fun readV2(root: JsonObject): BackupDocument {
        val data = root["data"] as? JsonObject ?: root

        return BackupDocument(
            bookmarks = parseItems(data["bookmarks"], ::parseBookmark),
            settings = data["settings"] as? JsonObject,
            collections = parseItems(data["collections"], ::parseCollection),
        )
    }

    private fun readV1(root: JsonObject): BackupDocument {
        val bookmarks = (root["bookmarks"] as? JsonArray)?.let { array ->
            BookmarkModel.fromJson(array).map { model ->
                BackupBookmark(
                    id = null,
                    chapterNumber = model.chapterNo,
                    startVerseNumber = model.fromVerseNo,
                    endVerseNumber = model.toVerseNo,
                    note = model.note,
                    createdAt = null,
                    updatedAt = null,
                    legacyCreatedAtText = model.date,
                )
            }.filter { bookmark ->
                bookmark.chapterNumber > 0 &&
                        bookmark.startVerseNumber > 0 &&
                        bookmark.endVerseNumber >= bookmark.startVerseNumber
            }
        }

        val settings = root["settings"] as? JsonObject
        val (nested, issues) = settings?.let { SettingsContract.fromV1(it) }
            ?: (null to emptyList())

        return BackupDocument(bookmarks, nested, null, issues)
    }
}

private object BackupSchema {
    fun assertValid(schema: String, document: JsonElement) {
        val validator = JsonSchemaLoader.create()
            .withSchemaOption(
                SchemaOption.FORMAT_BEHAVIOR_OPTION,
                FormatBehavior.ANNOTATION_AND_ASSERTION,
            )
            .fromDefinition(schema)

        val errors = mutableListOf<ValidationError>()
        if (!validator.validate(document, errors::add)) {
            throw BackupReadException(BackupReadFailure.INVALID)
        }
    }
}

private fun <T> parseItems(element: JsonElement?, parse: (JsonObject) -> T?): List<T>? {
    val array = element as? JsonArray ?: return null
    return array.mapNotNull { item ->
        val node = item as? JsonObject ?: return@mapNotNull null
        try {
            parse(node)
        } catch (_: Exception) {
            null
        }
    }
}

private fun parseBookmark(node: JsonObject): BackupBookmark? {
    val chapter = node.optionalInt("chapterNumber") ?: return null
    val start = node.optionalInt("startVerseNumber") ?: return null
    val end = node.optionalInt("endVerseNumber") ?: return null

    if (chapter < 1 || start < 1 || end < start) return null

    return BackupBookmark(
        id = node.optionalString("id")?.takeIf { it.isNotBlank() },
        chapterNumber = chapter,
        startVerseNumber = start,
        endVerseNumber = end,
        note = node.optionalString("note"),
        createdAt = node.optionalString("createdAt")?.let(::parseInstantOrNull),
        updatedAt = node.optionalString("updatedAt")?.let(::parseInstantOrNull),
        legacyCreatedAtText = node.optionalString("legacyCreatedAtText"),
    )
}

private fun parseCollection(node: JsonObject): BackupCollection? {
    val name = node.optionalString("name")?.trim().orEmpty()
    if (name.isEmpty()) return null

    val items = (node["items"] as? JsonArray)?.mapIndexedNotNull { index, element ->
        val item = element as? JsonObject ?: return@mapIndexedNotNull null
        try {
            parseCollectionItem(item, index)
        } catch (_: Exception) {
            null
        }
    }.orEmpty()

    val updatedAt = node.optionalString("updatedAt")?.let(::parseInstantOrNull)

    return BackupCollection(
        id = node.optionalString("id")?.takeIf { it.isNotBlank() },
        name = name,
        description = node.optionalString("description"),
        iconKey = node.optionalString("iconKey"),
        color = node.optionalString("color"),
        sortIndex = node.optionalInt("sortIndex"),
        createdAt = node.optionalString("createdAt")?.let(::parseInstantOrNull),
        updatedAt = updatedAt,
        timestampsTrusted = updatedAt != null,
        items = items,
    )
}

private fun parseCollectionItem(node: JsonObject, index: Int): BackupCollectionItem? {
    val chapter = node.optionalInt("chapterNumber") ?: return null
    val verse = node.optionalInt("verseNumber") ?: return null

    if (chapter < 1 || verse < 1) return null

    return BackupCollectionItem(
        chapterNumber = chapter,
        verseNumber = verse,
        note = node.optionalString("note"),
        position = node.optionalInt("position") ?: index,
        addedAt = node.optionalString("addedAt")?.let(::parseInstantOrNull),
    )
}

private fun CollectionExportModel.toBackupCollection(): BackupCollection {
    return BackupCollection(
        id = null,
        name = name,
        description = description,
        iconKey = iconKey,
        color = color,
        sortIndex = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        timestampsTrusted = false,
        items = verses.mapIndexed { index, verse ->
            BackupCollectionItem(
                chapterNumber = verse.chapterNo,
                verseNumber = verse.verseNo,
                note = verse.note,
                position = index,
                addedAt = verse.addedAt,
            )
        },
    )
}

private fun List<BackupBookmark>.toBookmarkArray() = buildJsonArray {
    for (bookmark in this@toBookmarkArray) {
        add(buildJsonObject {
            put("id", requireNotNull(bookmark.id))
            put("chapterNumber", bookmark.chapterNumber)
            put("startVerseNumber", bookmark.startVerseNumber)
            put("endVerseNumber", bookmark.endVerseNumber)

            bookmark.note?.let { put("note", it) }

            if (bookmark.legacyCreatedAtText.isNullOrBlank()) {
                bookmark.createdAt?.let { put("createdAt", formatInstant(it)) }
            } else {
                put("legacyCreatedAtText", bookmark.legacyCreatedAtText)
            }

            bookmark.updatedAt?.let { put("updatedAt", formatInstant(it)) }
        })
    }
}

private fun List<BackupCollection>.toCollectionArray() = buildJsonArray {
    for (collection in this@toCollectionArray) {
        add(buildJsonObject {
            put("id", requireNotNull(collection.id))
            put("name", collection.name)

            collection.description?.let { put("description", it) }
            collection.iconKey?.let { put("iconKey", it) }
            collection.color?.let { put("color", it) }
            collection.sortIndex?.let { put("sortIndex", it) }
            collection.createdAt?.let { put("createdAt", formatInstant(it)) }
            collection.updatedAt?.let { put("updatedAt", formatInstant(it)) }

            put("items", buildJsonArray {
                for (item in collection.items) {
                    add(buildJsonObject {
                        put("chapterNumber", item.chapterNumber)
                        put("verseNumber", item.verseNumber)
                        item.note?.let { put("note", it) }
                        put("position", item.position)
                        item.addedAt?.let { put("addedAt", formatInstant(it)) }
                    })
                }
            })
        })
    }
}

internal fun formatInstant(instant: Instant): String =
    Instant.fromEpochSeconds(instant.epochSeconds).toString()

internal fun parseInstant(value: String): Instant =
    parseInstantOrNull(value) ?: throw BackupReadException(BackupReadFailure.INVALID)

private fun parseInstantOrNull(value: String): Instant? = try {
    Instant.parse(if (value.endsWith('z')) value.dropLast(1) + 'Z' else value)
} catch (_: Exception) {
    null
}

private fun JsonObject.optionalString(key: String): String? = this[key]?.contentOrNull()

private fun JsonObject.optionalInt(key: String): Int? {
    val element = this[key] ?: return null
    element.intOrNull()?.let { return it }
    return element.contentOrNull()?.toIntOrNull()
}

private fun JsonElement.intOrNull(): Int? = try {
    val primitive = jsonPrimitive
    primitive.intOrNull ?: primitive.content.toDoubleOrNull()
        ?.takeIf { it % 1.0 == 0.0 }
        ?.toInt()
} catch (_: Exception) {
    null
}

private fun JsonElement.contentOrNull(): String? = try {
    val primitive = jsonPrimitive
    if (primitive.isString) primitive.content else null
} catch (_: Exception) {
    null
}
