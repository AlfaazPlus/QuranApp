/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 1/3/2022.
 * All rights reserved.
 */
package com.quranapp.android.utils.app

import android.animation.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import com.peacedesign.android.utils.AppBridge
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.components.AppUpdateInfo
import com.quranapp.android.databinding.LytUpdateAppDialogBinding
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlin.math.pow

data class UpdateBannerDecision(
    val priority: Int,
    val showCriticalDialog: Boolean,
    val showMajorDialog: Boolean,
    val showInlineBanner: Boolean,
)

class UpdateManager(private val ctx: Context) {
    private val mIconAnimationHandler = Handler(Looper.getMainLooper())
    private var mIconAnimators = ArrayList<ObjectAnimator>()

    suspend fun fetchAndSaveUpdates() {
        withContext(Dispatchers.IO) {
            try {
                val updates = RetrofitInstance.github.getAppUpdates()
                val updatesString = JsonHelper.json.encodeToString(updates)
                Logger.print("updatesString: $updatesString")

                FileUtils.newInstance(ctx).apply {
                    val updatesFile = appUpdatesFile
                    if (createFile(updatesFile)) {
                        updatesFile.writeText(updatesString)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshAppUpdatesJson() {
        CoroutineScope(Dispatchers.IO).launch { fetchAndSaveUpdates() }
    }

    fun check4CriticalUpdate(): Boolean {
        val decision = getBannerDecision()
        if (decision.showCriticalDialog) {
            Logger.print("UpdateManager:", "Critical update available")
            showUpdateAvailableDialog(true)
            return true
        }
        return false
    }

    fun showUpdateDialogsIfNeeded() {
        val decision = getBannerDecision()
        Logger.print("Update priority = ${decision.priority}")
        when {
            decision.showCriticalDialog -> showUpdateAvailableDialog(true)
            decision.showMajorDialog -> showUpdateAvailableDialog(false)
        }
    }

    fun getBannerDecision(): UpdateBannerDecision {
        val priority = AppUpdateInfo(ctx).getMostImportantUpdate().priority

        return UpdateBannerDecision(
            priority = priority,
            showCriticalDialog = priority == AppUpdateInfo.CRITICAL,
            showMajorDialog = priority == AppUpdateInfo.MAJOR,
            showInlineBanner = priority in AppUpdateInfo.MAJOR downTo AppUpdateInfo.COSMETIC,
        )
    }

    fun openPlayStore() {
        AppBridge.newOpener(ctx).openPlayStore(null)
    }

    private fun showUpdateAvailableDialog(isCritical: Boolean, runOnDismiss: Runnable? = null) {
        val binding = LytUpdateAppDialogBinding.inflate(android.view.LayoutInflater.from(ctx))
        binding.txt.setText(
            if (isCritical) R.string.strMsgUpdateAvailable2Continue else R.string.strMsgUpdateAvailable4Dialog
        )
        mIconAnimators.add(animateUpdateIcon(binding.icon))

        val builder = PeaceDialog.newBuilder(ctx)
        builder.setView(binding.root)
        builder.setCancelable(false)
        builder.setOnDismissListener { runOnDismiss?.run() }
        builder.setPositiveButton(R.string.strLabelUpdate, ColorUtils.DANGER) { _, _ ->
            openPlayStore()
        }
        builder.setDismissOnPositive(!isCritical)

        if (!isCritical) {
            builder.setNeutralButton(R.string.strLabelLater, null)
        }

        builder.show()
    }

    private fun animateUpdateIcon(iconView: View): ObjectAnimator {
        val pvhTransY = PropertyValuesHolder.ofFloat(
            View.TRANSLATION_Y,
            0f,
            11f,
            -20f,
            10f,
            -3f,
            0f
        )
        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, .8f, 1.3f, 1.03f, 1f)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, .8f, 1.1f, 0.9f, 1f, 1f)
        return ObjectAnimator.ofPropertyValuesHolder(iconView, pvhTransY, pvhScaleX, pvhScaleY).apply {
            interpolator = TimeInterpolator { v -> (1.toFloat() - (1 - v).toDouble().pow(2.0)).toFloat() }
            duration = 1000
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (iconView.isAttachedToWindow) {
                        mIconAnimationHandler.postDelayed({ start() }, 1500)
                    }
                }
            })
            start()
        }
    }

    fun onPause() {
        mIconAnimators.forEach { it.cancel() }
        mIconAnimationHandler.removeCallbacksAndMessages(null)
    }

    fun onResume() {
        mIconAnimators.forEach { it.start() }
    }
}
