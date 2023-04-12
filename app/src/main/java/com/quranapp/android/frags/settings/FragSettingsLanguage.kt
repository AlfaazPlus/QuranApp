package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.updatePaddingRelative
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.databinding.FragSettingsLangBinding
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getStringArray
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import com.quranapp.android.widgets.compound.PeaceCompoundButton
import com.quranapp.android.widgets.radio.PeaceRadioButton
import java.util.*

class FragSettingsLanguage : FragSettingsBase() {
    private lateinit var binding: FragSettingsLangBinding
    private var initialLocale: String? = null

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleAppLanguage)

    override val layoutResource = R.layout.frag_settings_lang

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback(object : BoldHeaderCallback {
                override fun onBackIconClick() {
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                override fun onRightIconClick() {
                    changeCheckpoint()
                }
            })

            setShowRightIcon(true)
            disableRightBtn(true)
            setRightIconRes(R.drawable.dr_icon_check, activity.getString(R.string.strLabelDone))
            setShowSearchIcon(false)
        }
    }

    override val shouldCreateScrollerView = true

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        binding = FragSettingsLangBinding.bind(view)
        initialLocale = SPAppConfigs.getLocale(ctx)

        initLanguage(binding)
    }

    private fun initLanguage(binding: FragSettingsLangBinding) {
        val ctx = binding.root.context

        val availableLocalesValues = ctx.getStringArray(R.array.availableLocalesValues)
        val availableLocaleNames = ctx.getStringArray(R.array.availableLocalesNames)

        val forcedTextGravity = if (WindowUtils.isRTL(ctx)) {
            PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_RIGHT
        } else {
            PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_LEFT
        }

        var preCheckedRadioId = View.NO_ID

        availableLocalesValues.forEachIndexed { index, value ->
            PeaceRadioButton(ctx).apply {
                tag = value

                setCompoundDirection(PeaceCompoundButton.COMPOUND_TEXT_LEFT)
                setBackgroundResource(R.drawable.dr_bg_hover)
                setSpaceBetween(ctx.dp2px(20f))
                setTextAppearance(R.style.TextAppearanceCommonTitle)
                setForceTextGravity(forcedTextGravity)

                val padH = ctx.dp2px(20f)
                val padV = ctx.dp2px(12f)

                updatePaddingRelative(start = padH, end = padH, top = padV, bottom = padV)

                setTexts(availableLocaleNames[index], null)

                binding.list.addView(this)

                if (value == initialLocale) {
                    preCheckedRadioId = id
                }
            }
        }

        if (preCheckedRadioId != View.NO_ID) {
            binding.list.check(preCheckedRadioId)
        }

        binding.list.onCheckChangedListener = { button, _ ->
            activity()?.header?.disableRightBtn(button.tag == initialLocale)
        }
    }

    private fun changeCheckpoint() {
        val button = binding.list.findViewById<View>(binding.list.getCheckedRadioId()) ?: return
        val locale = button.tag as String?

        if (locale == null || locale == initialLocale) return

        restartApp(binding.list.context, locale)
    }

    private fun restartApp(ctx: Context, locale: String) {
        SPAppConfigs.setLocale(ctx, Locale(locale).toLanguageTag())
        restartMainActivity(ctx)
    }
}
