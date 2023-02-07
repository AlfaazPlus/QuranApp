/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.components.recitation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecitationModel(
    val slug: String,
    val reciter: String,
    val style: String,
    @SerialName("url-host") var urlHost: String,
    @SerialName("url-path") val urlPath: String,
    var isChecked: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (other !is RecitationModel) {
            return false
        }
        return other.slug == slug
    }

    override fun toString(): String {
        return "slug:$slug, reciter:$reciter, style:$style"
    }

    override fun hashCode(): Int {
        return slug.hashCode()
    }

}