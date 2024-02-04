/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */
package com.quranapp.android.frags.settings

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.updatePaddingRelative
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.databinding.LytScriptDownloadProgressBinding
import com.quranapp.android.databinding.LytSettingsScriptItemBinding
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.extensions.getFont
import com.quranapp.android.utils.extensions.visible
import com.quranapp.android.utils.reader.*
import com.quranapp.android.utils.receivers.KFQPCScriptFontsDownloadReceiver
import com.quranapp.android.utils.services.KFQPCScriptFontsDownloadService
import com.quranapp.android.utils.services.KFQPCScriptFontsDownloadService.Companion.ALL_PART_DOWNLOADS_FINISHED
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.views.BoldHeader
import kotlin.math.ceil

class FragSettingsScripts : FragSettingsBase(), ServiceConnection {

    private var initScript: String? = null
    private var mScriptDownloadReceiver: KFQPCScriptFontsDownloadReceiver? = null
    private var scriptDownloadService: KFQPCScriptFontsDownloadService? = null

    override fun onStart() {
        super.onStart()

        activity?.let { bindDownloadService(it) }
    }

    override fun onStop() {
        super.onStop()

        unregisterDownloadService()
    }

    override fun getFragTitle(ctx: Context): String = ctx.getString(R.string.strTitleSelectScripts)

    override val layoutResource = 0

    override val shouldCreateScrollerView = true

    override fun getFragView(ctx: Context): View {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL

            updatePaddingRelative(
                top = ctx.dp2px(10f),
                bottom = ctx.getDimenPx(R.dimen.dmnPadHuge)
            )
        }
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback { activity.onBackPressedDispatcher.onBackPressed() }
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
                preview.setText(slug.getScriptPreviewText())
                preview.typeface = ctx.getFont(slug.getQuranScriptFontRes())
                preview.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    ctx.getDimenPx(slug.getQuranScriptVerseTextSizeMediumRes()).toFloat()
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
        if (scriptDownloadService?.isDownloadRunning == true) {
            if (slug == scriptDownloadService?.currentScriptKey) {
                showProgressDialog(ctx, slug)
            } else {
                alertDownloadInProgress(ctx)
            }
            return
        }

        val msg = StringBuilder(ctx.getString(R.string.msgDownloadKFQPCResources)).append("\n")
        if (!scriptDownloaded) {
            msg.append("\n").append(
                ctx.getString(R.string.msgDownloadKFQPCResourcesScript, slug.getQuranScriptName())
            )
        }

        val averageFontKB = 161.23
        val compressionFactor = 0.54
        val remainingMB = ceil(((kfqpcFontDownloadedCount.third * averageFontKB) / 1024 * compressionFactor).toFloat()).toInt()

        if (kfqpcFontDownloadedCount.third > 0) {
            msg.append("\n").append(
                ctx.getString(
                    R.string.msgDownloadKFQPCResourcesFonts,
                    kfqpcFontDownloadedCount.second,
                    kfqpcFontDownloadedCount.first,
                    remainingMB
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
                startScriptDownload(ctx, slug)
            }
        }.show()
    }

    private fun startScriptDownload(ctx: Context, scriptKey: String) {
        showProgressDialog(ctx, scriptKey)

        KFQPCScriptFontsDownloadService.STARTED_BY_USER = true
        ContextCompat.startForegroundService(
            ctx,
            Intent(ctx, KFQPCScriptFontsDownloadService::class.java).apply {
                putExtra(QuranScriptUtils.KEY_SCRIPT, scriptKey)
            }
        )
        bindDownloadService(ctx)
    }

    private fun showProgressDialog(ctx: Context, scriptKey: String) {
        val binding = LytScriptDownloadProgressBinding.inflate(LayoutInflater.from(ctx))

        val dialog = PeaceDialog.newBuilder(ctx).apply {
            setTitle(R.string.textDownloading)
            setView(binding.root)
            setCancelable(false)
            setButtonsDirection(PeaceDialog.STACKED)
            setNegativeButton(R.string.strLabelCancel) { _, _ ->
                scriptDownloadService?.cancel()
                unregisterDownloadService()
            }
            setPositiveButton(R.string.labelBackgroundDownload) { _, _ ->
                mScriptDownloadReceiver?.removeListener()
            }
        }.create()

        dialog.show()

        binding.title.text = scriptKey.getQuranScriptName()

        val txtDownloadingScript = ctx.getString(R.string.msgDownloadingScript)
        val txtDownloadingFonts = ctx.getString(R.string.msgDownloadingFonts)

        var lastPartNo: Int? = -1

        mScriptDownloadReceiver = KFQPCScriptFontsDownloadReceiver().apply {
            setDownloadStateListener(object : KFQPCScriptFontsDownloadReceiver.KFQPCScriptFontsDownload {
                override fun onStart(partNo: Int?) {
                    binding.progressIndicator.isIndeterminate = false

                    if (partNo == null) {
                        binding.subtitle.text = txtDownloadingScript
                    } else {
                        binding.subtitle.text = txtDownloadingFonts
                    }

                    binding.subtitle.visible()

                    dialog.setupDimension()
                    lastPartNo = partNo
                }

                override fun onProgress(partNo: Int?, progress: Int) {
                    binding.progressIndicator.progress = progress

                    if (partNo != null) {
                        binding.countText.text = ctx.getString(
                            R.string.msgFontsDonwloadProgressShort,
                            partNo,
                            QuranScriptUtils.TOTAL_DOWNLOAD_PARTS
                        )
                        binding.countText.visible()
                    }

                    if (lastPartNo != partNo) {
                        dialog.setupDimension()
                    }
                }

                override fun onComplete(partNo: Int?) {
                    if (partNo == ALL_PART_DOWNLOADS_FINISHED) {
                        dialog.dismiss()
                        showCompletedDialog(ctx, false)
                        scriptDownloadService?.finish()
                        unregisterDownloadService()
                    }
                }

                override fun onFailed(partNo: Int?) {
                    if (partNo == null) {
                        dialog.dismiss()
                        showCompletedDialog(ctx, true)
                        scriptDownloadService?.finish()
                        unregisterDownloadService()
                    }
                }
            })

            ContextCompat.registerReceiver(
                ctx,
                this,
                IntentFilter(KFQPCScriptFontsDownloadReceiver.ACTION_DOWNLOAD_STATUS),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun showCompletedDialog(ctx: Context, isFailed: Boolean) {
        PeaceDialog.newBuilder(ctx).apply {
            setTitle(if (isFailed) R.string.strTitleError else R.string.strTitleSuccess)
            setMessage(
                if (isFailed) R.string.msgDownloadFailed else R.string.msgScriptFontsDownloaded
            )
            setPositiveButton(R.string.strLabelOkay, null)
        }.show()
    }

    private fun alertDownloadInProgress(ctx: Context) {
        PeaceDialog.newBuilder(ctx).apply {
            setTitle(R.string.strTitleInfo)
            setMessage(ctx.getString(R.string.msgAnotherDownloadInProgress))
            setNeutralButton(R.string.strLabelGotIt, null)
        }.show()
    }

    private fun bindDownloadService(ctx: Context) {
        ctx.bindService(
            Intent(ctx, KFQPCScriptFontsDownloadService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun unbindDownloadService(actvt: Activity) {
        // if scriptDownloadService is null, it means the service is already unbound
        // or it was not bound in the first place.
        if (scriptDownloadService == null) {
            return
        }
        try {
            actvt.unbindService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        scriptDownloadService = (binder as KFQPCScriptFontsDownloadService.LocalBinder).service
    }

    override fun onServiceDisconnected(name: ComponentName) {
        scriptDownloadService = null
    }

    private fun unregisterDownloadService() {
        try {
            mScriptDownloadReceiver?.removeListener()

            activity?.let {
                it.unregisterReceiver(mScriptDownloadReceiver)
                unbindDownloadService(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
