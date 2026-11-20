package com.quranapp.android.utils.reader.recommended

import com.quranapp.android.locale.appFallbackLanguageCodes
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.miscellenous.DateUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
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
import kotlin.time.Instant

data class Recommendation(
    val title: String,
    val description: String,
    val reference: RecommendationRef,
    val notificationDedupeEpochDay: Long,
)

sealed class RecommendationRef {
    data class Chapter(val number: Int) : RecommendationRef()
    data class Verses(val spec: String) : RecommendationRef()
}

object Recommended {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    suspend fun getRecommendations(): List<Recommendation> {
        val doc = loadDocument() ?: return emptyList()
        val strings = loadStrings()

        val now = DateUtils.now()

        return doc.rules
            .filter { it.enabled && it.matches(now, doc.defaults?.timeZone) }
            .sortedByDescending { it.priority }
            .flatMap { rule ->
                val dedupeEpochDay =
                    rule.notificationDedupeEpochDay(now, doc.defaults?.timeZone)

                rule.toRecommendations(strings, dedupeEpochDay)
            }
            .take(3)
    }

    private suspend fun loadDocument(): RecommendedRulesDoc? = try {
        json.decodeFromString<RecommendedRulesDoc>(
            Platform.assets.readAssetText("verses/recommended/rules.json"),
        )
    } catch (e: Exception) {
        null
    }

    private suspend fun loadStrings(): Map<String, RuleCopy> {
        val text = appFallbackLanguageCodes().firstNotNullOfOrNull { lang ->
            Platform.assets.readAssetTextOrNull("verses/recommended/lang_$lang.json")
        } ?: ""

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
private data class RecommendedRulesDoc(
    val defaults: Defaults? = null,
    val rules: List<Rule>,
)

@Serializable
private data class Defaults(
    val timeZone: String? = null,
)

@Serializable
private data class Rule(
    val id: String,
    val enabled: Boolean = true,
    val priority: Int,
    @SerialName("when") val schedule: Schedule,
    val ref: JsonElement,
)

@Serializable
private data class Schedule(
    val timeZone: String? = null,
    val clauses: List<Clause>,
)

@Serializable
private data class Clause(
    val weekdays: JsonElement? = null,
    val hourRanges: JsonElement? = null,
) {
    val hrRanges: HourRanges by lazy {
        when (hourRanges) {
            is JsonPrimitive ->
                if (hourRanges.isString && hourRanges.content == "*") {
                    HourRanges.Wildcard
                } else {
                    HourRanges.Listed(emptyList())
                }

            is JsonArray ->
                HourRanges.Listed(
                    hourRanges.mapNotNull { item ->
                        val r = item as? JsonArray ?: return@mapNotNull null
                        if (r.size < 2) return@mapNotNull null

                        HourRange(
                            start = r[0].jsonPrimitive.intOrNull ?: 0,
                            end = r[1].jsonPrimitive.intOrNull ?: 0,
                        )
                    },
                )

            else -> HourRanges.Listed(emptyList())
        }
    }
}

private sealed class HourRanges {
    data object Wildcard : HourRanges()

    data class Listed(val ranges: List<HourRange>) : HourRanges()
}

private data class HourRange(val start: Int, val end: Int) {
    fun containsHour(hour: Int): Boolean =
        if (start <= end) hour in start until end
        else hour >= start || hour < end

    fun dedupeEpochDay(localDateTime: LocalDateTime): Long {
        val date = if (start > end && localDateTime.hour < end) {
            localDateTime.date.minus(DatePeriod(days = 1))
        } else {
            localDateTime.date
        }

        return date.toEpochDays()
    }
}

@Serializable
private data class RuleCopy(val title: String, val description: String?)

private fun resolveTimeZone(timeZone: String?): TimeZone {
    return if (timeZone.isNullOrBlank() ||
        timeZone.equals("local", ignoreCase = true)
    ) {
        TimeZone.currentSystemDefault()
    } else {
        TimeZone.of(timeZone)
    }
}

private fun Rule.matches(
    instant: Instant,
    defaultTz: String?,
): Boolean {
    val tz = resolveTimeZone(schedule.timeZone ?: defaultTz)
    val dateTime = instant.toLocalDateTime(tz)

    return schedule.clauses.any { it.matches(dateTime) }
}

private fun Rule.notificationDedupeEpochDay(instant: Instant, defaultTz: String?): Long {
    val tz = resolveTimeZone(schedule.timeZone ?: defaultTz)
    val dateTime = instant.toLocalDateTime(tz)

    val clause = schedule.clauses.firstOrNull {
        it.matches(dateTime)
    } ?: return dateTime.date.toEpochDays()

    // For wrapping hour ranges, hours after midnight belong
    // to the calendar day when the range started.
    return when (val hr = clause.hrRanges) {
        HourRanges.Wildcard ->
            dateTime.date.toEpochDays()

        is HourRanges.Listed -> {
            hr.ranges.firstOrNull {
                it.containsHour(dateTime.hour)
            }?.dedupeEpochDay(dateTime)
                ?: dateTime.date.toEpochDays()
        }
    }
}

private fun Clause.matches(dateTime: LocalDateTime): Boolean {
    val dayOk = weekdays?.let { el ->
        when (el) {
            is JsonPrimitive ->
                el.content == "*" ||
                        el.intOrNull == dateTime.dayOfWeek.isoDayNumber

            is JsonArray ->
                el.any {
                    it.jsonPrimitive.intOrNull ==
                            dateTime.dayOfWeek.isoDayNumber
                }

            else -> false
        }
    } ?: true

    val hourOk = when (val hr = hrRanges) {
        HourRanges.Wildcard -> true

        is HourRanges.Listed ->
            hr.ranges.any { it.containsHour(dateTime.hour) }
    }

    return dayOk && hourOk
}

private fun Rule.toRecommendations(
    strings: Map<String, RuleCopy>,
    notificationDedupeEpochDay: Long
): List<Recommendation> {
    val segments = when (val el = ref) {
        is JsonPrimitive ->
            el.content.split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { RawSegment(it) }

        is JsonObject ->
            el["segments"]?.jsonArray?.map { seg ->
                if (seg is JsonPrimitive) {
                    RawSegment(seg.content.trim(), null)
                } else {
                    RawSegment(
                        seg.jsonObject["verseRef"]?.jsonPrimitive?.content ?: "",
                        seg.jsonObject["langKey"]?.jsonPrimitive?.content,
                    )
                }
            } ?: emptyList()

        else -> emptyList()
    }

    if (segments.isEmpty()) return emptyList()

    return segments.map { segment ->
        val key = segment.langKey ?: id

        val copy = strings[key]

        Recommendation(
            title = copy?.title ?: key,
            description = copy?.description.orEmpty(),
            reference = segment.ref.toIntOrNull()?.let {
                RecommendationRef.Chapter(it)
            } ?: RecommendationRef.Verses(segment.ref),
            notificationDedupeEpochDay = notificationDedupeEpochDay,
        )
    }
}

private data class RawSegment(val ref: String, val langKey: String? = null)
