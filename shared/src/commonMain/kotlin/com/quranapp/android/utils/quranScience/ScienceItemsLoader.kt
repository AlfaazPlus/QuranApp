package com.quranapp.android.utils.quranScience

import com.quranapp.android.api.safeInt
import com.quranapp.android.api.safeJsonObject
import com.quranapp.android.api.safeString
import com.quranapp.android.api.toStringMap
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.ic_science_astronomy
import com.quranapp.android.generated.resources.ic_science_biology
import com.quranapp.android.generated.resources.ic_science_botany
import com.quranapp.android.generated.resources.ic_science_embryology
import com.quranapp.android.generated.resources.ic_science_general
import com.quranapp.android.generated.resources.ic_science_geography
import com.quranapp.android.generated.resources.ic_science_geology
import com.quranapp.android.generated.resources.ic_science_medicine
import com.quranapp.android.generated.resources.ic_science_oceanography
import com.quranapp.android.generated.resources.ic_science_physics
import com.quranapp.android.generated.resources.ic_science_physiology
import com.quranapp.android.generated.resources.ic_science_zoology
import com.quranapp.android.models.QuranScienceItem
import com.quranapp.android.platform.Platform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.DrawableResource

fun getQuranScienceDrawableRes(id: String): DrawableResource {
    return when (id) {
        "astronomy" -> Res.drawable.ic_science_astronomy
        "physics" -> Res.drawable.ic_science_physics
        "geography" -> Res.drawable.ic_science_geography
        "geology" -> Res.drawable.ic_science_geology
        "oceanography" -> Res.drawable.ic_science_oceanography
        "biology" -> Res.drawable.ic_science_biology
        "botany" -> Res.drawable.ic_science_botany
        "zoology" -> Res.drawable.ic_science_zoology
        "medicine" -> Res.drawable.ic_science_medicine
        "physiology" -> Res.drawable.ic_science_physiology
        "embryology" -> Res.drawable.ic_science_embryology
        "general_science" -> Res.drawable.ic_science_general
        else -> Res.drawable.ic_science_physics
    }
}

suspend fun loadScienceItems(): List<QuranScienceItem> {
    val items = mutableListOf<QuranScienceItem>()
    val jsonArray = Json.parseToJsonElement(
        Platform.assets.readAssetText("science/index.json"),
    ).jsonArray

    jsonArray.forEach {
        val item = it.jsonObject

        items.add(
            QuranScienceItem(
                item.safeString("title", ""),
                item.safeInt("referencesCount", 0),
                item.safeString("path", ""),
                getQuranScienceDrawableRes(item.safeString("id", "")),
                item.safeJsonObject("translations")?.toStringMap() ?: mapOf()
            )
        )
    }

    return items
}
