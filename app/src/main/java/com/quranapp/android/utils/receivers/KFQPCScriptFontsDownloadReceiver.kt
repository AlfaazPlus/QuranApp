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
        if (listener == null || intent == null || intent.action != ACTION_DOWNLOAD_STATUS) {
            return
        }


        val downloadFlow = intent.serializableExtra<DownloadFlow>(KEY_DOWNLOAD_FLOW)

        val l = listener!!

        when (downloadFlow) {
            is DownloadFlow.Start -> l.onStart(downloadFlow.pageNo)
            is DownloadFlow.Progress -> {
                l.onProgress(downloadFlow.pageNo, downloadFlow.progress)
            }
            is DownloadFlow.Complete -> l.onComplete(downloadFlow.pageNo)
            is DownloadFlow.Failed -> {
                l.onFailed(downloadFlow.pageNo)
            }
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
        fun onStart(pageNo: Int?)
        fun onProgress(pageNo: Int?, progress: Int)
        fun onComplete(pageNo: Int?)
        fun onFailed(pageNo: Int?)
    }

    companion object {
        const val ACTION_DOWNLOAD_STATUS = "action.download_status"
        const val KEY_DOWNLOAD_FLOW = "key.download_flow"
    }
}