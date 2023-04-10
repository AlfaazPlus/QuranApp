/*
 * Created by Faisal Khan on (c) 22/8/2021.
 */
package com.quranapp.android.adapters.recitation

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.adapters.recitation.ADPRecitationTranslations.VHRecitationTranslation
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.widgets.radio.PeaceRadioButton

class ADPRecitationTranslations : RecyclerView.Adapter<VHRecitationTranslation>() {
    private var models: List<RecitationTranslationInfoModel> = ArrayList()
    private var selectedPos = -1

    init {
        setHasStableIds(true)
    }

    fun setModels(models: List<RecitationTranslationInfoModel>) {
        this.models = ArrayList(models)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHRecitationTranslation {
        return VHRecitationTranslation(PeaceRadioButton(parent.context).apply {

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
        }

        fun bind(model: RecitationTranslationInfoModel) {
            radio.setTexts(model.langName, model.getReciterName())
            radio.isChecked = model.isChecked

            if (model.isChecked) {
                selectedPos = bindingAdapterPosition
            }

            radio.visibility = View.VISIBLE
            radio.onCheckChangedListener = { button, _ ->
                select(bindingAdapterPosition)
                SPReader.setSavedRecitationSlug(button.context, model.slug)
            }
        }

        private fun select(position: Int) {
            try {
                val oldModel = models[selectedPos]
                oldModel.isChecked = false
                notifyItemChanged(selectedPos)
            } catch (ignored: Exception) {
            }

            val newModel = models[position]
            newModel.isChecked = true

            notifyItemChanged(position)
            selectedPos = position
        }
    }
}