package com.quranapp.android.utils.quranScience

import android.content.Context
import com.quranapp.android.api.safeInt
import com.quranapp.android.api.safeJsonObject
import com.quranapp.android.api.safeString
import com.quranapp.android.api.toStringMap
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.core.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

fun getQuranScienceDrawableRes(id: String): Int {
    return when (id) {
        "astronomy" -> R.drawable.ic_science_astronomy
        "physics" -> R.drawable.ic_science_physics
        "geography" -> R.drawable.ic_science_geography
        "geology" -> R.drawable.ic_science_geology
        "oceanography" -> R.drawable.ic_science_oceanography
        "biology" -> R.drawable.ic_science_biology
        "botany" -> R.drawable.ic_science_botany
        "zoology" -> R.drawable.ic_science_zoology
        "medicine" -> R.drawable.ic_science_medicine
        "physiology" -> R.drawable.ic_science_physiology
        "embryology" -> R.drawable.ic_science_embryology
        "general_science" -> R.drawable.ic_science_general
        else -> R.drawable.ic_science_physics
    }
}

fun loadScienceItems(context: Context): List<QuranScienceItem> {
    val items = mutableListOf<QuranScienceItem>()

    context.assets.open("science/index.json").use { inputStream ->
        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = Json.parseToJsonElement(json).jsonArray

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
    }

    return items
}
