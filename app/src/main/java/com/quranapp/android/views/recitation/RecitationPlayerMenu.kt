package com.quranapp.android.views.recitation

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.view.ViewGroup
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.ContextCompat
import com.peacedesign.android.utils.DrawableUtils
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.databinding.LytRecitationMenuBinding
import com.quranapp.android.utils.extensions.disableView
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.PopupWindow2
import com.quranapp.android.utils.univ.RelativePopupWindow.HorizontalPosition
import com.quranapp.android.utils.univ.RelativePopupWindow.VerticalPosition

class RecitationPlayerMenu(private val player: RecitationPlayer) {
    private val popup = createPopup()
    private var mBinding: LytRecitationMenuBinding? = null

    init {
        val inflater = AsyncLayoutInflater(player.context)
        inflater.inflate(R.layout.lyt_recitation_menu, null) { view, _, _ ->
            mBinding = LytRecitationMenuBinding.bind(view)
            popup.contentView = mBinding!!.root

            setupView(mBinding!!)
        }
    }

    val context: Context get() = player.context

    private fun createPopup(): PopupWindow2 {
        return PopupWindow2().apply {
            width = context.dp2px(220f)
            height = ViewGroup.LayoutParams.WRAP_CONTENT

            isFocusable = true
            elevation = context.dp2px(3f).toFloat()

            val bgColor = ContextCompat.getColor(context, R.color.colorBGRecitationMenu)
            setBackgroundDrawable(DrawableUtils.createBackground(bgColor, context.dp2px(10f).toFloat()))
        }
    }

    private fun setupView(binding: LytRecitationMenuBinding) {
        binding.repeat.setOnClickListener { binding.repeatCheckbox.toggle() }
        binding.autoplay.setOnClickListener { binding.autoplayCheckbox.toggle() }
        binding.selectReciter.setOnClickListener {
            close()
            player.activity.mBinding.readerHeader.openReaderSetting(ActivitySettings.SETTINGS_RECITER)
        }

        val resId = if (WindowUtils.isRTL(context)) R.drawable.dr_icon_chevron_left
        else R.drawable.dr_icon_chevron_right

        binding.selectReciter.setDrawables(
            player.activity.drawable(R.drawable.dr_icon_recitation),
            null,
            context.drawable(resId),
            null
        )

        binding.repeatCheckbox.setOnCheckedChangeListener { _, isChecked ->
            player.setRepeat(isChecked)
            binding.autoplay.disableView(isChecked)
        }

        binding.autoplayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            player.setContinueChapter(
                isChecked
            )
        }
    }

    private fun setup(binding: LytRecitationMenuBinding) {
        val repeatEnabled = SPReader.getRecitationRepeatVerse(context)
        binding.repeatCheckbox.isChecked = repeatEnabled
        binding.autoplayCheckbox.isChecked = SPReader.getRecitationContinueChapter(context)
        binding.autoplay.disableView(repeatEnabled)
        binding.selectReciter.text = prepareRecitationTitle(null)

        RecitationManager.prepare(context, false) {
            val subtitle = RecitationUtils.getReciterName(SPReader.getSavedRecitationSlug(context))
            binding.selectReciter.text = prepareRecitationTitle(subtitle)
        }
    }

    private fun prepareRecitationTitle(subtitle: String?): CharSequence {
        val ssb = SpannableStringBuilder(context.getString(R.string.strTitleSelectReciter))

        if (subtitle.isNullOrEmpty()) return ssb

        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        return ssb.append("\n").append(SpannableString(subtitle).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorText2)), 0, subtitle.length, flag)
            setSpan(RelativeSizeSpan(0.93f), 0, subtitle.length, flag)
            setSpan(TypefaceSpan("sans-serif"), 0, subtitle.length, flag)
        })
    }

    fun open(anchorView: View) {
        if (anchorView.windowToken != null) {
            if (mBinding != null) {
                setup(mBinding!!)
            }

            popup.showOnAnchor(anchorView, VerticalPosition.ABOVE, HorizontalPosition.ALIGN_RIGHT)
        }
    }

    fun close() {
        popup.dismiss()
    }
}