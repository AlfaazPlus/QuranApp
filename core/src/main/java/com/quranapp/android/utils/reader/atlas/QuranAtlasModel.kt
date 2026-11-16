package com.quranapp.android.utils.reader.atlas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Generator / meta.json `kind` for page-split glyph layouts (see layout document `page`). */
const val KIND_PAGE_GLYPH_ATLAS = "page_glyph_atlas"

/** Generator / meta.json `kind` for word-global glyph layouts. */
const val KIND_WORD_GLYPH_ATLAS = "word_glyph_atlas"

fun AtlasMetaRoot.isPageScopedGlyphAtlas(): Boolean = kind == KIND_PAGE_GLYPH_ATLAS

@Serializable
data class AtlasMetaRoot(
    @SerialName("schema_version")
    val schemaVersion: Int? = null,
    @SerialName("kind")
    val kind: String? = null,
    @SerialName("font")
    val font: AtlasFontMeta,
    @SerialName("base_ppem")
    val basePpem: Int = 32,
    @SerialName("layout")
    val layout: AtlasMetaLayoutRef,
    @SerialName("sizes")
    val sizes: List<AtlasSizeEntry> = emptyList(),
    @SerialName("bundle")
    val bundle: AtlasBundleRef? = null,
)

@Serializable
data class AtlasMetaLayoutRef(
    val kind: String,
    val file: String,
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
    @SerialName("height_fu")
    val heightFu: Int? = null,
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
    val meta: String = "",
    @SerialName("textures")
    val textures: List<AtlasTextureSlice> = emptyList(),
)

/** One entry in `atlas.json` `textures[]` or `meta.sizes[].textures[]` (generator shape). */
@Serializable
data class AtlasTextureSlice(
    val index: Int,
    val width: Int,
    val height: Int,
    val padding: Int = 0,
    val channels: String = "L",
    val format: String = "png",
    val image: String,
)

@Serializable
data class AtlasBundleRef(
    @SerialName("scale")
    val scale: String,
    @SerialName("ppem")
    val ppem: Int,
)

/** Root of zip `atlas.json` (generator schema). Persisted as `layer_json`. */
@Serializable
data class AtlasLayerJson(
    @SerialName("schema_version")
    val schemaVersion: Int? = null,
    @SerialName("ppem")
    val ppem: Int,
    @SerialName("textures")
    val textures: List<AtlasTextureSlice>,
    @SerialName("glyphs")
    val glyphs: Map<String, AtlasGlyphJson> = emptyMap(),
    @SerialName("label")
    val label: String? = null,
    @SerialName("scale")
    val scale: Int? = null,
)

@Serializable
data class AtlasGlyphJson(
    @SerialName("atlas")
    val textureIndex: Int = 0,
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

/** Root of zip layout JSON (`layout.json` or `meta.layout.file`). */
@Serializable
data class AtlasLayoutRoot(
    @SerialName("schema_version")
    val schemaVersion: Int? = null,
    @SerialName("documents")
    val documents: Map<String, AtlasLayoutDocument>,
)

@Serializable
data class AtlasLayoutDocument(
    @SerialName("text")
    val text: String,
    @SerialName("glyphs")
    val glyphs: List<AtlasGlyphPlacement> = emptyList(),
    @SerialName("page")
    val page: Int? = null,
)

val atlasJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

val atlasPlacementListSerializer = ListSerializer(AtlasGlyphPlacement.serializer())
