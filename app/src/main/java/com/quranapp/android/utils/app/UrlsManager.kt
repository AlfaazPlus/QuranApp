package com.quranapp.android.utils.app

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.AppUrls
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        const val URL_KEY_DONATION = "donation"

        private val DIR_NAME_4_URLS = FileUtils.createPath(
            BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
            "urls"
        )
        private const val URLS_FILE_NAME = "urls.json"
        private var sAppUrls: AppUrls? = null
    }

    private val mFileUtils = FileUtils.newInstance(ctx)
    private var mCancelled = false

    private fun getUrlsFile(): File {
        val dir = FileUtils.makeAndGetAppResourceDir(DIR_NAME_4_URLS)
        return File(dir, URLS_FILE_NAME)
    }

    suspend fun refresh(): AppUrls = withContext(Dispatchers.IO) {
        try {
            val urls = RetrofitInstance.github.getAppUrls()
            val urlsFile = getUrlsFile()
            if (mFileUtils.createFile(urlsFile)) {
                urlsFile.writeText(JsonHelper.json.encodeToString(urls))
                sAppUrls = urls
            }
            urls
        } catch (e: Exception) {
            Log.saveError(e, "UrlsManager.refresh")
            throw e
        }
    }

    fun getUrlsJson(
        readyCallback: (AppUrls) -> Unit,
        failedCallback: ((Exception) -> Unit)?
    ) {
        if (sAppUrls != null) {
            readyCallback(sAppUrls!!)
            return
        }

        val urlsFile = getUrlsFile()

        if (urlsFile.exists() && urlsFile.length() > 0) {
            try {
                val urlsData = urlsFile.readText()
                sAppUrls = JsonHelper.json.decodeFromString(urlsData)
                readyCallback(sAppUrls!!)
            } catch (e: Exception) {
                failedCallback?.invoke(e)
            }
        } else {
            if (!NetworkStateReceiver.canProceed(ctx)) {
                failedCallback?.invoke(IOException("No network connection to fetch URLs."))
                return
            }

            val failureListener = { e: Exception ->
                failedCallback?.invoke(e)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val urls = refresh()

                    withContext(Dispatchers.Main) {
                        if (mCancelled) {
                            mCancelled = false
                            failureListener(CancellationException("Canceled by the user."))
                            return@withContext
                        }

                        readyCallback(urls)
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
