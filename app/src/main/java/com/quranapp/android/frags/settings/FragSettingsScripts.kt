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
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.databinding.LytSettingsScriptItemBinding
import com.quranapp.android.utils.reader.*
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.views.BoldHeader
import kotlin.math.ceil

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
        makeScriptOptions(view as LinearLayout, initScript!!)
    }

    private fun makeScriptOptions(list: LinearLayout, initialScript: String) {
        val ctx = list.context
        val availableScriptSlugs = QuranScriptUtils.availableScriptSlugs()

        for (slug in availableScriptSlugs) {
            LytSettingsScriptItemBinding.inflate(LayoutInflater.from(ctx), list, false).apply {
                radio.text = slug.getQuranScriptName()
                radio.isChecked = slug == initialScript
                preview.setText(slug.getScriptPreviewRes())
                preview.typeface = ResUtils.getFont(ctx, slug.getQuranScriptFontRes())
                preview.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    ResUtils.getDimenPx(ctx, slug.getQuranScriptFontDimenRes()).toFloat()
                )
                root.setOnClickListener { onItemClick(list, this, slug) }

                list.addView(root)
            }
        }
    }

    private fun onItemClick(list: LinearLayout, binding: LytSettingsScriptItemBinding, slug: String) {
        if (binding.radio.isChecked) return

        val ctx = list.context

        if (slug.isKFQPCScript()) {
            val scriptDownloaded = QuranScriptUtils.verifyKFQPCScriptDownloaded(ctx, slug)
            val fontDownloadedCount = QuranScriptUtils.getKFQPCFontDownloadedCount(ctx, slug)

            if (!scriptDownloaded || fontDownloadedCount.third > 0) {
                alertToDownloadScriptResources(ctx, slug, scriptDownloaded, fontDownloadedCount)
                return
            }
        }

        list.children.forEach {
            it.findViewById<RadioButton>(R.id.radio)?.isChecked = false
        }

        binding.radio.isChecked = true

        if (binding.radio.isChecked) {
            SPReader.setSavedScript(ctx, slug)
        }
    }

    private fun alertToDownloadScriptResources(
        ctx: Context,
        slug: String,
        scriptDownloaded: Boolean,
        kfqpcFontDownloadedCount: Triple<Int, Int, Int>
    ) {
        val msg = StringBuilder(ctx.getString(R.string.msgDownloadKFQPCResources)).append("\n")
        if (!scriptDownloaded) {
            msg.append("\n").append(ctx.getString(R.string.msgDownloadKFQPCResourcesScript, slug.getQuranScriptName()))
        }

        val averageFontKB = 161.23
        val remainingMB = ceil(((kfqpcFontDownloadedCount.third * averageFontKB) / 1024).toFloat()).toInt()

        if (kfqpcFontDownloadedCount.third > 0) {
            msg.append("\n").append(
                ctx.getString(
                    R.string.msgDownloadKFQPCResourcesFonts,
                    kfqpcFontDownloadedCount.second,
                    kfqpcFontDownloadedCount.first,
                    "${remainingMB}MB"
                )
            )
        }

        PeaceDialog.newBuilder(ctx).apply {
            setTitle(R.string.titleDownloadScriptResources)
            setMessage(msg)
            setTitleTextAlignment(View.TEXT_ALIGNMENT_TEXT_START)
            setMessageTextAlignment(View.TEXT_ALIGNMENT_TEXT_START)
            setNeutralButton(R.string.strLabelNotNow, null)
            setPositiveButton(R.string.labelDownload) { _, _ ->
            }
        }.show()
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