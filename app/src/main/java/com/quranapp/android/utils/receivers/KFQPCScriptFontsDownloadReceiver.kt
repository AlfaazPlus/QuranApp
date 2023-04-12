/*
 * Created by Faisal Khan on (c) 26/8/2021.
 */
package com.quranapp.android.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.services.DownloadFlow

class KFQPCScriptFontsDownloadReceiver : BroadcastReceiver() {
    private var listener: KFQPCScriptFontsDownload? = null
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (listener == null || intent.action != ACTION_DOWNLOAD_STATUS) return

        val downloadFlow = intent.serializableExtra<DownloadFlow>(KEY_DOWNLOAD_FLOW)

        val l = listener!!

        when (downloadFlow) {
            is DownloadFlow.Start -> l.onStart(downloadFlow.partNo)
            is DownloadFlow.Progress -> l.onProgress(downloadFlow.partNo, downloadFlow.progress)
            is DownloadFlow.Complete -> l.onComplete(downloadFlow.partNo)
            is DownloadFlow.Failed -> l.onFailed(downloadFlow.partNo)
            else -> {}
        }
    }

    fun setDownloadStateListener(listener: KFQPCScriptFontsDownload) {
        this.listener = listener
    }

    fun removeListener() {
        this.listener = null
    }

    interface KFQPCScriptFontsDownload {
        fun onStart(partNo: Int?)
        fun onProgress(partNo: Int?, progress: Int)
        fun onComplete(partNo: Int?)
        fun onFailed(partNo: Int?)
    }

    companion object {
        const val ACTION_DOWNLOAD_STATUS = "action.download_status"
        const val KEY_DOWNLOAD_FLOW = "key.download_flow"
    }
}
