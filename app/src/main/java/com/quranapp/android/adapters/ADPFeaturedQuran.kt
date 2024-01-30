package com.quranapp.android.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.adapters.ADPFeaturedQuran.VHFeaturedQuran
import com.quranapp.android.components.FeaturedQuranModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.LytFeaturedQuranItemBinding
import com.quranapp.android.utils.gesture.HoverPushEffect
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect
import com.quranapp.android.utils.reader.factory.ReaderFactory.startVerseRange

class ADPFeaturedQuran(private val mQuranMeta: QuranMeta, private val mModels: List<FeaturedQuranModel>) :
    RecyclerView.Adapter<ADPFeaturedQuran.VHFeaturedQuran>() {
    override fun getItemCount(): Int {
        return mModels.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHFeaturedQuran {
        val ctx = parent.context
        val binding = LytFeaturedQuranItemBinding.inflate(LayoutInflater.from(ctx))

        val params = ViewGroup.LayoutParams(Dimen.dp2px(ctx, 200f), Dimen.dp2px(ctx, 150f))
        binding.root.layoutParams = params
        return VHFeaturedQuran(binding)
    }

    override fun onBindViewHolder(holder: VHFeaturedQuran, position: Int) {
        holder.bind(mModels[position])
    }

    inner class VHFeaturedQuran(private val binding: LytFeaturedQuranItemBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(model: FeaturedQuranModel) {
            binding.name.text = model.name
            binding.miniInfo.text = model.miniInfo

            binding.root.setOnClickListener { v ->
                val chapterNo = model.chapterNo
                val verseRange = model.verseRange
                if (QuranMeta.isChapterValid(chapterNo) &&
                    mQuranMeta.isVerseRangeValid4Chapter(chapterNo, verseRange.first, verseRange.second)) {
                    startVerseRange(itemView.context, chapterNo, verseRange)
                }
            }

            binding.root.setOnTouchListener(HoverPushOpacityEffect(HoverPushEffect.Pressure.LOW))
        }
    }
}
