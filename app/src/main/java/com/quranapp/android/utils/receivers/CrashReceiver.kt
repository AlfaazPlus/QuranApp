package com.quranapp.android.utils.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.quranapp.android.R
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.sharedPrefs.SPLog

class CrashReceiver : BroadcastReceiver() {
    companion object {
        const val CRASH_ACTION_COPY_LOG = "quranapp.action.crash.COPY_LOG"
        const val CRASH_ACTION_CREATE_ISSUE = "quranapp.action.crash.CREATE_ISSUE"
        const val NOTIFICATION_ID_CRASH = 1012
    }

    override fun onReceive(context: Context, intent: Intent?) {
        ContextCompat.getSystemService(context, NotificationManager::class.java)?.cancel(NOTIFICATION_ID_CRASH)

        if (intent?.action == CRASH_ACTION_COPY_LOG || intent?.action == CRASH_ACTION_CREATE_ISSUE) {
            context.copyToClipboard(intent.getStringExtra(Intent.EXTRA_TEXT)!!)
            SPLog.removeLastCrashLogFilename(context)
        }

        when (intent?.action) {
            CRASH_ACTION_COPY_LOG -> {
                Toast.makeText(context, R.string.copiedToClipboard, Toast.LENGTH_LONG).show()
            }

            CRASH_ACTION_CREATE_ISSUE -> {
                Toast.makeText(context, R.string.pasteCrashLogGithubIssue, Toast.LENGTH_LONG).show()

                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ApiConfig.GITHUB_ISSUES_BUG_REPORT_URL))
                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(browserIntent)
            }
        }
    }
}