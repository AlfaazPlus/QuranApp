package com.quranapp.android.utils.recommended

import android.content.Context
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.utils.univ.ResUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class Recommendation(
    val title: String,
    val description: String,
    val reference: RecommendationRef
)

sealed class RecommendationRef {
    data class Chapter(val number: Int) : RecommendationRef()
    data class Verses(val spec: String) : RecommendationRef()
}

object Recommended {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    fun getRecommendations(
        context: Context,
    ): List<Recommendation> {
        val doc = loadDocument(context) ?: return emptyList()
        val strings = loadStrings(context)

        return doc.rules
            .filter { it.enabled && it.matches(Instant.now(), doc.defaults?.timeZone) }
            .sortedByDescending { it.priority }
            .flatMap { rule ->
                rule.toRecommendations(strings)
            }.take(3)
    }

    private fun loadDocument(context: Context): RecommendedRulesDoc? = try {
        val text = ResUtils.readAssetsTextFile(context, "verses/recommended/rules.json")
        json.decodeFromString<RecommendedRulesDoc>(text)
    } catch (e: Exception) {
        null
    }

    private fun loadStrings(context: Context): Map<String, RuleCopy> {
        val text = appFallbackLanguageCodes().map { lang ->
            ResUtils.readAssetsTextFile(context, "verses/recommended/lang_$lang.json")
        }.firstOrNull { it.isNotBlank() } ?: ""

        if (text.isBlank()) return emptyMap()

        return try {
            val root = json.parseToJsonElement(text).jsonObject
            root.mapValues { json.decodeFromJsonElement<RuleCopy>(it.value) }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

@Serializable
private data class RecommendedRulesDoc(val defaults: Defaults? = null, val rules: List<Rule>)

@Serializable
private data class Defaults(val timeZone: String? = null)

@Serializable
private data class Rule(
    val id: String,
    val enabled: Boolean = true,
    val priority: Int,
    @SerialName("when") val schedule: Schedule,
    val ref: JsonElement,
)

@Serializable
private data class Schedule(val timeZone: String? = null, val clauses: List<Clause>)

@Serializable
private data class Clause(val weekdays: JsonElement? = null, val hourRanges: JsonElement? = null)

@Serializable
private data class RuleCopy(val title: String, val description: String?)

private fun Rule.matches(instant: Instant, defaultTz: String?): Boolean {
    val tz = schedule.timeZone ?: defaultTz ?: "local"
    val zoneId = if (tz.lowercase() == "local") ZoneId.systemDefault() else ZoneId.of(tz)
    val zdt = instant.atZone(zoneId)
    return schedule.clauses.any { it.matches(zdt) }
}

private fun Clause.matches(zdt: ZonedDateTime): Boolean {
    val dayOk = weekdays?.let { el ->
        when (el) {
            is JsonPrimitive -> el.content == "*" || el.intOrNull == zdt.dayOfWeek.value
            is JsonArray -> el.any { it.jsonPrimitive.intOrNull == zdt.dayOfWeek.value }
            else -> false
        }
    } ?: true

    val hourOk = hourRanges?.let { el ->
        when (el) {
            is JsonPrimitive -> el.content == "*"
            is JsonArray -> el.any { range ->
                val r = range.jsonArray

                if (r.size < 2) return@any false

                val start = r[0].jsonPrimitive.intOrNull ?: 0
                val end = r[1].jsonPrimitive.intOrNull ?: 0

                if (start <= end) {
                    zdt.hour in start until end
                } else {
                    zdt.hour >= start || zdt.hour < end
                }
            }

            else -> false
        }
    } ?: true

    return dayOk && hourOk
}

private fun Rule.toRecommendations(strings: Map<String, RuleCopy>): List<Recommendation> {
    val segments = when (val el = ref) {
        is JsonPrimitive -> el.content.split(',').map { it.trim() }
            .filter { it.isNotBlank() }
            .map { RawSegment(it) }

        is JsonObject -> el["segments"]?.jsonArray?.map { seg ->
            if (seg is JsonPrimitive) RawSegment(seg.content.trim(), null)
            else RawSegment(
                seg.jsonObject["verseRef"]?.jsonPrimitive?.content ?: "",
                seg.jsonObject["langKey"]?.jsonPrimitive?.content
            )
        } ?: emptyList()

        else -> emptyList()
    }

    if (segments.isEmpty()) return emptyList()

    return segments.map { segment ->
        val key = segment.langKey ?: id
        buildRec(strings, key, segment.ref.toRecRef())
    }
}

private data class RawSegment(val ref: String, val langKey: String? = null)

private fun String.toRecRef() = this.toIntOrNull()?.let { RecommendationRef.Chapter(it) }
    ?: RecommendationRef.Verses(this)

private fun buildRec(
    strings: Map<String, RuleCopy>,
    key: String,
    ref: RecommendationRef
): Recommendation {
    val copy = strings[key]

    return Recommendation(
        title = copy?.title ?: key,
        description = copy?.description.orEmpty(),
        reference = ref
    )
}
