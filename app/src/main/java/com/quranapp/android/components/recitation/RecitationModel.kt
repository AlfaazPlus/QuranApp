/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.components.recitation

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

class RecitationModel(val id: Int, val slug: String) : Serializable {
    var reciter = ""
    var style = ""
    var urlHost = ""
    var urlPath = ""
    var isChecked = false

    @Throws(JSONException::class)
    fun toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("slug", slug)
        jsonObject.put("reciter", reciter)
        jsonObject.put("style", style)
        jsonObject.put("url-host", urlHost)
        jsonObject.put("url-path", urlPath)
        return jsonObject.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RecitationModel) {
            return false
        }
        return other.id == id && other.slug == slug
    }

    override fun toString(): String {
        return "id:$id, slug:$slug, reciter:$reciter, style:$style"
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + slug.hashCode()
        return result
    }

}