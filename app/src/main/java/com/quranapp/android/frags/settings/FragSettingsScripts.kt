/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */
package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.updatePaddingRelative
import com.peacedesign.android.utils.ResUtils
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.databinding.LytSettingsScriptItemBinding
import com.quranapp.android.utils.reader.ScriptUtils
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.views.BoldHeader

class FragSettingsScripts : FragSettingsBase() {

    private var initScript: String? = null

    override fun getFragTitle(ctx: Context): String = ctx.getString(R.string.strTitleScripts)

    override val layoutResource = 0

    override val shouldCreateScrollerView = true

    override fun getFragView(ctx: Context): View {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL

            updatePaddingRelative(
                top = dp2px(ctx, 10f),
                bottom = dimen(ctx, R.dimen.dmnPadHuge)
            )
        }
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback { activity.onBackPressed() }
            disableRightBtn(true)
            setShowSearchIcon(false)
            setShowRightIcon(false)
        }
    }


    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        initScript = SPReader.getSavedScript(ctx)
        makeScriptOptions(view as LinearLayout, initScript)
    }

    private fun makeScriptOptions(list: LinearLayout, initialScript: String?) {
        val ctx = list.context
        val availableScriptSlugs = ScriptUtils.availableScriptSlugs()

        for (slug in availableScriptSlugs) {
            LytSettingsScriptItemBinding.inflate(LayoutInflater.from(ctx), list, false).apply {
                miniInfo.visibility = View.GONE
                radio.text = ScriptUtils.getScriptName(slug)
                radio.isChecked = slug == initialScript
                preview.setText(ScriptUtils.getScriptPreviewRes(slug))
                preview.typeface = ResUtils.getFont(ctx, ScriptUtils.getScriptFontRes(slug))
                preview.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    ResUtils.getDimenPx(ctx, ScriptUtils.getScriptFontDimenRes(slug)).toFloat()
                )
                root.setOnClickListener { onItemClick(list, this, slug) }

                list.addView(root)
            }
        }
    }

    private fun onItemClick(list: LinearLayout, binding: LytSettingsScriptItemBinding, slug: String) {
        if (binding.radio.isChecked) return

        list.children.forEach {
            it.findViewById<RadioButton>(R.id.radio)?.isChecked = false
        }

        binding.radio.isChecked = true

        if (binding.radio.isChecked) {
            SPReader.setSavedScript(list.context, slug)
        }
    }

    override fun getFinishingResult(ctx: Context): Bundle? {
        if (SPReader.getSavedScript(ctx) != initScript) {
            return bundleOf(
                ActivityReader.KEY_SCRIPT_CHANGED to true
            )
        }

        return null
    }
}