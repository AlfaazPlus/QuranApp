package com.quranapp.android.utils.app

import android.content.Context
import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.AppUrls
import com.quranapp.android.utils.files.AppFiles
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UrlsManager(private val ctx: Context) {
    companion object {
        const val URL_KEY_FEEDBACK = "feedback"
        const val URL_KEY_PRIVACY_POLICY = "privacy-policy"
        const val URL_KEY_ABOUT = "about"
        const val URL_KEY_HELP = "help"
        const val URL_KEY_DISCORD = "discord"
        const val URL_KEY_DONATION = "donation"

        private const val DIR_NAME = "urls"
        private const val URLS_FILE_NAME = "urls.json"
        private var sAppUrls: AppUrls? = null
    }

    private var mCancelled = false

    private fun getUrlsFile(): File {
        return AppFiles.savedFile(DIR_NAME, URLS_FILE_NAME).toFile()
    }

    suspend fun refresh(): AppUrls = withContext(Dispatchers.IO) {
        try {
            val urls = ApiClient.getAppUrls()
            val urlsFile = getUrlsFile()
            if (AppFiles.createFile(urlsFile.toOkioPath())) {
                urlsFile.writeText(JsonHelper.json.encodeToString(urls))
                sAppUrls = urls
            }
            urls
        } catch (e: Exception) {
            Logger.saveError(e, "UrlsManager.refresh")
            throw e
        }
    }

    suspend fun load(): AppUrls = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        getUrlsJson(
            readyCallback = { urls ->
                if (cont.isActive) cont.resume(urls)
            },
            failedCallback = { error ->
                if (cont.isActive) cont.resumeWithException(error)
            },
        )
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
            val failureListener = { e: Exception ->
                failedCallback?.invoke(e)
            }

            CoroutineScope(Dispatchers.Main.immediate).launch {
                if (!NetworkStateReceiver.canProceed(ctx)) {
                    failedCallback?.invoke(IOException("No network connection to fetch URLs."))
                    return@launch
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
    }

    fun cancel() {
        mCancelled = true
    }
}
