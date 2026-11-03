package com.quranapp.android.backup

import com.github.erosb.jsonsKema.FormatValidationPolicy
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.Validator
import com.github.erosb.jsonsKema.ValidatorConfig
import com.quranapp.android.components.bookmark.BookmarkModel
import com.quranapp.android.components.bookmark.CollectionExportModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val json = Json { prettyPrint = false }
private val offsetSuffix = Regex("""(?:[zZ]|[+-]\d{2}:\d{2})$""")

object BackupFiles {
    const val MAX_COMPRESSED_BYTES = 8 * 1024 * 1024
    const val MAX_PLAIN_BYTES = 32 * 1024 * 1024

    fun readLimited(input: InputStream, maxBytes: Int = MAX_PLAIN_BYTES): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read

            if (total > maxBytes) throw BackupReadException(BackupReadFailure.TOO_LARGE)

            output.write(buffer, 0, read)
        }

        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) throw BackupReadException(BackupReadFailure.UNREADABLE)

        val plain = if (isGzip(bytes)) {
            if (bytes.size > MAX_COMPRESSED_BYTES) throw BackupReadException(BackupReadFailure.TOO_LARGE)
            gunzip(bytes)
        } else {
            if (bytes.size > MAX_PLAIN_BYTES) throw BackupReadException(BackupReadFailure.TOO_LARGE)
            bytes
        }

        return utf8(plain)
    }

    fun gzip(text: String): ByteArray {
        val output = ByteArrayOutputStream()

        GZIPOutputStream(output).use { it.write(text.toByteArray(Charsets.UTF_8)) }

        return output.toByteArray()
    }

    private fun isGzip(bytes: ByteArray): Boolean {
        return bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
    }

    private fun gunzip(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()

        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            val buffer = ByteArray(8192)
            var total = 0

            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read

                if (total > MAX_PLAIN_BYTES) throw BackupReadException(BackupReadFailure.TOO_LARGE)

                output.write(buffer, 0, read)
            }
        }

        return output.toByteArray()
    }

    private fun utf8(bytes: ByteArray): String {
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            throw BackupReadException(BackupReadFailure.UNREADABLE)
        }
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

        val text = json.encodeToString(JsonObject.serializer(), document)

        BackupSchema.assertValid(schema, text)

        return text
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
        if (legacy != null && legacy != 1) throw BackupReadException(BackupReadFailure.UNSUPPORTED_VERSION)

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
        val collections = (root["collections"] as? JsonArray)?.let { array ->
            CollectionExportModel.fromJson(array).map { it.toBackupCollection() }
        }

        val settings = root["settings"] as? JsonObject
        val (nested, issues) = settings?.let { SettingsContract.fromV1(it) }
            ?: (null to emptyList())

        return BackupDocument(bookmarks, nested, collections, issues)
    }
}

private object BackupSchema {
    fun assertValid(schema: String, document: String) {
        val loaded = SchemaLoader(schema).load()

        val failure = Validator.create(
            loaded,
            ValidatorConfig(validateFormat = FormatValidationPolicy.ALWAYS),
        ).validate(document)

        if (failure != null) throw BackupReadException(BackupReadFailure.INVALID)
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
        createdAt = createdAt.toInstantSafe(),
        updatedAt = updatedAt.toInstantSafe(),
        timestampsTrusted = false,
        items = verses.mapIndexed { index, verse ->
            BackupCollectionItem(
                chapterNumber = verse.chapterNo,
                verseNumber = verse.verseNo,
                note = verse.note,
                position = index,
                addedAt = verse.addedAt.toInstantSafe(),
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

internal fun formatInstant(instant: Instant): String {
    return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.SECONDS))
}

internal fun parseInstant(value: String): Instant {
    return parseInstantOrNull(value) ?: throw BackupReadException(BackupReadFailure.INVALID)
}

private fun parseInstantOrNull(value: String): Instant? {
    return try {
        if (offsetSuffix.containsMatchIn(value)) OffsetDateTime.parse(value).toInstant()
        else Instant.parse(value)
    } catch (_: Exception) {
        null
    }
}

private fun Date.toInstantSafe(): Instant = Instant.ofEpochMilli(time)

private fun JsonObject.optionalString(key: String): String? = this[key]?.contentOrNull()

private fun JsonObject.optionalInt(key: String): Int? {
    val element = this[key] ?: return null
    element.intOrNull()?.let { return it }
    return element.contentOrNull()?.toIntOrNull()
}

private fun JsonElement.intOrNull(): Int? {
    return try {
        val primitive = jsonPrimitive
        primitive.intOrNull ?: primitive.content.toDoubleOrNull()?.takeIf { it % 1.0 == 0.0 }
            ?.toInt()
    } catch (_: Exception) {
        null
    }
}

private fun JsonElement.contentOrNull(): String? {
    return try {
        val primitive = jsonPrimitive
        if (primitive.isString) primitive.content else null
    } catch (_: Exception) {
        null
    }
}
