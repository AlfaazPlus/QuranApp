/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 21/2/2022.
 * All rights reserved.
 */
package com.quranapp.android.widgets.chapterCard

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.TextAppearanceSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.extensions.setTextSizePx
import com.quranapp.android.utils.extensions.updateMarginHorizontal
import com.quranapp.android.utils.extensions.updatePaddings
import com.quranapp.android.utils.sharedPrefs.SPFavouriteChapters
import com.quranapp.android.views.reader.ChapterIcon
import java.util.Locale

open class ChapterCard @JvmOverloads constructor(
    context: Context,
    private val showFavouriteIcon: Boolean = false
) : ConstraintLayout(context) {
    var chapterNumber = 0
        set(value) {
            field = value
            updateChapterNumber(value)
        }
    var onFavoriteButtonClickListener: Runnable? = null

    init {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val pad = context.dp2px(10f)
        setPaddingRelative(pad, pad, context.dp2px(5f), pad)

        val serialView = createSerialView()
        val nameView = createNameView()
        val rightView = createRightView()
        val favIcon = createFavIcon()

        (serialView.layoutParams as LayoutParams).apply {
            topToTop = LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
            startToStart = LayoutParams.PARENT_ID
            endToStart = nameView.id
            serialView.layoutParams = this
        }

        (nameView.layoutParams as LayoutParams).apply {
            horizontalWeight = 1f

            topToTop = LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
            startToEnd = serialView.id

            if (rightView != null) endToStart = rightView.id
            else endToEnd = LayoutParams.PARENT_ID

            nameView.layoutParams = this
        }

        if (rightView != null) {
            (rightView.layoutParams as LayoutParams).apply {
                if (favIcon == null) marginEnd = context.dp2px(5f)

                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                startToEnd = nameView.id

                if (favIcon != null) endToStart = favIcon.id
                else endToEnd = LayoutParams.PARENT_ID

                rightView.layoutParams = this
            }
        }

        if (favIcon != null) {
            (favIcon.layoutParams as LayoutParams).apply {
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                startToEnd = rightView!!.id
                endToEnd = LayoutParams.PARENT_ID

                favIcon.layoutParams = this
            }
        }
    }

    private fun createSerialView(): View {
        val size = context.dp2px(35f)

        val serialView = AppCompatTextView(context).apply {
            id = R.id.chapterCardSerial
            setBackgroundResource(R.drawable.dr_bg_reader_chapter_serial)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
            maxLines = 1
            setTextColor(ContextCompat.getColorStateList(context, R.color.color_reader_spinner_item_label))
            setTextSizePx(R.dimen.dmnCommonSize2)

            layoutParams = LayoutParams(size, size)
        }

        addView(serialView)

        return serialView
    }

    private fun createNameView(): View {
        val nameView = AppCompatTextView(context).apply {
            textAlignment = TEXT_ALIGNMENT_VIEW_START
            id = R.id.chapterCardName

            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                updateMarginHorizontal(context.dp2px(10f))
            }
        }

        addView(nameView)

        return nameView
    }

    protected open fun createRightView(): View? {
        val chapIcon = ChapterIcon(context).apply {
            id = R.id.chapterCardIcon
            setTextSizePx(R.dimen.dmnChapterIcon2)
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setTextColor(ContextCompat.getColorStateList(context, R.color.color_reader_spinner_item_label))
        }

        addView(chapIcon)

        return chapIcon
    }

    protected open fun createFavIcon(): View? {
        if (!showFavouriteIcon) return null

        val dimen = context.getDimenPx(R.dimen.dmnActionButtonSmall)

        val view = AppCompatImageView(context).apply {
            id = R.id.chapterCardFavIcon
            setBackgroundResource(R.drawable.dr_bg_hover_round)
            updatePaddings(context.dp2px(5f))

            layoutParams = LayoutParams(dimen, dimen).apply {
                leftMargin = context.dp2px(5f)
            }

            setOnClickListener {
                if (chapterNumber > 0 && onFavoriteButtonClickListener != null) {
                    onFavoriteButtonClickListener!!.run()
                }
            }

            updateFavIcon()
        }
        addView(view)
        return view
    }

    private fun updateFavIcon() {
        val isAdded = SPFavouriteChapters.isAddedToFavorites(context, chapterNumber)

        findViewById<ImageView>(R.id.chapterCardFavIcon)?.let {
            it.setImageResource(
                if (isAdded) R.drawable.icon_star_filled
                else R.drawable.icon_star_outlined
            )
            it.setColorFilter(context.color(if (isAdded) R.color.colorPrimary else R.color.colorIcon))
        }
    }

    private fun updateChapterNumber(chapterNo: Int) {
        findViewById<TextView>(R.id.chapterCardSerial)?.let {
            it.text = String.format(Locale.getDefault(), "%d", chapterNo)
        }

        findViewById<ChapterIcon?>(R.id.chapterCardIcon)?.setChapterNumber(chapterNo)
        updateFavIcon()
    }

    fun setName(chapterName: String, chapterTransl: String) {
        findViewById<TextView?>(R.id.chapterCardName)?.let {
            it.text = makeName(context, chapterName, chapterTransl)
        }
    }

    private fun makeName(ctx: Context, chapterName: String, chapterTransl: String?): CharSequence {
        val nameSS = SpannableString(chapterName)
        setSpan(
            nameSS,
            TextAppearanceSpan(
                "sans-serif",
                Typeface.BOLD,
                -1,
                ContextCompat.getColorStateList(ctx, R.color.color_reader_spinner_item_label),
                null
            )
        )

        return SpannableStringBuilder().apply {
            append(nameSS)

            if (!chapterTransl.isNullOrEmpty()) {
                val translSS = SpannableString(chapterTransl)
                setSpan(
                    translSS,
                    TextAppearanceSpan(
                        "sans-serif",
                        Typeface.NORMAL,
                        ctx.getDimenPx(R.dimen.dmnCommonSize2),
                        ContextCompat.getColorStateList(ctx, R.color.color_reader_spinner_item_label2),
                        null
                    )
                )
                append("\n").append(translSS)
            }
        }
    }

    private fun setSpan(ss: SpannableString, obj: Any) {
        ss.setSpan(obj, 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}