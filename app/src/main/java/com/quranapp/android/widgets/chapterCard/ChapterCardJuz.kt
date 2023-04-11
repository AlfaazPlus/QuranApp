/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 21/2/2022.
 * All rights reserved.
 */
package com.quranapp.android.widgets.chapterCard

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R

class ChapterCardJuz(context: Context) : ChapterCard(context, false) {
    override fun createRightView(): View? {
        val v = AppCompatTextView(context)
        v.id = R.id.chapterCardVerseCount
        val p = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.marginStart = Dimen.dp2px(context, 10f)
        v.layoutParams = p
        v.gravity = Gravity.END
        v.setTextColor(ContextCompat.getColorStateList(context, R.color.color_reader_spinner_item_label))
        addView(v)
        return v
    }

    fun setVersesCount(versesInJuzPrefix: String, fromVerse: Int, toVerse: Int) {
        findViewById<TextView?>(R.id.chapterCardVerseCount)?.let {
            it.text = TextUtils.concat(
                SpannableString("$versesInJuzPrefix:\n").apply {
                    setSpan(
                        RelativeSizeSpan(0.75f),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                },
                SpannableString("$fromVerse-$toVerse").apply {
                    setSpan(
                        TypefaceSpan("sans-serif-medium"),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            )
        }
    }
}