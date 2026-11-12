package com.quranapp.android.utils.share

import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.safeFloat
import com.quranapp.android.api.safeInt
import com.quranapp.android.api.safeJsonArray
import com.quranapp.android.api.safeJsonObject
import com.quranapp.android.api.safeString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.math.max
import kotlin.math.min

data class VerseTemplateCatalog(
    val version: Int,
    val templates: List<VerseTemplate>,
)

data class VerseTemplate(
    val id: String,
    val name: String,
    val bgSrc: String,
    val layout: VerseTemplateLayout,
)

enum class TemplateHorizontalAlignment {
    START,
    CENTER,
    END;

    companion object {
        fun fromRaw(raw: String?): TemplateHorizontalAlignment = when (raw?.trim()?.lowercase()) {
            "start" -> START
            "end" -> END
            else -> CENTER
        }
    }
}

enum class TemplateVerticalAlignment {
    TOP,
    CENTER,
    BOTTOM;

    companion object {
        fun fromRaw(raw: String?): TemplateVerticalAlignment = when (raw?.trim()?.lowercase()) {
            "top" -> TOP
            "bottom" -> BOTTOM
            else -> CENTER
        }
    }
}

data class TemplateSafeArea(
    val top: Float,
    val right: Float,
    val bottom: Float,
    val left: Float,
)

data class SpacingRange(
    val min: Float,
    val max: Float,
)

data class TemplateSpacing(
    val arabicTranslation: SpacingRange,
    val translationReference: SpacingRange,
)

data class TextBlockStyle(
    val color: Int,
    val maxSize: Float,
    val minSize: Float,
    val lineHeight: Float,
    val minLineHeight: Float,
)

enum class ReferenceMode {
    FLOW,
    FREE;

    companion object {
        fun fromRaw(raw: String?): ReferenceMode = when (raw?.trim()?.lowercase()) {
            "free" -> FREE
            else -> FLOW
        }
    }
}

data class ReferenceBlockStyle(
    val mode: ReferenceMode,
    val color: Int,
    val maxSize: Float,
    val minSize: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

enum class LogoAnchor {
    BOTTOM_CENTER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_CENTER,
    TOP_LEFT,
    TOP_RIGHT;

    val isHorizontalCenter: Boolean
        get() = this == BOTTOM_CENTER || this == TOP_CENTER

    val isLeft: Boolean
        get() = this == BOTTOM_LEFT || this == TOP_LEFT

    val isTop: Boolean
        get() = this == TOP_CENTER || this == TOP_LEFT || this == TOP_RIGHT

    companion object {
        fun fromRaw(raw: String?): LogoAnchor = when (raw?.trim()?.lowercase()) {
            "bottom_left" -> BOTTOM_LEFT
            "bottom_right" -> BOTTOM_RIGHT
            "top_center" -> TOP_CENTER
            "top_left" -> TOP_LEFT
            "top_right" -> TOP_RIGHT
            else -> BOTTOM_CENTER
        }
    }
}

enum class LogoVariant {
    DARK,
    LIGHT;

    companion object {
        fun fromRaw(raw: String?): LogoVariant = when (raw?.trim()?.lowercase()) {
            "light" -> LIGHT
            else -> DARK
        }
    }
}

data class LogoBlockStyle(
    val anchor: LogoAnchor,
    val insetX: Float,
    val insetY: Float,
    val maximumWidth: Float,
    val opacity: Float,
    val variant: LogoVariant,
)

data class VerseTemplateLayout(
    val aspectRatioRaw: String,
    val aspectRatio: Float,
    val alignment: TemplateHorizontalAlignment,
    val verticalAlignment: TemplateVerticalAlignment,
    val safeArea: TemplateSafeArea,
    val spacing: TemplateSpacing,
    val arabic: TextBlockStyle,
    val translation: TextBlockStyle,
    val reference: ReferenceBlockStyle,
    val logo: LogoBlockStyle,
)

object VerseTemplateParser {
    private const val DEFAULT_ARABIC_COLOR = 0xFF193C37.toInt()
    private const val DEFAULT_TRANSLATION_COLOR = 0xFF344B50.toInt()
    private const val DEFAULT_REFERENCE_COLOR = 0xFF806344.toInt()

    fun parseCatalog(jsonString: String): VerseTemplateCatalog? {
        if (jsonString.isBlank()) return null

        val root = try {
            JsonHelper.json.parseToJsonElement(jsonString).jsonObject
        } catch (_: Exception) {
            return null
        }

        val version = root.safeInt("version", 1)
        val rawTemplates = root.safeJsonArray("templates") ?: return null

        val templates = ArrayList<VerseTemplate>(rawTemplates.size)

        for (element in rawTemplates) {
            val obj = try {
                element.jsonObject
            } catch (_: Exception) {
                continue
            }

            parseTemplate(obj)?.let(templates::add)
        }

        return VerseTemplateCatalog(
            version = version,
            templates = templates,
        )
    }

    fun parseTemplate(obj: JsonObject): VerseTemplate? {
        val id = obj.safeString("id")?.trim().orEmpty()
        val bgSrc = obj.safeString("bgSrc")?.trim().orEmpty()

        if (id.isEmpty() || bgSrc.isEmpty()) return null

        val name = obj.safeString("name")?.trim()?.ifEmpty { id } ?: id
        val layoutObj = obj.safeJsonObject("layout") ?: return null
        val layout = parseLayout(layoutObj) ?: return null

        return VerseTemplate(
            id = id,
            name = name,
            bgSrc = bgSrc,
            layout = layout,
        )
    }

    /**
     * Returns null when layout is empty or unauthored (e.g. `{}` or `{ "aspectRatio": "1:1" }`).
     */
    fun parseLayout(obj: JsonObject): VerseTemplateLayout? {
        if (obj.isEmpty()) return null

        val hasAuthoredBlocks = obj.containsKey("arabic") ||
            obj.containsKey("translation") ||
            obj.containsKey("reference")

        if (!hasAuthoredBlocks) return null

        val aspectRatioRaw = obj.safeString("aspectRatio", "1:1").trim().ifEmpty { "1:1" }
        val aspectRatio = parseAspectRatio(aspectRatioRaw)

        val safeArea = parseSafeArea(obj.safeJsonObject("safeArea"))

        val spacingObj = obj.safeJsonObject("spacing")
        val spacing = TemplateSpacing(
            arabicTranslation = parseSpacingRange(
                spacingObj,
                key = "arabicTranslation",
                defaultMin = 0.012f,
                defaultMax = 0.035f,
            ),
            translationReference = parseSpacingRange(
                spacingObj,
                key = "translationReference",
                defaultMin = 0.008f,
                defaultMax = 0.025f,
            ),
        )

        val arabic = parseTextBlockStyle(
            obj = obj.safeJsonObject("arabic"),
            defaultColor = DEFAULT_ARABIC_COLOR,
            defaultMaxSize = 0.058f,
            defaultMinSize = 0.032f,
            defaultLineHeight = 1.75f,
            defaultMinLineHeight = 1.42f,
        )

        val translation = parseTextBlockStyle(
            obj = obj.safeJsonObject("translation"),
            defaultColor = DEFAULT_TRANSLATION_COLOR,
            defaultMaxSize = 0.029f,
            defaultMinSize = 0.02f,
            defaultLineHeight = 1.4f,
            defaultMinLineHeight = 1.18f,
        )

        val reference = parseReferenceStyle(obj.safeJsonObject("reference"))
        val logo = parseLogoStyle(obj.safeJsonObject("logo"))

        return VerseTemplateLayout(
            aspectRatioRaw = aspectRatioRaw,
            aspectRatio = aspectRatio,
            alignment = TemplateHorizontalAlignment.fromRaw(obj.safeString("alignment")),
            verticalAlignment = TemplateVerticalAlignment.fromRaw(obj.safeString("verticalAlignment")),
            safeArea = safeArea,
            spacing = spacing,
            arabic = arabic,
            translation = translation,
            reference = reference,
            logo = logo,
        )
    }

    fun parseAspectRatio(raw: String): Float {
        val parts = raw.split(':')
        if (parts.size != 2) return 1f

        val w = parts[0].trim().toFloatOrNull() ?: return 1f
        val h = parts[1].trim().toFloatOrNull() ?: return 1f

        if (w <= 0f || h <= 0f || !w.isFinite() || !h.isFinite()) return 1f

        return (w / h).coerceIn(0.25f, 4f)
    }

    private fun parseSafeArea(obj: JsonObject?): TemplateSafeArea {
        var top = (obj.safeFloat("top", 0.12f)).coerceAtLeast(0f)
        var right = (obj.safeFloat("right", 0.09f)).coerceAtLeast(0f)
        var bottom = (obj.safeFloat("bottom", 0.16f)).coerceAtLeast(0f)
        var left = (obj.safeFloat("left", 0.09f)).coerceAtLeast(0f)

        val horizSum = left + right
        if (horizSum > 0.95f && horizSum > 0f) {
            val scale = 0.95f / horizSum
            left *= scale
            right *= scale
        }

        val vertSum = top + bottom
        if (vertSum > 0.95f && vertSum > 0f) {
            val scale = 0.95f / vertSum
            top *= scale
            bottom *= scale
        }

        return TemplateSafeArea(
            top = top,
            right = right,
            bottom = bottom,
            left = left,
        )
    }

    private fun parseSpacingRange(
        spacingObj: JsonObject?,
        key: String,
        defaultMin: Float,
        defaultMax: Float,
    ): SpacingRange {
        if (spacingObj == null) {
            return SpacingRange(min = defaultMin, max = defaultMax)
        }

        val element = spacingObj[key]
        if (element is JsonPrimitive) {
            val bare = element.floatOrNull
            if (bare != null && bare >= 0f) {
                val derivedMin = min(bare * 0.4f, defaultMax).coerceAtLeast(0f)
                val resolvedMax = max(bare, derivedMin)
                return SpacingRange(min = derivedMin, max = resolvedMax)
            }
        }

        val rangeObj = spacingObj.safeJsonObject(key)
            ?: return SpacingRange(min = defaultMin, max = defaultMax)

        val minVal = rangeObj.safeFloat("min", defaultMin).coerceAtLeast(0f)
        val maxVal = max(minVal, rangeObj.safeFloat("max", defaultMax).coerceAtLeast(0f))

        return SpacingRange(min = minVal, max = maxVal)
    }

    private fun parseTextBlockStyle(
        obj: JsonObject?,
        defaultColor: Int,
        defaultMaxSize: Float,
        defaultMinSize: Float,
        defaultLineHeight: Float,
        defaultMinLineHeight: Float,
    ): TextBlockStyle {
        val color = parseHexColor(obj.safeString("color"), defaultColor)
        val rawMaxSize = obj.safeFloat("maxSize", defaultMaxSize).coerceAtLeast(0.005f)

        val rawMinSize = obj.safeFloat("minSize")
            ?: obj.safeFloat("minimumScale")?.let { scale ->
                rawMaxSize * scale.coerceIn(0.3f, 1f)
            }
            ?: defaultMinSize

        val minSize = rawMinSize.coerceAtLeast(0.005f)
        val maxSize = max(rawMaxSize, minSize)

        val minLineHeight = obj.safeFloat("minLineHeight", defaultMinLineHeight).coerceAtLeast(0.8f)
        val lineHeight = max(minLineHeight, obj.safeFloat("lineHeight", defaultLineHeight))

        return TextBlockStyle(
            color = color,
            maxSize = maxSize,
            minSize = minSize,
            lineHeight = lineHeight,
            minLineHeight = minLineHeight,
        )
    }

    private fun parseReferenceStyle(obj: JsonObject?): ReferenceBlockStyle {
        val mode = ReferenceMode.fromRaw(obj.safeString("mode"))
        val color = parseHexColor(obj.safeString("color"), DEFAULT_REFERENCE_COLOR)
        val rawMaxSize = obj.safeFloat("maxSize", 0.021f).coerceAtLeast(0.005f)

        val rawMinSize = obj.safeFloat("minSize")
            ?: obj.safeFloat("minimumScale")?.let { scale ->
                rawMaxSize * scale.coerceIn(0.3f, 1f)
            }
            ?: 0.016f

        val minSize = rawMinSize.coerceAtLeast(0.005f)
        val maxSize = max(rawMaxSize, minSize)

        var left = 0.1f
        var top = 0.82f
        var right = 0.1f
        var bottom = 0.08f

        if (mode == ReferenceMode.FREE && obj != null) {
            val hasBoxInsets = obj.containsKey("left") ||
                obj.containsKey("top") ||
                obj.containsKey("right") ||
                obj.containsKey("bottom")

            if (hasBoxInsets) {
                left = obj.safeFloat("left", 0.1f)
                top = obj.safeFloat("top", 0.82f)
                right = obj.safeFloat("right", 0.1f)
                bottom = obj.safeFloat("bottom", 0.08f)
            } else if (obj.containsKey("x") || obj.containsKey("y")) {
                val x = obj.safeFloat("x", 0.5f)
                val y = obj.safeFloat("y", 0.9f)
                val maxWidth = obj.safeFloat("maxWidth", 0.8f).coerceIn(0.04f, 1f)
                val halfW = maxWidth * 0.5f
                val band = 0.035f

                left = x - halfW
                right = 1f - x - halfW
                top = y - band * 0.5f
                bottom = 1f - y - band * 0.5f
            }

            left = left.coerceAtLeast(0f)
            right = right.coerceAtLeast(0f)
            top = top.coerceAtLeast(0f)
            bottom = bottom.coerceAtLeast(0f)

            val horizSum = left + right
            if (horizSum > 0.96f && horizSum > 0f) {
                val scale = 0.96f / horizSum
                left *= scale
                right *= scale
            }

            val vertSum = top + bottom
            if (vertSum > 0.96f && vertSum > 0f) {
                val scale = 0.96f / vertSum
                top *= scale
                bottom *= scale
            }
        }

        return ReferenceBlockStyle(
            mode = mode,
            color = color,
            maxSize = maxSize,
            minSize = minSize,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
    }

    private fun parseLogoStyle(obj: JsonObject?): LogoBlockStyle {
        val anchor = LogoAnchor.fromRaw(obj.safeString("anchor"))
        val insetX = obj.safeFloat("insetX", 0.035f).coerceIn(0f, 0.48f)
        val insetY = (obj.safeFloat("insetY") ?: obj.safeFloat("bottomInset") ?: 0.035f)
            .coerceIn(0f, 0.48f)
        val maximumWidth = obj.safeFloat("maximumWidth", 0.25f).coerceIn(0.03f, 0.8f)
        val opacity = obj.safeFloat("opacity", 1f).coerceIn(0f, 1f)
        val variant = LogoVariant.fromRaw(obj.safeString("variant"))

        return LogoBlockStyle(
            anchor = anchor,
            insetX = insetX,
            insetY = insetY,
            maximumWidth = maximumWidth,
            opacity = opacity,
            variant = variant,
        )
    }

    fun parseHexColor(raw: String?, defaultColor: Int): Int {
        if (raw.isNullOrBlank()) return defaultColor

        val cleaned = raw.trim().removePrefix("#")

        val expanded = when (cleaned.length) {
            3 -> buildString(8) {
                append("FF")
                for (ch in cleaned) {
                    append(ch)
                    append(ch)
                }
            }

            6 -> "FF$cleaned"
            8 -> cleaned
            else -> return defaultColor
        }

        val parsed = expanded.toLongOrNull(16) ?: return defaultColor
        return parsed.toInt()
    }
}
