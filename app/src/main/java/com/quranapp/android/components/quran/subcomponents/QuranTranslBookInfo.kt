/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 6/6/2022.
 * All rights reserved.
 */

package com.quranapp.android.components.quran.subcomponents

import com.quranapp.android.utils.univ.StringUtils
import java.io.Serializable

/**
 * Holds information about a translation book.
 * e.g., information about Sahih International
 * */

class QuranTranslBookInfo(val slug: String) : Serializable {
    companion object {
        const val DISPLAY_NAME_DEFAULT_WITHOUT_HYPHEN = false
    }

    var bookName = ""
    var authorName = ""
    var displayName = ""
    var langName = ""
    var langCode = ""
    var lastUpdated: Long = -1
    var downloadPath = ""

    val isUrdu get() = langCode == "ur"

    fun getDisplayName(withoutHyphen: Boolean = DISPLAY_NAME_DEFAULT_WITHOUT_HYPHEN): String {
        return if (withoutHyphen) displayName else "${StringUtils.HYPHEN} $displayName"
    }

    fun getDisplayNameWithHyphen(): String {
        return getDisplayName(false)
    }

    override fun toString(): String {
        return "QuranTranslBookInfo(slug='$slug', langCode='$langCode'"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is QuranTranslBookInfo) return false

        if (slug != other.slug) return false

        return true
    }

    override fun hashCode(): Int {
        var result = slug.hashCode()
        result = 31 * result + bookName.hashCode()
        result = 31 * result + authorName.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + langName.hashCode()
        result = 31 * result + langCode.hashCode()
        result = 31 * result + isUrdu.hashCode()
        return result
    }
}
