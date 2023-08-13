package com.quranapp.android.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.peacedesign.android.utils.span.LineHeightSpan2
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.components.quran.QuranProphet
import com.quranapp.android.databinding.LytQuranProphetItemBinding
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.reader.factory.ReaderFactory.prepareReferenceVerseIntent
import java.text.MessageFormat
import java.util.LinkedList

class ADPProphets(ctx: Context, private val itemWidth: Int, private val limit: Int) :
    RecyclerView.Adapter<ADPProphets.VHProphet>() {
    private val txtSize = ctx.getDimenPx(R.dimen.dmnCommonSize2)
    private val txtColor2 = ContextCompat.getColorStateList(ctx, R.color.colorText2)
    var prophets: List<QuranProphet.Prophet> = LinkedList()

    override fun getItemCount(): Int {
        return if (limit > 0) prophets.size.coerceAtMost(limit) else prophets.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHProphet {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LytQuranProphetItemBinding.inflate(inflater, parent, false)
        return VHProphet(binding)
    }

    override fun onBindViewHolder(holder: VHProphet, position: Int) {
        holder.bind(prophets[position])
    }

    private fun prepareTexts(title: String, subTitle: CharSequence, inChapters: String?): CharSequence {
        val titleSS = SpannableString(title)
        val titleTASpan = TextAppearanceSpan("sans-serif", Typeface.BOLD, txtSize, null, null)
        titleSS.setSpan(titleTASpan, 0, titleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val subTitleSS = SpannableString(subTitle)
        val subTitleTASpan = TextAppearanceSpan("sans-serif", Typeface.NORMAL, txtSize, null, null)
        subTitleSS.setSpan(subTitleTASpan, 0, subTitleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        subTitleSS.setSpan(LineHeightSpan2(20, false, true), 0, subTitleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val chaptersSS = SpannableString(inChapters)
        val inChaptersTASpan = TextAppearanceSpan(
            "sans-serif-light", Typeface.NORMAL, txtSize,
            txtColor2,
            null
        )
        chaptersSS.setSpan(inChaptersTASpan, 0, chaptersSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return TextUtils.concat(titleSS, "\n", subTitleSS, "\n", chaptersSS)
    }

    inner class VHProphet(private val binding: LytQuranProphetItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.let {
                it.elevation = Dimen.dp2px(it.context, 4f).toFloat()
                it.layoutParams = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

                if (itemWidth > 0) {
                    it.minimumHeight = Dimen.dp2px(it.context, 110f)
                    it.setBackgroundResource(R.drawable.dr_bg_chapter_card_bordered)
                } else {
                    it.setBackgroundResource(R.drawable.dr_bg_chapter_card)
                }
            }

        }

        fun bind(prophet: QuranProphet.Prophet) {
            setupActions(prophet)

            binding.icon.setImageDrawable(binding.root.context.drawable(prophet.iconRes))

            val name = MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)
            val nameEng = "English : " + prophet.nameEn
            binding.text.text = prepareTexts(name, nameEng, prophet.inChapters)
        }

        private fun setupActions(prophet: QuranProphet.Prophet) {
            val ctx = binding.root.context

            val title = ctx.getString(
                R.string.strMsgReferenceInQuran,
                MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)
            )
            val desc = ctx.getString(R.string.strMsgReferenceFoundPlaces, title, prophet.verses.size)
            val intent = prepareReferenceVerseIntent(
                true,
                title,
                desc,
                arrayOf(),
                prophet.chapters,
                prophet.verses
            )

            intent.setClass(ctx, ActivityReference::class.java)
            binding.root.setOnClickListener { ctx.startActivity(intent) }
        }
    }
}