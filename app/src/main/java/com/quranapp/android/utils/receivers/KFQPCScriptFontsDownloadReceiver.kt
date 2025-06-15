/*
 * Created by Faisal Khan on (c) 26/8/2021.
 */
package com.quranapp.android.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quranapp.android.utils.Logger
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
            is DownloadFlow.Start -> l.onStart()
            is DownloadFlow.Progress -> l.onProgress(downloadFlow.progress)
            is DownloadFlow.Extracting -> l.onExtracting()
            is DownloadFlow.Complete -> l.onComplete()
            is DownloadFlow.Failed -> l.onFailed()
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
        fun onStart()
        fun onProgress(progress: Int)
        fun onExtracting()
        fun onComplete()
        fun onFailed()
    }

    companion object {
        const val ACTION_DOWNLOAD_STATUS = "action.download_status"
        const val KEY_DOWNLOAD_FLOW = "key.download_flow"
    }
}
