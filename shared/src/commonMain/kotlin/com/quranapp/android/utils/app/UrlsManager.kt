package com.quranapp.android.utils.app

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.AppUrls
import com.quranapp.android.extensions.canProceed
import com.quranapp.android.platform.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UrlsManager {
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

    private fun getUrlsFile(): Path {
        return AppFiles.savedFile(DIR_NAME, URLS_FILE_NAME)
    }

    suspend fun refresh(): AppUrls = withContext(Dispatchers.IO) {
        try {
            val urls = ApiClient.getAppUrls()
            val urlsFile = getUrlsFile()
            if (AppFiles.createFile(urlsFile)) {
                AppFiles.writeText(urlsFile, JsonHelper.json.encodeToString(urls))
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

        if (AppFiles.size(urlsFile) > 0) {
            try {
                val urlsData = AppFiles.readText(urlsFile)
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
                if (!Platform.networkMonitor.canProceed()) {
                    failedCallback?.invoke(IllegalStateException("No network connection to fetch URLs."))
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
