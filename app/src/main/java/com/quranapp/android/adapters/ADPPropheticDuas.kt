package com.quranapp.android.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.peacedesign.android.utils.span.LineHeightSpan2
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.components.quran.QuranProphet
import com.quranapp.android.components.quran.QuranPropheticDua
import com.quranapp.android.databinding.LytQuranProphetDuaItemBinding
import com.quranapp.android.databinding.LytQuranProphetItemBinding
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.reader.factory.ReaderFactory.prepareReferenceVerseIntent
import java.text.MessageFormat
import java.util.LinkedList

class ADPPropheticDuas(ctx: Context) :
    RecyclerView.Adapter<ADPPropheticDuas.VHProphet>() {
    private val txtSize = ctx.getDimenPx(R.dimen.dmnCommonSize)
    private val txtSize2 = ctx.getDimenPx(R.dimen.dmnCommonSize2)
    private val txtColor2 = ContextCompat.getColorStateList(ctx, R.color.colorText2)
    var prophets: List<QuranPropheticDua.Prophet> = LinkedList()

    override fun getItemCount(): Int {
        return prophets.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHProphet {
        val inflater = LayoutInflater.from(parent.context)
        return VHProphet(LytQuranProphetDuaItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: VHProphet, position: Int) {
        holder.bind(prophets[position])
    }

    private fun prepareTexts(title: String, inChapters: String?): CharSequence {
        val ssb = SpannableStringBuilder()

        val titleSS = SpannableString(title)
        val titleTASpan = TextAppearanceSpan("sans-serif", Typeface.BOLD, txtSize, null, null)
        titleSS.setSpan(titleTASpan, 0, titleSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        ssb.append(titleSS)

        if (inChapters != null) {
            val chaptersSS = SpannableString(inChapters)
            val inChaptersTASpan = TextAppearanceSpan(
                "sans-serif-light", Typeface.NORMAL, txtSize2,
                txtColor2,
                null
            )
            chaptersSS.setSpan(inChaptersTASpan, 0, chaptersSS.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            ssb.append("\n").append(chaptersSS)
        }

        return ssb
    }

    inner class VHProphet(private val binding: LytQuranProphetDuaItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.let {
                it.elevation = Dimen.dp2px(it.context, 4f).toFloat()
                it.setBackgroundResource(R.drawable.dr_bg_chapter_card)
            }
        }

        fun bind(prophet: QuranPropheticDua.Prophet) {
            setupActions(prophet)

            val ctx = binding.root.context
            binding.icon.setImageDrawable(ctx.drawable(prophet.iconRes))

            val name = MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)

            binding.text.text = prepareTexts(
                ctx.getString(R.string.strMsgDuaOf, name),
                prophet.inChapters
            )
        }

        private fun setupActions(prophet: QuranPropheticDua.Prophet) {
            val ctx = binding.root.context

            val title = ctx.getString(
                R.string.strMsgPropheticDuaInQuran,
                MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)
            )

            val intent = prepareReferenceVerseIntent(
                true,
                title,
                ctx.getString(R.string.strMsgReferenceDuas),
                arrayOf(),
                prophet.chapters,
                prophet.verses
            )

            intent.setClass(ctx, ActivityReference::class.java)
            binding.root.setOnClickListener { ctx.startActivity(intent) }
        }
    }
}