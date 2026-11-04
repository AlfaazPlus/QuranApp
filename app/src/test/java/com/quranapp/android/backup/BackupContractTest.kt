package com.quranapp.android.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

class BackupContractTest {
    private val schema = schemaFile().readText()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun v1SettingsUseCanonicalPaths() {
        val sample = json.parseToJsonElement(v1Sample().readText()).jsonObject
        val (nested, issues) = SettingsContract.fromV1(sample.getValue("settings").jsonObject)
        val read = SettingsContract.read(nested)

        assertEquals("default", text(read, SettingsContract.LOCALE))
        assertEquals("mode_vbv", text(read, SettingsContract.STYLE))
        assertEquals("abdul-basit", text(read, SettingsContract.RECITER))
        assertEquals(listOf("en_102_the-clear-quran"), texts(read, SettingsContract.TRANSLATIONS))
        assertTrue(issues.contains("rec.option_audio"))
        assertTrue(issues.contains("rec.repeat"))
        assertTrue(issues.contains("rec.scroll_sync"))
        assertTrue(issues.contains("rec.continue_chapter"))
        assertTrue(read.accepted.none { it.path == SettingsContract.AUDIO_MODE })
    }

    @Test
    fun legacyReaderModeNameMapsToStyleId() {
        val flat = buildJsonObject { put("reader.mode", "Reading") }
        val (nested, issues) = SettingsContract.fromV1(flat)
        assertTrue(issues.isEmpty())
        assertEquals("mode_reading", text(SettingsContract.read(nested), SettingsContract.STYLE))
    }

    @Test
    fun unsupportedLegacyStyleIsReported() {
        val flat = buildJsonObject { put("reader.style", 99) }
        val (nested, issues) = SettingsContract.fromV1(flat)
        assertEquals(emptyList<AcceptedSetting>(), SettingsContract.read(nested).accepted)
        assertTrue(issues.contains("reader.style"))
    }

    @Test
    fun wrongSettingTypeIsNotAccepted() {
        val settings = buildJsonObject {
            put("reader", buildJsonObject {
                put("text", buildJsonObject {
                    put("arabic", buildJsonObject { put("enabled", "yes") })
                })
            })
        }
        val read = SettingsContract.read(settings)
        assertTrue(read.accepted.isEmpty())
        assertTrue(read.issues.contains(SettingsContract.ARABIC_ENABLED))
    }

    @Test
    fun unknownSettingIsReported() {
        val settings = buildJsonObject {
            put("reader", buildJsonObject { put("futureFlag", true) })
            put("app", buildJsonObject { put("locale", "default") })
        }
        val read = SettingsContract.read(settings)
        assertEquals("default", text(read, SettingsContract.LOCALE))
        assertTrue(read.issues.contains("reader.futureFlag"))
    }

    @Test
    fun v2RoundTripValidatesAndKeepsIdentity() {
        val id = "b9b9b5e7-8758-4f7b-a9b1-7c8f8a09d3d1"
        val bookmark = BackupBookmark(
            id = id,
            chapterNumber = 32,
            startVerseNumber = 23,
            endVerseNumber = 23,
            note = "Sample note",
            createdAt = null,
            updatedAt = Instant.parse("2026-11-02T18:10:00Z"),
            legacyCreatedAtText = "2025-05-22 08:20:09",
        )
        val text = BackupCodec.write(
            exportedAt = Instant.parse("2026-11-03T10:30:00Z"),
            bookmarks = listOf(bookmark),
            settings = buildJsonObject {
                put("app", buildJsonObject { put("locale", "en-IN") })
                put("reader", buildJsonObject { put("extra", true) })
            },
            collections = emptyList(),
            schema = schema,
        )
        val document = BackupCodec.read(text, schema)
        val imported = document.bookmarks!!.single()
        assertEquals(id, imported.id)
        assertNull(imported.createdAt)
        assertEquals("2025-05-22 08:20:09", imported.legacyCreatedAtText)
        assertEquals(Instant.parse("2026-11-02T18:10:00Z"), imported.updatedAt)
        val again = BackupCodec.read(BackupFiles.decode(BackupFiles.gzip(text)), schema)
        assertEquals(imported, again.bookmarks!!.single())
    }

    @Test
    fun damagedEntriesAreSkipped() {
        val mixed = envelope(
            2,
            """{"bookmarks":[
                {"id":"not-a-uuid","chapterNumber":"1","startVerseNumber":1,"endVerseNumber":1,"extra":true,"createdAt":"not-a-date"},
                {"chapterNumber":1,"startVerseNumber":4,"endVerseNumber":2},
                "skip"
            ],"settings":{"appearance":{"themeId":"nope"},"app":{"locale":"en"}},
            "collections":[{"id":"also-not-a-uuid","name":"Saved","items":[{"chapterNumber":2,"verseNumber":255},{"verseNumber":1}]}]}""",
        )
        val document = BackupCodec.read(mixed, schema)
        val bookmark = document.bookmarks!!.single()
        assertEquals("not-a-uuid", bookmark.id)
        assertNull(bookmark.createdAt)
        assertEquals("Saved", document.collections!!.single().name)
        assertEquals(1, document.collections.single().items.size)
        val read = SettingsContract.read(document.settings!!)
        assertEquals("en", text(read, SettingsContract.LOCALE))
        assertTrue(read.issues.contains(SettingsContract.THEME))

        val future = BackupCodec.read(envelope(9, """{"bookmarks":[]}"""), schema)
        assertEquals(emptyList<BackupBookmark>(), future.bookmarks)
    }

    @Test
    fun verseOrderAndMissingCategory() {
        val backwards = envelope(
            2,
            """{"bookmarks":[{"id":"b9b9b5e7-8758-4f7b-a9b1-7c8f8a09d3d1","chapterNumber":1,"startVerseNumber":4,"endVerseNumber":2}]}""",
        )
        assertEquals(emptyList<BackupBookmark>(), BackupCodec.read(backwards, schema).bookmarks)

        val settingsOnly = BackupCodec.write(
            exportedAt = Instant.parse("2026-11-03T10:30:00Z"),
            bookmarks = null,
            settings = buildJsonObject { put("app", buildJsonObject { put("locale", "ar") }) },
            collections = null,
            schema = schema,
        )
        val document = BackupCodec.read(settingsOnly, schema)
        assertNull(document.bookmarks)
        assertNull(document.collections)
    }

    @Test
    fun currentSettingsShapeValidates() {
        val settings = SettingsContract.nest(
            mapOf(
                SettingsContract.LOCALE to JsonPrimitive("default"),
                SettingsContract.THEME to JsonPrimitive("app.theme.default"),
                SettingsContract.THEME_COLOR to JsonPrimitive("blue"),
                SettingsContract.DYNAMIC_COLOR to JsonPrimitive(false),
                SettingsContract.DOWNLOAD_SOURCE to JsonPrimitive("alfaazplus"),
                SettingsContract.AUTO_SCROLL_SPEED to JsonPrimitive(7),
                SettingsContract.ARABIC_ENABLED to JsonPrimitive(true),
                SettingsContract.STYLE to JsonPrimitive("mode_vbv"),
                SettingsContract.KEEP_SCREEN_ON to JsonPrimitive(true),
                SettingsContract.READING_LAYOUT to JsonPrimitive("page"),
                SettingsContract.NUMERAL_SYSTEM to JsonPrimitive("latn"),
                SettingsContract.DEVELOPER_MODE to JsonPrimitive(true),
                SettingsContract.DAILY_REMINDER to JsonPrimitive(false),
                SettingsContract.PLAYBACK_SPEED to JsonPrimitive(1.0),
                SettingsContract.REPEAT_COUNT to JsonPrimitive(0),
                SettingsContract.VERSE_GROUP_SIZE to JsonPrimitive(1),
                SettingsContract.RECITER to JsonPrimitive("abdul-basit"),
                SettingsContract.VOICE to JsonPrimitive("ibrahim-walk"),
                SettingsContract.AUDIO_MODE to JsonPrimitive("only_quran"),
                SettingsContract.END_BEHAVIOUR to JsonPrimitive("stop_playback"),
                SettingsContract.TAFSIR_SCALE to JsonPrimitive(1.03),
                SettingsContract.ARABIC_SCALE to JsonPrimitive(1.18),
                SettingsContract.TRANSLATION_SCALE to JsonPrimitive(1.14),
                SettingsContract.WBW_SCALE to JsonPrimitive(1.0),
                SettingsContract.WBW_SOURCE to JsonPrimitive("en"),
                SettingsContract.WBW_TRANSLATION to JsonPrimitive(false),
                SettingsContract.WBW_TRANSLITERATION to JsonPrimitive(false),
                SettingsContract.WBW_TOOLTIP_TRANSLATION to JsonPrimitive(true),
                SettingsContract.WBW_TOOLTIP_TRANSLITERATION to JsonPrimitive(true),
                SettingsContract.WBW_RECITATION to JsonPrimitive(true),
                SettingsContract.SCRIPT to JsonPrimitive("uthmani"),
                SettingsContract.TAFSIR to JsonPrimitive("en_tafsir-ibn-kathir"),
                SettingsContract.TRANSLATIONS to buildJsonArray { add(JsonPrimitive("en_102_the-clear-quran")) },
            )
        )
        val text = BackupCodec.write(
            exportedAt = Instant.parse("2026-11-03T10:30:00Z"),
            bookmarks = null,
            settings = settings,
            collections = null,
            schema = schema,
        )
        val read = SettingsContract.read(BackupCodec.read(text, schema).settings!!)
        assertEquals("default", text(read, SettingsContract.LOCALE))
        assertEquals(true, bool(read, SettingsContract.DEVELOPER_MODE))
        assertTrue(read.issues.isEmpty())
        assertTrue(!text.contains('\n'))
    }

    @Test
    fun v1FileStillImports() {
        val plain = v1Sample().readText()
        val document = BackupCodec.read(plain, schema)
        val fromGzip = BackupCodec.read(BackupFiles.decode(BackupFiles.gzip(plain)), schema)
        assertEquals(document, fromGzip)
        assertEquals(2, document.bookmarks!!.size)
        assertEquals("2025-05-22 08:20:09", document.bookmarks[0].legacyCreatedAtText)
        assertNull(document.bookmarks[0].createdAt)
        assertNull(document.bookmarks[0].updatedAt)
        assertNull(document.bookmarks[0].id)
    }

    @Test
    fun bookmarkMergeRules() {
        val time = Instant.parse("2026-11-02T18:10:00Z")
        assertEquals(BookmarkChange.UNCHANGED, bookmarkChange(time, time, sameContent = true))
        assertEquals(BookmarkChange.UPDATE, bookmarkChange(time.minusSeconds(5), time, sameContent = false))
        assertEquals(BookmarkChange.UNCHANGED, bookmarkChange(time.plusSeconds(5), time, sameContent = false))
        assertEquals(BookmarkChange.CONFLICT, bookmarkChange(time, time, sameContent = false))
        assertEquals(BookmarkChange.CONFLICT, bookmarkChange(null, time, sameContent = false))
        assertEquals(BookmarkChange.UNCHANGED, bookmarkChange(null, null, sameContent = true))
    }

    private fun text(read: SettingsRead, path: String): String {
        return (read.accepted.first { it.path == path }.value as SettingValue.Text).value
    }

    private fun bool(read: SettingsRead, path: String): Boolean {
        return (read.accepted.first { it.path == path }.value as SettingValue.Bool).value
    }

    private fun texts(read: SettingsRead, path: String): List<String> {
        return (read.accepted.first { it.path == path }.value as SettingValue.Texts).value
    }

    private fun envelope(version: Int, data: String): String {
        return "{\"\$schema\":\"$BACKUP_SCHEMA\",\"formatVersion\":$version," +
            "\"exportedAt\":\"2026-11-03T10:30:00Z\",\"data\":$data}"
    }

    private fun assertFails(failure: BackupReadFailure, block: () -> Unit) {
        try {
            block()
        } catch (error: BackupReadException) {
            assertEquals(failure, error.failure)
            return
        }
        throw AssertionError("Expected $failure")
    }

    private fun schemaFile(): File {
        return listOf(
            File("../backup/schemas/backup-v2.schema.json"),
            File("backup/schemas/backup-v2.schema.json"),
        ).first { it.isFile }
    }

    private fun v1Sample(): File {
        return listOf(
            File("src/main/assets/quranapp-exported-data-v1.json"),
            File("app/src/main/assets/quranapp-exported-data-v1.json"),
        ).first { it.isFile }
    }
}
