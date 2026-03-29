package com.quranapp.android.components.quran

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object QuranMeta2 {
    suspend fun prepareInstance(context: Context): QuranMeta {
        return suspendCancellableCoroutine { cont ->
            QuranMeta.prepareInstance(context, object : OnResultReadyCallback<QuranMeta> {
                override fun onReady(r: QuranMeta) {
                    cont.resume(r)
                }
            })
        }
    }

    // composable
    @Composable
    fun rememberQuranMeta(): QuranMeta? {
        val context = LocalContext.current

        val quranMetaState = produceState<QuranMeta?>(initialValue = null, context) {
            value = prepareInstance(context)
        }

        return quranMetaState.value
    }
}