package com.quranapp.android.adapters.transl

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.databinding.LytSettingsDownlTranslItemBinding
import com.quranapp.android.interfaceUtils.TranslDownloadExplorerImpl
import com.quranapp.android.utils.extensions.*

class ADPDownloadTranslations(
    private val impl: TranslDownloadExplorerImpl,
    private val models: ArrayList<TranslModel>
) : RecyclerView.Adapter<ADPDownloadTranslations.VHDownloadTransl>() {

    init {

        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return models.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHDownloadTransl {
        return VHDownloadTransl(
            LytSettingsDownlTranslItemBinding.inflate(
                parent.layoutInflater,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VHDownloadTransl, position: Int) {
        holder.bind(models[position])
    }

    inner class VHDownloadTransl(private val binding: LytSettingsDownlTranslItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(translModel: TranslModel) {
            val bookInfo = translModel.bookInfo

            binding.book.text = bookInfo.bookName
            binding.author.let {
                it.text = bookInfo.authorName
                it.visibility = if (bookInfo.authorName.isEmpty()) View.GONE else View.VISIBLE
            }

            binding.loader.visibility = if (translModel.isDownloading) View.VISIBLE else View.GONE

            binding.iconDownload.visibility = if (translModel.isDownloading) View.GONE else View.VISIBLE
            binding.iconDownload.disableView(translModel.isDownloadingDisabled)

            createMiniInfo(binding.miniInfosCont, translModel.miniInfos)

            binding.root.let {
                it.isClickable = !translModel.isDownloading && !translModel.isDownloadingDisabled
                it.setOnClickListener { v ->
                    if (translModel.isDownloadingDisabled) {
                        Toast.makeText(v.context, R.string.msgAnotherDownloadInProgress, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    impl.onDownloadAttempt(this@ADPDownloadTranslations, this, v, translModel)
                }
            }
        }

        private fun createMiniInfo(container: LinearLayout, miniInfo: List<String>) {
            container.removeAllViews()

            val ctx = container.context

            for (info in miniInfo) {
                if (info.isEmpty()) continue

                val miniInfoView = AppCompatTextView(container.context).apply {
                    text = info
                    setBackgroundResource(R.drawable.dr_bg_primary_cornered)
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    setTextColor(ctx.color(R.color.white))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, ctx.getDimenPx(R.dimen.dmnCommonSize2_5).toFloat())
                    updatePaddings(ctx.dp2px(5f), ctx.dp2px(2f))
                }
                container.addView(miniInfoView, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    marginEnd = Dimen.dp2px(ctx, 6f)
                })
            }
        }
    }
}