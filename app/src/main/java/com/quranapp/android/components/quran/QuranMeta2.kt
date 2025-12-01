package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object QuranMeta2 {
    suspend fun prepareInstance(context: Context): QuranMeta {
        return suspendCoroutine { cont ->
            QuranMeta.prepareInstance(context, object : OnResultReadyCallback<QuranMeta> {
                override fun onReady(r: QuranMeta) {
                    cont.resume(r)
                }
            })
        }
    }
}