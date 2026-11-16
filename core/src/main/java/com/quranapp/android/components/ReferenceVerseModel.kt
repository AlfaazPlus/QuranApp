package com.quranapp.android.components

import android.os.Bundle
import androidx.annotation.DrawableRes

sealed class ReferenceThumbnail {
    data class RemoteUrl(val url: String) : ReferenceThumbnail()
    data class ResourceId(@DrawableRes val id: Int) : ReferenceThumbnail()
}

data class ReferenceVerseModel(
    val title: String,
    val desc: String?,
    val translSlugs: Set<String> = emptySet(),
    val chapters: Set<Int>,
    val verses: Set<String>,
    val thumbnail: ReferenceThumbnail? = null
) {

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("title", title)
            putString("desc", desc)
            putStringArrayList("translSlugs", ArrayList(translSlugs))
            putIntegerArrayList("chapters", ArrayList(chapters))
            putStringArrayList("verses", ArrayList(verses))

            when (thumbnail) {
                is ReferenceThumbnail.RemoteUrl -> {
                    putString("thumbnail_url", thumbnail.url)
                }

                is ReferenceThumbnail.ResourceId -> {
                    putInt("thumbnail_res_id", thumbnail.id)
                }

                else -> {
                    // noop
                }
            }
        }
    }

    companion object {
        fun fromBundle(bundle: Bundle?): ReferenceVerseModel? {
            if (bundle == null) {
                return null
            }

            val thumbnailRes = bundle.getInt("thumbnail_res_id", 0)
            val thumbnailUrl = bundle.getString("thumbnail_url")

            return ReferenceVerseModel(
                title = bundle.getString("title") ?: "",
                desc = bundle.getString("desc"),
                translSlugs = bundle.getStringArray("translSlugs")?.let { it.toSet() }
                    ?: emptySet(),
                chapters = bundle.getIntegerArrayList("chapters")?.let { it.toSet() } ?: emptySet(),
                verses = bundle.getStringArrayList("verses")?.let { it.toSet() } ?: emptySet(),
                thumbnail = when {
                    thumbnailRes != 0 -> ReferenceThumbnail.ResourceId(thumbnailRes)
                    thumbnailUrl != null -> ReferenceThumbnail.RemoteUrl(thumbnailUrl)
                    else -> null
                }
            )
        }
    }
}
