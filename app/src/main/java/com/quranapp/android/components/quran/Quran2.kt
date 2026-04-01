package com.quranapp.android.components.quran

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


object Quran2 {
    suspend fun prepareInstance(context: Context): Quran {
        val quranMeta = QuranMeta2.prepareInstance(context)

        return suspendCancellableCoroutine { cont ->
            Quran.prepareInstance(context, quranMeta, object : OnResultReadyCallback<Quran> {
                override fun onReady(r: Quran) {
                    cont.resume(r)
                }
            })
        }
    }

    // composable
    @Composable
    fun rememberQuran(): Quran? {
        val context = LocalContext.current

        val quranState = produceState<Quran?>(initialValue = null, context) {
            value = prepareInstance(context)
        }

        return quranState.value
    }
}