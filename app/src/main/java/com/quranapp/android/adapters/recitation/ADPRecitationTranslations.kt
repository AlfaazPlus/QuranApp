/*
 * Created by Faisal Khan on (c) 22/8/2021.
 */
package com.quranapp.android.adapters.recitation

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.adapters.recitation.ADPRecitationTranslations.VHRecitationTranslation
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.frags.settings.recitations.FragSettingsRecitationsBase
import com.quranapp.android.frags.settings.recitations.manage.FragSettingsManageAudioReciter
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.updatePaddingHorizontal
import com.quranapp.android.utils.extensions.updatePaddingVertical
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.widgets.radio.PeaceRadioButton

class ADPRecitationTranslations(private val frag: FragSettingsRecitationsBase) : RecyclerView.Adapter<VHRecitationTranslation>() {
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
        val context = parent.context

        return VHRecitationTranslation(PeaceRadioButton(context).apply {
            setBackgroundResource(R.drawable.dr_bg_hover)
            updatePaddingHorizontal(context.dp2px(20F))
            updatePaddingVertical(context.dp2px(10F))
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setTextAppearance(R.style.TextAppearanceCommonTitle)
            setSpaceBetween(context.dp2px(15F))
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
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

    inner class VHRecitationTranslation(private val radio: PeaceRadioButton) : RecyclerView.ViewHolder(radio) {

        init {
            radio.isClickable = false
            radio.isFocusable = false
            radio.getCompoundButton().visibility = if (isManageAudio) View.GONE else View.VISIBLE

            if (isManageAudio) radio.setSpaceBetween(0)
        }

        fun bind(model: RecitationTranslationInfoModel) {
            radio.setTexts(model.getReciterName(), itemView.context.getString(R.string.textTranslationReciterInfo, model.langName, model.book ?: ""))
            radio.isChecked = model.isChecked

            if (model.isChecked) {
                selectedPos = bindingAdapterPosition
            }

            radio.visibility = View.VISIBLE
            radio.setOnClickListener { v ->
                if (isManageAudio) {
                    frag.launchFrag(FragSettingsManageAudioReciter::class.java, Bundle().apply {
                        putSerializable(FragSettingsManageAudioReciter.KEY_RECITATION_INFO_MODEL, model)
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