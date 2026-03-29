package com.quranapp.android.utils.reader.recitation.player

import android.widget.Toast
import androidx.core.net.toUri
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.services.RecitationService
import java.io.File

open class RecitationPlayerVerseLoadCallback(private val service: RecitationService?) {
    private var reciter: String? = null
    private var translReciter: String? = null
    private var chapterNo = 0
    private var verseNo = 0

    fun setCurrentVerse(slug: String?, translSlug: String?, chapterNo: Int, verseNo: Int) {
        this.reciter = slug
        this.translReciter = translSlug
        this.chapterNo = chapterNo
        this.verseNo = verseNo
    }

    fun preLoad() {
        service?.let {
            it.recPlayer?.reveal()
            it.setupOnLoadingInProgress(true)
        }
    }

    fun onProgress(progress: String) {
        if ((reciter == null && translReciter == null) || chapterNo == -1 || verseNo == -1) {
            return
        }

        service?.recPlayer?.binding?.progressText?.text = progress
    }

    open fun onLoaded(verseFile: File?, verseTranslFile: File?) {
        if ((reciter == null && translReciter == null) || chapterNo == -1 || verseNo == -1) {
            return
        }

        Log.d("Verse loaded! - $chapterNo:$verseNo")

        service?.prepareMediaPlayer(
            verseFile?.toUri(),
            verseTranslFile?.toUri(),
            reciter,
            translReciter,
            chapterNo,
            verseNo
        )
    }

    open fun onFailed(
        e: Throwable?,
        verseFile: File?,
        verseTranslFile: File?,
        deleteVerseFile: Boolean,
        deleteTranslFile: Boolean
    ) {
        if (deleteVerseFile) verseFile?.delete()
        if (deleteTranslFile) verseTranslFile?.delete()

        if (e is HttpNotFoundException || e?.cause is HttpNotFoundException) {
            // Audio was unable to load from the url because url was not found,
            // may be recitation manifest has a new url. So force update the manifest file.
            service?.forceManifestFetch = true
            Log.saveError(e, "RecitationPlayerVerseLoadCallback.onFailed")
        }

        service?.updatePlayerProgress()

        service?.recPlayer?.let {

            if (!it.readerChanging) {
                service.popMiniMsg("Something happened wrong while loading the verse.", Toast.LENGTH_LONG)
            }
            it.readerChanging = false
        }

        Log.d("Verse failed to load! - $chapterNo:$verseNo")
    }

    fun postLoad() {
        service?.setupOnLoadingInProgress(false)
    }
}