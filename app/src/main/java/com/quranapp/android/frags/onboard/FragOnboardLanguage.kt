package com.quranapp.android.frags.onboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.NestedScrollView
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getStringArray
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import com.quranapp.android.widgets.compound.PeaceCompoundButton
import com.quranapp.android.widgets.radio.PeaceRadioButton
import com.quranapp.android.widgets.radio.PeaceRadioGroup

class FragOnboardLanguage : FragOnboardBase() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return NestedScrollView(inflater.context).apply {
            addView(PeaceRadioGroup(inflater.context))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLanguages((view as ViewGroup).getChildAt(0) as PeaceRadioGroup)
    }

    private fun initLanguages(group: PeaceRadioGroup) {
        val ctx = group.context

        val availableLocalesValues = ctx.getStringArray(R.array.availableLocalesValues)
        val availableLocaleNames = ctx.getStringArray(R.array.availableLocalesNames)

        val forcedTextGravity = if (WindowUtils.isRTL(ctx)) {
            PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_RIGHT
        } else {
            PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_LEFT
        }

        val initialLocale = SPAppConfigs.getLocale(ctx)
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

                group.addView(this)

                if (value == initialLocale) {
                    preCheckedRadioId = id
                }
            }
        }

        if (preCheckedRadioId != View.NO_ID) {
            group.check(preCheckedRadioId)
        }

        group.onCheckChangedListener = { button, _ ->
            val localeValue = button.tag as String
            SPAppConfigs.setLocale(ctx, localeValue)

            restartMainActivity(ctx)
        }
    }
}
