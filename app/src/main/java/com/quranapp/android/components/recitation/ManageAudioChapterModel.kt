package com.quranapp.android.components.recitation

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.quranapp.android.R
import com.quranapp.android.api.models.recitation.RecitationInfoBaseModel
import com.quranapp.android.components.quran.QuranMeta.ChapterMeta
import com.quranapp.android.utils.univ.StringUtils

class ManageAudioChapterModel(
    val chapterMeta: ChapterMeta,
    val reciterModel: RecitationInfoBaseModel
) : java.io.Serializable {
    @Transient
    var title: CharSequence = ""
    var downloaded: Boolean = false
    var downloading: Boolean = false
    var progress = -1

    fun prepareTitle(ctx: Context) {
        val title = SpannableString(chapterMeta.name)
        title.setSpan(StyleSpan(Typeface.BOLD), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        var subtitle = ctx.getString(R.string.strLabelVerseCountWithColon, chapterMeta.verseCount)
        if (downloading) {
            subtitle += " ${StringUtils.VERTICAL_BAR} ${ctx.getString(R.string.textDownloading)}"
            if (progress > 0) subtitle += " $progress%"
        } else if (downloaded) {
            subtitle += " ${StringUtils.VERTICAL_BAR} ${ctx.getString(R.string.textDownloaded)}"
        }

        val subtitleSS = SpannableString(subtitle)
        subtitleSS.setSpan(RelativeSizeSpan(.85F), 0, subtitleSS.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        this.title = TextUtils.concat(title, "\n", subtitleSS)
    }
}