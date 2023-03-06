package com.quranapp.android.frags.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.updatePaddingRelative
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.databinding.FragSettingsLangBinding
import com.quranapp.android.utils.IntentUtils
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs.getLocale
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs.setLocale
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.widgets.compound.PeaceCompoundButton
import com.quranapp.android.widgets.radio.PeaceRadioButton

class FragSettingsLanguage : FragSettingsBase() {
    private var initialLocale: String? = null

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleAppLanguage)

    override val layoutResource = R.layout.frag_settings_lang

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback { activity.onBackPressed() }
            setShowSearchIcon(false)
            setShowRightIcon(false)
        }
    }

    override val shouldCreateScrollerView = true

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        val binding = FragSettingsLangBinding.bind(view)
        initialLocale = getLocale(ctx)

        initLanguage(binding)
    }

    private fun initLanguage(binding: FragSettingsLangBinding) {
        val ctx = binding.root.context

        val availableLocalesValues = strArray(ctx, R.array.availableLocalesValues)
        val availableLocaleNames = strArray(ctx, R.array.availableLocalesNames)

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
                setSpaceBetween(dp2px(ctx, 20f))
                setTextAppearance(R.style.TextAppearanceCommonTitle)
                setForceTextGravity(forcedTextGravity)

                val padH = dp2px(ctx, 20f)
                val padV = dp2px(ctx, 12f)

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
            val localeValue = button.tag as String
            setLocale(ctx, localeValue)

            ctx.sendBroadcast(
                Intent(IntentUtils.INTENT_ACTION_APP_LANGUAGE_CHANGED).apply {
                    putExtra("locale", localeValue)
                }
            )
        }
    }
}
