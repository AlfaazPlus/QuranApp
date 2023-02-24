/*
 * Created by Faisal Khan on (c) 26/8/2021.
 */
package com.quranapp.android.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.peacedesign.android.utils.Log
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.services.DownloadFlow
import com.quranapp.android.utils.services.KFQPCScriptFontsDownloadService

class KFQPCScriptFontsDownloadReceiver : BroadcastReceiver() {
    private var listener: KFQPCScriptFontsDownload? = null
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        Log.d(intent.action == ACTION_DOWNLOAD_CANCEL)
        if (intent.action == ACTION_DOWNLOAD_CANCEL) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, KFQPCScriptFontsDownloadService::class.java).apply {
                    action = ACTION_DOWNLOAD_CANCEL
                }
            )
            return
        }
        
        if (listener == null || intent.action != ACTION_DOWNLOAD_STATUS) return

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
        const val ACTION_DOWNLOAD_CANCEL = "action.download_cancel"
    }
}