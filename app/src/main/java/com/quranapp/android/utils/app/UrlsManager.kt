package com.quranapp.android.utils.app

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.AppUrls
import com.quranapp.android.utils.app.AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPAppActions.addToPendingAction
import com.quranapp.android.utils.sharedPrefs.SPAppActions.setFetchUrlsForce
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException

class UrlsManager(private val ctx: Context) {
    companion object {
        const val URL_KEY_FEEDBACK = "feedback"
        const val URL_KEY_PRIVACY_POLICY = "privacy-policy"
        const val URL_KEY_ABOUT = "about"
        const val URL_KEY_HELP = "help"
        const val URL_KEY_DISCORD = "discord"

        private val DIR_NAME_4_URLS = FileUtils.createPath(
            BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
            "urls"
        )
        private const val URLS_FILE_NAME = "urls.json"
        private var sAppUrls: AppUrls? = null
    }

    private val mFileUtils = FileUtils.newInstance(ctx)
    private var mCancelled = false

    fun getUrlsJson(
        readyCallback: (AppUrls) -> Unit,
        failedCallback: ((Exception) -> Unit)?
    ) {
        if (sAppUrls != null) {
            readyCallback(sAppUrls!!)
            return
        }

        val urlsFile = File(FileUtils.makeAndGetAppResourceDir(DIR_NAME_4_URLS), URLS_FILE_NAME)
        val forceUrlsDownload = SPAppActions.getFetchUrlsForce(ctx)

        if (!forceUrlsDownload && urlsFile.exists() && urlsFile.length() > 0) {
            try {
                val urlsData = urlsFile.readText()
                sAppUrls = JsonHelper.json.decodeFromString(urlsData)
                readyCallback(sAppUrls!!)
                setFetchUrlsForce(ctx, false)
            } catch (e: Exception) {
                addToPendingAction(ctx, AppActions.APP_ACTION_URLS_UPDATE, null)
                failedCallback?.invoke(e)
            }
        } else {
            if (!urlsFile.exists() && !mFileUtils.createFile(urlsFile)) {
                failedCallback?.invoke(IOException("Could not create urlsFile."))
                return
            }

            if (!NetworkStateReceiver.canProceed(ctx)) {
                return
            }

            val failureListener = { e: Exception ->
                addToPendingAction(ctx, AppActions.APP_ACTION_URLS_UPDATE, null)
                failedCallback?.invoke(e)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    sAppUrls = RetrofitInstance.github.getAppUrls()
                    urlsFile.writeText(JsonHelper.json.encodeToString(sAppUrls!!))

                    withContext(Dispatchers.Main) {
                        if (mCancelled) {
                            mCancelled = false
                            failureListener(CancellationException("Canceled by the user."))
                            return@withContext
                        }
                        readyCallback(sAppUrls!!)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        failureListener(e)
                    }
                }
            }
        }
    }

    fun cancel() {
        mCancelled = true
    }
}
