package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import java.util.concurrent.atomic.AtomicReference

abstract class QuranMetaPossessingActivity : BaseActivity() {
    // Don't initialize QuranMeta as it is being null checked in getQuranMetaSafely.
    @JvmField
    val mQuranMetaRef = AtomicReference<QuranMeta?>(null)
    private val mPendingMetaRequesters: MutableSet<OnResultReadyCallback<QuranMeta?>> = HashSet()

    final override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        preQuranMetaPrepare(activityView, intent, savedInstanceState)
        QuranMeta.prepareInstance(this, object : OnResultReadyCallback<QuranMeta> {
            override fun onReady(r: QuranMeta) {
                mQuranMetaRef.set(r)
                onQuranMetaAvailable(r)
                onQuranMetaReady(activityView, intent, savedInstanceState, r)
            }
        })
    }

    protected open fun preQuranMetaPrepare(activityView: View, intent: Intent, savedInstanceState: Bundle?) {}
    protected abstract fun onQuranMetaReady(
        activityView: View,
        intent: Intent,
        savedInstanceState: Bundle?,
        quranMeta: QuranMeta
    )

    @Synchronized
    fun getQuranMetaSafely(readyCallback: OnResultReadyCallback<QuranMeta?>) {
        if (mQuranMetaRef.get() != null) {
            readyCallback.onReady(mQuranMetaRef.get())
        } else {
            mPendingMetaRequesters.add(readyCallback)
        }
    }

    private fun onQuranMetaAvailable(quranMeta: QuranMeta?) {
        for (requester in mPendingMetaRequesters) {
            requester.onReady(quranMeta)
        }
        mPendingMetaRequesters.clear()
    }

}