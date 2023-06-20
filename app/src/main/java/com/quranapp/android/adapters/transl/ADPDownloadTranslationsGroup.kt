package com.quranapp.android.adapters.transl

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.components.transls.TranslationGroupModel
import com.quranapp.android.databinding.LytDownloadTranslationGroupItemBinding
import com.quranapp.android.interfaceUtils.TranslDownloadExplorerImpl
import com.quranapp.android.utils.extensions.*


class ADPDownloadTranslationsGroup(
    private val models: List<TranslationGroupModel>,
    private val impl: TranslDownloadExplorerImpl
) : RecyclerView.Adapter<ADPDownloadTranslationsGroup.VHDownloadTranslationGroup>() {

    override fun getItemCount(): Int {
        return models.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHDownloadTranslationGroup {
        return VHDownloadTranslationGroup(
            LytDownloadTranslationGroupItemBinding.inflate(
                parent.layoutInflater,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHDownloadTranslationGroup, position: Int) {
        holder.bind(models[position])
    }


    fun onDownloadStatus(slug: String?, downloading: Boolean) {
        var foundTheDownloadModel = false

        for (group in models) {
            for (model in group.translations) {
                if (!foundTheDownloadModel && slug == model.bookInfo.slug) {
                    foundTheDownloadModel = true
                    model.isDownloading = downloading
                } else {
                    model.isDownloadingDisabled = downloading
                }
            }
        }

        notifyDataSetChanged()
    }


    fun remove(slug: String) {
        var removedModelPos = -1
        var removedModelGroupPos = -1

        for ((groupIndex, group) in models.withIndex()) {
            for ((itemIndex, model) in group.translations.withIndex()) {
                if (slug == model.bookInfo.slug) {
                    removedModelGroupPos = groupIndex
                    removedModelPos = itemIndex
                    break
                }
            }
        }

        if (removedModelGroupPos != -1 && removedModelPos != -1) {
            models[removedModelGroupPos].translations.removeAt(removedModelPos)
            notifyItemChanged(removedModelGroupPos)
        }
    }

    inner class VHDownloadTranslationGroup(private val binding: LytDownloadTranslationGroupItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.list.layoutManager = LinearLayoutManager(binding.root.context)
            binding.root.clipToOutline = true
        }

        @SuppressLint("RtlHardcoded")
        fun bind(group: TranslationGroupModel) {
            binding.title.let {
                it.text = if (group.isExpanded) group.langName else prepareText(group.langName, group.translations.size)
                it.gravity = if (itemView.context.isRTL()) Gravity.RIGHT else Gravity.LEFT
                it.setDrawables(
                    null,
                    null,
                    itemView.context.drawable(R.drawable.dr_icon_chevron_right).rotate(
                        itemView.context,
                        if (group.isExpanded) -90f else 90f
                    ),
                    null
                )

                it.setOnClickListener {
                    group.isExpanded = !group.isExpanded
                    notifyItemChanged(bindingAdapterPosition)
                }
            }

            binding.list.visibility = (if (group.isExpanded) RecyclerView.VISIBLE else RecyclerView.GONE)

            binding.list.adapter = ADPDownloadTranslations(impl, group.translations)
        }

        private fun prepareText(langName: String, size: Int): CharSequence {
            val descSS = SpannableString(
                if (size <= 1)
                    itemView.context.getString(R.string.nItem, size)
                else
                    itemView.context.getString(R.string.nItems, size)
            )
            descSS.setSpan(
                RelativeSizeSpan(0.8f),
                0,
                descSS.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            descSS.setSpan(
                ForegroundColorSpan(itemView.context.color(R.color.colorText2)),
                0,
                descSS.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            return TextUtils.concat(langName, "\n", descSS)
        }
    }
}