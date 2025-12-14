/*
 * Created by Faisal Khan on (c) 22/8/2021.
 */
package com.quranapp.android.adapters.recitation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.adapters.recitation.ADPRecitationTranslations.VHRecitationTranslation
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.databinding.LytSettingsRecitationItemBinding
import com.quranapp.android.frags.settings.recitations.FragSettingsRecitationsBase
import com.quranapp.android.frags.settings.recitations.manage.FragSettingsManageAudioReciter
import com.quranapp.android.utils.sharedPrefs.SPReader

class ADPRecitationTranslations(private val frag: FragSettingsRecitationsBase) :
    RecyclerView.Adapter<VHRecitationTranslation>() {
    private var models: List<RecitationTranslationInfoModel> = ArrayList()
    private var selectedPos = -1
    var isManageAudio = false

    init {
        setHasStableIds(true)
    }

    fun setModels(models: List<RecitationTranslationInfoModel>) {
        this.models = ArrayList(models)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHRecitationTranslation {
        return VHRecitationTranslation(
            LytSettingsRecitationItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHRecitationTranslation, position: Int) {
        holder.bind(models[position])
    }

    override fun getItemCount(): Int {
        return models.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class VHRecitationTranslation(private val binding: LytSettingsRecitationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.radio.isClickable = false
            binding.radio.isFocusable = false
            binding.radio.visibility = if (isManageAudio) View.GONE else View.VISIBLE
            binding.chevron.visibility = if (!isManageAudio) View.GONE else View.VISIBLE

            if (isManageAudio) {
                (binding.reciter.layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart = 0
                (binding.style.layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart = 0
            }
        }

        fun bind(model: RecitationTranslationInfoModel) {
            binding.reciter.text = model.getReciterName()

            val description = itemView.context.getString(
                R.string.textTranslationReciterInfo,
                model.langName,
                model.book ?: ""
            )

            if (description.isEmpty()) {
                binding.style.visibility = View.GONE
            } else {
                binding.style.visibility = View.VISIBLE
                binding.style.text = description
            }


            binding.radio.isChecked = model.isChecked


            if (model.isChecked) {
                selectedPos = bindingAdapterPosition
            }

            binding.root.setOnClickListener { v ->
                if (isManageAudio) {
                    frag.launchFrag(FragSettingsManageAudioReciter::class.java, Bundle().apply {
                        putSerializable(
                            FragSettingsManageAudioReciter.KEY_RECITATION_INFO_MODEL,
                            model
                        )
                    })
                } else {
                    select(bindingAdapterPosition)
                    SPReader.setSavedRecitationTranslationSlug(v.context, model.slug)
                }
            }
        }

        private fun select(position: Int) {
            try {
                models[selectedPos].isChecked = false
                notifyItemChanged(selectedPos)
            } catch (ignored: Exception) {
            }

            models[position].isChecked = true

            notifyItemChanged(position)
            selectedPos = position
        }
    }
}