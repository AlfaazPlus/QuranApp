package com.quranapp.android.utils.reader.atlas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class AtlasMetaRoot(
    @SerialName("font")
    val font: AtlasFontMeta,
    @SerialName("base_ppem")
    val basePpem: Int = 32,
    @SerialName("sizes")
    val sizes: List<AtlasSizeEntry> = emptyList(),
    @SerialName("bundle")
    val bundle: AtlasBundleRef? = null,
)

@Serializable
data class AtlasFontMeta(
    @SerialName("units_per_em")
    val unitsPerEm: Int = 1000,
    @SerialName("ascender_fu")
    val ascenderFu: Int = 0,
    @SerialName("descender_fu")
    val descenderFu: Int = 0,
    @SerialName("line_gap_fu")
    val lineGapFu: Int = 0,
)

@Serializable
data class AtlasSizeEntry(
    @SerialName("label")
    val label: String,
    @SerialName("scale")
    val scale: Int = 1,
    @SerialName("ppem")
    val ppem: Int,
    @SerialName("atlas")
    val atlas: String,
    @SerialName("meta")
    val meta: String,
)

@Serializable
data class AtlasBundleRef(
    @SerialName("scale")
    val scale: String,
    @SerialName("ppem")
    val ppem: Int,
)

@Serializable
data class AtlasLayerJson(
    @SerialName("ppem")
    val ppem: Int,
    @SerialName("atlas")
    val atlas: AtlasTextureJson,
    @SerialName("glyphs")
    val glyphs: Map<String, AtlasGlyphJson> = emptyMap(),
)

@Serializable
data class AtlasTextureJson(
    @SerialName("width")
    val width: Int,
    @SerialName("height")
    val height: Int,
    @SerialName("padding")
    val padding: Int = 0,
    @SerialName("channels")
    val channels: String = "L",
)

@Serializable
data class AtlasGlyphJson(
    @SerialName("x")
    val x: Int,
    @SerialName("y")
    val y: Int,
    @SerialName("w")
    val w: Int,
    @SerialName("h")
    val h: Int,
    @SerialName("bearing_x")
    val bearingX: Int = 0,
    @SerialName("bearing_y")
    val bearingY: Int = 0,
    @SerialName("advance")
    val advance: Double = 0.0,
)

@Serializable
data class AtlasGlyphPlacement(
    @SerialName("g")
    val gid: Int,
    @SerialName("xa")
    val xAdvanceFu: Double = 0.0,
    @SerialName("ya")
    val yAdvanceFu: Double = 0.0,
    @SerialName("xo")
    val xOffsetFu: Double = 0.0,
    @SerialName("yo")
    val yOffsetFu: Double = 0.0,
)


val atlasJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

val atlasPlacementListSerializer = ListSerializer(AtlasGlyphPlacement.serializer())
