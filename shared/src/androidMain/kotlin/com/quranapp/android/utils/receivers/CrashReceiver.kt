package com.quranapp.android.utils.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.copiedToClipboard
import com.quranapp.android.generated.resources.pasteCrashLogGithubIssue
import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.utils.extensions.copyToClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class CrashReceiver : BroadcastReceiver() {
    companion object {
        const val CRASH_ACTION_COPY_LOG = "quranapp.action.crash.COPY_LOG"
        const val CRASH_ACTION_CREATE_ISSUE = "quranapp.action.crash.CREATE_ISSUE"
        const val NOTIFICATION_ID_CRASH = 1012
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                ContextCompat.getSystemService(context, NotificationManager::class.java)
                    ?.cancel(NOTIFICATION_ID_CRASH)

                if (intent?.action == CRASH_ACTION_COPY_LOG || intent?.action == CRASH_ACTION_CREATE_ISSUE) {
                    context.copyToClipboard(intent.getStringExtra(Intent.EXTRA_TEXT)!!)
                    AppPreferences.removeLastCrashLogFilename()
                }

                when (intent?.action) {
                    CRASH_ACTION_COPY_LOG -> {
                        Toast.makeText(
                            context,
                            getString(Res.string.copiedToClipboard),
                            Toast.LENGTH_LONG,
                        ).show()
                    }

                    CRASH_ACTION_CREATE_ISSUE -> {
                        Toast.makeText(
                            context,
                            getString(Res.string.pasteCrashLogGithubIssue),
                            Toast.LENGTH_LONG,
                        ).show()

                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            ApiConfig.GITHUB_ISSUES_BUG_REPORT_URL.toUri()
                        )
                        browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(browserIntent)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
