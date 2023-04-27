/*
 * Created by Faisal Khan on (c) 26/8/2021.
 */
package com.quranapp.android.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quranapp.android.components.recitation.ManageAudioChapterModel
import com.quranapp.android.utils.extensions.serializableExtra

class RecitationChapterDownloadReceiver : BroadcastReceiver() {
    companion object {
        const val KEY_RECITATION_CHAPTER_MODEL = "key.recitation_chapter_model"
        const val ACTION_RECITATION_DOWNLOAD_STATUS = "RecitationChapterDownload.action.status"
        const val KEY_RECITATION_DOWNLOAD_STATUS = "RecitationChapterDownload.key.download.status"
        const val KEY_RECITATION_DOWNLOAD_PROGRESS = "RecitationChapterDownload.key.download.progress"
        const val RECITATION_DOWNLOAD_STATUS_PROGRESS = "RecitationChapterDownloadStatus.download.progress"
        const val RECITATION_DOWNLOAD_STATUS_CANCELED = "RecitationChapterDownloadStatus.download.canceled"
        const val RECITATION_DOWNLOAD_STATUS_FAILED = "RecitationChapterDownloadStatus.download.failed"
        const val RECITATION_DOWNLOAD_STATUS_SUCCEED = "RecitationChapterDownloadStatus.download.succeed"
    }

    var stateListener: DownloadStateListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (stateListener == null || ACTION_RECITATION_DOWNLOAD_STATUS != intent.action) return

        stateListener!!.onDownloadStatus(
            intent.serializableExtra(KEY_RECITATION_CHAPTER_MODEL)!!,
            intent.getStringExtra(KEY_RECITATION_DOWNLOAD_STATUS) ?: "",
            intent.getIntExtra(KEY_RECITATION_DOWNLOAD_PROGRESS, -1)
        )
    }

    interface DownloadStateListener {
        fun onDownloadStatus(chapterModel: ManageAudioChapterModel, status: String, progress: Int)
    }
}