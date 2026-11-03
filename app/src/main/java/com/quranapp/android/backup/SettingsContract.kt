package com.quranapp.android.backup

import com.quranapp.android.compose.components.player.dialogs.AudioEndBehaviour
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.components.reader.ReadingLayout
import com.quranapp.android.compose.utils.NumeralSystem
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.utils.app.ResourceDownloadProxy
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

enum class SettingKind { BOOLEAN, NUMBER, TEXT, TEXTS }

data class SettingSpec(
    val path: String,
    val kind: SettingKind,
    val supported: Boolean,
    val allowed: Set<String>? = null,
    val range: ClosedFloatingPointRange<Double>? = null,
    val v1Key: String? = null,
)

sealed class SettingValue {
    data class Bool(val value: Boolean) : SettingValue()
    data class Num(val value: Double) : SettingValue()
    data class Text(val value: String) : SettingValue()
    data class Texts(val value: List<String>) : SettingValue()
}

data class AcceptedSetting(val path: String, val value: SettingValue)

data class SettingsRead(
    val accepted: List<AcceptedSetting>,
    val issues: List<String>,
)

object SettingsContract {
    const val LOCALE = "app.locale"
    const val NUMERAL_SYSTEM = "app.numeralSystemId"
    const val THEME = "appearance.themeId"
    const val DOWNLOAD_SOURCE = "downloads.sourceId"
    const val DAILY_REMINDER = "reminders.dailyVerse.enabled"
    const val AUTO_SCROLL_SPEED = "reader.autoScroll.speed"
    const val ARABIC_ENABLED = "reader.text.arabic.enabled"
    const val STYLE = "reader.display.styleId"
    const val KEEP_SCREEN_ON = "reader.display.keepScreenOn"
    const val READING_LAYOUT = "reader.display.layoutId"
    const val PLAYBACK_SPEED = "audio.playback.speed"
    const val AUDIO_MODE = "audio.playback.mode"
    const val END_BEHAVIOUR = "audio.playback.endBehaviour"
    const val REPEAT_COUNT = "audio.playback.repeatCount"
    const val VERSE_GROUP_SIZE = "audio.playback.verseGroupSize"
    const val RECITER = "audio.recitation.reciterId"
    const val VOICE = "audio.translation.voiceId"
    const val TAFSIR_SCALE = "reader.text.tafsir.scale"
    const val ARABIC_SCALE = "reader.text.arabic.scale"
    const val TRANSLATION_SCALE = "reader.text.translation.scale"
    const val WBW_SCALE = "reader.wordByWord.scale"
    const val SCRIPT = "reader.text.scriptId"
    const val SCRIPT_VARIANT = "reader.text.scriptVariantId"
    const val TAFSIR = "reader.content.tafsirId"
    const val TRANSLATIONS = "reader.content.translationIds"
    const val WBW_SOURCE = "reader.wordByWord.sourceId"
    const val WBW_TRANSLATION = "reader.wordByWord.showTranslation"
    const val WBW_TRANSLITERATION = "reader.wordByWord.showTransliteration"
    const val WBW_TOOLTIP_TRANSLATION = "reader.wordByWord.tooltipTranslation"
    const val WBW_TOOLTIP_TRANSLITERATION = "reader.wordByWord.tooltipTransliteration"
    const val WBW_RECITATION = "reader.wordByWord.recitation"

    private val textScale = 0.5..2.0
    private val localePattern = Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$")

    val specs: List<SettingSpec> = listOf(
        SettingSpec(LOCALE, SettingKind.TEXT, true, v1Key = "config.lang"),
        SettingSpec(
            NUMERAL_SYSTEM,
            SettingKind.TEXT,
            true,
            allowed = NumeralSystem.entries.map { it.storageKey }.toSet(),
        ),
        SettingSpec(DAILY_REMINDER, SettingKind.BOOLEAN, true),
        SettingSpec(
            THEME,
            SettingKind.TEXT,
            true,
            allowed = setOf(
                ThemeUtils.THEME_MODE_DEFAULT,
                ThemeUtils.THEME_MODE_LIGHT,
                ThemeUtils.THEME_MODE_DARK,
            ),
            v1Key = "config.theme",
        ),
        SettingSpec(
            DOWNLOAD_SOURCE,
            SettingKind.TEXT,
            true,
            allowed = ResourceDownloadProxy.entries.map { it.value }.toSet(),
            v1Key = "config.dlSrc",
        ),
        SettingSpec(
            AUTO_SCROLL_SPEED,
            SettingKind.NUMBER,
            true,
            range = 1.0..10.0,
            v1Key = "reader.autoScrollSpeed"
        ),
        SettingSpec(ARABIC_ENABLED, SettingKind.BOOLEAN, true, v1Key = "reader.arabicTextEnabled"),
        SettingSpec(
            STYLE,
            SettingKind.TEXT,
            true,
            allowed = ReaderMode.entries.map { it.value }.toSet(),
            v1Key = "reader.style",
        ),
        SettingSpec(KEEP_SCREEN_ON, SettingKind.BOOLEAN, true),
        SettingSpec(
            READING_LAYOUT,
            SettingKind.TEXT,
            true,
            allowed = ReadingLayout.entries.map { it.value }.toSet(),
        ),
        SettingSpec(
            PLAYBACK_SPEED,
            SettingKind.NUMBER,
            true,
            range = 0.1..3.0,
            v1Key = "rec.speed"
        ),
        SettingSpec(REPEAT_COUNT, SettingKind.NUMBER, true, range = 0.0..9.0),
        SettingSpec(VERSE_GROUP_SIZE, SettingKind.NUMBER, true, range = 1.0..10.0),
        SettingSpec(RECITER, SettingKind.TEXT, true, v1Key = "rec.reciter"),
        SettingSpec(VOICE, SettingKind.TEXT, true, v1Key = "rec.reciter_translation"),
        SettingSpec(
            AUDIO_MODE,
            SettingKind.TEXT,
            true,
            allowed = AudioOption.entries.map { it.value }.toSet(),
            v1Key = "rec.option_audio",
        ),
        SettingSpec(
            END_BEHAVIOUR,
            SettingKind.TEXT,
            true,
            allowed = AudioEndBehaviour.entries.map { it.value }.toSet(),
            v1Key = "rec.audio_end_behaviour",
        ),
        SettingSpec(
            TAFSIR_SCALE,
            SettingKind.NUMBER,
            true,
            range = textScale,
            v1Key = "text.size_mult_tafsir"
        ),
        SettingSpec(
            ARABIC_SCALE,
            SettingKind.NUMBER,
            true,
            range = textScale,
            v1Key = "text.size_mult_arabic"
        ),
        SettingSpec(
            TRANSLATION_SCALE,
            SettingKind.NUMBER,
            true,
            range = textScale,
            v1Key = "text.size_mult_translation"
        ),
        SettingSpec(WBW_SCALE, SettingKind.NUMBER, true, range = textScale),
        SettingSpec(SCRIPT, SettingKind.TEXT, true, v1Key = "script.current"),
        SettingSpec(
            SCRIPT_VARIANT,
            SettingKind.TEXT,
            true,
            allowed = QuranScriptVariant.entries.map { it.value }.toSet() + "",
            v1Key = "script_variant.current",
        ),
        SettingSpec(TAFSIR, SettingKind.TEXT, true, v1Key = "tafsir.current"),
        SettingSpec(TRANSLATIONS, SettingKind.TEXTS, true, v1Key = "translation.current"),
        SettingSpec(WBW_SOURCE, SettingKind.TEXT, true),
        SettingSpec(WBW_TRANSLATION, SettingKind.BOOLEAN, true),
        SettingSpec(WBW_TRANSLITERATION, SettingKind.BOOLEAN, true),
        SettingSpec(WBW_TOOLTIP_TRANSLATION, SettingKind.BOOLEAN, true),
        SettingSpec(WBW_TOOLTIP_TRANSLITERATION, SettingKind.BOOLEAN, true),
        SettingSpec(WBW_RECITATION, SettingKind.BOOLEAN, true),
    )

    private val byPath = specs.associateBy { it.path }
    private val byV1Key = specs.mapNotNull { spec -> spec.v1Key?.let { it to spec } }.toMap()
    private val styleKey = "reader.mode"
    private val objectPaths: Set<String> = specs.flatMap { spec ->
        val parts = spec.path.split('.')
        (1 until parts.size).map { index -> parts.take(index).joinToString(".") }
    }.toSet()

    fun nest(values: Map<String, JsonElement>): JsonObject {
        val root = mutableMapOf<String, Any?>()

        for ((path, value) in values) {
            val parts = path.split('.')
            var node = root
            for (part in parts.dropLast(1)) {
                @Suppress("UNCHECKED_CAST")
                node =
                    node.getOrPut(part) { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
            }
            node[parts.last()] = value
        }

        return toJsonObject(root)
    }

    fun fromV1(flat: JsonObject): Pair<JsonObject, List<String>> {
        val issues = mutableListOf<String>()
        val values = linkedMapOf<String, JsonElement>()
        val consumed = mutableSetOf<String>()

        fun put(spec: SettingSpec, element: JsonElement?, sourceKey: String) {
            consumed += sourceKey

            val converted = element?.let { convertV1(spec, sourceKey, it) }
            if (converted == null) {
                issues += sourceKey
            } else {
                values[spec.path] = converted
            }
        }

        flat[styleKey]?.let { put(byPath.getValue(STYLE), it, styleKey) }

        for ((key, element) in flat) {
            if (key == styleKey) continue
            val spec = byV1Key[key]

            if (spec == null) {
                issues += key
            } else if (spec.path !in values) {
                put(spec, element, key)
            } else {
                consumed += key
            }
        }

        return nest(values) to issues
    }

    fun read(settings: JsonObject): SettingsRead {
        val accepted = mutableListOf<AcceptedSetting>()
        val issues = mutableListOf<String>()

        walk(settings, "", accepted, issues)

        return SettingsRead(accepted, issues)
    }

    private fun walk(
        node: JsonObject,
        prefix: String,
        accepted: MutableList<AcceptedSetting>,
        issues: MutableList<String>,
    ) {
        for ((key, value) in node) {
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            val spec = byPath[path]

            when {
                spec != null -> accept(spec, value, accepted, issues)
                path in objectPaths && value is JsonObject -> walk(value, path, accepted, issues)
                else -> issues += path
            }
        }
    }

    private fun accept(
        spec: SettingSpec,
        value: JsonElement,
        accepted: MutableList<AcceptedSetting>,
        issues: MutableList<String>,
    ) {
        val parsed = parse(spec, value)

        if (!spec.supported || parsed == null) {
            issues += spec.path
        } else {
            accepted += AcceptedSetting(spec.path, parsed)
        }
    }

    private fun parse(spec: SettingSpec, value: JsonElement): SettingValue? {
        return when (spec.kind) {
            SettingKind.BOOLEAN -> value.booleanOrNull()?.let(SettingValue::Bool)
            SettingKind.NUMBER -> numberOrNull(value)?.takeIf { spec.range == null || it in spec.range }
                ?.let(SettingValue::Num)

            SettingKind.TEXT -> textOrNull(value)?.takeIf { textAllowed(spec, it) }
                ?.let(SettingValue::Text)

            SettingKind.TEXTS -> textsOrNull(value)?.let(SettingValue::Texts)
        }
    }

    private fun textAllowed(spec: SettingSpec, value: String): Boolean {
        if (spec.path == LOCALE) {
            return value == SPAppConfigs.LOCALE_DEFAULT || localePattern.matches(value)
        }
        if (spec.path == SCRIPT_VARIANT) return value.isEmpty() || value in spec.allowed.orEmpty()
        if (spec.allowed != null) return value in spec.allowed
        return value.isNotBlank()
    }

    private fun convertV1(
        spec: SettingSpec,
        sourceKey: String,
        element: JsonElement
    ): JsonElement? {
        if (spec.path == STYLE && sourceKey == spec.v1Key) {
            val primitive = element as? JsonPrimitive ?: return null
            if (primitive.isString) return primitive
            return when (primitive.intOrNull) {
                1 -> JsonPrimitive(ReaderMode.VerseByVerse.value)
                2 -> JsonPrimitive(ReaderMode.Reading.value)
                else -> null
            }
        }

        if (spec.path == STYLE && sourceKey == styleKey) {
            val text = element.contentOrNull() ?: return null
            val value = when (text) {
                ReaderMode.VerseByVerse.name -> ReaderMode.VerseByVerse.value
                ReaderMode.Reading.name -> ReaderMode.Reading.value
                ReaderMode.Translation.name -> ReaderMode.Translation.value
                else -> text
            }
            return JsonPrimitive(value)
        }

        if (spec.path == AUDIO_MODE) {
            val primitive = element as? JsonPrimitive ?: return null
            if (!primitive.isString) return null
            return primitive
        }

        return element
    }

    private fun textsOrNull(value: JsonElement): List<String>? {
        val array = value as? JsonArray ?: return null

        if (array.isEmpty()) return emptyList()

        val ids = array.mapNotNull { element ->
            element.contentOrNull()?.takeIf { it.isNotBlank() }
        }.distinct()

        return ids.takeIf { it.isNotEmpty() }
    }

    private fun numberOrNull(value: JsonElement): Double? {
        val primitive = value as? JsonPrimitive ?: return null

        if (primitive.booleanOrNull != null && !primitive.isString) return null

        return primitive.content.toDoubleOrNull()
    }

    private fun textOrNull(value: JsonElement): String? {
        val primitive = value as? JsonPrimitive ?: return null

        if (!primitive.isString) return null

        return primitive.content
    }

    private fun JsonElement.contentOrNull(): String? = textOrNull(this)

    private fun JsonElement.booleanOrNull(): Boolean? {
        val primitive = this as? JsonPrimitive ?: return null

        if (!primitive.isString) return primitive.booleanOrNull

        return when (primitive.content.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toJsonObject(node: Map<String, Any?>): JsonObject {
        return buildJsonObject {
            for ((key, value) in node) {
                when (value) {
                    is MutableMap<*, *> -> put(key, toJsonObject(value as Map<String, Any?>))
                    is JsonElement -> put(key, value)
                }
            }
        }
    }
}
